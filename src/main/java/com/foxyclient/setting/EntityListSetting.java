package com.foxyclient.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A setting that stores a mutable list of {@link EntityType}s.
 * Supports JSON serialization via entity registry IDs and an
 * optional change-callback.
 */
public class EntityListSetting extends Setting<List<EntityType<?>>> {

    private Runnable onListChanged;

    public EntityListSetting(String name, String description) {
        super(name, description, new ArrayList<>());
    }

    public EntityListSetting(String name, String description, EntityType<?>... defaults) {
        super(name, description, new ArrayList<>(Arrays.asList(defaults)));
    }

    public void setOnListChanged(Runnable callback) {
        this.onListChanged = callback;
    }

    private void fireChanged() {
        if (onListChanged != null) onListChanged.run();
    }

    public List<EntityType<?>> getEntities() {
        return get();
    }

    public boolean contains(EntityType<?> type) {
        return get().contains(type);
    }

    public int size() {
        return get().size();
    }

    public boolean isEmpty() {
        return get().isEmpty();
    }

    public void addEntity(EntityType<?> type) {
        if (!get().contains(type)) {
            get().add(type);
            fireChanged();
        }
    }

    public void removeEntity(EntityType<?> type) {
        if (get().remove(type)) {
            fireChanged();
        }
    }

    public void toggleEntity(EntityType<?> type) {
        if (get().contains(type)) {
            get().remove(type);
        } else {
            get().add(type);
        }
        fireChanged();
    }

    public void clear() {
        if (!get().isEmpty()) {
            get().clear();
            fireChanged();
        }
    }

    public static String getDisplayName(EntityType<?> type) {
        return type.getName().getString();
    }

    @Override
    public JsonElement toJson() {
        JsonArray array = new JsonArray();
        for (EntityType<?> t : get()) {
            Identifier id = Registries.ENTITY_TYPE.getId(t);
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
                if (id != null && Registries.ENTITY_TYPE.containsId(id)) {
                    get().add(Registries.ENTITY_TYPE.get(id));
                }
            }
        }
    }
}
