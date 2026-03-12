package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.mixin.EntityVelocityUpdateS2CPacketAccessor;
import com.foxyclient.mixin.ExplosionS2CPacketAccessor;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.setting.ModeSetting;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

/**
 * Reduces or cancels knockback from attacks and explosions.
 *
 * Modes:
 * - Cancel: Completely cancels all knockback (ignores horizontal/vertical settings)
 * - Reduce: Applies horizontal/vertical multipliers to incoming knockback
 * - Packet: Cancels the velocity packet entirely (most aggressive)
 */
public class Velocity extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Anti-KB mode", "Cancel", "Cancel", "Reduce", "Packet"));
    private final NumberSetting horizontal = addSetting(new NumberSetting("Horizontal", "Horizontal knockback %", 0, 0, 100));
    private final NumberSetting vertical = addSetting(new NumberSetting("Vertical", "Vertical knockback %", 0, 0, 100));

    public Velocity() {
        super("Velocity", "Reduces/cancels knockback", Category.COMBAT);
    }

    @EventHandler
    public void onPacket(PacketEvent.Receive event) {
        if (nullCheck() || !isEnabled()) return;

        // Handle server-sent velocity updates (knockback from attacks)
        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
            if (packet.getEntityId() == mc.player.getId()) {
                handleVelocityPacket(event, packet);
            }
        }

        // Handle explosion knockback
        if (event.getPacket() instanceof ExplosionS2CPacket packet) {
            handleExplosionPacket(event, packet);
        }
    }

    private void handleVelocityPacket(PacketEvent.Receive event, EntityVelocityUpdateS2CPacket packet) {
        String currentMode = mode.get();

        if (currentMode.equals("Packet") || currentMode.equals("Cancel")) {
            // Cancel: zero out velocity; Packet: drop the packet entirely
            if (currentMode.equals("Packet")) {
                event.cancel();
            } else {
                ((EntityVelocityUpdateS2CPacketAccessor) packet).setVelocity(Vec3d.ZERO);
            }
            return;
        }

        // Reduce mode: apply horizontal/vertical multipliers
        double hMult = horizontal.get() / 100.0;
        double vMult = vertical.get() / 100.0;
        Vec3d vel = packet.getVelocity();
        Vec3d newVel = new Vec3d(vel.x * hMult, vel.y * vMult, vel.z * hMult);
        ((EntityVelocityUpdateS2CPacketAccessor) packet).setVelocity(newVel);
    }

    private void handleExplosionPacket(PacketEvent.Receive event, ExplosionS2CPacket packet) {
        ExplosionS2CPacketAccessor accessor = (ExplosionS2CPacketAccessor) (Object) packet;
        Optional<Vec3d> knockback = accessor.getPlayerKnockback();

        if (knockback.isEmpty()) return;

        String currentMode = mode.get();

        if (currentMode.equals("Packet") || currentMode.equals("Cancel")) {
            // Remove the knockback component entirely
            accessor.setPlayerKnockback(Optional.empty());
            return;
        }

        // Reduce mode: apply multipliers
        double hMult = horizontal.get() / 100.0;
        double vMult = vertical.get() / 100.0;
        Vec3d kb = knockback.get();
        Vec3d newKb = new Vec3d(kb.x * hMult, kb.y * vMult, kb.z * hMult);
        accessor.setPlayerKnockback(Optional.of(newKb));
    }

    public String getMode() { return mode.get(); }
    public double getHorizontalMultiplier() { return horizontal.get() / 100.0; }
    public double getVerticalMultiplier() { return vertical.get() / 100.0; }
}
