package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.awt.Color;
import java.util.*;

/**
 * OreFinder - Scans for ores and renders ESP highlights.
 * Each ore type has its own show toggle; all enabled ores are scanned.
 */
public class OreFinder extends Module {
    // Settings
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Scan range", 32, 8, 64));
    private final NumberSetting updateDelay = addSetting(new NumberSetting("UpdateDelay", "Scan interval (ticks)", 40, 5, 200));

    private final BoolSetting tracers = addSetting(new BoolSetting("Tracers", "Draw tracer lines", true));

    // Ore show toggles
    private final BoolSetting showDiamond = addSetting(new BoolSetting("Diamond", "Show diamond ore", true));
    private final BoolSetting showDebris = addSetting(new BoolSetting("AncientDebris", "Show ancient debris", true));
    private final BoolSetting showEmerald = addSetting(new BoolSetting("Emerald", "Show emerald ore", true));
    private final BoolSetting showGold = addSetting(new BoolSetting("Gold", "Show gold ore", true));
    private final BoolSetting showIron = addSetting(new BoolSetting("Iron", "Show iron ore", false));
    private final BoolSetting showLapis = addSetting(new BoolSetting("Lapis", "Show lapis ore", true));
    private final BoolSetting showRedstone = addSetting(new BoolSetting("Redstone", "Show redstone ore", false));
    private final BoolSetting showCopper = addSetting(new BoolSetting("Copper", "Show copper ore", false));
    private final BoolSetting showCoal = addSetting(new BoolSetting("Coal", "Show coal ore", false));

    // State
    private final List<OreEntry> foundOres = new ArrayList<>();
    private int tickCounter = 0;

    // Ore color map
    private static final Map<Block, Color> ORE_COLORS = new HashMap<>();

    static {
        ORE_COLORS.put(Blocks.DIAMOND_ORE, new Color(0, 255, 255));
        ORE_COLORS.put(Blocks.DEEPSLATE_DIAMOND_ORE, new Color(0, 255, 255));
        ORE_COLORS.put(Blocks.ANCIENT_DEBRIS, new Color(165, 42, 42));
        ORE_COLORS.put(Blocks.EMERALD_ORE, new Color(0, 200, 0));
        ORE_COLORS.put(Blocks.DEEPSLATE_EMERALD_ORE, new Color(0, 200, 0));
        ORE_COLORS.put(Blocks.GOLD_ORE, new Color(255, 215, 0));
        ORE_COLORS.put(Blocks.DEEPSLATE_GOLD_ORE, new Color(255, 215, 0));
        ORE_COLORS.put(Blocks.NETHER_GOLD_ORE, new Color(255, 215, 0));
        ORE_COLORS.put(Blocks.IRON_ORE, new Color(210, 180, 140));
        ORE_COLORS.put(Blocks.DEEPSLATE_IRON_ORE, new Color(210, 180, 140));
        ORE_COLORS.put(Blocks.LAPIS_ORE, new Color(30, 30, 200));
        ORE_COLORS.put(Blocks.DEEPSLATE_LAPIS_ORE, new Color(30, 30, 200));
        ORE_COLORS.put(Blocks.REDSTONE_ORE, new Color(200, 0, 0));
        ORE_COLORS.put(Blocks.DEEPSLATE_REDSTONE_ORE, new Color(200, 0, 0));
        ORE_COLORS.put(Blocks.COPPER_ORE, new Color(184, 115, 51));
        ORE_COLORS.put(Blocks.DEEPSLATE_COPPER_ORE, new Color(184, 115, 51));
        ORE_COLORS.put(Blocks.COAL_ORE, new Color(50, 50, 50));
        ORE_COLORS.put(Blocks.DEEPSLATE_COAL_ORE, new Color(50, 50, 50));
    }

    public OreFinder() {
        super("OreFinder", "Scan and highlight ores with per-ore toggles", Category.RENDER);
    }

