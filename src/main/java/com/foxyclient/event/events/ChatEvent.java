package com.foxyclient.event.events;

import com.foxyclient.event.Event;

/**
 * Fired when a chat message is sent.
 */
public class ChatEvent extends Event {
    private String message;

    public ChatEvent(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
