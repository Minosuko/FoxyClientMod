package com.foxyclient.module.player;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;

/** SilentDisconnect - Disconnects from the server without sending a disconnect packet. */
public class SilentDisconnect extends Module {
    public SilentDisconnect() { super("SilentDisconnect", "Disconnect without notifying server", Category.PLAYER); }

    @Override
    public void onEnable() {
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().getConnection().disconnect(net.minecraft.text.Text.literal("Silent Disconnect"));
            info("§aSilently disconnected!");
        }
        toggle();
    }
}
