package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.util.math.Vec3d;

/**
 * Automatically aims bow at target's predicted position.
 */
public class BowAimbot extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Max aim range", 50.0, 10.0, 100.0));
    private final BoolSetting predictMovement = addSetting(new BoolSetting("Predict", "Predict target movement", true));
    private final BoolSetting playersOnly = addSetting(new BoolSetting("PlayersOnly", "Only aim at players", false));
    private final BoolSetting normalizeScreen = addSetting(new BoolSetting("NormalizeScreen", "Keep screen normal while aiming", false));

    private int lingerTicks = 0;
    private float lastYaw, lastPitch;

    public BowAimbot() {
        super("BowAimbot", "Automatically aims bow at targets", Category.COMBAT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        
        boolean isBowing = mc.player.getActiveItem().getItem() instanceof BowItem;
        
        if (isBowing) {
            int charge = mc.player.getItemUseTime();
            if (charge < 1) return;

            LivingEntity target = findTarget();
            if (target != null) {
                Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ()).add(0, target.getHeight() * 0.85, 0);

                // Predict movement
                if (predictMovement.get()) {
                    Vec3d velocity = target.getVelocity();
                    double dist = mc.player.distanceTo(target);
                    double travelTime = dist / 3.0;
                    targetPos = targetPos.add(velocity.multiply(travelTime));
                }

                // Aim math
                double dx = targetPos.x - mc.player.getX();
                double dy = targetPos.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
                double dz = targetPos.z - mc.player.getZ();
                double horizontalDist = Math.sqrt(dx * dx + dz * dz);

                float velocity2 = Math.min(charge / 20.0f, 1.0f) * 3.0f;
                double gravity = 0.05;
                double angle = Math.atan((velocity2 * velocity2 - Math.sqrt(
                    Math.pow(velocity2, 4) - gravity * (gravity * horizontalDist * horizontalDist + 2 * dy * velocity2 * velocity2)
                )) / (gravity * horizontalDist));

                if (Double.isNaN(angle)) angle = Math.atan2(dy, horizontalDist);

                lastYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                lastPitch = (float) -Math.toDegrees(angle);
                lastPitch = Math.max(-90, Math.min(90, lastPitch));
                
                lingerTicks = 3; // Keep aiming for 3 ticks after release
            }
        }

        if (lingerTicks > 0) {
            com.foxyclient.util.RotationManager.setRotation(lastYaw, lastPitch, normalizeScreen.get(), com.foxyclient.util.RotationManager.Priority.HIGH);
            
            if (!normalizeScreen.get()) {
                mc.player.setYaw(lastYaw);
                mc.player.setPitch(lastPitch);
            }
            
            if (!isBowing) lingerTicks--;
        }
    }

    private LivingEntity findTarget() {
        LivingEntity closest = null;
        double closestDist = range.get();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity == mc.player) continue;
            if (!living.isAlive()) continue;
            if (playersOnly.get() && !(entity instanceof PlayerEntity)) continue;

            double dist = mc.player.distanceTo(entity);
            if (dist < closestDist) {
                closestDist = dist;
                closest = living;
            }
        }
        return closest;
    }
}
