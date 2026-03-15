package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.FoxyRenderer;
import com.foxyclient.util.RenderLayers;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Predicts and draws the trajectory of projectiles.
 */
public class BowIndicator extends Module {
    private final BoolSetting bows = addSetting(new BoolSetting("Bows", "Show bow trajectory", true));
    private final BoolSetting throwables = addSetting(new BoolSetting("Throwables", "Show throwable trajectory", true));
    private final NumberSetting thickness = addSetting(new NumberSetting("Thickness", "Line thickness", 1.5, 0.5, 5.0));

    public BowIndicator() {
        super("BowIndicator", "Draws projectile trajectories", Category.RENDER);
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;

        Item item = mc.player.getMainHandStack().getItem();
        boolean isBow = item instanceof BowItem || item instanceof CrossbowItem;
        boolean isThrowable = item == Items.ENDER_PEARL || item == Items.SNOWBALL || item == Items.EGG || item == Items.LINGERING_POTION || item == Items.SPLASH_POTION || item == Items.TRIDENT;

        if ((isBow && !bows.get()) || (isThrowable && !throwables.get()) || (!isBow && !isThrowable)) {
            item = mc.player.getOffHandStack().getItem();
            isBow = item instanceof BowItem || item instanceof CrossbowItem;
            isThrowable = item == Items.ENDER_PEARL || item == Items.SNOWBALL || item == Items.EGG || item == Items.LINGERING_POTION || item == Items.SPLASH_POTION || item == Items.TRIDENT;
            if ((isBow && !bows.get()) || (isThrowable && !throwables.get()) || (!isBow && !isThrowable)) {
                return;
            }
        }

        // Calculate initial velocity based on item type
        double velocity = 0.0;
        double gravity = 0.0;
        boolean isPotion = false;

        if (item instanceof BowItem) {
            int useTime = mc.player.getItemUseTime();
            if (useTime > 0) {
                velocity = Math.min((useTime) / 20.0, 1.0) * 3.0;
            } else {
                velocity = 3.0; // Draw max if not drawing
            }
            gravity = 0.05;
        } else if (item instanceof CrossbowItem) {
            velocity = 3.15;
            gravity = 0.05;
        } else if (item == Items.TRIDENT) {
            velocity = 2.5;
            gravity = 0.05;
        } else if (item == Items.ENDER_PEARL || item == Items.SNOWBALL || item == Items.EGG) {
            velocity = 1.5;
            gravity = 0.03;
        } else if (item == Items.LINGERING_POTION || item == Items.SPLASH_POTION) {
            velocity = 0.5;
            gravity = 0.05;
            isPotion = true;
        }

        // Calculate initial positions and angles
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();

        double posX = mc.player.getX() - MathHelper.cos(yaw / 180.0F * (float) Math.PI) * 0.16F;
        double posY = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()) - 0.1;
        double posZ = mc.player.getZ() - MathHelper.sin(yaw / 180.0F * (float) Math.PI) * 0.16F;

        double motionX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
        double motionY = -MathHelper.sin((pitch - (isPotion ? 20 : 0)) / 180.0F * (float) Math.PI);
        double motionZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);

        double length = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        motionX = (motionX / length) * velocity;
        motionY = (motionY / length) * velocity;
        motionZ = (motionZ / length) * velocity;

        // Account for player movement
        motionX += mc.player.getVelocity().x;
        motionZ += mc.player.getVelocity().z;
        if (!mc.player.isOnGround()) {
            motionY += mc.player.getVelocity().y;
        }

        List<Vec3d> path = new ArrayList<>();
        path.add(new Vec3d(posX, posY, posZ));

        HitResult hit = null;
        for (int i = 0; i < 300; i++) {
            Vec3d currentPos = new Vec3d(posX, posY, posZ);
            Vec3d nextPos = new Vec3d(posX + motionX, posY + motionY, posZ + motionZ);

            // Raycast for blocks
            RaycastContext context = new RaycastContext(currentPos, nextPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            hit = mc.world.raycast(context);

            if (hit != null && hit.getType() != HitResult.Type.MISS) {
                nextPos = hit.getPos();
            }

            // Raycast for entities
            Box boundingBox = new Box(posX - 0.15, posY - 0.15, posZ - 0.15, posX + 0.15, posY + 0.15, posZ + 0.15).stretch(motionX, motionY, motionZ).expand(1.0);
            EntityHitResult entityHit = ProjectileUtil.raycast(mc.player, currentPos, nextPos, boundingBox, e -> !e.isSpectator() && e.canHit(), 0.3);

            if (entityHit != null) {
                hit = entityHit;
                nextPos = entityHit.getPos();
            }

            path.add(nextPos);

            if (hit != null && hit.getType() != HitResult.Type.MISS) {
                break;
            }

            posX += motionX;
            posY += motionY;
            posZ += motionZ;

            // Apply friction and gravity
            boolean inWater = mc.world.isWater(net.minecraft.util.math.BlockPos.ofFloored(posX, posY, posZ));
            double drag = inWater ? 0.8 : 0.99;
            motionX *= drag;
            motionY *= drag;
            motionZ *= drag;
            motionY -= (inWater && isBow) ? 0.0 : gravity; // Arrows don't fall fast in water usually, but rough approx
        }

        MatrixStack matrices = event.getMatrices();
        VertexConsumerProvider.Immediate vcp = event.getVertexConsumers();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos();

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        Color lineColor = new Color(0, 255, 255, 200);
        Color boxColor = new Color(255, 50, 50, 200);

        // Draw path line
        VertexConsumer buffer = vcp.getBuffer(RenderLayers.getBypassLines());
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float r = lineColor.getRed() / 255f;
        float g = lineColor.getGreen() / 255f;
        float b = lineColor.getBlue() / 255f;
        float a = lineColor.getAlpha() / 255f;
        
        for (int i = 0; i < path.size() - 1; i++) {
            Vec3d p1 = path.get(i);
            Vec3d p2 = path.get(i + 1);
            
            float dx = (float)(p2.x - p1.x);
            float dy = (float)(p2.y - p1.y);
            float dz = (float)(p2.z - p1.z);
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len > 0) { dx /= len; dy /= len; dz /= len; } else { dx = 1; dy = 0; dz = 0; }
            
            buffer.vertex(matrix, (float)p1.x, (float)p1.y, (float)p1.z).color(r, g, b, a).normal(dx, dy, dz).lineWidth(thickness.get().floatValue());
            buffer.vertex(matrix, (float)p2.x, (float)p2.y, (float)p2.z).color(r, g, b, a).normal(dx, dy, dz).lineWidth(thickness.get().floatValue());
        }

        // Draw hit box
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            if (hit instanceof BlockHitResult blockHit) {
                Box box = mc.world.getBlockState(blockHit.getBlockPos()).getCollisionShape(mc.world, blockHit.getBlockPos()).getBoundingBox().offset(blockHit.getBlockPos());
                FoxyRenderer.drawBox(matrices, vcp, box, boxColor, 0.4f);
                FoxyRenderer.drawLines(matrices, vcp, box, boxColor);
            } else if (hit instanceof EntityHitResult entityHit) {
                Entity entity = entityHit.getEntity();
                Box box = entity.getBoundingBox();
                FoxyRenderer.drawBox(matrices, vcp, box, boxColor, 0.4f);
                FoxyRenderer.drawLines(matrices, vcp, box, boxColor);
            }
        }

        matrices.pop();
    }
}
