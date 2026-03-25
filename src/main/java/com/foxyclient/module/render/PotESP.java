package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.ColorSetting;
import com.foxyclient.util.RenderUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;

import java.awt.*;

/**
 * Highlights thrown potions and lingering clouds.
 */
public class PotESP extends Module {
    private final ColorSetting potionColor = addSetting(new ColorSetting("PotionColor", "Color for thrown potions", new Color(255, 50, 255)));
    private final ColorSetting cloudColor = addSetting(new ColorSetting("CloudColor", "Color for effect clouds", new Color(255, 150, 255)));

    public PotESP() {
        super("PotESP", "Highlight thrown potions", Category.RENDER);
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PotionEntity) {
                RenderUtil.drawEntityBox(event.getMatrices(), entity, potionColor.get(), 2.0f, event.getTickDelta(), event.getVertexConsumers());
            } else if (entity instanceof AreaEffectCloudEntity) {
                RenderUtil.drawEntityBox(event.getMatrices(), entity, cloudColor.get(), 2.0f, event.getTickDelta(), event.getVertexConsumers());
            }
        }
    }
}
