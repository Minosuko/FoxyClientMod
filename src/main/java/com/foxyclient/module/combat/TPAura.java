package com.foxyclient.module.combat;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
/** TPAura - Teleports to and attacks nearby entities. */
public class TPAura extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("Range", "TP range", 16, 4, 64));
    public TPAura() { super("TPAura", "Teleport and attack entities", Category.COMBAT); }
    @EventHandler public void onTick(TickEvent event) {
        if (nullCheck()) return;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || le == mc.player || !le.isAlive()) continue;
            if (mc.player.distanceTo(le) > range.get()) continue;
            mc.player.setPosition(le.getX(), le.getY(), le.getZ());
            mc.interactionManager.attackEntity(mc.player, le);
            mc.player.swingHand(Hand.MAIN_HAND);
            return;
        }
    }
}
