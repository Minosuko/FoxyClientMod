package com.foxyclient.module.combat;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
/** TriggerBot - Automatically attacks the entity you're looking at. */
public class TriggerBot extends Module {
    private final NumberSetting delay = addSetting(new NumberSetting("Delay", "Attack delay (ticks)", 0, 0, 10));
    private int timer = 0;
    public TriggerBot() { super("TriggerBot", "Auto attack crosshair target", Category.COMBAT); }
    @EventHandler public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (timer > 0) { timer--; return; }
        if (mc.targetedEntity instanceof LivingEntity le && le.isAlive()) {
            if (mc.player.getAttackCooldownProgress(0.5f) >= 1.0f) {
                mc.interactionManager.attackEntity(mc.player, le);
                mc.player.swingHand(Hand.MAIN_HAND);
                timer = delay.get().intValue();
            }
        }
    }
}
