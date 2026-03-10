package com.foxyclient.seedcracker.util;

import com.seedfinding.mcbiome.biome.Biomes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class BiomeFixer {

    private static final Map<String, com.seedfinding.mcbiome.biome.Biome> COMPATREGISTRY = new HashMap<>();

    static {
        for (com.seedfinding.mcbiome.biome.Biome biome : Biomes.REGISTRY.values()) {
            COMPATREGISTRY.put(biome.getName(), biome);
        }
        //renamed
        COMPATREGISTRY.put("snowy_plains", Biomes.SNOWY_TUNDRA);
        COMPATREGISTRY.put("old_growth_birch_forest", Biomes.TALL_BIRCH_FOREST);
        COMPATREGISTRY.put("old_growth_pine_taiga", Biomes.GIANT_TREE_TAIGA);
        COMPATREGISTRY.put("old_growth_spruce_taiga", Biomes.GIANT_TREE_TAIGA);
        COMPATREGISTRY.put("windswept_hills", Biomes.EXTREME_HILLS);
        COMPATREGISTRY.put("windswept_forest", Biomes.WOODED_MOUNTAINS);
        COMPATREGISTRY.put("windswept_gravelly_hills", Biomes.GRAVELLY_MOUNTAINS);
        COMPATREGISTRY.put("windswept_savanna", Biomes.SHATTERED_SAVANNA);
        COMPATREGISTRY.put("sparse_jungle", Biomes.JUNGLE_EDGE);
        COMPATREGISTRY.put("stony_shore", Biomes.STONE_SHORE);
        //new
        COMPATREGISTRY.put("meadow", Biomes.PLAINS);
        COMPATREGISTRY.put("grove", Biomes.TAIGA);
        COMPATREGISTRY.put("snowy_slopes", Biomes.SNOWY_TUNDRA);
        COMPATREGISTRY.put("frozen_peaks", Biomes.TAIGA);
        COMPATREGISTRY.put("jagged_peaks", Biomes.TAIGA);
        COMPATREGISTRY.put("stony_peaks", Biomes.TAIGA);
        COMPATREGISTRY.put("mangrove_swamp", Biomes.SWAMP);

        //unsure what to do with those, they'll return THE_VOID for now
        // The following line was moved out of the static initializer block to a place where it would be valid.
        // It's placed as a comment here to reflect its original position in the instruction, but it cannot be executed here.
        // Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        //dripstone_caves
        //lush_caves
        //deep_dark
    }

    public static com.seedfinding.mcbiome.biome.Biome swap(net.minecraft.world.biome.Biome biome) {
        if (MinecraftClient.getInstance().world == null) return Biomes.VOID;
        
        Identifier biomeID = MinecraftClient.getInstance().world.getRegistryManager()
                .getOrThrow(RegistryKeys.BIOME).getId(biome);

        if (biomeID == null) return Biomes.THE_VOID;

        return COMPATREGISTRY.getOrDefault(biomeID.getPath(), Biomes.VOID);
    }
}
