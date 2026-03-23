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
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;

import java.awt.*;

/**
 * CollectibleESP - Highlight collectible items.
 */
public class CollectibleESP extends Module {
    private final BoolSetting items = addSetting(new BoolSetting("Items", "Show dropped items", true));
    private final ColorSetting itemColor = addSetting(new ColorSetting("ItemColor", "Color for dropped items", new Color(0, 255, 255)));

    private final BoolSetting xpOrbs = addSetting(new BoolSetting("XPOrbs", "Show experience orbs", true));
    private final ColorSetting xpColor = addSetting(new ColorSetting("XPColor", "Color for XP orbs", new Color(0, 255, 0)));

    private final BoolSetting arrows = addSetting(new BoolSetting("Arrows", "Show arrows", true));
    private final ColorSetting arrowColor = addSetting(new ColorSetting("ArrowColor", "Color for arrows", new Color(255, 255, 0)));

    private final BoolSetting tracers = addSetting(new BoolSetting("Tracers", "Draw lines to collectibles", false));
    private final NumberSetting lineWidth = addSetting(new NumberSetting("LineWidth", "Box line width", 2.0, 0.5, 5.0));

    public CollectibleESP() {
        super("CollectibleESP", "Highlight collectible items", Category.RENDER);
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;

            Color color = null;

            if (entity instanceof ItemEntity && items.get()) {
                color = itemColor.get();
            } else if (entity instanceof ExperienceOrbEntity && xpOrbs.get()) {
                color = xpColor.get();
            } else if (entity instanceof PersistentProjectileEntity && arrows.get()) {
                color = arrowColor.get();
            }

            if (color != null) {
                RenderUtil.drawEntityBox(event.getMatrices(), entity, color, lineWidth.get().floatValue(), event.getTickDelta(), event.getVertexConsumers());
                
                if (tracers.get()) {
                    RenderUtil.drawTracerLine(event.getMatrices(), entity, color, event.getTickDelta(), event.getVertexConsumers());
                }
            }
        }
    }
}
