package com.foxyclient.module.misc;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;

import java.util.HashSet;
import java.util.Set;

/**
 * Cancel specific packet types from being sent or received.
 */
public class PacketCanceller extends Module {
    private final BoolSetting cancelCloseScreen = addSetting(new BoolSetting("CloseScreen", "Cancel close screen packets", false));
    private final BoolSetting cancelPlayerAction = addSetting(new BoolSetting("PlayerAction", "Cancel player action packets", false));
    private final BoolSetting cancelEntityAction = addSetting(new BoolSetting("EntityAction", "Cancel entity action packets", false));
    private final BoolSetting logPackets = addSetting(new BoolSetting("Log", "Log cancelled packets", true));

    private final Set<String> cancelledTypes = new HashSet<>();

    public PacketCanceller() {
        super("PacketCanceller", "Cancel specific packets", Category.MISC);
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (nullCheck()) return;
        String name = event.getPacket().getClass().getSimpleName();

        if (cancelCloseScreen.get() && name.contains("CloseScreen")) {
            event.cancel();
            if (logPackets.get()) info("§cCancelled: " + name);
        }
        if (cancelPlayerAction.get() && name.contains("PlayerAction")) {
            event.cancel();
            if (logPackets.get()) info("§cCancelled: " + name);
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck()) return;
        String name = event.getPacket().getClass().getSimpleName();

        if (cancelEntityAction.get() && name.contains("EntityAnimation")) {
            event.cancel();
            if (logPackets.get()) info("§cCancelled: " + name);
        }
    }
}
