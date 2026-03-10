package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

/** TreeAura - Automatically attacks mobs hiding behind trees or near logs. */
public class TreeAura extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Attack range", 4.5, 2, 6));

    public TreeAura() { super("TreeAura", "Attack entities near trees", Category.COMBAT); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || le == mc.player || !le.isAlive()) continue;
            if (mc.player.distanceTo(le) > range.get()) continue;
            if (isNearTree(le.getBlockPos())) {
                mc.interactionManager.attackEntity(mc.player, le);
                mc.player.swingHand(Hand.MAIN_HAND);
                return;
            }
        }
    }

    private boolean isNearTree(BlockPos pos) {
        for (int x = -2; x <= 2; x++)
            for (int y = -1; y <= 3; y++)
                for (int z = -2; z <= 2; z++)
                    if (mc.world.getBlockState(pos.add(x, y, z)).isIn(BlockTags.LOGS)) return true;
        return false;
    }
}
