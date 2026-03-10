package com.foxyclient.seedcracker.render;

import com.foxyclient.util.RenderLayers;
import net.minecraft.client.render.RenderLayer;

public class NoDepthLayer {

    public static RenderLayer getLayer() {
        return RenderLayers.getBypassTranslucent();
    }

}
