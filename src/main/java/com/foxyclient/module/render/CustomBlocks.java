package com.foxyclient.module.render;
import com.foxyclient.module.Category; import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
/** CustomBlocks - Custom block model rendering. */
public class CustomBlocks extends Module {
    private final BoolSetting glowOres = addSetting(new BoolSetting("GlowOres", "Make ores glow", true));
    public CustomBlocks() { super("CustomBlocks", "Custom block rendering", Category.RENDER); }
}
