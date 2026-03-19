package com.foxyclient.module.ui;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.module.render.Freecam;
import com.foxyclient.module.render.Freelook;
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
 * Shows own nametag when the camera is detached (F5, Freelook, or Freecam).
 */
public class Nametags extends Module {
    private final BoolSetting showHealth   = addSetting(new BoolSetting("Health", "Show health", true));
    private final BoolSetting showPing     = addSetting(new BoolSetting("Ping", "Show ping", true));
    private final BoolSetting showDistance = addSetting(new BoolSetting("Distance", "Show distance", true));
    private final BoolSetting showSelf     = addSetting(new BoolSetting("Show Self", "Show own nametag in F5 / Freelook", true));
    private final NumberSetting scale      = addSetting(new NumberSetting("Scale", "Nametag scale", 1.5, 0.5, 3.0));

    public Nametags() {
        super("Nametags", "Better nametags with extra info", Category.UI);
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (mc.world == null || mc.player == null) return;

        MatrixStack matrices = event.getMatrices();
        if (matrices == null) return;

        float tickDelta = event.getTickDelta();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos();

        // Get a fresh VCP — never call vcp.draw() ourselves; let the pipeline flush it.
        VertexConsumerProvider.Immediate vcp = mc.getBufferBuilders().getEntityVertexConsumers();

        for (PlayerEntity player : mc.world.getPlayers()) {
            boolean isSelf = (player == mc.player);

            if (isSelf) {
                if (!showSelf.get()) continue;
                if (!isCameraDetached()) continue;
            } else {
                if (player.isInvisible()) continue;
            }

            Vec3d pos = player.getLerpedPos(tickDelta);
            double x = pos.x - camPos.x;
            double y = pos.y - camPos.y;
            double z = pos.z - camPos.z;

            double dist = Math.sqrt(x * x + y * y + z * z);
            if (dist > 200) continue;

            // Adaptive scale: grow with distance so text stays readable
            float scaleVal = scale.get().floatValue() * 0.025f;
            float distScale = Math.max(1.0f, (float) dist * 0.12f);
            distScale = Math.min(distScale, 3.0f);
            if (isSelf) distScale = Math.max(distScale, 1.2f);
            float finalScale = scaleVal * distScale;

            String text = formatNametag(player);
            float tagY = (float) (y + player.getHeight() + (isSelf ? 0.5 : 0.3));

            matrices.push();
            matrices.translate(x, tagY, z);
            matrices.multiply(camera.getRotation());
            matrices.scale(-finalScale, -finalScale, finalScale);

            TextRenderer tr = mc.textRenderer;
            float halfW = tr.getWidth(text) / 2.0f;
            Matrix4f mat = matrices.peek().getPositionMatrix();
            int bg = mc.options.getTextBackgroundColor(0.25f);

            // See-through layer (visible through walls, dimmed)
            tr.draw(text, -halfW, 0, 0x55FFFFFF, false, mat, vcp,
                    TextRenderer.TextLayerType.SEE_THROUGH, bg,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE);

            // Normal layer (full brightness, proper depth)
            tr.draw(text, -halfW, 0, 0xFFFFFFFF, false, mat, vcp,
                    TextRenderer.TextLayerType.NORMAL, bg,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE);

            matrices.pop();
        }
    }

    /**
     * Camera is not first-person bound: F5, Freelook, or Freecam.
     */
    private boolean isCameraDetached() {
        if (!mc.options.getPerspective().isFirstPerson()) return true;

        Freelook freelook = Freelook.get();
        if (freelook != null && freelook.isEnabled()) return true;

        Freecam freecam = Freecam.get();
        if (freecam != null && freecam.isEnabled()) return true;

        return false;
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
