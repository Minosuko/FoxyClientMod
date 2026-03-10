package com.foxyclient.module.render;
import com.foxyclient.event.EventHandler; import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category; import com.foxyclient.module.Module;
import com.foxyclient.util.RenderUtil;
import net.minecraft.entity.Entity; import net.minecraft.entity.LivingEntity; import net.minecraft.entity.mob.MobEntity;
import java.awt.Color;
/** MobGearESP - Highlights mobs wearing gear/holding items. */
public class MobGearESP extends Module {
    public MobGearESP() { super("MobGearESP", "Highlight geared mobs", Category.RENDER); }
    @EventHandler public void onRender(RenderEvent event) {
        if (nullCheck()) return;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof MobEntity mob) || !mob.isAlive() || mc.player.distanceTo(mob) > 64) continue;
            boolean geared = false;
            for (var slot : net.minecraft.entity.EquipmentSlot.values()) {
                if (slot.isArmorSlot() && !mob.getEquippedStack(slot).isEmpty()) geared = true;
            }
            if (!mob.getMainHandStack().isEmpty()) geared = true;
            if (geared) RenderUtil.drawEntityBox(event.getMatrices(), mob, new Color(255,165,0), 2f, event.getTickDelta(), event.getVertexConsumers());
        }
    }
}
