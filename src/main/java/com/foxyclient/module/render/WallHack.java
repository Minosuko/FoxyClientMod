package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import java.util.HashSet;
import java.util.Set;

/**
 * X-Ray / WallHack - Makes most blocks transparent to see ores.
 */
public class WallHack extends Module {
    private final BoolSetting diamonds = addSetting(new BoolSetting("Diamonds", "Show diamonds", true));
    private final BoolSetting emeralds = addSetting(new BoolSetting("Emeralds", "Show emeralds", true));
    private final BoolSetting gold = addSetting(new BoolSetting("Gold", "Show gold", true));
    private final BoolSetting iron = addSetting(new BoolSetting("Iron", "Show iron", true));
    private final BoolSetting coal = addSetting(new BoolSetting("Coal", "Show coal", false));
    private final BoolSetting lapis = addSetting(new BoolSetting("Lapis", "Show lapis", true));
    private final BoolSetting redstone = addSetting(new BoolSetting("Redstone", "Show redstone", true));
    private final BoolSetting netherite = addSetting(new BoolSetting("Netherite", "Show ancient debris", true));
    private final BoolSetting chests = addSetting(new BoolSetting("Chests", "Show chests", true));
    private final BoolSetting spawners = addSetting(new BoolSetting("Spawners", "Show spawners", true));

    private final Set<Block> visibleBlocks = new HashSet<>();

    public WallHack() {
        super("WallHack", "X-Ray vision for ores", Category.RENDER);
    }

    @Override
    public void onEnable() {
        updateVisibleBlocks();
        if (!nullCheck()) mc.worldRenderer.reload();
    }

    @Override
    public void onDisable() {
        if (!nullCheck()) mc.worldRenderer.reload();
    }

    public void updateVisibleBlocks() {
        visibleBlocks.clear();
        if (diamonds.get()) { visibleBlocks.add(Blocks.DIAMOND_ORE); visibleBlocks.add(Blocks.DEEPSLATE_DIAMOND_ORE); }
        if (emeralds.get()) { visibleBlocks.add(Blocks.EMERALD_ORE); visibleBlocks.add(Blocks.DEEPSLATE_EMERALD_ORE); }
        if (gold.get()) { visibleBlocks.add(Blocks.GOLD_ORE); visibleBlocks.add(Blocks.DEEPSLATE_GOLD_ORE); visibleBlocks.add(Blocks.NETHER_GOLD_ORE); }
        if (iron.get()) { visibleBlocks.add(Blocks.IRON_ORE); visibleBlocks.add(Blocks.DEEPSLATE_IRON_ORE); }
        if (coal.get()) { visibleBlocks.add(Blocks.COAL_ORE); visibleBlocks.add(Blocks.DEEPSLATE_COAL_ORE); }
        if (lapis.get()) { visibleBlocks.add(Blocks.LAPIS_ORE); visibleBlocks.add(Blocks.DEEPSLATE_LAPIS_ORE); }
        if (redstone.get()) { visibleBlocks.add(Blocks.REDSTONE_ORE); visibleBlocks.add(Blocks.DEEPSLATE_REDSTONE_ORE); }
        if (netherite.get()) visibleBlocks.add(Blocks.ANCIENT_DEBRIS);
        if (chests.get()) { visibleBlocks.add(Blocks.CHEST); visibleBlocks.add(Blocks.ENDER_CHEST); visibleBlocks.add(Blocks.TRAPPED_CHEST); }
        if (spawners.get()) visibleBlocks.add(Blocks.SPAWNER);

        // Always show structural blocks
        visibleBlocks.add(Blocks.LAVA);
        visibleBlocks.add(Blocks.WATER);
        visibleBlocks.add(Blocks.BEDROCK);
    }

    public boolean shouldRender(Block block) {
        if (!isEnabled()) return true;
        return visibleBlocks.contains(block);
    }
}
