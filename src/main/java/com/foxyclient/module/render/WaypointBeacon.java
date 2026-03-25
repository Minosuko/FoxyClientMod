package com.foxyclient.module.render;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.ColorSetting;
import com.foxyclient.util.RenderUtil;
import com.foxyclient.util.WaypointManager.Waypoint;
import net.minecraft.util.math.Box;

import java.awt.Color;

public class WaypointBeacon extends Module {

    private final ColorSetting beamColor = addSetting(new ColorSetting("Color", "Beacon beam color", new Color(0, 255, 200, 100)));

    public WaypointBeacon() {
        super("WaypointBeacon", "Draws a beacon beam at waypoints", Category.RENDER);
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;

        String currentDim = mc.world.getRegistryKey().getValue().toString();
        Color c = beamColor.get();
        float alpha = 0.3f; // Enforce a translucent look for the beam core

        for (Waypoint wp : FoxyClient.INSTANCE.getWaypointManager().getAll()) {
            if (!wp.dimension().equals(currentDim)) continue;

            Box box = new Box(wp.x() + 0.3, wp.y(), wp.z() + 0.3, wp.x() + 0.7, 320, wp.z() + 0.7);
            
            RenderUtil.drawBox(event.getMatrices(), event.getVertexConsumers(), box, c, alpha);
            
            // Render the waypoint name floating just above the target coordinate
            RenderUtil.drawNametag(event.getMatrices(), event.getVertexConsumers(), wp.name(), wp.x() + 0.5, wp.y() + 1.5, wp.z() + 0.5, 0.035f);
        }
    }
}
