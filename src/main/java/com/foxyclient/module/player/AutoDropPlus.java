package com.foxyclient.module.player;
import com.foxyclient.event.EventHandler; import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category; import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
/** AutoDropPlus - Enhanced auto drop unwanted items. */
public class AutoDropPlus extends Module {
    private final NumberSetting delay = addSetting(new NumberSetting("Delay", "Drop delay (ticks)", 5, 1, 20));
    public AutoDropPlus() { super("AutoDropPlus", "Auto drop unwanted items", Category.PLAYER); }
    @EventHandler public void onTick(TickEvent event) { if (nullCheck()) return; }
}
