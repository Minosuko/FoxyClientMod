package com.foxyclient.module.combat;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.setting.ModeSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
/** Hunt - Hunts and chases down nearby players aggressively. */
public class Hunt extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Hunt range", 32, 8, 64));
    private final ModeSetting priority = addSetting(new ModeSetting("Priority", "Target priority", "Closest", "Closest", "Lowest HP", "Farthest"));
    public Hunt() { super("Hunt", "Chase and kill nearby players", Category.COMBAT); }
    @EventHandler public void onTick(TickEvent event) {
        if (nullCheck()) return;
        LivingEntity target = null; double best = Double.MAX_VALUE;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof PlayerEntity p) || p == mc.player || !p.isAlive()) continue;
            double d = mc.player.distanceTo(p);
            if (d > range.get()) continue;
            double score = switch(priority.get()) { case "Lowest HP" -> p.getHealth(); case "Farthest" -> -d; default -> d; };
            if (score < best) { best = score; target = p; }
        }
        if (target != null) {
            com.foxyclient.FoxyClient.INSTANCE.getPathFinder().pathTo(target.getBlockPos());
            if (mc.player.distanceTo(target) <= 4) { mc.interactionManager.attackEntity(mc.player, target); mc.player.swingHand(Hand.MAIN_HAND); }
        }
    }
}
