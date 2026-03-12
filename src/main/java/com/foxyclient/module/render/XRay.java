package com.foxyclient.module.render;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BlockListSetting;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;

import java.util.HashSet;
import java.util.Set;

/**
 * XRay — Renders only selected blocks, making the rest of the world transparent
 * or semi-transparent. Uses registry-ID pattern matching for ore detection.
 * Custom blocks can be managed via the block selector GUI.
 */
public class XRay extends Module {

    // ── Settings ────────────────────────────────────────────────────────

    public final BoolSetting showOres = addSetting(new BoolSetting("Ores", "Show ores and raw ore blocks", true));
    public final BoolSetting showLiquids = addSetting(new BoolSetting("Liquids", "Show water and lava", false));
    public final BoolSetting showContainers = addSetting(new BoolSetting("Containers", "Show chests, barrels, shulkers, etc.", true));
    public final BoolSetting showMobs = addSetting(new BoolSetting("Mobs", "Show living entities through walls", true));

    public final BlockListSetting customBlocks = addSetting(
        new BlockListSetting("Custom Blocks", "Additional blocks to always render")
    );

    public final NumberSetting opacity = addSetting(
        new NumberSetting("Opacity", "Opacity of hidden blocks (0 = invisible)", 0.0, 0.0, 1.0)
    );

    // ── Pre-built block sets ────────────────────────────────────────────

    private final Set<Block> oreBlocks = new HashSet<>();
    private final Set<Block> containerBlocks = new HashSet<>();

    // ── Constructor ─────────────────────────────────────────────────────

    public XRay() {
        super("XRay", "See through blocks to find valuables", Category.RENDER);
        buildBlockSets();
        wireReloadCallbacks();
    }

    // ── Lifecycle ───────────────────────────────────────────────────────

    @Override
    public void onEnable() {
        mc.worldRenderer.reload();
    }

    @Override
    public void onDisable() {
        mc.worldRenderer.reload();
    }

    // ── Public API (called by mixins) ───────────────────────────────────

    /**
     * Returns {@code true} if the given block should be rendered.
     */
    public boolean shouldRender(Block block) {
        if (!isEnabled()) return true;

        if (showOres.get() && oreBlocks.contains(block)) return true;
        if (showLiquids.get() && isLiquid(block)) return true;
        if (showContainers.get() && containerBlocks.contains(block)) return true;
        if (customBlocks.contains(block)) return true;

        return false;
    }

    // ── Internals ───────────────────────────────────────────────────────

    /**
     * Build the ore and container block sets by scanning the registry.
     */
    private void buildBlockSets() {
        oreBlocks.clear();
        containerBlocks.clear();

        for (Block block : Registries.BLOCK) {
            String path = Registries.BLOCK.getId(block).getPath();

            // Ores: *_ore, raw_*_block, ancient_debris, spawner
            if (path.endsWith("_ore")
                    || (path.startsWith("raw_") && path.endsWith("_block"))
                    || path.equals("ancient_debris")) {
                oreBlocks.add(block);
            }

            // Containers
            if (path.equals("chest") || path.equals("trapped_chest") || path.equals("ender_chest")
                    || path.equals("barrel") || path.equals("hopper")
                    || path.equals("dropper") || path.equals("dispenser")
                    || path.contains("shulker_box")) {
                containerBlocks.add(block);
            }
        }

        oreBlocks.add(Blocks.SPAWNER);
    }

    /**
     * Wire onChanged callbacks so toggling any setting auto-reloads chunks.
     */
    private void wireReloadCallbacks() {
        Runnable reload = () -> {
            if (isEnabled() && mc.worldRenderer != null) {
                mc.worldRenderer.reload();
            }
        };

        showOres.setOnChanged(v -> reload.run());
        showLiquids.setOnChanged(v -> reload.run());
        showContainers.setOnChanged(v -> reload.run());
        showMobs.setOnChanged(v -> reload.run());
        opacity.setOnChanged(v -> reload.run());
        customBlocks.setOnListChanged(reload);
    }

    private static boolean isLiquid(Block block) {
        return block == Blocks.WATER || block == Blocks.LAVA;
    }
}
