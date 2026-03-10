package com.foxyclient.module.misc;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.RenderLayers;
import com.foxyclient.util.RotationManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import java.awt.Color;

/**
 * Spinbot - Server-side spinning while screen stays normal.
 * In F5 and to other players, your player spins. Your screen remains unchanged.
 */
public class Spinbot extends Module {
    // General Settings
    private final BoolSetting spinjitsu = addSetting(new BoolSetting("Spinjitsu", "Render a hat while spinning", false));
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Degrees per tick to spin", 18.0, 0.0, 180.0));
    private final BoolSetting faceGround = addSetting(new BoolSetting("FaceGround", "Lock pitch to ground", true));

    private float currentYaw = 0;
    private float currentPitch = 0;
    private float prevYaw = 0;
    private float prevPitch = 0;
    private int ticks = 0;

    public Spinbot() {
        super("Spinbot", "Server-side body spinning", Category.MISC);
    }

    @Override
    public void onEnable() {
        if (!nullCheck()) {
            currentYaw = mc.player.getYaw();
            currentPitch = mc.player.getPitch();
            prevYaw = currentYaw;
            prevPitch = currentPitch;
            ticks = 0;
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        // Functional approach: calculate absolute rotation based on total ticks
        // This avoids compounding floating point drift or wrap glitches entirely
        currentYaw = (mc.player.age * speed.get().floatValue()) % 360.0f;
        
        if (faceGround.get() || spinjitsu.get()) {
            currentPitch = 90.0f;
        } else {
            // Keep normal pitch if neither face ground nor spinjitsu is enabled
            currentPitch = mc.player.getPitch();
        }

        // We do NOT wrap the currentYaw going into RotationManager
        // RotationManager actually expects a continuous, unwrapped angle
        // to properly calculate the shortest path for prevYaw -> serverYaw
        RotationManager.setRotation(currentYaw, currentPitch, true, RotationManager.Priority.EXTREME);
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck() || !spinjitsu.get()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d lerpedPos = mc.player.getLerpedPos(event.getTickDelta());
        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();

        matrices.push();
        matrices.translate(lerpedPos.x - camPos.x, lerpedPos.y - camPos.y, lerpedPos.z - camPos.z);
        
        // Use functional absolute rotation + tickDelta for mathematically perfect visual spin
        float absoluteYaw = ((mc.player.age + event.getTickDelta()) * speed.get().floatValue()) % 360.0f;
        
        // Spin the hat with the player
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-absoluteYaw - 180));

        float height = mc.player.getHeight();
        float hatHeight = 0.5f;
        float hatRadius = 0.45f;
        int segments = 24;

        VertexConsumer buffer = event.getVertexConsumers().getBuffer(RenderLayers.getBypassTranslucent());
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float rainbow = (System.currentTimeMillis() % 2000) / 2000f;
        Color color = Color.getHSBColor(rainbow, 1f, 1f);
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = 0.6f;

        // Round pyramid (Cone)
        float apexX = 0, apexY = height + hatHeight, apexZ = 0;
        
        for (int i = 0; i < segments; i++) {
            double angle1 = Math.toRadians((i * 360.0) / segments);
            double angle2 = Math.toRadians(((i + 1) * 360.0) / segments);

            float x1 = (float) (Math.cos(angle1) * hatRadius);
            float z1 = (float) (Math.sin(angle1) * hatRadius);
            float x2 = (float) (Math.cos(angle2) * hatRadius);
            float z2 = (float) (Math.sin(angle2) * hatRadius);

            // Side triangle
            drawTriangle(buffer, matrix, apexX, apexY, apexZ, x1, height, z1, x2, height, z2, r, g, b, a);
            // Base triangle (to center)
            drawTriangle(buffer, matrix, 0, height, 0, x2, height, z2, x1, height, z1, r, g, b, a);
        }

        matrices.pop();
    }

    private void drawTriangle(VertexConsumer buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float r, float g, float b, float a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        buffer.vertex(matrix, x3, y3, z3).color(r, g, b, a);
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a); // Closing the quad (VCP bypass translucent uses QUADS)
    }
}
