package com.foxyclient.module.ui;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;

/**
 * Controls visibility of HUD elements.
 */
public class HUD extends Module {
    public final BoolSetting watermark = addSetting(new BoolSetting("Watermark", "Show client name", true));
    public final BoolSetting cheatList = addSetting(new BoolSetting("Cheat List", "Show active modules", true));
    public final BoolSetting fps = addSetting(new BoolSetting("FPS", "Show frames per second", true));
    public final BoolSetting ping = addSetting(new BoolSetting("Ping", "Show network latency", true));
    public final BoolSetting tps = addSetting(new BoolSetting("TPS", "Show server TPS", true));
    public final BoolSetting speed = addSetting(new BoolSetting("Speed", "Show movement speed", true));
    public final BoolSetting coordinates = addSetting(new BoolSetting("Coordinates", "Show XYZ coordinates", true));
    public final BoolSetting server = addSetting(new BoolSetting("Server", "Show server address", true));
    public final BoolSetting facing = addSetting(new BoolSetting("Facing", "Show cardinal direction", true));

    public HUD() {
        super("HUD", "Configure HUD elements", Category.UI);
        setEnabled(true);
    }
}
