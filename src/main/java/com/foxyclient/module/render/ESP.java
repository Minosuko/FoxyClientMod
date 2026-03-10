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
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

/**
 * Draws boxes around entities.
 */
public class ESP extends Module {
    private final BoolSetting players = addSetting(new BoolSetting("Players", "Show players", true));
    private final BoolSetting mobs = addSetting(new BoolSetting("Mobs", "Show hostile mobs", true));
    private final BoolSetting animals = addSetting(new BoolSetting("Animals", "Show animals", false));
    private final BoolSetting items = addSetting(new BoolSetting("Items", "Show items", false));
    private final BoolSetting vehicles = addSetting(new BoolSetting("Vehicles", "Show minecarts/boats", false));
    private final BoolSetting projectiles = addSetting(new BoolSetting("Projectiles", "Show arrows, pearls, etc", false));
    private final BoolSetting crystals = addSetting(new BoolSetting("Crystals", "Show end crystals", false));
    private final BoolSetting others = addSetting(new BoolSetting("Others", "Show all other entities", false));
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
            } else if (entity instanceof HostileEntity) {
                if (mobs.get()) color = mobColor.get();
            } else if (entity instanceof AnimalEntity) {
                if (animals.get()) color = Color.GREEN;
            } else if (entity instanceof ItemEntity) {
                if (items.get()) color = Color.CYAN;
            } else if (entity instanceof net.minecraft.entity.vehicle.VehicleEntity) {
                if (vehicles.get()) color = Color.ORANGE;
            } else if (entity instanceof net.minecraft.entity.projectile.ProjectileEntity) {
                if (projectiles.get()) color = Color.MAGENTA;
            } else if (entity instanceof net.minecraft.entity.decoration.EndCrystalEntity) {
                if (crystals.get()) color = Color.PINK;
            } else if (others.get()) {
                color = Color.WHITE;
            }

            if (color != null) {
                RenderUtil.drawEntityBox(event.getMatrices(), entity, color, lineWidth.get().floatValue(), event.getTickDelta(), event.getVertexConsumers());
            }
        }
    }
}
