package com.foxyclient.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Waypoints system - named locations you can navigate to.
 */
public class WaypointManager {
    private final List<Waypoint> waypoints = new ArrayList<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path filePath;

    public WaypointManager() {
        filePath = FabricLoader.getInstance().getConfigDir().resolve("foxyclient/waypoints.json");
        load();
    }

    public void add(String name, int x, int y, int z, String dimension) {
        waypoints.add(new Waypoint(name, x, y, z, dimension));
        save();
    }

    public boolean remove(String name) {
        boolean removed = waypoints.removeIf(wp -> wp.name().equalsIgnoreCase(name));
        if (removed) save();
        return removed;
    }

    public Waypoint get(String name) {
        return waypoints.stream()
            .filter(wp -> wp.name().equalsIgnoreCase(name))
            .findFirst().orElse(null);
    }

    public List<Waypoint> getAll() { return Collections.unmodifiableList(waypoints); }

    public List<Waypoint> getForDimension(String dimension) {
        return waypoints.stream()
            .filter(wp -> wp.dimension().equals(dimension))
            .toList();
    }

    public void save() {
        try {
            Files.createDirectories(filePath.getParent());
            try (Writer writer = Files.newBufferedWriter(filePath)) {
                gson.toJson(waypoints, writer);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void load() {
        if (!Files.exists(filePath)) return;
        try (Reader reader = Files.newBufferedReader(filePath)) {
            List<Waypoint> list = gson.fromJson(reader, new TypeToken<List<Waypoint>>(){}.getType());
            if (list != null) waypoints.addAll(list);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public record Waypoint(String name, int x, int y, int z, String dimension) {}
}
