package com.foxyclient.module.render;
import com.foxyclient.event.EventHandler; import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category; import com.foxyclient.module.Module;
import com.foxyclient.util.RenderUtil;
import net.minecraft.entity.Entity; import net.minecraft.entity.decoration.ItemFrameEntity;
import java.awt.Color;
/** ItemFrameESP - Highlights item frames. */
public class ItemFrameESP extends Module {
    public ItemFrameESP() { super("ItemFrameESP", "Highlight item frames", Category.RENDER); }
    @EventHandler public void onRender(RenderEvent event) {
        if (nullCheck()) return;
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof ItemFrameEntity f && mc.player.distanceTo(f) <= 64) {
                Color c = f.getHeldItemStack().isEmpty() ? new Color(100,100,100) : new Color(255,215,0);
                RenderUtil.drawEntityBox(event.getMatrices(), f, c, 1.0f, event.getTickDelta(), event.getVertexConsumers());
            }
        }
    }
}
