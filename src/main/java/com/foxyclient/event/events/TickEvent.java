package com.foxyclient.event.events;

import com.foxyclient.event.Event;

/**
 * Fired every client tick.
 */
public class TickEvent extends Event {
    public static final TickEvent INSTANCE = new TickEvent();
}
