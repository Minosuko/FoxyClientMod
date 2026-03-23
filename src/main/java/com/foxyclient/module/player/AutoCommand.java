package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.setting.StringSetting;
import net.minecraft.client.network.ClientPlayerEntity;

/** 
 * AutoCommand - Auto send commands on world join.
 */
public class AutoCommand extends Module {
    
    private final NumberSetting delay = addSetting(new NumberSetting("Delay (Ticks)", "Wait before sending commands", 20.0, 0.0, 200.0));
    private final StringSetting command1 = addSetting(new StringSetting("Command 1", "First command (without /)", "help"));
    private final StringSetting command2 = addSetting(new StringSetting("Command 2", "Second command (without /)", "ping"));
    private final StringSetting command3 = addSetting(new StringSetting("Command 3", "Third command (without /)", ""));
    private final StringSetting command4 = addSetting(new StringSetting("Command 4", "Fourth command (without /)", ""));

    private boolean hasRun = false;
    private int tickCount = 0;

    public AutoCommand() { 
        super("AutoCommand", "Auto send commands on join", Category.PLAYER); 
    }

    @Override
    public void onEnable() {
        hasRun = false;
        tickCount = 0;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) {
            hasRun = false; // Reset if we disconnect
            tickCount = 0;
            return;
        }

        if (!hasRun) {
            tickCount++;
            if (tickCount >= delay.get().intValue()) {
                ClientPlayerEntity player = mc.player;
                if (player != null && mc.getNetworkHandler() != null) {
                    // We're fully joined and in the world
                    String[] cmds = {command1.get(), command2.get(), command3.get(), command4.get()};
                    for (String cmd : cmds) {
                        if (cmd != null && !cmd.trim().isEmpty()) {
                            // Trim leading slash if user added it accidentally
                            String finalCmd = cmd.trim();
                            if (finalCmd.startsWith("/")) {
                                finalCmd = finalCmd.substring(1);
                            }
                            mc.getNetworkHandler().sendChatCommand(finalCmd);
                        }
                    }
                    hasRun = true;
                }
            }
        }
    }
}
