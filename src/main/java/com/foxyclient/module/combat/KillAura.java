package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.RotationManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

/**
 * Automatically attacks nearby entities.
 */
public class KillAura extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Attack range", 4.5, 1.0, 6.0));
    private final NumberSetting cps = addSetting(new NumberSetting("CPS", "Clicks per second", 12.0, 1.0, 20.0));
    private final BoolSetting players = addSetting(new BoolSetting("Players", "Attack players", true));
    private final BoolSetting mobs = addSetting(new BoolSetting("Mobs", "Attack hostile mobs", true));
    private final BoolSetting animals = addSetting(new BoolSetting("Animals", "Attack animals", false));
    private final BoolSetting vehicles = addSetting(new BoolSetting("Vehicles", "Attack minecarts/boats", false));
    private final BoolSetting projectiles = addSetting(new BoolSetting("Projectiles", "Attack shulker bullets etc", false));
    private final BoolSetting crystals = addSetting(new BoolSetting("Crystals", "Attack end crystals", false));
    private final BoolSetting others = addSetting(new BoolSetting("Others", "Attack all other entities", false));
    private final ModeSetting priority = addSetting(new ModeSetting("Priority", "Target priority", "Distance", "Distance", "Health", "Angle"));
    private final BoolSetting autoSwitch = addSetting(new BoolSetting("AutoSwitch", "Switch to best weapon", true));
    private final BoolSetting normalizeScreen = addSetting(new BoolSetting("NormalizeScreen", "Keep screen normal while attacking", true));

    private int tickDelay = 0;

    public KillAura() {
        super("KillAura", "Automatically attacks nearby entities", Category.COMBAT, GLFW.GLFW_KEY_R);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        LivingEntity target = findTarget();
        if (target == null) return;

        if (tickDelay > 0) { tickDelay--; }
        
        // Face target
        double dx = target.getX() - mc.player.getX();
        double dy = (target.getY() + target.getHeight() / 2) - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double dz = target.getZ() - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        // Use RotationManager for rotations
        RotationManager.setRotation(yaw, pitch, normalizeScreen.get(), RotationManager.Priority.HIGH);

        if (!normalizeScreen.get()) {
            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
        }

        if (tickDelay <= 0) {
            tickDelay = (int) (20.0 / cps.get().doubleValue());
            // Attack
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private LivingEntity findTarget() {
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity == mc.player) continue;
            if (!living.isAlive()) continue;
            if (mc.player.distanceTo(entity) > range.get()) continue;

            if (entity instanceof PlayerEntity && !players.get()) continue;
            if (entity instanceof HostileEntity && !mobs.get()) continue;
            if (entity instanceof AnimalEntity && !animals.get()) continue;
            if (entity instanceof net.minecraft.entity.vehicle.VehicleEntity && !vehicles.get()) continue;
            if (entity instanceof net.minecraft.entity.projectile.ProjectileEntity && !projectiles.get()) continue;
            if (entity instanceof net.minecraft.entity.decoration.EndCrystalEntity && !crystals.get()) continue;

            if (!(entity instanceof PlayerEntity) && 
                !(entity instanceof HostileEntity) && 
                !(entity instanceof AnimalEntity) &&
                !(entity instanceof net.minecraft.entity.vehicle.VehicleEntity) &&
                !(entity instanceof net.minecraft.entity.projectile.ProjectileEntity) &&
                !(entity instanceof net.minecraft.entity.decoration.EndCrystalEntity)) {
                if (!others.get()) continue;
            }

            double score = switch (priority.get()) {
                case "Health" -> living.getHealth();
                case "Angle" -> getAngleTo(entity);
                default -> mc.player.distanceTo(entity);
            };

            if (score < bestScore) {
                bestScore = score;
                best = living;
            }
        }
        return best;
    }

    private double getAngleTo(Entity entity) {
        double dx = entity.getX() - mc.player.getX();
        double dz = entity.getZ() - mc.player.getZ();
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float diff = Math.abs(mc.player.getYaw() - targetYaw) % 360;
        return diff > 180 ? 360 - diff : diff;
    }
}
