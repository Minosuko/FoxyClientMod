package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Locale;

/**
 * Renders the explosion timer above primed TNTs.
 */
public class TntCountdown extends Module {
    private final NumberSetting scale = addSetting(new NumberSetting("Scale", "Countdown text scale", 1.5, 0.5, 3.0));

    public TntCountdown() {
        super("TntCountdown", "Shows fuse time on ignited TNT", Category.RENDER);
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (mc.world == null || mc.player == null) return;
        MatrixStack matrices = event.getMatrices();
        if (matrices == null) return;

        float tickDelta = event.getTickDelta();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos();
        VertexConsumerProvider.Immediate vcp = mc.getBufferBuilders().getEntityVertexConsumers();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof TntEntity tnt) {
                int fuse = tnt.getFuse();
                double seconds = fuse / 20.0;
                
                String text = String.format(Locale.US, "%.1fs", seconds);

                Vec3d pos = tnt.getLerpedPos(tickDelta);
                double x = pos.x - camPos.x;
                double y = pos.y - camPos.y + tnt.getHeight() + 0.5; // Above the TNT
                double z = pos.z - camPos.z;

                double dist = Math.sqrt(x * x + y * y + z * z);
                if (dist > 200) continue;

                float scaleVal = scale.get().floatValue() * 0.025f;
                float distScale = Math.max(1.0f, (float) dist * 0.12f);
                distScale = Math.min(distScale, 3.0f);
                float finalScale = scaleVal * distScale;

                matrices.push();
                matrices.translate(x, y, z);
                matrices.multiply(camera.getRotation());
                matrices.scale(-finalScale, -finalScale, finalScale);

                Matrix4f mat = matrices.peek().getPositionMatrix();
                TextRenderer tr = mc.textRenderer;
                float halfW = tr.getWidth(text) / 2.0f;
                int bg = mc.options.getTextBackgroundColor(0.25f);

                int color = 0xFFFFFFFF; // White
                if (seconds <= 1.5) color = 0xFFFF5555; // Red
                else if (seconds <= 3.0) color = 0xFFFFAA00; // Orange

                // See-through layer
                tr.draw(text, -halfW, 0, (color & 0x00FFFFFF) | 0x55000000, false, mat, vcp,
                        TextRenderer.TextLayerType.SEE_THROUGH, bg,
                        LightmapTextureManager.MAX_LIGHT_COORDINATE);

                // Normal layer
                tr.draw(text, -halfW, 0, color, false, mat, vcp,
                        TextRenderer.TextLayerType.NORMAL, bg,
                        LightmapTextureManager.MAX_LIGHT_COORDINATE);

                matrices.pop();
            }
        }
    }
}
