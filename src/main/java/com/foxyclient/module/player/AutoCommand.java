package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.client.network.ClientPlayerEntity;

/** 
 * AutoCommand - Auto send commands on world join.
 */
public class AutoCommand extends Module {
    
    // Commands to run when entering a world
    // In a real client these would be a configurable list/array setting.
    // We hardcode some example typical commands.
    private final String[] commandsToRun = {
        "help",
        "ping"
    };

    private boolean hasRun = false;

    public AutoCommand() { 
        super("AutoCommand", "Auto send commands on join", Category.PLAYER); 
    }

    @Override
    public void onEnable() {
        // Reset when the module is toggled on so it will run next time we're in world
        hasRun = false;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) {
            hasRun = false; // Reset if we disconnect
            return;
        }

        if (!hasRun) {
            ClientPlayerEntity player = mc.player;
            if (player != null && mc.getNetworkHandler() != null) {
                // We're fully joined and in the world
                for (String cmd : commandsToRun) {
                    mc.getNetworkHandler().sendChatCommand(cmd);
                }
                hasRun = true;
            }
        }
    }
}
