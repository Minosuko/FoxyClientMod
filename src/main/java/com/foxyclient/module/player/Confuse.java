package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

/** Confuse - Walk in circles around target to confuse them. */
public class Confuse extends Module {
    private final NumberSetting radius = addSetting(new NumberSetting("Radius", "Circle radius", 3, 1, 6));
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Circle speed", 5, 1, 15));
    private double angle = 0;

    public Confuse() { super("Confuse", "Circle around target to confuse", Category.PLAYER); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck() || mc.targetedEntity == null) return;
        Entity target = mc.targetedEntity;
        angle += Math.toRadians(speed.get());
        double r = radius.get();
        double x = target.getX() + Math.cos(angle) * r;
        double z = target.getZ() + Math.sin(angle) * r;
        mc.player.setPosition(x, mc.player.getY(), z);
    }
}
