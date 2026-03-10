package com.foxyclient.module.ui;

import com.foxyclient.FoxyClient;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;

/**
 * Extended configuration options for the client.
 */
public class ExtendedConfig extends Module {
    private final BoolSetting autoSave = addSetting(new BoolSetting("AutoSave", "Save config on toggle", true));
    private final BoolSetting notifications = addSetting(new BoolSetting("Notifications", "Toggle notifications", true));

    public ExtendedConfig() {
        super("ExtendedConfig", "Advanced config settings", Category.UI);
    }

    @Override
    public void onEnable() {
        if (autoSave.get()) {
            FoxyClient.INSTANCE.getModuleManager().saveConfig();
            info("Config saved.");
        }
    }

    @Override
    public void onDisable() {
        if (autoSave.get()) {
            FoxyClient.INSTANCE.getModuleManager().saveConfig();
        }
    }
}
