package com.foxyclient.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Friends system - track friendly players to avoid targeting them.
 */
public class FriendsManager {
    private final Set<String> friends = new HashSet<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path filePath;

    public FriendsManager() {
        filePath = FabricLoader.getInstance().getConfigDir().resolve("foxyclient/friends.json");
        load();
    }

    public void addFriend(String name) {
        friends.add(name.toLowerCase());
        save();
    }

    public void removeFriend(String name) {
        friends.remove(name.toLowerCase());
        save();
    }

    public boolean isFriend(String name) {
        return friends.contains(name.toLowerCase());
    }

    public Set<String> getFriends() { return Collections.unmodifiableSet(friends); }

    public void save() {
        try {
            Files.createDirectories(filePath.getParent());
            try (Writer writer = Files.newBufferedWriter(filePath)) {
                gson.toJson(new ArrayList<>(friends), writer);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void load() {
        if (!Files.exists(filePath)) return;
        try (Reader reader = Files.newBufferedReader(filePath)) {
            List<String> list = gson.fromJson(reader, new TypeToken<List<String>>(){}.getType());
            if (list != null) friends.addAll(list);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
