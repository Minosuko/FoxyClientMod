package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.RenderUtil;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

/** NewerNewChunks - Improved new chunk detection using fluid states exploit. */
public class NewerNewChunks extends Module {

    private final Set<ChunkPos> newChunks = new HashSet<>();
    private final NumberSetting yOffset = addSetting(new NumberSetting("Y Offset", "Render Y offset", 0, -64, 320));

    public NewerNewChunks() { 
        super("NewerNewChunks", "Improved new chunk detection", Category.RENDER); 
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck()) return;

        if (event.getPacket() instanceof BlockUpdateS2CPacket packet) {
            checkState(packet.getPos(), packet.getState());
        } 
        else if (event.getPacket() instanceof ChunkDeltaUpdateS2CPacket packet) {
            packet.visitUpdates((pos, state) -> checkState(pos, state));
        }
    }

    private void checkState(BlockPos pos, BlockState state) {
        FluidState fluid = state.getFluidState();
        if (!fluid.isEmpty() && !fluid.isStill()) {
            ChunkPos chunkPos = new ChunkPos(pos);
            newChunks.add(chunkPos);
        }
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck() || newChunks.isEmpty()) return;
        
        Color color = new Color(255, 50, 50, 100);
        int y = yOffset.get().intValue() == 0 ? mc.player.getBlockY() : yOffset.get().intValue();
        
        for (ChunkPos cp : newChunks) {
            BlockPos corner = new BlockPos(cp.getStartX(), y, cp.getStartZ());
            BlockPos corner2 = new BlockPos(cp.getEndX(), y, cp.getEndZ());
            
            // Draw a flat box at the given Y level for the whole chunk
            net.minecraft.util.math.Box box = new net.minecraft.util.math.Box(
                corner.getX(), y, corner.getZ(),
                corner2.getX() + 1, y, corner2.getZ() + 1
            );
            
            RenderUtil.drawBox(event.getMatrices(), event.getVertexConsumers(), box, color, 1.0f);
        }
    }

    @Override
    public void onDisable() {
        newChunks.clear();
    }
}
