package com.foxyclient.module.render;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

/**
 * Free camera mode - detach the camera and fly around without moving the player.
 * 
 * This implementation does NOT spawn any entity into the world. Instead it
 * tracks camera position / rotation as plain fields and overrides
 * {@code Camera.update()} via {@code MixinCamera} to use those values.
 */
public class Freecam extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Camera speed", 1.0, 0.1, 5.0));
    public final BoolSetting noclip = addSetting(new BoolSetting("Noclip", "Pass through blocks", true));
    private final BoolSetting showPlayer = addSetting(new BoolSetting("ShowPlayer", "Show your original body", true));

    // Camera state
    private double camX, camY, camZ;
    private float camYaw, camPitch;

    // Saved player state for restoration
    private double savedX, savedY, savedZ;
    private float savedYaw, savedPitch;

    // Delta-time tracking
    private long lastMoveTime;

    public Freecam() {
        super("Freecam", "Detach camera from player", Category.RENDER);
    }

    // ── Lifecycle ───────────────────────────────────────────────────────

    @Override
    public void onEnable() {
        if (nullCheck()) return;

        // Snapshot current player position / rotation into camera state
        camX = mc.player.getX();
        camY = mc.player.getY() + mc.player.getStandingEyeHeight();
        camZ = mc.player.getZ();
        camYaw = mc.player.getYaw();
        camPitch = mc.player.getPitch();

        savedX = mc.player.getX();
        savedY = mc.player.getY();
        savedZ = mc.player.getZ();
        savedYaw = mc.player.getYaw();
        savedPitch = mc.player.getPitch();

        lastMoveTime = System.nanoTime();
    }

    @Override
    public void onDisable() {
        if (nullCheck()) return;
        // Restore player rotation so the view doesn't snap to where the freecam was
        mc.player.setYaw(savedYaw);
        mc.player.setPitch(savedPitch);
    }

    // ── Mouse rotation (called from MixinEntity.changeLookDirection) ───

    public void updateRotation(double deltaX, double deltaY) {
        camYaw += (float) (deltaX * 0.15);
        camPitch += (float) (deltaY * 0.15);
        camPitch = Math.max(-90f, Math.min(90f, camPitch));
    }

    // ── Tick: freeze the real player ────────────────────────────────────

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        com.foxyclient.pathfinding.PathFinder pf = FoxyClient.INSTANCE.getPathFinder();
        if (pf != null && pf.isAnyProcessActive()) {
            return; // Let FoxyBot handle movement
        }

        // Zero velocity so the player body stays perfectly still
        mc.player.setVelocity(Vec3d.ZERO);
        if (mc.player.input != null) {
            mc.player.input.playerInput =
                new net.minecraft.util.PlayerInput(false, false, false, false, false, false, false);
        }
    }

    // ── Per-frame movement (before Camera.update) ──────────────────────

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;

        long now = System.nanoTime();
        double delta = (now - lastMoveTime) / 1_000_000_000.0;
        lastMoveTime = now;

        double spd = speed.get() * 20.0 * delta;

        Vec3d velocity = Vec3d.ZERO;
        Vec3d forward = Vec3d.fromPolar(camPitch, camYaw);
        Vec3d right   = Vec3d.fromPolar(0, camYaw + 90);
        Vec3d up      = new Vec3d(0, 1, 0);

        if (mc.options.forwardKey.isPressed()) velocity = velocity.add(forward);
        if (mc.options.backKey.isPressed())    velocity = velocity.subtract(forward);
        if (mc.options.leftKey.isPressed())    velocity = velocity.subtract(right);
        if (mc.options.rightKey.isPressed())   velocity = velocity.add(right);
        if (mc.options.jumpKey.isPressed())    velocity = velocity.add(up);
        if (mc.options.sneakKey.isPressed())   velocity = velocity.subtract(up);

        if (velocity.lengthSquared() > 0) {
            velocity = velocity.normalize().multiply(spd);
        }

        camX += velocity.x;
        camY += velocity.y;
        camZ += velocity.z;
    }

    // ── Block outgoing movement packets ────────────────────────────────

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (event.getPacket() instanceof PlayerMoveC2SPacket) {
            com.foxyclient.pathfinding.PathFinder pf = FoxyClient.INSTANCE.getPathFinder();
            if (pf != null && pf.isAnyProcessActive()) {
                return; // Let FoxyBot send movement packets
            }
            event.cancel();
        }
    }

    // ── Accessors for MixinCamera / MixinEntityRenderer ────────────────

    public double getCamX()    { return camX; }
    public double getCamY()    { return camY; }
    public double getCamZ()    { return camZ; }
    public float  getCamYaw()  { return camYaw; }
    public float  getCamPitch(){ return camPitch; }

    public Vec3d getCamPos() {
        return new Vec3d(camX, camY, camZ);
    }

    public boolean shouldHidePlayer() {
        return isEnabled() && !showPlayer.get();
    }

    public static Freecam get() {
        return FoxyClient.INSTANCE.getModuleManager().getModule(Freecam.class);
    }
}
