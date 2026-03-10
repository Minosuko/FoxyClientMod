package com.foxyclient.module.movement;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
/** FlyPlus - Enhanced flight with anti-kick and speed control. */
public class FlyPlus extends Module {
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Fly speed", 2.0, 0.5, 10.0));
    private int ticks = 0;
    public FlyPlus() { super("FlyPlus", "Enhanced flight", Category.MOVEMENT); }
    @EventHandler public void onTick(TickEvent event) {
        if (nullCheck()) return;
        mc.player.getAbilities().flying = true;
        mc.player.getAbilities().setFlySpeed(speed.get().floatValue() / 20f);
        if (++ticks % 30 == 0) mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 0.04, mc.player.getZ(), false, mc.player.horizontalCollision));
    }
    @Override public void onDisable() { if (mc.player != null && !mc.player.isCreative()) { mc.player.getAbilities().flying = false; mc.player.getAbilities().setFlySpeed(0.05f); } }
}
