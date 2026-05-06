package com.foxyclient.module.ui;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;

public class Nametags extends Module {
    public static Nametags INSTANCE;

    public final BoolSetting showLogo = addSetting(new BoolSetting("Show Logo", "Show FoxyClient logo", true));
    public final BoolSetting perspective = addSetting(new BoolSetting("Perspective", "Show through walls", true));
    public final BoolSetting hideSelf = addSetting(new BoolSetting("Hide Self", "Hide your own nametag", false));
    public final BoolSetting showOther = addSetting(new BoolSetting("Show Other", "Show other players' nametags", true));
    public final BoolSetting healthCount = addSetting(new BoolSetting("Health Count", "Show health on other players", false));
    public final BoolSetting distance = addSetting(new BoolSetting("Distance", "Show distance on other players", false));

    public Nametags() {
        super("Nametags", "Advanced player nametags", Category.UI);
        INSTANCE = this;
        // Default to enabled since it's hardcoded to work anyway
        setEnabled(true);
    }
}
