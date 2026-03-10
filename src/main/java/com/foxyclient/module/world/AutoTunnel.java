package com.foxyclient.module.world;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Automatically digs a tunnel in the direction you are facing.
 */
public class AutoTunnel extends Module {
    private final NumberSetting width = addSetting(new NumberSetting("Width", "Tunnel width", 3, 1, 5));
    private final NumberSetting height = addSetting(new NumberSetting("Height", "Tunnel height", 3, 2, 5));

    public AutoTunnel() {
        super("AutoTunnel", "Automatically digs tunnels", Category.WORLD);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        Direction dir = mc.player.getHorizontalFacing();
        BlockPos base = mc.player.getBlockPos().offset(dir);

        for (int h = 0; h < height.get(); h++) {
            for (int w = -(int)(width.get()/2); w <= width.get()/2; w++) {
                BlockPos pos = base.up(h).offset(dir.rotateYClockwise(), w);
                if (mc.world.getBlockState(pos).getHardness(mc.world, pos) != -1) {
                    mc.interactionManager.attackBlock(pos, Direction.UP);
                }
            }
        }
    }
}
