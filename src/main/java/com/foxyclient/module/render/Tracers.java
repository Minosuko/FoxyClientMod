package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.*;
import com.foxyclient.util.RenderLayers;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.*;

/**
 * Draws lines from the camera to entities in the world.
 * Uses thin camera-facing quads via BYPASS_TRANSLUCENT (proven working layer).
 */
public class Tracers extends Module {

    // ── General ───────────────────────────────────────────────────────
    private final ModeSetting targetPoint = addSetting(
        new ModeSetting("Target Point", "Where on the entity the tracer aims", "Center", "Feet", "Center", "Eyes")
    );

    // ── Stem ──────────────────────────────────────────────────────────
    private final BoolSetting stem = addSetting(
        new BoolSetting("Stem", "Draw a vertical line on the target entity", false)
    );

    // ── Fade ──────────────────────────────────────────────────────────
    private final BoolSetting fade = addSetting(
        new BoolSetting("Fade", "Fade tracers by distance", true)
    );
    private final NumberSetting maxDistance = addSetting(
        new NumberSetting("Max Distance", "Distance at which tracers fully fade", 100.0, 10.0, 500.0)
    );

    // ── Width ─────────────────────────────────────────────────────────
    private final NumberSetting lineWidth = addSetting(
        new NumberSetting("Width", "Tracer line width", 0.02, 0.005, 0.1)
    );

    // ── Entity Types ─────────────────────────────────────────────────
    private final BoolSetting players = addSetting(new BoolSetting("Players", "Trace to players", true));
    private final ColorSetting playerColor = addSetting(new ColorSetting("Player Color", "Color for players", new Color(255, 50, 50)));

    private final BoolSetting mobs = addSetting(new BoolSetting("Mobs", "Trace to hostile mobs", true));
    private final ColorSetting mobColor = addSetting(new ColorSetting("Mob Color", "Color for hostiles", new Color(255, 200, 0)));

    private final BoolSetting animals = addSetting(new BoolSetting("Animals", "Trace to passive mobs", false));
    private final ColorSetting animalColor = addSetting(new ColorSetting("Animal Color", "Color for passives", new Color(50, 255, 50)));

    private final BoolSetting vehicles = addSetting(new BoolSetting("Vehicles", "Trace to boats/minecarts", false));
    private final ColorSetting vehicleColor = addSetting(new ColorSetting("Vehicle Color", "Color for vehicles", new Color(255, 150, 0)));

    private final BoolSetting projectiles = addSetting(new BoolSetting("Projectiles", "Trace to projectiles", false));
    private final ColorSetting projectileColor = addSetting(new ColorSetting("Projectile Color", "Color for projectiles", new Color(0, 255, 255)));

    private final BoolSetting crystals = addSetting(new BoolSetting("Crystals", "Trace to end crystals", false));
    private final ColorSetting crystalColor = addSetting(new ColorSetting("Crystal Color", "Color for crystals", new Color(255, 0, 255)));

    private final BoolSetting others = addSetting(new BoolSetting("Others", "Trace to other entities", false));
    private final ColorSetting otherColor = addSetting(new ColorSetting("Other Color", "Color for others", new Color(200, 200, 200)));

    public Tracers() {
        super("Tracers", "Draw lines to entities", Category.RENDER);
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos();
        float tickDelta = event.getTickDelta();
        MatrixStack matrices = event.getMatrices();
        VertexConsumerProvider.Immediate vcp = event.getVertexConsumers();

        // Un-bobbed eye position: player lerped pos + eye height (no view bob)
        Vec3d playerPos = mc.player.getLerpedPos(tickDelta);
        Vec3d eyePos = playerPos.add(0, mc.player.getStandingEyeHeight(), 0);

        // Camera look direction from yaw/pitch (stable, no bob)
        float yaw = camera.getYaw();
        float pitch = camera.getPitch();
        double yawRad = Math.toRadians(-yaw);
        double pitchRad = Math.toRadians(-pitch);
        double lookX = Math.sin(yawRad) * Math.cos(pitchRad);
        double lookY = Math.sin(pitchRad);
        double lookZ = Math.cos(yawRad) * Math.cos(pitchRad);

        // Crosshair = eye + lookDir * 1 block (stable start point)
        double crosshairDist = 1.0;
        Vec3d crosshairPos = eyePos.add(lookX * crosshairDist, lookY * crosshairDist, lookZ * crosshairDist);

        // Crosshair in camera-relative coords
        float sx = (float) (crosshairPos.x - camPos.x);
        float sy = (float) (crosshairPos.y - camPos.y);
        float sz = (float) (crosshairPos.z - camPos.z);

        // Use BYPASS_TRANSLUCENT (QUADS) — same layer that works for ESP boxes
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer buffer = vcp.getBuffer(RenderLayers.getBypassTranslucent());

        float halfW = lineWidth.get().floatValue() / 2f;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || entity.isRemoved()) continue;

            Color color = getEntityColor(entity);
            if (color == null) continue;

