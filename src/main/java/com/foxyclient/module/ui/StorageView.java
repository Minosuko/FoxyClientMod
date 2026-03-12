package com.foxyclient.module.ui;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.Render2DEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.block.entity.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import com.foxyclient.event.events.PacketEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shows the contents of storage blocks (Chest, Shulker Box, Barrel, Ender Chest)
 * when looking at them. Renders an item grid overlay on the HUD.
 */
public class StorageView extends Module {
    private final ModeSetting displayMode = addSetting(new ModeSetting("Display", "Where to show items", "Crosshair", "Crosshair", "Top", "Left", "Right"));
    private final NumberSetting opacity = addSetting(new NumberSetting("Opacity", "Overlay opacity (%)", 100, 10, 100));
    private final NumberSetting scale = addSetting(new NumberSetting("Scale", "Item render scale", 1.0, 0.5, 2.0));
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Detection range (blocks)", 5.0, 3.0, 6.0));
    private final NumberSetting dwell = addSetting(new NumberSetting("Dwell", "Delay before peeking (ticks)", 6, 0, 20));
    private final BoolSetting background = addSetting(new BoolSetting("Background", "Draw background panel", true));
    private final BoolSetting showName = addSetting(new BoolSetting("Show Name", "Show storage block name", true));

    // Cached data for current target
    private List<ItemStack> cachedItems = null;
    private String cachedName = null;
    private int cachedRows = 0;
    private BlockPos cachedPos = null;
    private Direction cachedSide = null;

    // Peeking state
    private BlockPos peekingPos = null;
    private int peekingSyncId = -1;
    private int peekingTimeout = 0;
    
    // Silence window for lid animations
    private BlockPos silentTargetPos = null;
    private int silentTimeout = 0;
    
    // Cached render data to avoid flicker
    private BlockPos lastHoveredPos = null;
    private int currentDwellTicks = 0;

    // Persistent cache for Ender Chest (survives rejoins)
    private static final List<ItemStack> ENDER_CHEST_CACHE = new ArrayList<>();

    // Position-based cache for world containers (chests, barrels, etc)
    private static final Map<BlockPos, StorageInfo> WORLD_STORAGE_CACHE = new HashMap<>();

    // Keep track of the last window opened to map packets to block positions
    private BlockPos lastOpenedPos = null;

    // Map of currently open screens to their block positions
    private final Map<Integer, BlockPos> openScreens = new HashMap<>();

    // Slot rendering constants
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_PADDING = 1;
    private static final int GRID_COLS = 9;
    private static final int PANEL_PADDING = 7;
    private static final int TITLE_HEIGHT = 12;

    public StorageView() {
        super("StorageView", "Shows storage block contents when looking at them", Category.UI);
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (nullCheck()) return;

        // Detect storage block via crosshair target
        BlockPos targetPos = null;
        Direction hitSide = null;
        boolean foundStorage = false;

        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) mc.crosshairTarget;
            targetPos = blockHit.getBlockPos();
            hitSide = blockHit.getSide();

