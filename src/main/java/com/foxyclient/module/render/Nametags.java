package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.RenderUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Renders custom nametags with additional info.
 */
public class Nametags extends Module {
    private final BoolSetting showHealth = addSetting(new BoolSetting("Health", "Show health", true));
    private final BoolSetting showPing = addSetting(new BoolSetting("Ping", "Show ping", true));
    private final BoolSetting showDistance = addSetting(new BoolSetting("Distance", "Show distance", true));
    private final NumberSetting scale = addSetting(new NumberSetting("Scale", "Nametag scale", 1.5, 0.5, 3.0));

    public Nametags() {
        super("Nametags", "Better nametags with extra info", Category.RENDER);
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player && mc.options.getPerspective().isFirstPerson()) continue;
            if (player.isSpectator() || player.isInvisible()) continue;
            if (mc.player.distanceTo(player) > 200) continue; // max render dist

            double x = MathHelper.lerp(event.getTickDelta(), player.lastRenderX, player.getX());
            double y = MathHelper.lerp(event.getTickDelta(), player.lastRenderY, player.getY()) + player.getHeight() + 0.5;
            double z = MathHelper.lerp(event.getTickDelta(), player.lastRenderZ, player.getZ());

            String text = formatNametag(player);
            float scaleValue = scale.get().floatValue() * 0.02666667f; 
            
            // FoxyRenderer text rendering
            RenderUtil.drawNametag(event.getMatrices(), text, x, y, z, scaleValue);
        }
    }

    public boolean shouldRenderNametag() {
        return isEnabled();
    }

    public String formatNametag(PlayerEntity player) {
        StringBuilder sb = new StringBuilder();
        sb.append(player.getName().getString());
        if (showHealth.get()) {
            int health = (int) (player.getHealth() + player.getAbsorptionAmount());
            String color = health > 15 ? "§a" : health > 10 ? "§e" : health > 5 ? "§6" : "§c";
            sb.append(" ").append(color).append(health).append("§f❤");
        }
        if (showPing.get() && mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerListEntry(player.getUuid()) != null) {
            int ping = mc.getNetworkHandler().getPlayerListEntry(player.getUuid()).getLatency();
            sb.append(" §7[").append(ping).append("ms]");
        }
        if (showDistance.get() && mc.player != null) {
            int dist = (int) mc.player.distanceTo(player);
            sb.append(" §7[").append(dist).append("m]");
        }
        return sb.toString();
    }
}