            // Compute alpha (distance fade)
            float alphaF = 1.0f;
            if (fade.get()) {
                double dist = mc.player.distanceTo(entity);
                alphaF = (float) Math.max(0.0, 1.0 - (dist / maxDistance.get()));
                if (alphaF <= 0f) continue;
            }

            float r = color.getRed() / 255f;
            float g = color.getGreen() / 255f;
            float b = color.getBlue() / 255f;
            float a = alphaF * 0.85f;

            // Entity target in camera-relative coords
            Vec3d lerpedPos = entity.getLerpedPos(tickDelta);
            Vec3d target = getTargetPos(entity, lerpedPos);
            float tx = (float) (target.x - camPos.x);
            float ty = (float) (target.y - camPos.y);
            float tz = (float) (target.z - camPos.z);

            // Draw tracer: crosshair → entity target
            drawLineQuad(buffer, matrix, sx, sy, sz, tx, ty, tz, halfW, r, g, b, a);

            // Stem
            if (stem.get()) {
                float ex = (float) (lerpedPos.x - camPos.x);
                float ez = (float) (lerpedPos.z - camPos.z);
                float feetY = (float) (lerpedPos.y - camPos.y);
                float eyesY = feetY + entity.getStandingEyeHeight();
                drawLineQuad(buffer, matrix, ex, feetY, ez, ex, eyesY, ez, halfW, 1f, 1f, 1f, a * 0.7f);
            }
        }
    }

    /**
     * Draw a line segment as a thin camera-facing quad using POSITION_COLOR vertex format.
     * Computes a perpendicular offset so the quad always faces the camera.
     */
    private void drawLineQuad(VertexConsumer buffer, Matrix4f matrix,
                               float x1, float y1, float z1,
                               float x2, float y2, float z2,
                               float halfWidth,
                               float r, float g, float b, float a) {
        // Line direction
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f) return;

        Vector3f lineDir = new Vector3f(dx / len, dy / len, dz / len);

        // View direction: from camera to midpoint of line
        float mx = (x1 + x2) * 0.5f;
        float my = (y1 + y2) * 0.5f;
        float mz = (z1 + z2) * 0.5f;
        float mLen = (float) Math.sqrt(mx * mx + my * my + mz * mz);

        Vector3f viewDir;
        if (mLen < 0.001f) {
            // Midpoint is at camera origin — use the end point direction
            viewDir = new Vector3f(x2, y2, z2).normalize();
        } else {
            viewDir = new Vector3f(mx / mLen, my / mLen, mz / mLen);
        }

        // Perpendicular direction = cross(lineDir, viewDir)
        Vector3f perp = new Vector3f();
        lineDir.cross(viewDir, perp);
        float perpLen = perp.length();
        if (perpLen < 0.0001f) {
            // Line is parallel to view — use arbitrary up
            Vector3f up = Math.abs(lineDir.y) > 0.99f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
            lineDir.cross(up, perp);
            perpLen = perp.length();
            if (perpLen < 0.0001f) return;
        }
        perp.mul(halfWidth / perpLen);

        // 4 corners of the quad
        float ax = x1 - perp.x, ay = y1 - perp.y, az = z1 - perp.z;
        float bx = x1 + perp.x, by = y1 + perp.y, bz = z1 + perp.z;
        float cx = x2 + perp.x, cy = y2 + perp.y, cz = z2 + perp.z;
        float ex = x2 - perp.x, ey = y2 - perp.y, ez = z2 - perp.z;

        // Emit quad (POSITION_COLOR format, CCW winding)
        buffer.vertex(matrix, ax, ay, az).color(r, g, b, a);
        buffer.vertex(matrix, bx, by, bz).color(r, g, b, a);
        buffer.vertex(matrix, cx, cy, cz).color(r, g, b, a);
        buffer.vertex(matrix, ex, ey, ez).color(r, g, b, a);
    }

    /**
     * Compute the world-space target position on the entity.
     */
    private Vec3d getTargetPos(Entity entity, Vec3d lerpedPos) {
        if (targetPoint.is("Eyes")) {
            return lerpedPos.add(0, entity.getStandingEyeHeight(), 0);
        } else if (targetPoint.is("Feet")) {
            return lerpedPos;
        } else {
            return lerpedPos.add(0, entity.getStandingEyeHeight() * 0.5, 0);
        }
    }

    /**
     * Returns the configured color for an entity type, or null if disabled.
     */
    private Color getEntityColor(Entity entity) {
        if (entity instanceof PlayerEntity) return players.get() ? playerColor.get() : null;
        if (entity instanceof HostileEntity) return mobs.get() ? mobColor.get() : null;
        if (entity instanceof AnimalEntity) return animals.get() ? animalColor.get() : null;
        if (entity instanceof VehicleEntity) return vehicles.get() ? vehicleColor.get() : null;
        if (entity instanceof ProjectileEntity) return projectiles.get() ? projectileColor.get() : null;
        if (entity instanceof EndCrystalEntity) return crystals.get() ? crystalColor.get() : null;
        return others.get() ? otherColor.get() : null;
    }
}
