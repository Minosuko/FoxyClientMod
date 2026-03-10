package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.event.events.Render2DEvent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * AimAssist - Smoothly rotates toward nearby enemies.
 * Removed from Meteor in ee391, ported here.
 */
public class AimAssist extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Target range", 4.5, 1, 8));
    private final NumberSetting fov = addSetting(new NumberSetting("FOV", "Field of view angle", 60.0, 1.0, 360.0));
    private final BoolSetting drawCircle = addSetting(new BoolSetting("Draw Circle", "Render FOV circle on HUD", true));
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Rotation speed", 5.0, 1, 20));
    private final ModeSetting priority = addSetting(new ModeSetting("Priority", "Targeting priority", "Distance", "Distance", "Health", "Angle"));
    private final BoolSetting playersOnly = addSetting(new BoolSetting("PlayersOnly", "Only target players", true));
    private final BoolSetting invisibles = addSetting(new BoolSetting("Invisibles", "Target invisible entities", false));
    private final BoolSetting requireSight = addSetting(new BoolSetting("RequireSight", "Only aim if visible", true));

    public AimAssist() {
        super("AimAssist", "Smoothly rotates toward nearby targets", Category.COMBAT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        LivingEntity target = findTarget();
        if (target == null) return;

        Vec3d targetPos = target.getEyePos();
        double dx = targetPos.x - mc.player.getX();
        double dy = targetPos.y - mc.player.getEyeY();
        double dz = targetPos.z - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        float spd = speed.get().floatValue();

        mc.player.setYaw(mc.player.getYaw() + MathHelper.wrapDegrees(targetYaw - mc.player.getYaw()) / (20f / spd));
        mc.player.setPitch(mc.player.getPitch() + MathHelper.wrapDegrees(targetPitch - mc.player.getPitch()) / (20f / spd));
    }

    private LivingEntity findTarget() {
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity le)) continue;
            if (le == mc.player) continue;
            if (!le.isAlive()) continue;
            if (playersOnly.get() && !(le instanceof PlayerEntity)) continue;
            if (!invisibles.get() && le.isInvisible()) continue;
            double d = mc.player.distanceTo(le);
            if (d > range.get()) continue;
            if (requireSight.get() && !mc.player.canSee(le)) continue;

            double dx = le.getX() - mc.player.getX();
            double dy = le.getEyeY() - mc.player.getEyeY();
            double dz = le.getZ() - mc.player.getZ();
            double distToEye = Math.sqrt(dx * dx + dz * dz);

            float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, distToEye));

            float yawDiff = MathHelper.wrapDegrees(mc.player.getYaw() - targetYaw);
            float pitchDiff = MathHelper.wrapDegrees(mc.player.getPitch() - targetPitch);
            double totalAngle = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

            if (totalAngle > fov.get().floatValue() / 2.0) continue;

            double score = switch (priority.get()) {
                case "Health" -> le.getHealth();
                case "Angle" -> {
                    Vec3d look = mc.player.getRotationVec(1.0f);
                    Vec3d lePos = new Vec3d(le.getX(), le.getY(), le.getZ());
                    Vec3d pPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
                    Vec3d toTarget = lePos.subtract(pPos).normalize();
                    yield 1.0 - look.dotProduct(toTarget);
                }
                default -> d;
            };

            if (score < bestScore) {
                bestScore = score;
                best = le;
            }
        }
        return best;
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (nullCheck() || !drawCircle.get()) return;

        DrawContext context = event.getContext();
        int centerX = mc.getWindow().getScaledWidth() / 2;
        int centerY = mc.getWindow().getScaledHeight() / 2;

        float currentFov = mc.options.getFov().getValue().floatValue();
        float radius = (fov.get().floatValue() / currentFov) * (mc.getWindow().getScaledHeight() / 2f);

        int color = 0xAAFFFFFF;

        for (int i = 0; i < 360; i += 2) {
            double angle = Math.toRadians(i);
            int x = (int) (centerX + Math.cos(angle) * radius);
            int y = (int) (centerY + Math.sin(angle) * radius);
            context.fill(x, y, x + 1, y + 1, color);
        }
    }
}
