package com.foxyclient.module.misc;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;

/**
 * InteractionMenu - Adds quick-action menu on targeted players.
 * When looking at a player and the module is active, provides quick
 * actions like messaging, teleporting, dueling, and friending via
 * chat commands. Actions are triggered via keybind or toggle.
 */
public class InteractionMenu extends Module {
    private final ModeSetting action = addSetting(new ModeSetting("Action", "Quick action to perform",
        "Message", "Message", "TPA", "Duel", "Friend", "Profile", "Report"));
    private final BoolSetting autoExecute = addSetting(new BoolSetting("AutoExecute", "Execute action on toggle", true));
    private final BoolSetting showInfo = addSetting(new BoolSetting("ShowInfo", "Show player info", true));

    public InteractionMenu() {
        super("InteractionMenu", "Right-click menu on players", Category.MISC);
    }

    @Override
    public void onEnable() {
        if (nullCheck()) {
            toggle();
            return;
        }

        if (autoExecute.get()) {
            executeAction();
            toggle();
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        // Show info about looked-at player
        if (showInfo.get() && mc.crosshairTarget instanceof EntityHitResult hitResult) {
            Entity target = hitResult.getEntity();
            if (target instanceof PlayerEntity player) {
                // Info is shown via the HUD; this just prepares data
            }
        }
    }

    private void executeAction() {
        if (!(mc.crosshairTarget instanceof EntityHitResult hitResult)) {
            error("You are not looking at an entity.");
            return;
        }

        Entity target = hitResult.getEntity();
        if (!(target instanceof PlayerEntity player)) {
            error("Target is not a player.");
            return;
        }

        String name = player.getName().getString();

        switch (action.get()) {
            case "Message" -> {
                mc.player.networkHandler.sendChatCommand("msg " + name + " ");
                info("§aOpened DM with §f" + name);
            }
            case "TPA" -> {
                mc.player.networkHandler.sendChatCommand("tpa " + name);
                info("§aSent TPA request to §f" + name);
            }
            case "Duel" -> {
                mc.player.networkHandler.sendChatCommand("duel " + name);
                info("§aSent duel request to §f" + name);
            }
            case "Friend" -> {
                mc.player.networkHandler.sendChatCommand("friend add " + name);
                info("§aSent friend request to §f" + name);
            }
            case "Profile" -> {
                info("§bPlayer: §f" + name);
                info("§bHealth: §f" + String.format("%.1f", player.getHealth()));
                info("§bDistance: §f" + String.format("%.1f", mc.player.distanceTo(player)) + " blocks");
                info("§bArmor: §f" + player.getArmor());
            }
            case "Report" -> {
                mc.player.networkHandler.sendChatCommand("report " + name);
                info("§cReported §f" + name);
            }
        }
    }

    /**
     * Gets the player entity the crosshair is targeting, if any.
     */
    public PlayerEntity getTargetedPlayer() {
        if (mc.crosshairTarget instanceof EntityHitResult hitResult) {
            if (hitResult.getEntity() instanceof PlayerEntity player) {
                return player;
            }
        }
        return null;
    }
}
