package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.util.RenderUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.awt.*;

/**
 * Draws lines from crosshair to entities.
 */
public class Tracers extends Module {
    private final BoolSetting players = addSetting(new BoolSetting("Players", "Trace to players", true));
    private final BoolSetting mobs = addSetting(new BoolSetting("Mobs", "Trace to mobs", true));
    private final BoolSetting animals = addSetting(new BoolSetting("Animals", "Trace to animals", false));
    private final BoolSetting vehicles = addSetting(new BoolSetting("Vehicles", "Trace to minecarts/boats", false));
    private final BoolSetting projectiles = addSetting(new BoolSetting("Projectiles", "Trace to shulker bullets etc", false));
    private final BoolSetting crystals = addSetting(new BoolSetting("Crystals", "Trace to end crystals", false));
    private final BoolSetting others = addSetting(new BoolSetting("Others", "Trace to all other entities", false));

    public Tracers() {
        super("Tracers", "Draw lines to entities", Category.RENDER);
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;

            Color color = null;

            if (entity instanceof PlayerEntity && players.get()) {
                color = Color.RED;
            } else if (entity instanceof HostileEntity && mobs.get()) {
                color = Color.YELLOW;
            } else if (entity instanceof net.minecraft.entity.passive.AnimalEntity && animals.get()) {
                color = Color.GREEN;
            } else if (entity instanceof net.minecraft.entity.vehicle.VehicleEntity && vehicles.get()) {
                color = Color.ORANGE;
            } else if (entity instanceof net.minecraft.entity.projectile.ProjectileEntity && projectiles.get()) {
                color = Color.CYAN;
            } else if (entity instanceof net.minecraft.entity.decoration.EndCrystalEntity && crystals.get()) {
                color = Color.MAGENTA;
            } else if (!(entity instanceof PlayerEntity) && 
                       !(entity instanceof HostileEntity) && 
                       !(entity instanceof net.minecraft.entity.passive.AnimalEntity) &&
                       !(entity instanceof net.minecraft.entity.vehicle.VehicleEntity) &&
                       !(entity instanceof net.minecraft.entity.projectile.ProjectileEntity) &&
                       !(entity instanceof net.minecraft.entity.decoration.EndCrystalEntity) && 
                       others.get()) {
                color = Color.WHITE;
            }

            if (color != null) {
                RenderUtil.drawTracerLine(event.getMatrices(), entity, color, event.getTickDelta(), event.getVertexConsumers());
            }
        }
    }
}
