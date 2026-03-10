package com.foxyclient.module.movement;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.ModeSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

/**
 * Allows walking through blocks using various exploits.
 */
public class Phase extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Phase method", "Sandwich", "Sandwich", "Packet"));

    public Phase() {
        super("Phase", "Walk through blocks", Category.MOVEMENT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        if (mode.is("Sandwich")) {
            if (mc.player.horizontalCollision) {
                double x = -Math.sin(Math.toRadians(mc.player.getYaw())) * 0.01;
                double z = Math.cos(Math.toRadians(mc.player.getYaw())) * 0.01;
                mc.player.setPosition(mc.player.getX() + x, mc.player.getY(), mc.player.getZ() + z);
            }
        } else if (mode.is("Packet")) {
            if (mc.player.horizontalCollision) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 0.01, mc.player.getZ(), false, false));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 0.0001, mc.player.getZ(), false, false));
            }
        }
    }
}
