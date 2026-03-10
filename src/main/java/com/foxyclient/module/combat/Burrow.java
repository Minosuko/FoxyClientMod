package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.util.WorldUtil;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;

/**
 * Exploits jumping to place a block inside your own feet (Burrow).
 */
public class Burrow extends Module {
    public Burrow() {
        super("Burrow", "Self-trap exploit", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;

        int obsidianSlot = WorldUtil.findHotbarItem(net.minecraft.item.Items.OBSIDIAN);
        if (obsidianSlot == -1) {
            error("No obsidian!");
            setEnabled(false);
            return;
        }

        // Jump and place
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.41999998688698, mc.player.getZ(), true, false));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.75319998052115, mc.player.getZ(), true, false));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.00133597911214, mc.player.getZ(), true, false));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.16610926093821, mc.player.getZ(), true, false));

        WorldUtil.placeBlock(mc.player.getBlockPos(), obsidianSlot, true);

        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.2, mc.player.getZ(), false, false));
        
        setEnabled(false);
    }
}
