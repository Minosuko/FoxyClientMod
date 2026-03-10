package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.setting.ModeSetting;

/**
 * Reduces or cancels knockback from attacks.
 */
public class Velocity extends Module {
    private final NumberSetting horizontal = addSetting(new NumberSetting("Horizontal", "Horizontal knockback %", 0, 0, 100));
    private final NumberSetting vertical = addSetting(new NumberSetting("Vertical", "Vertical knockback %", 0, 0, 100));
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Anti-KB mode", "Cancel", "Cancel", "Reduce", "Packet"));

    public Velocity() {
        super("Velocity", "Reduces/cancels knockback", Category.COMBAT);
    }

    @EventHandler
    public void onPacket(PacketEvent.Receive event) {
        if (nullCheck()) return;
        
        if (mode.get().equals("Packet")) {
            if (event.getPacket() instanceof net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket packet) {
                if (packet.getEntityId() == mc.player.getId()) {
                    event.cancel();
                }
            }
            if (event.getPacket() instanceof net.minecraft.network.packet.s2c.play.ExplosionS2CPacket packet) {
                // To properly cancel explosion knockback with just a packet cancel, we can cancel the whole packet
                // Or modify it to have 0 player velocity. We'll modify it.
                // Wait, it's easier to just cancel the knockback in Mixin or set the velocity variables to 0.
                // But canceling the packet might stop the explosion particles. 
                // Let's rely on MixinLivingEntity or just cancel velocity updates since FoxyClient usually injects into the packet.
                // We'll leave Explosion Packet handling to Mixin if we can't easily modify the packet without accessors.
            }
        }
    }

    public String getMode() { return mode.get(); }

    public double getHorizontalMultiplier() { return horizontal.get() / 100.0; }
    public double getVerticalMultiplier() { return vertical.get() / 100.0; }
}
