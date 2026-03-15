package com.foxyclient.util;

import java.util.UUID;

/**
 * Represents an alternate account for the Alt Manager.
 */
public class AltAccount {
    private final String username;
    private final String uuid;

    public AltAccount(String username) {
        this.username = username;
        // Generate offline-mode UUID (same algorithm Minecraft uses)
        this.uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
    }

    public AltAccount(String username, String uuid) {
        this.username = username;
        this.uuid = uuid;
    }

    public String getUsername() { return username; }
    public String getUuid() { return uuid; }
}
