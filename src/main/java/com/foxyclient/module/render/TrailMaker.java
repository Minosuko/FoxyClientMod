package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

/**
 * TrailMaker - Leave particle trails behind you.
 */
public class TrailMaker extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting("Particle", "Type of particle", "Heart", "Heart", "Flame", "Smoke", "Magic", "Crit", "Snow", "Slime", "Totem"));
    private final NumberSetting amount = addSetting(new NumberSetting("Amount", "Particles per tick", 1.0, 1.0, 10.0));
    private final BoolSetting onlyMoving = addSetting(new BoolSetting("OnlyMoving", "Only spawn when moving", true));

    private double lastX = 0, lastY = 0, lastZ = 0;

    public TrailMaker() {
        super("TrailMaker", "Leave particle trails", Category.RENDER);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        double dx = mc.player.getX() - lastX;
        double dy = mc.player.getY() - lastY;
        double dz = mc.player.getZ() - lastZ;

        lastX = mc.player.getX();
        lastY = mc.player.getY();
        lastZ = mc.player.getZ();

        if (onlyMoving.get()) {
            if (dx * dx + dy * dy + dz * dz < 0.001) {
                return;
            }
        }

        ParticleEffect effect = ParticleTypes.HEART;
        switch (mode.get()) {
            case "Flame": effect = ParticleTypes.FLAME; break;
            case "Smoke": effect = ParticleTypes.SMOKE; break;
            case "Magic": effect = ParticleTypes.WITCH; break;
            case "Crit": effect = ParticleTypes.ENCHANTED_HIT; break;
            case "Snow": effect = ParticleTypes.ITEM_SNOWBALL; break;
            case "Slime": effect = ParticleTypes.ITEM_SLIME; break;
            case "Totem": effect = ParticleTypes.TOTEM_OF_UNDYING; break;
        }

        int count = amount.get().intValue();
        for (int i = 0; i < count; i++) {
            double x = mc.player.getX() + (Math.random() - 0.5) * 0.5;
            double y = mc.player.getY() + (Math.random() * mc.player.getHeight());
            double z = mc.player.getZ() + (Math.random() - 0.5) * 0.5;
            
            mc.particleManager.addParticle(effect, x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
