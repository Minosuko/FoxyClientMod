package com.foxyclient.module.render;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;

public class CapePhysics extends Module {
    public final NumberSetting gravity = addSetting(new NumberSetting("Gravity", "How much the cape hangs down", 0.0, -10.0, 10.0));
    public final NumberSetting intensity = addSetting(new NumberSetting("Intensity", "Multiplier for movement-based swing", 1.0, 0.0, 3.0));
    public final BoolSetting smooth = addSetting(new BoolSetting("Smooth", "Apply smoother transitions", true));

    public CapePhysics() {
        super("CapePhysics", "Highly customizable cape physics for 1.21.1", Category.RENDER);
    }
}
