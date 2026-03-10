package com.foxyclient.module.render;
import com.foxyclient.event.EventHandler; import com.foxyclient.event.events.RenderEvent; import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category; import com.foxyclient.module.Module;
import com.foxyclient.util.RenderUtil;
import net.minecraft.entity.Entity; import net.minecraft.entity.LivingEntity; import net.minecraft.entity.player.PlayerEntity;
import java.awt.Color;
/** EyeFinder - Shows where players are looking. */
public class EyeFinder extends Module {
    public EyeFinder() { super("EyeFinder", "Show where players look", Category.RENDER); }
    @EventHandler public void onRender(RenderEvent event) {
        if (nullCheck()) return;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof PlayerEntity p) || p == mc.player || mc.player.distanceTo(p) > 64) continue;
            var look = p.getRotationVec(event.getTickDelta());
            var eye = p.getEyePos();
            var target = eye.add(look.multiply(10));
            RenderUtil.drawBlockBox(event.getMatrices(), net.minecraft.util.math.BlockPos.ofFloored(target), new Color(255, 0, 0), 1.5f, event.getVertexConsumers());
        }
    }
}
