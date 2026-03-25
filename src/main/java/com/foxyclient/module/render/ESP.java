package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ColorSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.RenderUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import com.foxyclient.setting.EntityListSetting;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

/**
 * Draws boxes around entities.
 */
public class ESP extends Module {
    private final BoolSetting players = addSetting(new BoolSetting("Players", "Show players", true));
    private final EntityListSetting targetedMobs = addSetting(new EntityListSetting("Targets", "Specific mobs to highlight"));
    private final BoolSetting items = addSetting(new BoolSetting("Items", "Show items", false));
    private final ColorSetting playerColor = addSetting(new ColorSetting("PlayerColor", "Player box color", new Color(255, 50, 50)));
    private final ColorSetting mobColor = addSetting(new ColorSetting("MobColor", "Mob box color", Color.YELLOW));
    private final NumberSetting lineWidth = addSetting(new NumberSetting("LineWidth", "Box line width", 2.0, 0.5, 5.0));

    public ESP() {
        super("ESP", "See entities through walls", Category.RENDER, GLFW.GLFW_KEY_X);
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;

            Color color = null;
            if (entity instanceof PlayerEntity) {
                if (players.get()) color = playerColor.get();
            } else if (targetedMobs.contains(entity.getType())) {
                color = mobColor.get();
            } else if (entity instanceof ItemEntity) {
                if (items.get()) color = Color.CYAN;
            }

            if (color != null) {
                RenderUtil.drawEntityBox(event.getMatrices(), entity, color, lineWidth.get().floatValue(), event.getTickDelta(), event.getVertexConsumers());
            }
        }
    }
}
