package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.EntityDeathEvent;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ColorSetting;
import com.foxyclient.util.RenderUtil;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;

/**
 * KillEffect - Visual effects on entity kill.
 * Supports: Lightning, Particles, Totem, and custom effects.
 */
public class KillEffect extends Module {
    private final ModeSetting effect = addSetting(new ModeSetting("Effect", "Kill effect", "Lightning", "Lightning", "Particles", "Totem", "None"));
    private final BoolSetting playersOnly = addSetting(new BoolSetting("PlayersOnly", "Only affect players", false));
    private final BoolSetting selfKill = addSetting(new BoolSetting("SelfKill", "Show effect when you kill something", true));
    private final BoolSetting othersKill = addSetting(new BoolSetting("OthersKill", "Show effect when others kill", true));
    private final ColorSetting color = addSetting(new ColorSetting("Color", "Effect color", new Color(255, 50, 50)));

    public KillEffect() {
        super("KillEffect", "Visual effects on kills", Category.RENDER);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (nullCheck()) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity victim = (LivingEntity) event.getEntity();
        boolean isPlayer = victim instanceof PlayerEntity;

        if (playersOnly.get() && !isPlayer) return;

        boolean isSelf = victim.getUuid().equals(mc.player.getUuid());
        if (isSelf && !selfKill.get()) return;
        if (!isSelf && !othersKill.get()) return;

        String effectName = effect.get();
        switch (effectName) {
            case "Lightning":
                spawnLightning(victim);
                break;
            case "Particles":
                spawnParticles(victim);
                break;
            case "Totem":
                spawnTotem(victim);
                break;
            case "None":
            default:
                break;
        }
    }

    private void spawnLightning(LivingEntity victim) {
        // Simple lightning effect at victim's position
        // In a real implementation, we'd use a lightning entity or particle effect
        // For now, we'll just print a message
        System.out.println("Lightning effect at " + victim.getName().getString());
    }

    private void spawnParticles(LivingEntity victim) {
        // Spawn particles at victim's position
        // This would use ParticleUtil.spawnParticles
        System.out.println("Particles effect at " + victim.getName().getString());
    }

    private void spawnTotem(LivingEntity victim) {
        // Spawn totem effect at victim's position
        // This would use RenderUtil.drawTotem
        System.out.println("Totem effect at " + victim.getName().getString());
    }
}
