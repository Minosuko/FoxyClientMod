package com.foxyclient.module.movement;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;

/**
 * Temporarily boosts speed by manipulating ticks.
 */
public class TickShift extends Module {
    private final NumberSetting packets = addSetting(new NumberSetting("Packets", "Number of packets to shift", 20, 1, 50));
    private int shiftCount = 0;

    public TickShift() {
        super("TickShift", "Temporarily boosts speed", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        shiftCount = 0;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        if (mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0) {
            if (shiftCount < packets.get()) {
                // This is a simplified version; real tickshift might need deeper timer manipulation
                mc.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly(mc.player.isOnGround(), false));
                shiftCount++;
            }
        } else {
            shiftCount = 0;
        }
    }
}
