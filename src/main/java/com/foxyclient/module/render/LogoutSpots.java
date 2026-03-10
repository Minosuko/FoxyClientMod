package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.ColorSetting;
import com.foxyclient.util.RenderUtil;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shows where players logged out.
 */
public class LogoutSpots extends Module {
    private final ColorSetting color = addSetting(new ColorSetting("Color", "Logout spot color", new Color(255, 0, 255, 100)));
    private final Map<UUID, LogoutPos> spots = new HashMap<>();

    public LogoutSpots() {
        super("LogoutSpots", "Tracks player disconnect spots", Category.RENDER);
    }

    @EventHandler
    public void onReceivePacket(PacketEvent.Receive event) {
        if (nullCheck()) return;
        if (event.getPacket() instanceof PlayerListS2CPacket packet) {
            // Logic to detect player removal from world
            // In a real client, we'd check packet entries for REMOVE_PLAYER
        }
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        for (LogoutPos spot : spots.values()) {
            // Render a box at the spot
            // RenderUtil.drawBox(...)
        }
    }

    private static record LogoutPos(String name, Vec3d pos) {}
}
