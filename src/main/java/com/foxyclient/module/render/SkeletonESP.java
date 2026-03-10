package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.util.RenderUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;

import java.awt.Color;

/** SkeletonESP - Renders skeleton wireframes on entities (JexClient port). */
public class SkeletonESP extends Module {
    public SkeletonESP() { super("SkeletonESP", "Render skeleton outlines on entities", Category.RENDER); }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || le == mc.player) continue;
            if (!le.isAlive() || mc.player.distanceTo(le) > 64) continue;
            
            // Render full hitbox instead of skeleton
            Color color = (le instanceof SkeletonEntity || le instanceof WitherSkeletonEntity)
                ? new Color(255, 100, 100) : new Color(255, 255, 255);
            RenderUtil.drawEntityBox(event.getMatrices(), le, color, 1.5f, event.getTickDelta(), event.getVertexConsumers());
        }
    }
}
