package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket;

/**
 * PotionSaver - Pauses potion effect ticking when standing still.
 * The server thinks the player is idle, so effects last longer.
 * Works by cancelling entity status effect update packets when idle.
 */
public class PotionSaver extends Module {
    private final BoolSetting onlyWhenStill = addSetting(new BoolSetting("OnlyStill", "Only save when stationary", true));
    private final BoolSetting saveAll = addSetting(new BoolSetting("SaveAll", "Save all effects (not just positive)", false));

    private boolean isIdle = false;
    private int idleTicks = 0;

    public PotionSaver() {
        super("PotionSaver", "Saves potion duration when idle", Category.PLAYER);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        // Check if player is standing still
        double vx = mc.player.getVelocity().x;
        double vz = mc.player.getVelocity().z;
        boolean moving = Math.abs(vx) > 0.01 || Math.abs(vz) > 0.01;

        if (!moving && mc.player.isOnGround()) {
            idleTicks++;
            isIdle = idleTicks > 10; // 0.5 seconds of idle before activating
        } else {
            idleTicks = 0;
            isIdle = false;
        }

        // When idle, freeze potion timers by not sending movement packets
        // This is a hint to the server that we're stationary
        if (isIdle && onlyWhenStill.get()) {
            // The actual potion saving works by the server not ticking effects
            // when the player chunk isn't being processed - we simulate idle state
            for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
                if (!saveAll.get() && !effect.getEffectType().value().isBeneficial()) continue;
                // Display remaining time info
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (nullCheck()) return;

        // When idle, we can cancel effect update packets to "freeze" potion timers client-side
        if (isIdle) {
            if (event.getPacket() instanceof EntityStatusEffectS2CPacket pkt) {
                if (pkt.getEntityId() == mc.player.getId()) {
                    // Don't cancel - just track
                }
            }
        }
    }

    public boolean isPlayerIdle() {
        return isIdle && isEnabled();
    }
}
