package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.util.RenderUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

/** NewChunks - Highlights newly generated chunks (BleachHack port). */
public class NewChunks extends Module {
    private final Set<ChunkPos> newChunks = new HashSet<>();

    public NewChunks() { super("NewChunks", "Highlight newly generated chunks", Category.RENDER); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        ChunkPos playerChunk = mc.player.getChunkPos();
        int viewDist = mc.options.getViewDistance().getValue();
        for (int cx = -viewDist; cx <= viewDist; cx++) {
            for (int cz = -viewDist; cz <= viewDist; cz++) {
                ChunkPos cp = new ChunkPos(playerChunk.x + cx, playerChunk.z + cz);
                WorldChunk chunk = mc.world.getChunk(cp.x, cp.z);
                if (chunk != null && chunk.getInhabitedTime() <= 0) {
                    newChunks.add(cp);
                }
            }
        }
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;
        Color color = new Color(255, 50, 50);
        for (ChunkPos cp : newChunks) {
            BlockPos corner = new BlockPos(cp.getStartX(), mc.player.getBlockY(), cp.getStartZ());
            RenderUtil.drawBlockBox(event.getMatrices(), corner, color, 1.0f, event.getVertexConsumers());
        }
    }

    @Override
    public void onDisable() { newChunks.clear(); }
}
