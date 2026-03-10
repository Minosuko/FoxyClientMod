package com.foxyclient.event.events;

import com.foxyclient.event.Event;
import net.minecraft.client.input.Input;

/**
 * Fired when movement input is processed.
 */
public class MovementInputEvent extends Event {
    private final Input input;

    public MovementInputEvent(Input input) {
        this.input = input;
    }

    public Input getInput() { return input; }
}
