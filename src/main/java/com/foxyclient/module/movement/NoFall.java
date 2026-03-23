package com.foxyclient.module.movement;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.mixin.PlayerMoveC2SPacketAccessor;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.ModeSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import com.foxyclient.FoxyClient;

/**
 * Prevents fall damage.
 */
public class NoFall extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "NoFall mode", "Packet", "Packet", "Bucket"));

    public NoFall() {
        super("NoFall", "Prevents fall damage", Category.MOVEMENT);
    }

    private boolean isFalling() {
        if (mc.player == null) return false;
        if (mc.player.fallDistance > 2.0f) return true;
        
        Module fly = FoxyClient.INSTANCE.getModuleManager().getModule(Fly.class);
        return fly != null && fly.isEnabled() && mc.player.getVelocity().y < 0;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (mode.get().equals("Packet") && isFalling()) {
            // Backup reactive method for redundancy
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (nullCheck()) return;
        if (mode.get().equals("Packet") && isFalling() && event.getPacket() instanceof PlayerMoveC2SPacket packet) {
            ((PlayerMoveC2SPacketAccessor) packet).setOnGround(true);
        }
    }
}