            // Check range
            Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            double dist = playerPos.distanceTo(Vec3d.ofCenter(targetPos));
            if (dist <= range.get()) {
                BlockEntity blockEntity = mc.world.getBlockEntity(targetPos);
                if (blockEntity != null) {
                    // Handle double chest position normalization
                    BlockPos storagePos = normalizePos(blockEntity, targetPos);
                    
                    StorageInfo info = getStorageInfo(blockEntity, storagePos);
                    if (info != null) {
                        cachedItems = info.items;
                        cachedName = info.name;
                        cachedRows = info.rows;
                        cachedPos = targetPos; // Render it on the actual block looked at
                        cachedSide = hitSide;
                        foundStorage = true;

                        // Targeted Automated Peek: Only peek if looking at the same block for 'dwell' ticks
                        long now = System.currentTimeMillis();
                        if (isPeekable(blockEntity) && !info.synced() && peekingPos == null && (now - info.lastPeekTime()) > 5000) {
                            if (storagePos.equals(lastHoveredPos)) {
                                currentDwellTicks++;
                                if (currentDwellTicks >= dwell.get().intValue()) {
                                    startPeek(targetPos, hitSide);
                                    currentDwellTicks = 0;
                                }
                            } else {
                                lastHoveredPos = storagePos;
                                currentDwellTicks = 0;
                            }
                        } else if (peekingPos != null) {
                            // Reset dwell if we're already peeking at something
                            currentDwellTicks = 0;
                        }
                    }
                }
            }
        }

        // Handle peeking timeout
        if (peekingPos != null && --peekingTimeout <= 0) {
            peekingPos = null;
        }

        if (silentTargetPos != null && --silentTimeout <= 0) {
            silentTargetPos = null;
        }

        // Reset dwell if not looking at a peekable storage
        if (!foundStorage) {
            lastHoveredPos = null;
            currentDwellTicks = 0;
            cachedItems = null;
            cachedName = null;
            cachedRows = 0;
            cachedPos = null;
            cachedSide = null;
            return;
        }

        if (cachedItems == null || cachedItems.isEmpty()) return;

        // Position-based mapping for packets: if we're looking at a storage, that's likely the one we'll open
        lastOpenedPos = targetPos;

        // Render the item grid at absolute screen coordinates
        renderStorageOverlay(event, cachedItems, cachedName, cachedRows, cachedPos, cachedSide);
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck()) return;

        // OpenScreenS2CPacket: Intercept server-side GUI openings
        if (event.getPacket() instanceof OpenScreenS2CPacket packet) {
            if (peekingPos != null) {
                peekingSyncId = packet.getSyncId();
                event.cancel(); // Prevent the GUI from opening on screen
            } else if (lastOpenedPos != null) {
                // Manual open: register the screen mapping
                openScreens.put(packet.getSyncId(), lastOpenedPos);
            }
        }

        // Cleanup on server-side close
        if (event.getPacket() instanceof CloseScreenS2CPacket packet) {
            openScreens.remove(packet.getSyncId());
        }

        // InventoryS2CPacket: Full inventory update (usually when opening a screen)
        if (event.getPacket() instanceof InventoryS2CPacket packet) {
            int syncId = packet.syncId();
            BlockPos activePos = peekingPos != null ? peekingPos : openScreens.getOrDefault(syncId, lastOpenedPos);

            if (syncId > 0 && activePos != null) {
                // Extract only container slots (ignore player inventory which is usually 36 slots)
                List<ItemStack> contents = packet.contents();
                int containerSize = Math.max(0, contents.size() - 36); // Dynamically support any size container
                
                List<ItemStack> items = new ArrayList<>();
                for (int i = 0; i < Math.min(containerSize, contents.size()); i++) {
                    items.add(contents.get(i).copy());
                }
                
                int rows = containerSize / GRID_COLS;
                String name = "Container";
                
                if (WORLD_STORAGE_CACHE.containsKey(activePos)) {
                    name = WORLD_STORAGE_CACHE.get(activePos).name();
                }

                WORLD_STORAGE_CACHE.put(activePos, new StorageInfo(items, name, rows, true, System.currentTimeMillis()));

                // If this was an automated peek, close the screen silently
                if (peekingPos != null && syncId == peekingSyncId) {
                    // Specific sync for Ender Chest persistent cache
                    if (mc.world.getBlockEntity(peekingPos) instanceof EnderChestBlockEntity) {
                        ENDER_CHEST_CACHE.clear();
                        ENDER_CHEST_CACHE.addAll(items.stream().map(ItemStack::copy).toList());
                    }
                    
                    mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(syncId));
                    peekingPos = null;
                    peekingSyncId = -1;
                }
            }
        }
        
        // ScreenHandlerSlotUpdateS2CPacket: Individual slot update
        if (event.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket packet) {
            int syncId = packet.getSyncId();
            BlockPos activePos = openScreens.getOrDefault(syncId, lastOpenedPos);
            
            if (syncId > 0 && activePos != null) {
                StorageInfo existing = WORLD_STORAGE_CACHE.get(activePos);
                if (existing != null && packet.getSlot() < existing.items().size()) {
                    existing.items().set(packet.getSlot(), packet.getStack().copy());
                }
            }
        }

        // BlockUpdateS2CPacket / BlockEntityUpdateS2CPacket: Invalidate cache for instant updates
        if (event.getPacket() instanceof BlockUpdateS2CPacket packet) {
            invalidateCache(packet.getPos());
        } else if (event.getPacket() instanceof BlockEntityUpdateS2CPacket packet) {
            invalidateCache(packet.getPos());
        }

        // BlockEventS2CPacket: Prevent lid animations for chests/shulkers
        if (event.getPacket() instanceof BlockEventS2CPacket packet) {
            if (silentTargetPos != null && packet.getPos().equals(silentTargetPos)) {
                // Type 1 is for chest/shulker animations. Block them during the silence window.
                if (packet.getType() == 1) {
                    event.cancel();
                }
            }
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (nullCheck()) return;

        // Cleanup on client-side close
        if (event.getPacket() instanceof CloseHandledScreenC2SPacket packet) {
            openScreens.remove(packet.getSyncId());
        }
    }

    private void invalidateCache(BlockPos pos) {
        // If it's a double chest, invalidate both halves
        BlockPos norm = pos;
        BlockEntity be = mc.world.getBlockEntity(pos);
        if (be != null) norm = normalizePos(be, pos);
        
        StorageInfo info = WORLD_STORAGE_CACHE.get(norm);
        if (info != null) {
            // Keep the lastPeekTime so we don't immediately re-peek if it just happened
            WORLD_STORAGE_CACHE.put(norm, new StorageInfo(info.items(), info.name(), info.rows(), false, info.lastPeekTime()));
        }
    }

    private void startPeek(BlockPos pos, Direction side) {
        if (mc.player == null) return;
        peekingPos = pos;
        peekingTimeout = 30; // 1.5 seconds
        
        // Start lid silence window
        silentTargetPos = pos;
        silentTimeout = 30;
        
        // Update lastPeekTime in cache to start cooldown even if it fails
        BlockPos norm = normalizePos(mc.world.getBlockEntity(pos), pos);
        StorageInfo info = WORLD_STORAGE_CACHE.get(norm);
        if (info != null) {
            WORLD_STORAGE_CACHE.put(norm, new StorageInfo(info.items(), info.name(), info.rows(), false, System.currentTimeMillis()));
        }
        
        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, side, pos, false);
        mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));
    }

    private boolean isPeekable(BlockEntity be) {
        return be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof ShulkerBoxBlockEntity || be instanceof EnderChestBlockEntity;
    }

    private boolean hasData(StorageInfo info) {
        return info.synced();
    }

    private BlockPos normalizePos(BlockEntity be, BlockPos pos) {
        if (be instanceof ChestBlockEntity) {
            net.minecraft.block.BlockState state = mc.world.getBlockState(pos);
            if (state.contains(net.minecraft.block.ChestBlock.CHEST_TYPE)) {
                net.minecraft.block.enums.ChestType type = state.get(net.minecraft.block.ChestBlock.CHEST_TYPE);
                if (type != net.minecraft.block.enums.ChestType.SINGLE) {
                    BlockPos neighborPos = pos.offset(net.minecraft.block.ChestBlock.getFacing(state));
                    // Double chest normalization: use the block with the lower coordinates as the cache key
                    if (neighborPos.getX() < pos.getX() || neighborPos.getY() < pos.getY() || neighborPos.getZ() < pos.getZ()) {
                        return neighborPos;
                    }
                }
            }
        }
        return pos;
    }

    /**
     * Extracts inventory items and metadata from a storage block entity.
     */
    private StorageInfo getStorageInfo(BlockEntity blockEntity, BlockPos pos) {
        // Try position-based cache first for non-shulkers/ender chests
        StorageInfo cached = WORLD_STORAGE_CACHE.get(pos);
        
        List<ItemStack> items = new ArrayList<>();
        String name = cached != null ? cached.name() : "Container";
        int rows = cached != null ? cached.rows() : 3;
        boolean forceUpdate = false;

        if (blockEntity instanceof ChestBlockEntity) {
            // Only update from block entity if it has items (server sync)
            net.minecraft.block.BlockState state = mc.world.getBlockState(pos);
            if (state.getBlock() instanceof net.minecraft.block.ChestBlock chestBlock) {
                try {
                    Inventory inv = net.minecraft.block.ChestBlock.getInventory(
                        chestBlock, state, mc.world, pos, true
                    );
                    if (inv != null) {
                        boolean hasItems = false;
                        List<ItemStack> blockItems = new ArrayList<>();
                        for (int i = 0; i < inv.size(); i++) {
                            ItemStack stack = inv.getStack(i);
                            if (!stack.isEmpty()) hasItems = true;
                            blockItems.add(stack.copy());
                        }
                        if (hasItems) {
                            items = blockItems;
                            name = "Chest";
                            rows = Math.max(1, inv.size() / GRID_COLS);
                            forceUpdate = true;
                        }
                    }
                } catch (Exception ignored) {}
            }
        } else if (blockEntity instanceof ShulkerBoxBlockEntity shulker) {
            // In 1.21, we can read shulker contents via components if they are synced
            ContainerComponent container = shulker.getComponents().get(DataComponentTypes.CONTAINER);
            if (container != null) {
                // Shulker boxes are always 27 slots
                DefaultedList<ItemStack> defaultedItems = DefaultedList.ofSize(27, ItemStack.EMPTY);
                container.copyTo(defaultedItems);
                for (ItemStack stack : defaultedItems) {
                    items.add(stack.copy());
                }
                forceUpdate = true; // Use component data as master source
            } else {
                for (int i = 0; i < shulker.size(); i++) {
                    items.add(shulker.getStack(i).copy());
                }
            }
            name = "Shulker Box";
            rows = 3;
        } else if (blockEntity instanceof BarrelBlockEntity barrel) {
            for (int i = 0; i < barrel.size(); i++) {
                items.add(barrel.getStack(i).copy());
            }
            name = "Barrel";
            rows = 3;
        } else if (blockEntity instanceof EnderChestBlockEntity) {
            Inventory enderInv = mc.player.getEnderChestInventory();
            if (enderInv != null) {
                boolean hasAny = false;
                for (int i = 0; i < enderInv.size(); i++) {
                    if (!enderInv.getStack(i).isEmpty()) {
                        hasAny = true;
                        break;
                    }
                }

                if (hasAny) {
                    ENDER_CHEST_CACHE.clear();
                    for (int i = 0; i < enderInv.size(); i++) {
                        ENDER_CHEST_CACHE.add(enderInv.getStack(i).copy());
                    }
                }
            }

            if (!ENDER_CHEST_CACHE.isEmpty()) {
                items.addAll(ENDER_CHEST_CACHE.stream().map(ItemStack::copy).toList());
            } else {
                // Provide empty slots if no data yet (user wants "include enderchest empty")
                for (int i = 0; i < 27; i++) items.add(ItemStack.EMPTY);
            }
            name = "Ender Chest";
            rows = 3;
        }
        
        // Ensure ender chest is marked synced if we have cache
        boolean isSynced = forceUpdate || (blockEntity instanceof EnderChestBlockEntity && !ENDER_CHEST_CACHE.isEmpty());

        StorageInfo info = forceUpdate ? 
            new StorageInfo(items, name, rows, true, System.currentTimeMillis()) : 
            (cached != null ? cached : new StorageInfo(items, name, rows, isSynced, 0L));
        
        WORLD_STORAGE_CACHE.put(pos, info);
        return info;
    }

    /**
     * Renders the item grid overlay at absolute screen coordinates.
     * Uses the same proven pattern as ArmorHUD and TargetInfoHUD.
     */
    private void renderStorageOverlay(Render2DEvent event, List<ItemStack> items, String storageName, int rows, BlockPos blockPos, Direction side) {
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        float s = scale.get().floatValue();

        int cols = GRID_COLS;
        int panelWidth = cols * SLOT_SIZE + PANEL_PADDING * 2;
        int panelHeight = rows * SLOT_SIZE + PANEL_PADDING * 2 + (showName.get() ? TITLE_HEIGHT : 0);

        // Calculate position based on display mode
        float baseX, baseY;
        String mode = displayMode.get();
        switch (mode) {
            case "Top" -> {
                baseX = (screenWidth / 2f - (panelWidth * s) / 2f) / s;
                baseY = 10 / s;
            }
            case "Left" -> {
                baseX = 10 / s;
                baseY = (screenHeight / 2f - (panelHeight * s) / 2f) / s;
            }
            case "Right" -> {
                baseX = (screenWidth - (panelWidth * s) - 10) / s;
                baseY = (screenHeight / 2f - (panelHeight * s) / 2f) / s;
            }
            default -> { // Crosshair
                baseX = (screenWidth / 2f + 15) / s;
                baseY = (screenHeight / 2f - (panelHeight * s) / 2f) / s;
            }
        }

        int alpha = (int) (opacity.get().floatValue() / 100f * 255f);
        if (alpha <= 0) return;

        event.getContext().getMatrices().pushMatrix();
        event.getContext().getMatrices().scale(s, s);

        int drawX = (int) baseX;
        int drawY = (int) baseY;

        // Draw background panel
        if (background.get()) {
            int bgColor = (alpha * 180 / 255) << 24;
            event.getContext().fill(drawX, drawY, drawX + panelWidth, drawY + panelHeight, bgColor);

            // Border
            int borderColor = (alpha << 24) | 0x404040;
            event.getContext().fill(drawX, drawY, drawX + panelWidth, drawY + 1, borderColor);
            event.getContext().fill(drawX, drawY + panelHeight - 1, drawX + panelWidth, drawY + panelHeight, borderColor);
            event.getContext().fill(drawX, drawY, drawX + 1, drawY + panelHeight, borderColor);
            event.getContext().fill(drawX + panelWidth - 1, drawY, drawX + panelWidth, drawY + panelHeight, borderColor);
        }

        // Draw title
        int contentOffsetY = PANEL_PADDING;
        if (showName.get() && storageName != null) {
            int textColor = (alpha << 24) | 0xFFFFFF;
            event.getContext().drawTextWithShadow(mc.textRenderer, storageName, drawX + PANEL_PADDING, drawY + PANEL_PADDING, textColor);
            contentOffsetY += TITLE_HEIGHT;
        }

        // Draw items
        for (int i = 0; i < items.size() && i < rows * cols; i++) {
            int col = i % cols;
            int row = i / cols;

            // Calculate unscaled internal slot positions
            int slotX = drawX + PANEL_PADDING + col * SLOT_SIZE;
            int slotY = drawY + contentOffsetY + row * SLOT_SIZE;
            int slotEndX = slotX + SLOT_SIZE - SLOT_PADDING * 2;
            int slotEndY = slotY + SLOT_SIZE - SLOT_PADDING * 2;

            // Draw slot background
            if (background.get()) {
                int slotBg = (alpha * 100 / 255) << 24 | 0x1a1a1a;
                event.getContext().fill(
                    slotX + SLOT_PADDING,
                    slotY + SLOT_PADDING,
                    slotEndX,
                    slotEndY,
                    slotBg
                );
            }

            // Draw item
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                int itemX = slotX + SLOT_PADDING;
                int itemY = slotY + SLOT_PADDING;
                event.getContext().drawItem(stack, itemX, itemY);
                event.getContext().drawStackOverlay(mc.textRenderer, stack, itemX, itemY);
            }
        }

        event.getContext().getMatrices().popMatrix();
    }

    @Override
    public void onDisable() {
        cachedItems = null;
        cachedName = null;
        cachedRows = 0;
        cachedPos = null;
        cachedSide = null;
        openScreens.clear();
    }

    private record StorageInfo(List<ItemStack> items, String name, int rows, boolean synced, long lastPeekTime) {}
}
