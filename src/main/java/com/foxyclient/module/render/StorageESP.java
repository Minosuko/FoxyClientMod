package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.util.RenderUtil;
import net.minecraft.block.entity.*;
import net.minecraft.util.math.BlockPos;

import java.awt.*;

/**
 * Highlights storage blocks (chests, shulkers, barrels, etc).
 */
public class StorageESP extends Module {
    private final BoolSetting chests = addSetting(new BoolSetting("Chests", "Show chests", true));
    private final BoolSetting enderChests = addSetting(new BoolSetting("EnderChests", "Show ender chests", true));
    private final BoolSetting shulkers = addSetting(new BoolSetting("Shulkers", "Show shulker boxes", true));
    private final BoolSetting barrels = addSetting(new BoolSetting("Barrels", "Show barrels", true));

    public StorageESP() {
        super("StorageESP", "Highlight storage blocks", Category.RENDER);
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;

        int viewDist = mc.options.getViewDistance().getValue();
        int chunkX = (int) (mc.player.getX() / 16);
        int chunkZ = (int) (mc.player.getZ() / 16);

        for (int x = chunkX - viewDist; x <= chunkX + viewDist; x++) {
            for (int z = chunkZ - viewDist; z <= chunkZ + viewDist; z++) {
                if (!mc.world.getChunkManager().isChunkLoaded(x, z)) continue;

                net.minecraft.world.chunk.WorldChunk chunk = mc.world.getChunk(x, z);
                if (chunk == null) continue;

                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    Color color = null;
                    if (be instanceof ChestBlockEntity) {
                        if (chests.get()) color = new Color(255, 200, 0);
                    } else if (be instanceof EnderChestBlockEntity) {
                        if (enderChests.get()) color = new Color(200, 0, 255);
                    } else if (be instanceof ShulkerBoxBlockEntity) {
                        if (shulkers.get()) color = new Color(255, 100, 200);
                    } else if (be instanceof BarrelBlockEntity) {
                        if (barrels.get()) color = new Color(200, 150, 50);
                    }

                    if (color != null) {
                        BlockPos pos = be.getPos();
                        RenderUtil.drawBlockBox(event.getMatrices(), pos, color, 2.0f, event.getVertexConsumers());
                    }
                }
            }
        }
    }
}
