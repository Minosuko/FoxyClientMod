package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.AttackEntityEvent;
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
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;

/**
 * KillEffect - Visual effects on entity kill.
 * Supports: Lightning, Particles, Totem, and custom effects.
 */
public class KillEffect extends Module {
    private final ModeSetting effect = addSetting(new ModeSetting("Effect", "Kill effect", "Lightning", "Lightning", "Particles", "Totem", "None"));
    private final ModeSetting particle = addSetting(new ModeSetting("Particle", "Particle type", "Crit", "Heart", "Flame", "Smoke", "Magic", "Crit", "Snow", "Slime", "Totem"));
    private final BoolSetting playersOnly = addSetting(new BoolSetting("PlayersOnly", "Only affect players", false));
    private final BoolSetting selfKill = addSetting(new BoolSetting("SelfKill", "Show effect when you kill something", true));
    private final BoolSetting othersKill = addSetting(new BoolSetting("OthersKill", "Show effect when others kill", true));

    private int lastAttackedId = -1;
    private long lastAttackTime = 0;

    public KillEffect() {
        super("KillEffect", "Visual effects on kills", Category.RENDER);
    }

    @EventHandler
    public void onAttack(AttackEntityEvent event) {
        lastAttackedId = event.getEntity().getId();
        lastAttackTime = System.currentTimeMillis();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (nullCheck()) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity victim = (LivingEntity) event.getEntity();
        boolean isPlayer = victim instanceof PlayerEntity;

        if (playersOnly.get() && !isPlayer) return;

        // Check if the player killed this entity
        boolean wasKilledByMe = victim.getId() == lastAttackedId && (System.currentTimeMillis() - lastAttackTime) < 2000;

        if (wasKilledByMe) {
            if (!selfKill.get()) return;
        } else {
            if (!othersKill.get()) return;
        }

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
        if (mc.world == null) return;
        
        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
        lightning.refreshPositionAfterTeleport(victim.getEntityPos());
        lightning.setCosmetic(true);
        mc.world.addEntity(lightning);
    }

    private void spawnParticles(LivingEntity victim) {
        if (mc.world == null) return;
        
        net.minecraft.particle.ParticleEffect type = switch (particle.get()) {
            case "Heart" -> ParticleTypes.HEART;
            case "Flame" -> ParticleTypes.FLAME;
            case "Smoke" -> ParticleTypes.SMOKE;
            case "Magic" -> ParticleTypes.ENCHANTED_HIT;
            case "Crit" -> ParticleTypes.CRIT;
            case "Snow" -> ParticleTypes.SNOWFLAKE;
            case "Slime" -> ParticleTypes.ITEM_SLIME;
            case "Totem" -> ParticleTypes.TOTEM_OF_UNDYING;
            default -> ParticleTypes.EXPLOSION;
        };
        
        for (int i = 0; i < 20; i++) {
            mc.world.addParticleClient(type, victim.getX(), victim.getY() + victim.getHeight() / 2, victim.getZ(), 
                mc.world.random.nextGaussian() * 0.1, 
                mc.world.random.nextGaussian() * 0.1, 
                mc.world.random.nextGaussian() * 0.1);
        }
    }

    private void spawnTotem(LivingEntity victim) {
        if (mc.player == null || mc.world == null) return;
        
        mc.gameRenderer.showFloatingItem(new ItemStack(Items.TOTEM_OF_UNDYING));
        mc.world.playSoundClient(mc.player.getX(), mc.player.getY(), mc.player.getZ(), 
            SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0F, 1.0F, false);
    }
}
