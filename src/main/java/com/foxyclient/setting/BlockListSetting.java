package com.foxyclient.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A setting that stores a mutable list of {@link Block}s.
 * Supports JSON serialization via block registry IDs and an
 * optional change-callback for triggering world reloads.
 */
public class BlockListSetting extends Setting<List<Block>> {

    private Runnable onListChanged;

    public BlockListSetting(String name, String description) {
        super(name, description, new ArrayList<>());
    }

    /**
     * Construct with default blocks already in the list.
     */
    public BlockListSetting(String name, String description, Block... defaults) {
        super(name, description, new ArrayList<>(Arrays.asList(defaults)));
    }

    // ── Change callback ────────────────────────────────────────────────

    /**
     * Set a callback that fires whenever the list is mutated.
     */
    public void setOnListChanged(Runnable callback) {
        this.onListChanged = callback;
    }

    private void fireChanged() {
        if (onListChanged != null) onListChanged.run();
    }

    // ── Query ──────────────────────────────────────────────────────────

    public List<Block> getBlocks() {
        return get();
    }

    public boolean contains(Block block) {
        return get().contains(block);
    }

    public int size() {
        return get().size();
    }

    public boolean isEmpty() {
        return get().isEmpty();
    }

    // ── Mutation (all fire the change callback) ────────────────────────

    public void addBlock(Block block) {
        if (!get().contains(block)) {
            get().add(block);
            fireChanged();
        }
    }

    public void removeBlock(Block block) {
        if (get().remove(block)) {
            fireChanged();
        }
    }

    public void toggleBlock(Block block) {
        if (get().contains(block)) {
            get().remove(block);
        } else {
            get().add(block);
        }
        fireChanged();
    }

    public void clear() {
        if (!get().isEmpty()) {
            get().clear();
            fireChanged();
        }
    }

    // ── Display ─────────────────────────────────────────────────────────

    /**
     * Returns a human-readable display name for a block,
     * e.g. "Diamond Ore" from "minecraft:diamond_ore".
     */
    public static String getDisplayName(Block block) {
        String path = Registries.BLOCK.getId(block).getPath();
        StringBuilder sb = new StringBuilder();
        for (String part : path.split("_")) {
            if (!part.isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    // ── JSON Serialization ──────────────────────────────────────────────

    @Override
    public JsonElement toJson() {
        JsonArray array = new JsonArray();
        for (Block b : get()) {
            Identifier id = Registries.BLOCK.getId(b);
            array.add(new JsonPrimitive(id.toString()));
        }
        return array;
    }

    @Override
    public void fromJson(JsonElement element) {
        get().clear();
        if (element.isJsonArray()) {
            for (JsonElement el : element.getAsJsonArray()) {
                String idStr = el.getAsString();
                Identifier id = Identifier.tryParse(idStr);
                if (id != null && Registries.BLOCK.containsId(id)) {
                    get().add(Registries.BLOCK.get(id));
                }
            }
        }
    }
}
