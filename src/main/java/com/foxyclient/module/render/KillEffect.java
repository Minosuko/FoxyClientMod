package com.foxyclient.module.render;
import com.foxyclient.module.Category; import com.foxyclient.module.Module;
import com.foxyclient.setting.ModeSetting;
/** KillEffect - Visual effects on entity kill. */
public class KillEffect extends Module {
    private final ModeSetting effect = addSetting(new ModeSetting("Effect", "Kill effect", "Lightning", "Lightning", "Particles", "Totem", "None"));
    public KillEffect() { super("KillEffect", "Visual effects on kills", Category.RENDER); }
}
