package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import java.util.UUID;

/**
 * FakePlayer - Spawns a fake player clone at your current position.
 * This is client-side only. Useful for testing combat modules (like AutoArmor, 
 * KillAura, etc.) or just seeing what your character looks like.
 */
public class FakePlayer extends Module {
    private final BoolSetting copyInventory = addSetting(new BoolSetting("CopyInventory", "Copy your armor and held items", true));

    private OtherClientPlayerEntity fakePlayer;
    private static final int FAKE_PLAYER_ID = -1337; // Negative ID to avoid conflicts

    public FakePlayer() {
        super("FakePlayer", "Spawn a client-side clone", Category.PLAYER);
    }

    @Override
    public void onEnable() {
        if (nullCheck()) {
            toggle();
            return;
        }

        // Create the fake player with our profile but a new UUID
        GameProfile profile = new GameProfile(UUID.randomUUID(), mc.player.getGameProfile().name(), mc.player.getGameProfile().properties());

        fakePlayer = new OtherClientPlayerEntity(mc.world, profile);
        
        // Copy position and rotation
        fakePlayer.copyPositionAndRotation(mc.player);
        fakePlayer.setHeadYaw(mc.player.getHeadYaw());
        fakePlayer.setBodyYaw(mc.player.getBodyYaw());
        fakePlayer.setYaw(mc.player.getYaw());
        fakePlayer.setPitch(mc.player.getPitch());

        // Copy inventory (armor, mainhand, offhand) if enabled
        if (copyInventory.get()) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                fakePlayer.equipStack(slot, mc.player.getEquippedStack(slot).copy());
            }
        }

        // Set health to match ours
        fakePlayer.setHealth(mc.player.getHealth());

        // Sync skin layers (DataTracker)
        fakePlayer.getDataTracker().set(com.foxyclient.mixin.PlayerLikeEntityAccessor.getPlayerModeCustomizationId(), mc.player.getDataTracker().get(com.foxyclient.mixin.PlayerLikeEntityAccessor.getPlayerModeCustomizationId()));

        // Give it a negative entity ID so we don't conflict with real entities
        fakePlayer.setId(FAKE_PLAYER_ID);

        // Spawn it in the client world
        mc.world.addEntity(fakePlayer);
        info("Fake player spawned at your position.");
    }

    @Override
    public void onDisable() {
        if (mc.world != null && fakePlayer != null) {
            mc.world.removeEntity(fakePlayer.getId(), net.minecraft.entity.Entity.RemovalReason.DISCARDED);
            info("Fake player removed.");
        }
        fakePlayer = null;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck() || fakePlayer == null) return;
        
        // Sync health if it changes so we can test damage/healing modules
        // but don't overwrite it if it's being damaged locally
        if (fakePlayer.getHealth() <= 0) {
            info("Fake player died!");
            toggle();
        }
    }
}
