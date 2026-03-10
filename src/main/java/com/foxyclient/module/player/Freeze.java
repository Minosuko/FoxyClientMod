package com.foxyclient.module.player;
import com.foxyclient.module.Category; import com.foxyclient.module.Module;
import com.foxyclient.event.EventHandler; import com.foxyclient.event.events.TickEvent;
/** Freeze - Freeze player in place. */
public class Freeze extends Module {
    private double fx, fy, fz;
    public Freeze() { super("Freeze", "Freeze player position", Category.PLAYER); }
    @Override public void onEnable() { if (mc.player != null) { fx = mc.player.getX(); fy = mc.player.getY(); fz = mc.player.getZ(); } }
    @EventHandler public void onTick(TickEvent event) { if (nullCheck()) return; mc.player.setPosition(fx, fy, fz); mc.player.setVelocity(0, 0, 0); }
}
