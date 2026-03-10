package com.foxyclient.module.ui;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.Render2DEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Displays information about your current combat target.
 */
public class CombatHUD extends Module {
    private final BoolSetting name = addSetting(new BoolSetting("Name", "Show target name", true));
    private final BoolSetting health = addSetting(new BoolSetting("Health", "Show target health", true));
    private final BoolSetting distance = addSetting(new BoolSetting("Distance", "Show target distance", true));

    public CombatHUD() {
        super("CombatHUD", "Shows target info", Category.UI);
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (nullCheck()) return;

        PlayerEntity target = findNearestTarget();
        if (target == null) return;

        int x = mc.getWindow().getScaledWidth() / 2 + 10;
        int y = mc.getWindow().getScaledHeight() / 2 + 10;

        if (name.get()) {
            event.getContext().drawTextWithShadow(mc.textRenderer, "Target: §f" + target.getName().getString(), x, y, 0xFFFFFFFF);
            y += 10;
        }
        if (health.get()) {
            String hp = String.format("HP: §a%.1f", target.getHealth() + target.getAbsorptionAmount());
            event.getContext().drawTextWithShadow(mc.textRenderer, hp, x, y, 0xFFFFFFFF);
            y += 10;
        }
        if (distance.get()) {
            String dist = String.format("Dist: §e%.1f", mc.player.distanceTo(target));
            event.getContext().drawTextWithShadow(mc.textRenderer, dist, x, y, 0xFFFFFFFF);
        }
    }

    private PlayerEntity findNearestTarget() {
        PlayerEntity nearest = null;
        double dist = Double.MAX_VALUE;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) continue;
            double d = mc.player.distanceTo(player);
            if (d < dist && d < 20) {
                dist = d;
                nearest = player;
            }
        }
        return nearest;
    }
}
