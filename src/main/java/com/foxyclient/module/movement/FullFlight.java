package com.foxyclient.module.movement;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

/** FullFlight - Creative-like flight with anti-kick. */
public class FullFlight extends Module {
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Fly speed", 2.0, 0.5, 10.0));
    private final BoolSetting antiKick = addSetting(new BoolSetting("AntiKick", "Prevent fly kick", true));
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Flight mode", "Vanilla", "Vanilla", "Packet", "Smooth"));
    private int tickCount = 0;

    public FullFlight() { super("FullFlight", "Flight with anti-kick", Category.MOVEMENT); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        tickCount++;
        float spd = speed.get().floatValue();

        mc.player.getAbilities().flying = true;
        mc.player.getAbilities().setFlySpeed(spd / 20f);

        if (antiKick.get() && tickCount % 40 == 0) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), mc.player.getY() - 0.04, mc.player.getZ(), false, mc.player.horizontalCollision));
        }
    }

    @Override public void onDisable() {
        if (mc.player != null && !mc.player.isCreative()) {
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().setFlySpeed(0.05f);
        }
    }
}
