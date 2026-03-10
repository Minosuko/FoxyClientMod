package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import java.awt.*;

/**
 * Highlights valuable blocks within range.
 */
public class BlockESP extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Scan range", 32, 8, 64));
    private final BoolSetting diamonds = addSetting(new BoolSetting("Diamonds", "Show diamonds", true));
    private final BoolSetting emeralds = addSetting(new BoolSetting("Emeralds", "Show emeralds", true));
    private final BoolSetting gold = addSetting(new BoolSetting("Gold", "Show gold", true));
    private final BoolSetting netherite = addSetting(new BoolSetting("Netherite", "Show ancient debris", true));
    private final BoolSetting spawners = addSetting(new BoolSetting("Spawners", "Show spawners", true));
    private final BoolSetting portals = addSetting(new BoolSetting("Portals", "Show portals", false));

    private int scanCooldown = 0;

    public BlockESP() {
        super("BlockESP", "Highlight valuable blocks", Category.RENDER);
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;
        int r = range.get().intValue();
        BlockPos center = mc.player.getBlockPos();

        for (int x = -r; x <= r; x++) {
            for (int y = -r / 2; y <= r / 2; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = center.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();
                    Color color = getBlockColor(block);
                    if (color != null) {
                        RenderUtil.drawBlockBox(event.getMatrices(), pos, color, 1.5f, event.getVertexConsumers());
                    }
                }
            }
        }
    }

    private Color getBlockColor(Block block) {
        if (diamonds.get() && (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE)) return Color.CYAN;
        if (emeralds.get() && (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE)) return Color.GREEN;
        if (gold.get() && (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE)) return Color.YELLOW;
        if (netherite.get() && block == Blocks.ANCIENT_DEBRIS) return new Color(139, 69, 19);
        if (spawners.get() && block == Blocks.SPAWNER) return Color.RED;
        if (portals.get() && (block == Blocks.NETHER_PORTAL || block == Blocks.END_PORTAL)) return Color.MAGENTA;
        return null;
    }
}
