package com.foxyclient.module.misc;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;

/**
 * Inventory quality-of-life tweaks.
 */
public class InventoryTweaks extends Module {
    private final BoolSetting autoSort = addSetting(new BoolSetting("AutoSort", "Auto sort inventory", false));
    private final BoolSetting shiftDrop = addSetting(new BoolSetting("ShiftDrop", "Shift+click to drop stack", true));
    private final BoolSetting armorSwap = addSetting(new BoolSetting("ArmorSwap", "Quick armor swap", true));
    private final BoolSetting stealButton = addSetting(new BoolSetting("StealButton", "Steal/dump buttons in chests", true));

    public InventoryTweaks() {
        super("InventoryTweaks", "Inventory QoL tweaks", Category.MISC);
    }

    public boolean autoSort() { return isEnabled() && autoSort.get(); }
    public boolean shiftDrop() { return isEnabled() && shiftDrop.get(); }
    public boolean armorSwap() { return isEnabled() && armorSwap.get(); }
    public boolean stealButton() { return isEnabled() && stealButton.get(); }
}
