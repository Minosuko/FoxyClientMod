package com.foxyclient.module.movement;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.util.PlayerInput;

/** RoboWalk - Automatic walking in a straight line until toggled off. */
public class RoboWalk extends Module {
    public RoboWalk() { super("RoboWalk", "Automatically walk forward", Category.MOVEMENT); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        mc.player.input.playerInput = new PlayerInput(true, mc.player.input.playerInput.backward(), mc.player.input.playerInput.left(), mc.player.input.playerInput.right(), mc.player.input.playerInput.jump(), mc.player.input.playerInput.sneak(), mc.player.input.playerInput.sprint());
        mc.player.setSprinting(true);
    }
}
