package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.ModeSetting;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * Makes every hit a critical hit by spoofing falling or adding small jumps.
 */
public class Criticals extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Critical method", "Packet", "Packet", "MiniJump"));

    public Criticals() {
        super("Criticals", "All hits become critical hits", Category.COMBAT);
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (nullCheck() || !isEnabled()) return;

        if (event.getPacket() instanceof PlayerInteractEntityC2SPacket packet) {
            boolean[] isAttack = {false};
            packet.handle(new PlayerInteractEntityC2SPacket.Handler() {
                @Override public void interact(Hand hand) {}
                @Override public void interactAt(Hand hand, Vec3d pos) {}
                @Override public void attack() { isAttack[0] = true; }
            });

            if (isAttack[0]) {
                if (mode.get().equals("Packet")) {
                    if (mc.player.isOnGround() && !mc.player.isSubmergedInWater() && !mc.player.isInLava() && !mc.player.isClimbing()) {
                        sendPacket(0.0625, false);
                        sendPacket(0.01, false);
                        sendPacket(0.0, false);
                    }
                } else if (mode.get().equals("MiniJump")) {
                    if (mc.player.isOnGround()) {
                        mc.player.addVelocity(0, 0.1, 0);
                    }
                }
            }
        }
    }

    private void sendPacket(double height, boolean onGround) {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + height, z, onGround, false));
    }

    public boolean shouldCrit() {
        if (!isEnabled() || nullCheck()) return false;
        return mc.player.isOnGround();
    }

    public String getMode() {
        return mode.get();
    }
}
