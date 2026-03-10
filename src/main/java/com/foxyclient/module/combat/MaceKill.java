package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * MaceKill - Exploits the mace's fall damage multiplier by spoofing vertical movement.
 *
 * The mace deals bonus damage based on fall distance. This module spoofs the player's
 * position to the server to simulate a large fall, then attacks the target for massive damage.
 *
 * Modes:
 * - Spoof: Sends fake position packets up then down to simulate fall distance (no actual movement)
 * - Jump: Actually jumps and auto-attacks on the way down
 * - Auto: Combines both — jumps if possible, spoofs extra fall distance for max damage
 */
public class MaceKill extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Attack method", "Spoof", "Spoof", "Jump", "Auto"));
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Target range", 4.5, 2.0, 6.0));
    private final NumberSetting spoofHeight = addSetting(new NumberSetting("Spoof Height", "Fake fall height for spoof mode", 50.0, 5.0, 200.0));
    private final NumberSetting spoofPackets = addSetting(new NumberSetting("Packets", "Position packets to send for spoof", 5.0, 1.0, 20.0));
    private final BoolSetting autoSwitch = addSetting(new BoolSetting("AutoSwitch", "Switch to mace automatically", true));
    private final BoolSetting playersOnly = addSetting(new BoolSetting("PlayersOnly", "Only target players", false));
    private final NumberSetting cooldown = addSetting(new NumberSetting("Cooldown", "Ticks between attacks", 20, 5, 100));
    private final BoolSetting autoJump = addSetting(new BoolSetting("AutoJump", "Auto-jump in Jump/Auto mode", true));

    private int ticksSinceAttack = 0;
    private boolean jumping = false;
    private int prevSlot = -1;

    public MaceKill() {
        super("MaceKill", "Exploit mace fall damage for massive hits", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        if (prevSlot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = prevSlot;
            prevSlot = -1;
        }
        jumping = false;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        ticksSinceAttack++;

        // Find mace in hotbar
        int maceSlot = findMace();
        if (maceSlot == -1) return;

        // Find target
        LivingEntity target = findTarget();
        if (target == null) {
            jumping = false;
            return;
        }

        String currentMode = mode.get();

        // Jump mode: actually jump and attack on descent
        if (currentMode.equals("Jump") || currentMode.equals("Auto")) {
            if (mc.player.isOnGround() && autoJump.get() && ticksSinceAttack >= cooldown.get().intValue()) {
                mc.player.jump();
                jumping = true;
            }

            if (jumping && mc.player.getVelocity().y < -0.1) {
                // Falling down — attack now
                switchToMace(maceSlot);
                attack(target);
                ticksSinceAttack = 0;
                jumping = false;
                return;
            }

            // In Auto mode, if we can't jump (already airborne), use spoof
            if (currentMode.equals("Auto") && !mc.player.isOnGround() && !jumping) {
                // Fall through to spoof logic
            } else {
                return;
            }
        }

        // Spoof mode: send fake position packets to simulate fall
        if (ticksSinceAttack < cooldown.get().intValue()) return;

        switchToMace(maceSlot);

        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        double height = spoofHeight.get();
        int packets = spoofPackets.get().intValue();

        // Send packets going UP (the server tracks position changes)
        for (int i = 1; i <= packets; i++) {
            double fakeY = py + (height * i / packets);
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                px, fakeY, pz, yaw, pitch, false, false));
        }

        // Send packets coming DOWN (to accumulate fall distance on the server)
        for (int i = packets; i >= 0; i--) {
            double fakeY = py + (height * i / packets);
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                px, fakeY, pz, yaw, pitch, false, false));
        }

        // Final ground packet at current position
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
            px, py, pz, yaw, pitch, true, false));

        // Attack
        attack(target);
        ticksSinceAttack = 0;
    }

    private void attack(LivingEntity target) {
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

        // Restore slot after attack
        if (prevSlot != -1 && autoSwitch.get()) {
            mc.player.getInventory().selectedSlot = prevSlot;
            prevSlot = -1;
        }
    }

    private void switchToMace(int slot) {
        if (mc.player.getInventory().selectedSlot != slot && autoSwitch.get()) {
            prevSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = slot;
        }
    }

    private int findMace() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.MACE) {
                return i;
            }
        }
        return -1;
    }

    private LivingEntity findTarget() {
        double maxRange = range.get();
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity == mc.player) continue;
            if (!entity.isAlive()) continue;
            if (playersOnly.get() && !(entity instanceof PlayerEntity)) continue;

            double dist = mc.player.squaredDistanceTo(entity);
            if (dist > maxRange * maxRange) continue;

            if (dist < bestDist) {
                bestDist = dist;
                best = living;
            }
        }
        return best;
    }
}