    @Override
    public void onDisable() {
        foundOres.clear();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        tickCounter++;
        if (tickCounter < updateDelay.get().intValue()) return;
        tickCounter = 0;

        scanForOres();
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;

        for (OreEntry ore : foundOres) {
            Color color = ORE_COLORS.getOrDefault(ore.block, Color.WHITE);

            // Block highlight
            RenderUtil.drawBlockBox(event.getMatrices(), ore.pos, color, 1.5f, event.getVertexConsumers());

            // Tracers
            if (tracers.get()) {
                RenderUtil.drawBlockTracer(event.getMatrices(), ore.pos, color, event.getVertexConsumers());
            }
        }
    }

    private void scanForOres() {
        foundOres.clear();
        BlockPos center = mc.player.getBlockPos();
        int r = range.get().intValue();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                int minY = mc.world.getBottomY();
                int maxY = minY + mc.world.getHeight();
                for (int y = minY - center.getY(); y < maxY - center.getY(); y++) {
                    BlockPos pos = center.add(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);
                    Block block = state.getBlock();

                    if (isTargetOre(block)) {
                        foundOres.add(new OreEntry(pos, block));
                    }
                }
            }
        }
    }

    private boolean isTargetOre(Block block) {
        if ((block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) && showDiamond.get()) return true;
        if (block == Blocks.ANCIENT_DEBRIS && showDebris.get()) return true;
        if ((block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) && showEmerald.get()) return true;
        if ((block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE) && showGold.get()) return true;
        if ((block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) && showIron.get()) return true;
        if ((block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) && showLapis.get()) return true;
        if ((block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) && showRedstone.get()) return true;
        if ((block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) && showCopper.get()) return true;
        if ((block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) && showCoal.get()) return true;

        return false;
    }

    private OreEntry getClosestOre() {
        BlockPos playerPos = mc.player.getBlockPos();
        OreEntry closest = null;
        double closestDist = Double.MAX_VALUE;

        for (OreEntry ore : foundOres) {
            double d = ore.pos.getSquaredDistance(playerPos);
            if (d < closestDist) {
                closestDist = d;
                closest = ore;
            }
        }
        return closest;
    }

    public List<OreEntry> getFoundOres() { return foundOres; }

    /**
     * Returns a list of all ore Block types that are currently enabled (toggled on).
     * Used by FoxyBot to know which ores to mine via Baritone.
     */
    public List<Block> getEnabledOreBlocks() {
        List<Block> blocks = new ArrayList<>();
        if (showDiamond.get()) { blocks.add(Blocks.DIAMOND_ORE); blocks.add(Blocks.DEEPSLATE_DIAMOND_ORE); }
        if (showDebris.get()) { blocks.add(Blocks.ANCIENT_DEBRIS); }
        if (showEmerald.get()) { blocks.add(Blocks.EMERALD_ORE); blocks.add(Blocks.DEEPSLATE_EMERALD_ORE); }
        if (showGold.get()) { blocks.add(Blocks.GOLD_ORE); blocks.add(Blocks.DEEPSLATE_GOLD_ORE); blocks.add(Blocks.NETHER_GOLD_ORE); }
        if (showIron.get()) { blocks.add(Blocks.IRON_ORE); blocks.add(Blocks.DEEPSLATE_IRON_ORE); }
        if (showLapis.get()) { blocks.add(Blocks.LAPIS_ORE); blocks.add(Blocks.DEEPSLATE_LAPIS_ORE); }
        if (showRedstone.get()) { blocks.add(Blocks.REDSTONE_ORE); blocks.add(Blocks.DEEPSLATE_REDSTONE_ORE); }
        if (showCopper.get()) { blocks.add(Blocks.COPPER_ORE); blocks.add(Blocks.DEEPSLATE_COPPER_ORE); }
        if (showCoal.get()) { blocks.add(Blocks.COAL_ORE); blocks.add(Blocks.DEEPSLATE_COAL_ORE); }
        return blocks;
    }

    public record OreEntry(BlockPos pos, Block block) {}
}
