package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ColorSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.setting.StringSetting;
import com.foxyclient.util.RenderUtil;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Hologram - Display floating text holograms above entities.
 * Supports custom text, colors, and positioning.
 */
public class Hologram extends Module {
    private final StringSetting text = addSetting(new StringSetting("Text", "Hologram text", "&7Hologram"));
    private final ColorSetting color = addSetting(new ColorSetting("Color", "Text color", new Color(255, 255, 255)));
    private final NumberSetting height = addSetting(new NumberSetting("Height", "Vertical offset", 2.5, 0.0, 10.0));
    private final BoolSetting players = addSetting(new BoolSetting("Players", "Show on players", true));
    private final BoolSetting mobs = addSetting(new BoolSetting("Mobs", "Show on mobs", false));
    private final BoolSetting animals = addSetting(new BoolSetting("Animals", "Show on animals", false));
    private final BoolSetting others = addSetting(new BoolSetting("Others", "Show on others", false));

    public Hologram() {
        super("Hologram", "Display floating text holograms", Category.RENDER);
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;

        for (Entity entity : mc.world.getEntities()) {
            if (!shouldRenderHologram(entity)) continue;

            Vec3d pos = entity.getEntityPos();
            double y = pos.y + entity.getHeight() + height.get();

            MatrixStack matrices = event.getMatrices();
            VertexConsumerProvider vcp = event.getVertexConsumers();

            // Render text
            renderText(matrices, vcp, entity, pos.x, y, pos.z);
        }
    }

    private boolean shouldRenderHologram(Entity entity) {
        if (entity == mc.player) return false;
        if (entity instanceof LivingEntity) {
            if (entity instanceof PlayerEntity) {
                return players.get();
            } else {
                return mobs.get();
            }
        } else {
            return animals.get();
        }
    }

    private void renderText(MatrixStack matrices, VertexConsumerProvider vcp, Entity entity, double x, double y, double z) {
        String rawText = text.get();
        String formattedText = formatText(rawText);

        // Convert to Text object
        Text textObj = Text.literal(formattedText);

        // Get text width
        int width = mc.textRenderer.getWidth(textObj);

        // Calculate centered position
        double xPos = x - width / 2.0;
        double yPos = y;
        double zPos = z;

        // Apply camera transformations
        matrices.push();
        matrices.translate(xPos, yPos, zPos);
        matrices.multiply(mc.gameRenderer.getCamera().getRotation());
        matrices.scale(-0.025f, -0.025f, 0.025f);

        // Render text
        float alpha = color.get().getAlpha() / 255.0f;
        mc.textRenderer.draw(textObj, 0, 0, color.get().getRGB(), false, matrices.peek().getPositionMatrix(), vcp, net.minecraft.client.font.TextRenderer.TextLayerType.SEE_THROUGH, 0, 0);

        matrices.pop();
    }

    private String formatText(String input) {
        // Simple color code replacement
        String output = input;
        output = output.replace("&0", "\u00A70");
        output = output.replace("&1", "\u00A71");
        output = output.replace("&2", "\u00A72");
        output = output.replace("&3", "\u00A73");
        output = output.replace("&4", "\u00A74");
        output = output.replace("&5", "\u00A75");
        output = output.replace("&6", "\u00A76");
        output = output.replace("&7", "\u00A77");
        output = output.replace("&8", "\u00A78");
        output = output.replace("&9", "\u00A79");
        output = output.replace("&a", "\u00A7a");
        output = output.replace("&b", "\u00A7b");
        output = output.replace("&c", "\u00A7c");
        output = output.replace("&d", "\u00A7d");
        output = output.replace("&e", "\u00A7e");
        output = output.replace("&f", "\u00A7f");
        output = output.replace("&k", "\u00A7k");
        output = output.replace("&l", "\u00A7l");
        output = output.replace("&m", "\u00A7m");
        output = output.replace("&n", "\u00A7n");
        output = output.replace("&o", "\u00A7o");
        output = output.replace("&r", "\u00A7r");
        return output;
    }
}
