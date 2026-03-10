package com.foxyclient.module.ui;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;

/**
 * Module to view shulker box contents in inventory.
 */
public class ShulkerView extends Module {
    public static ShulkerView INSTANCE;
    
    public final BoolSetting showEmpty = addSetting(new BoolSetting("ShowEmpty", "Show empty shulker boxes", false));

    public ShulkerView() {
        super("ShulkerView", "View shulker box contents in inventory", Category.UI);
        INSTANCE = this;
    }
}
