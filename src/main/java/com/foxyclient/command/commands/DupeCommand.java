package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * DupeCommand - Automates various duplication exploits.
 *
 * Usage:
 * .dupe [mode] [delay]
 *
 * Modes:
 * - container (default): Performs container desync (Chest/Shulker/etc)
 * - frame: Item Frame duplication attempt
 */
public class DupeCommand extends Command {

    private enum Mode { CONTAINER, FRAME }
    private enum State { IDLE, WAITING_FOR_OPEN, EXECUTING, CLEANUP }

    private Mode currentMode = Mode.CONTAINER;
    private State currentState = State.IDLE;
    private int waitTicks = 0;
    private int delayTicks = 2; // Default delay
    private ItemStack targetItem = ItemStack.EMPTY;
    private int initialSelectedSlot = -1;

    public DupeCommand() {
        super("dupe", "Automates duplication exploits.", "dupe [container/frame] [delay]");
    }

    @Override
    public void execute(String[] args) {
        if (mc.player == null || mc.world == null) {
            error("You must be in a world to use this command.");
            return;
        }

        if (currentState != State.IDLE) {
            error("A dupe sequence is already in progress!");
            return;
        }

        // Parse arguments
        currentMode = Mode.CONTAINER;
        delayTicks = 2;

        if (args.length >= 1) {
            try {
                currentMode = Mode.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                // If not a mode, maybe it's a delay?
                try {
                    delayTicks = Integer.parseInt(args[0]);
                } catch (NumberFormatException e2) {
                    error("Invalid mode or delay: " + args[0]);
                    return;
                }
            }
        }

        if (args.length >= 2) {
            try {
                delayTicks = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                error("Invalid delay: " + args[1]);
                return;
            }
        }

        targetItem = mc.player.getMainHandStack();
        if (targetItem.isEmpty() && currentMode == Mode.CONTAINER) {
            error("You must hold an item to duplicate (for container dupe).");
            return;
        }

        initialSelectedSlot = mc.player.getInventory().selectedSlot;

        switch (currentMode) {
            case CONTAINER -> startContainerDupe();
            case FRAME -> startFrameDupe();
        }
    }

    private void startContainerDupe() {
        // Find a nearby container (within ~4.5 blocks)
        BlockPos targetContainer = null;
        int searchRadius = 4;
        BlockPos playerPos = mc.player.getBlockPos();

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    BlockEntity be = mc.world.getBlockEntity(pos);
                    if (be instanceof LootableContainerBlockEntity) {
                        targetContainer = pos;
                        break;
                    }
                }
                if (targetContainer != null) break;
            }
            if (targetContainer != null) break;
        }

        if (targetContainer == null) {
            error("No valid container found nearby. Stand near a Chest, Barrel, or Shulker Box.");
            return;
        }

        currentState = State.WAITING_FOR_OPEN;
        waitTicks = 0;
        FoxyClient.INSTANCE.getEventBus().register(this);
        
        info("§e[Dupe] §fOpening container...");

        BlockState state = mc.world.getBlockState(targetContainer);
        Vec3d hitVec = new Vec3d(targetContainer.getX() + 0.5, targetContainer.getY() + 0.5, targetContainer.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, targetContainer, false);
        
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
    }

    private void startFrameDupe() {
        // Find nearby Item Frame
        ItemFrameEntity targetFrame = null;
        List<ItemFrameEntity> frames = mc.world.getEntitiesByClass(ItemFrameEntity.class, 
            mc.player.getBoundingBox().expand(4.0), entity -> true);
        
        if (frames.isEmpty()) {
            error("No Item Frame found nearby.");
            return;
        }
        
        targetFrame = frames.get(0);
        
        info("§e[Dupe] §fAttempting Item Frame dupe...");
        
        // Interaction sequence for frame dupe (generic attempt)
        mc.interactionManager.interactEntity(mc.player, targetFrame, Hand.MAIN_HAND);
        mc.interactionManager.attackEntity(mc.player, targetFrame);
        
        info("§a[Dupe] §fFrame dupe attempt complete!");
        // No tick logic for frame dupe as it's usually instant/packet-based
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (currentState == State.IDLE) return;

        if (mc.player == null || mc.world == null) {
            abort("Disconnected from world.");
            return;
        }

        waitTicks++;

        if (currentState == State.WAITING_FOR_OPEN) {
            boolean isContainerOpen = mc.currentScreen != null &&
                    (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler ||
                     mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler);

            if (isContainerOpen) {
                currentState = State.EXECUTING;
                waitTicks = 0;
            } else if (waitTicks > 60) {
                abort("Timed out waiting for container.");
            }
            return;
        }

        if (currentState == State.EXECUTING) {
            if (waitTicks < delayTicks) return;
            executeContainerExploit();
        }
    }

    private void executeContainerExploit() {
        int targetSlotId = -1;
        int containerSize = mc.player.currentScreenHandler.slots.size() - 36;

        // Find the slot containing our target item
        for (int i = containerSize; i < mc.player.currentScreenHandler.slots.size(); i++) {
            Slot slot = mc.player.currentScreenHandler.slots.get(i);
            if (slot.getStack().isOf(targetItem.getItem()) && slot.getStack().getCount() == targetItem.getCount()) {
                targetSlotId = i;
                break;
            }
        }

        if (targetSlotId == -1) {
            abort("Target item not found in inventory.");
            return;
        }

        // Find an empty slot in the container
        int emptyContainerSlot = -1;
        for (int i = 0; i < containerSize; i++) {
            if (!mc.player.currentScreenHandler.slots.get(i).hasStack()) {
                emptyContainerSlot = i;
                break;
            }
        }

        if (emptyContainerSlot == -1) {
            abort("Container is full.");
            return;
        }

        int syncId = mc.player.currentScreenHandler.syncId;

        // Sequence: Pick -> Place -> Force Close -> Rapid Retrieve
        mc.interactionManager.clickSlot(syncId, targetSlotId, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, emptyContainerSlot, 0, SlotActionType.PICKUP, mc.player);
        
        // Send packet directly to server to force closure BEFORE client closes
        mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(syncId));
        
        // Final click to retrieve (desync attempt)
        mc.interactionManager.clickSlot(syncId, emptyContainerSlot, 0, SlotActionType.QUICK_MOVE, mc.player);

        finish("Dupe sequence complete!");
    }

    private void finish(String message) {
        info("§a[Dupe] §f" + message);
        currentState = State.IDLE;
        FoxyClient.INSTANCE.getEventBus().unregister(this);
        if (mc.player != null && mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
            mc.player.closeHandledScreen();
        }
    }

    private void abort(String message) {
        error("§c[Dupe] §f" + message);
        currentState = State.IDLE;
        FoxyClient.INSTANCE.getEventBus().unregister(this);
        if (mc.player != null && mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
            mc.player.closeHandledScreen();
        }
    }

    @Override
    public List<String> getSuggestions(String[] args) {
        if (args.length == 1) {
            return List.of("container", "frame");
        }
        return super.getSuggestions(args);
    }
}
