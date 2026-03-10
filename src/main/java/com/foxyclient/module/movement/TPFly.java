package com.foxyclient.module.movement;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
/** TPFly - Flight via teleportation packets. */
public class TPFly extends Module {
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "TP speed", 1.0, 0.1, 5.0));
    public TPFly() { super("TPFly", "Fly via teleport packets", Category.MOVEMENT); }
    @EventHandler public void onTick(TickEvent event) {
        if (nullCheck()) return;
        double forward = mc.player.input.playerInput.forward() ? 1 : (mc.player.input.playerInput.backward() ? -1 : 0);
        double sideways = mc.player.input.playerInput.left() ? 1 : (mc.player.input.playerInput.right() ? -1 : 0);

        if (forward == 0 && sideways == 0 && !mc.options.jumpKey.isPressed() && !mc.options.sneakKey.isPressed()) return;

        Vec3d velocity = new Vec3d(0, 0, 0);
        if (forward != 0 || sideways != 0) {
            float yaw = mc.player.getYaw();
            if (forward != 0) {
                if (sideways > 0) {
                    yaw += (forward > 0 ? -45 : 45);
                } else if (sideways < 0) {
                    yaw += (forward > 0 ? 45 : -45);
                }
                sideways = 0;
                if (forward > 0) {
                    forward = 1;
                } else if (forward < 0) {
                    forward = -1;
                }
            }
            double sin = Math.sin(Math.toRadians(yaw + 90));
            double cos = Math.cos(Math.toRadians(yaw + 90));
            velocity = new Vec3d(forward * cos + sideways * sin, 0, forward * sin - sideways * cos).normalize().multiply(speed.get());
        }

        if (mc.options.jumpKey.isPressed()) {
            velocity = velocity.add(0, speed.get(), 0);
        }
        if (mc.options.sneakKey.isPressed()) {
            velocity = velocity.add(0, -speed.get(), 0);
        }

        double nx = mc.player.getX() + velocity.x;
        double ny = mc.player.getY() + velocity.y;
        double nz = mc.player.getZ() + velocity.z;

        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(nx, ny, nz, false, mc.player.horizontalCollision));
        mc.player.setPosition(nx, ny, nz);
    }
}
