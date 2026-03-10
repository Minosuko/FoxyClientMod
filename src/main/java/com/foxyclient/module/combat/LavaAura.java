package com.foxyclient.module.combat;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
/** LavaAura - Places lava on nearby enemies. */
public class LavaAura extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Range", 4, 2, 6));
    public LavaAura() { super("LavaAura", "Place lava on enemies", Category.COMBAT); }
    @EventHandler public void onTick(TickEvent event) {
        if (nullCheck()) return;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == net.minecraft.item.Items.LAVA_BUCKET) {
                for (Entity e : mc.world.getEntities()) {
                    if (!(e instanceof LivingEntity le) || le == mc.player) continue;
                    if (mc.player.distanceTo(le) <= range.get()) {
                        int old = mc.player.getInventory().selectedSlot;
                        mc.player.getInventory().selectedSlot = i;
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                        mc.player.getInventory().selectedSlot = old;
                        return;
                    }
                }
            }
        }
    }
}
