package com.foxyclient.event.events;

import com.foxyclient.event.Event;

/**
 * Event fired when the mouse wheel is scrolled.
 */
public class MouseScrollEvent extends Event {
    private final double horizontal;
    private final double vertical;

    public MouseScrollEvent(double horizontal, double vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public double getHorizontal() {
        return horizontal;
    }

    public double getVertical() {
        return vertical;
    }
}
