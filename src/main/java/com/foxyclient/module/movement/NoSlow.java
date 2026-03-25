package com.foxyclient.module.movement;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.util.PlayerInput;
import com.foxyclient.setting.BoolSetting;

/**
 * Prevents slowdown while eating, drinking, or blocking.
 */
public class NoSlow extends Module {
    private final BoolSetting items = addSetting(new BoolSetting("Items", "No slow from items", true));
    private final BoolSetting soulSand = addSetting(new BoolSetting("SoulSand", "No slow from soul sand", true));
    private final BoolSetting honeyBlock = addSetting(new BoolSetting("Honey", "No slow from honey", true));
    private final BoolSetting webs = addSetting(new BoolSetting("Webs", "No slow from cobwebs", true));
    private final BoolSetting plusMode = addSetting(new BoolSetting("Plus", "Prevent all forms of slowdowns", false));

    public NoSlow() {
        super("NoSlow", "No slowdown from items/blocks", Category.MOVEMENT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (items.get() && mc.player.isUsingItem()) {
            // In 1.21.2+, we can't easily multiply the input in the record.
            // But we can override it if we have a way to set it.
            // For now, we'll assume the player is trying to move normally.
            // To simulate "no slow", we might need a mixin to Input.
        }
        if (plusMode.get()) {
            mc.player.setMovementSpeed(0.1f);
        }
    }

    public boolean shouldNoSlow() { return isEnabled() && items.get(); }
    public boolean noSoulSand() { return isEnabled() && soulSand.get(); }
    public boolean noHoney() { return isEnabled() && honeyBlock.get(); }
    public boolean noWebs() { return isEnabled() && webs.get(); }
}
