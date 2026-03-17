package com.foxyclient.module.ui;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Renders custom nametags above players with health, ping, and distance info.
 * Uses the RenderEvent (world render phase) to iterate players ourselves,
 * billboard the text toward the camera, and render via TextRenderer.
 */
public class Nametags extends Module {
    private final BoolSetting showHealth  = addSetting(new BoolSetting("Health", "Show health", true));
    private final BoolSetting showPing    = addSetting(new BoolSetting("Ping", "Show ping", true));
    private final BoolSetting showDistance = addSetting(new BoolSetting("Distance", "Show distance", true));
    private final BoolSetting showSelf    = addSetting(new BoolSetting("Show Self", "Show own nametag in F5", true));
    private final NumberSetting scale     = addSetting(new NumberSetting("Scale", "Nametag scale", 1.5, 0.5, 3.0));

    public Nametags() {
        super("Nametags", "Better nametags with extra info", Category.UI);
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (mc.world == null || mc.player == null) return;

        MatrixStack matrices = event.getMatrices();
        VertexConsumerProvider.Immediate vcp = event.getVertexConsumers();
        float tickDelta = event.getTickDelta();

        // Render other players
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player) {
                renderTag(player, matrices, vcp, tickDelta);
            }
        }

        // Render self separately (always attempt, renderTag handles F5 check)
        renderTag(mc.player, matrices, vcp, tickDelta);

        // Flush text buffers
        vcp.draw();
    }

    private void renderTag(PlayerEntity player, MatrixStack matrices, VertexConsumerProvider.Immediate vcp, float tickDelta) {
        boolean isSelf = (player == mc.player);

        if (isSelf) {
            // Only show self nametag in third-person (F5) mode
            if (!showSelf.get()) return;
            if (mc.options.getPerspective().isFirstPerson()) return;
        } else {
            if (player.isInvisible()) return;
        }

        // Camera position
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos();

        // Interpolated entity position
        Vec3d lerpedPos = player.getLerpedPos(tickDelta);

        // For self: use the actual player position (getLerpedPos returns it)
        // The camera in F5 is behind the player, so the relative offset is correct
        double relX = lerpedPos.x - camPos.x;
        double relY = lerpedPos.y - camPos.y;
        double relZ = lerpedPos.z - camPos.z;

        // Distance from camera to entity (NOT player-to-player distance)
        double distFromCamera = Math.sqrt(relX * relX + relY * relY + relZ * relZ);

        // Distance cap (200 blocks from camera)
        if (distFromCamera > 200) return;

        // Build tag text
        String text = formatNametag(player);

        // Scale: base scale + distance-adaptive scaling so tags are readable at range
        float scaleVal = scale.get().floatValue() * 0.025f;
        float distScale = Math.max(1.0f, (float) distFromCamera * 0.12f);
        distScale = Math.min(distScale, 3.0f);
        // For self in F5, the camera is only ~4 blocks away, ensure a reasonable minimum
        if (isSelf) distScale = Math.max(distScale, 1.2f);
        float finalScale = scaleVal * distScale;

        matrices.push();
        // Position above the entity's head
        matrices.translate(relX, relY + player.getHeight() + (isSelf ? 0.5 : 0.3), relZ);

        // Billboard: rotate to face the camera
        matrices.multiply(camera.getRotation());
        matrices.scale(-finalScale, -finalScale, finalScale);

        TextRenderer textRenderer = mc.textRenderer;
        float halfWidth = textRenderer.getWidth(text) / 2.0f;
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Semi-transparent black background
        int bgColor = 0x59000000;

        // SEE_THROUGH layer: visible through walls
        textRenderer.draw(
            text,
            -halfWidth,
            0,
            0xFFFFFFFF,
            false,
            matrix,
            vcp,
            TextRenderer.TextLayerType.SEE_THROUGH,
            bgColor,
            LightmapTextureManager.MAX_LIGHT_COORDINATE
        );

        // NORMAL layer on top for proper depth rendering
        textRenderer.draw(
            text,
            -halfWidth,
            0,
            0xFFFFFFFF,
            false,
            matrix,
            vcp,
            TextRenderer.TextLayerType.NORMAL,
            0,
            LightmapTextureManager.MAX_LIGHT_COORDINATE
        );

        matrices.pop();
    }

    /**
     * Build the nametag string with optional health, ping, and distance.
     */
    public String formatNametag(PlayerEntity player) {
        StringBuilder sb = new StringBuilder();
        sb.append(player.getName().getString());

        if (showHealth.get()) {
            int health = (int) (player.getHealth() + player.getAbsorptionAmount());
            String color;
            if (health > 15) color = "§a";
            else if (health > 10) color = "§e";
            else if (health > 5) color = "§6";
            else color = "§c";
            sb.append(" ").append(color).append(health).append("§f❤");
        }

        if (showPing.get() && mc.getNetworkHandler() != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
            if (entry != null) {
                int ping = entry.getLatency();
                sb.append(" §7[").append(ping).append("ms]");
            }
        }

        if (showDistance.get() && mc.player != null) {
            int dist = (int) mc.player.distanceTo(player);
            sb.append(" §7[").append(dist).append("m]");
        }

        return sb.toString();
    }

    /** Used by MixinEntityRenderer to check if vanilla labels should be suppressed. */
    public boolean shouldRenderNametag() {
        return isEnabled();
    }
}
