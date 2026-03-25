package com.foxyclient.module.render;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;

public class ItemPhysics extends Module {
    public final NumberSetting scale = addSetting(new NumberSetting("Scale", "Item scale", 1.0, 0.5, 2.0));

    public ItemPhysics() {
        super("ItemPhysics", "Makes items lie flat on the ground", Category.RENDER);
    }
    
    public static ItemPhysics get() {
        if (com.foxyclient.FoxyClient.INSTANCE != null) {
            return com.foxyclient.FoxyClient.INSTANCE.getModuleManager().getModule(ItemPhysics.class);
        }
        return null;
    }
}
