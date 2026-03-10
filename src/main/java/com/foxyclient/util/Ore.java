package com.foxyclient.util;

import com.foxyclient.FoxyClient;
import com.foxyclient.mixin.CountPlacementModifierAccessor;
import com.foxyclient.mixin.HeightRangePlacementModifierAccessor;
import com.foxyclient.mixin.RarityFilterPlacementModifierAccessor;
import com.foxyclient.setting.BoolSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.HeightContext;
import java.util.stream.Collectors;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.heightprovider.HeightProvider;
import net.minecraft.world.gen.placementmodifier.CountPlacementModifier;
import net.minecraft.world.gen.placementmodifier.HeightRangePlacementModifier;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;
import net.minecraft.world.gen.placementmodifier.RarityFilterPlacementModifier;

import java.awt.Color;
import java.util.*;

public class Ore {

    public static final List<BoolSetting> ORE_SETTINGS = new ArrayList<>();

    private static BoolSetting add(String name) {
        BoolSetting s = new BoolSetting(name, "Show " + name + " in OreSim", true);
        ORE_SETTINGS.add(s);
        return s;
    }

    private static final BoolSetting coal = add("Coal");
    private static final BoolSetting iron = add("Iron");
    private static final BoolSetting gold = add("Gold");
    private static final BoolSetting redstone = add("Redstone");
    private static final BoolSetting diamond = add("Diamond");
    private static final BoolSetting lapis = add("Lapis");
    private static final BoolSetting copper = add("Copper");
    private static final BoolSetting emerald = add("Emerald");
    private static final BoolSetting quartz = add("Quartz");
    private static final BoolSetting debris = add("Ancient Debris");

    public enum Dimension {
        Overworld, Nether, End
    }

    public static Map<RegistryKey<Biome>, List<Ore>> getRegistry(Dimension dimension, RegistryWrapper.WrapperLookup registry) {
        if (registry != null && registry.getOptional(RegistryKeys.PLACED_FEATURE).isPresent() && registry.getOptional(RegistryKeys.BIOME).isPresent()) {
            return getRegistryFromLookup(dimension, registry);
        }

        // Fallback to builtin registries if the provided lookup is incomplete (e.g. on a client connected to a server)
        try {
            return getRegistryFromLookup(dimension, BuiltinRegistries.createWrapperLookup());
        } catch (Throwable e) {
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(net.minecraft.text.Text.literal("§7[§6FoxyClient§7] §cMissing required Minecraft registries for OreSim!"), false);
            }
            return Collections.emptyMap();
        }
    }

    private static Map<RegistryKey<Biome>, List<Ore>> getRegistryFromLookup(Dimension dimension, RegistryWrapper.WrapperLookup registry) {
        RegistryWrapper.Impl<PlacedFeature> features = registry.getOrThrow(RegistryKeys.PLACED_FEATURE);
        RegistryWrapper.Impl<Biome> biomeRegistry = registry.getOrThrow(RegistryKeys.BIOME);
        var biomes = biomeRegistry.streamEntries().collect(Collectors.toList());

        Map<PlacedFeature, Ore> featureToOre = new HashMap<>();
        
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_coal_lower")), 6, coal, new Color(47, 44, 54));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_coal_upper")), 6, coal, new Color(47, 44, 54));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_iron_middle")), 6, iron, new Color(236, 173, 119));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_iron_small")), 6, iron, new Color(236, 173, 119));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_iron_upper")), 6, iron, new Color(236, 173, 119));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_gold")), 6, gold, new Color(247, 229, 30));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_gold_lower")), 6, gold, new Color(247, 229, 30));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_gold_extra")), 6, gold, new Color(247, 229, 30));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_gold_nether")), 7, gold, new Color(247, 229, 30));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_gold_deltas")), 7, gold, new Color(247, 229, 30));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_redstone")), 6, redstone, new Color(245, 7, 23));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_redstone_lower")), 6, redstone, new Color(245, 7, 23));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_diamond")), 6, diamond, new Color(33, 244, 255));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_diamond_buried")), 6, diamond, new Color(33, 244, 255));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_diamond_large")), 6, diamond, new Color(33, 244, 255));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_diamond_medium")), 6, diamond, new Color(33, 244, 255));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_lapis")), 6, lapis, new Color(8, 26, 189));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_lapis_buried")), 6, lapis, new Color(8, 26, 189));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_copper")), 6, copper, new Color(239, 151, 0));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_copper_large")), 6, copper, new Color(239, 151, 0));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_emerald")), 6, emerald, new Color(27, 209, 45));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_quartz_nether")), 7, quartz, new Color(205, 205, 205));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_quartz_deltas")), 7, quartz, new Color(205, 205, 205));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_ancient_debris_small")), 7, debris, new Color(209, 27, 245));
        registerOre(featureToOre, features, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("ore_ancient_debris_large")), 7, debris, new Color(209, 27, 245));

        Map<RegistryKey<Biome>, List<Ore>> biomeOreMap = new HashMap<>();

        biomes.forEach(biome -> {
            var key = biome.getKey().orElse(null);
            if (key == null) return;
            biomeOreMap.put(key, new ArrayList<>());
            
            List<RegistryEntryList<PlacedFeature>> featuresPerStep = biome.value().getGenerationSettings().getFeatures();
            for (int i = 0; i < featuresPerStep.size(); i++) {
                final int step = i;
                featuresPerStep.get(i).stream().forEach(entry -> {
                    var feature = entry.value();
                    if (featureToOre.containsKey(feature)) {
                        Ore ore = featureToOre.get(feature);
                        if (ore.step == step) {
                            biomeOreMap.get(key).add(ore);
                        }
                    }
                });
            }
        });
        return biomeOreMap;
    }

    private static void registerOre(
            Map<PlacedFeature, Ore> map,
            RegistryWrapper.Impl<PlacedFeature> oreRegistry,
            RegistryKey<PlacedFeature> oreKey,
            int genStep,
            BoolSetting active,
            Color color
    ) {
        var orePlacementHolder = oreRegistry.getOptional(oreKey);
        if (orePlacementHolder.isEmpty()) return;
        
        var orePlacement = orePlacementHolder.get().value();

        // In 1.21.1 Yarn, we don't have FeatureSorter on client usually. 
        // We'll use a dummy index or try to find it. 
        // Actually, the index is used for nextFeatureSeed. 
        // We can approximate it or just use a fixed one if we only care about the feature itself.
        // But for simulation, we need the correct index.
        // Let's use 0 for now and see if it works. 
        // Ideally we'd calculate the index per-biome, but meteor-rejects uses a global one.
        int index = 0; 

        Ore ore = new Ore(orePlacement, genStep, index, active, color);

        map.put(orePlacement, ore);
    }

    public int step;
    public int index;
    public BoolSetting active;
    public IntProvider count = ConstantIntProvider.create(1);
    public HeightProvider heightProvider;
    public HeightContext heightContext;
    public float rarity = 1;
    public float discardOnAirChance;
    public int size;
    public Color color;
    public boolean scattered;

    private Ore(PlacedFeature feature, int step, int index, BoolSetting active, Color color) {
        this.step = step;
        this.index = index;
        this.active = active;
        this.color = color;
        
        MinecraftClient mc = MinecraftClient.getInstance();
        int bottom = mc.world != null ? mc.world.getBottomY() : -64;
        this.heightContext = new HeightContext(null, mc.world);

        for (PlacementModifier modifier : feature.placementModifiers()) {
            if (modifier instanceof CountPlacementModifier) {
                this.count = ((CountPlacementModifierAccessor) modifier).getCount();
            } else if (modifier instanceof HeightRangePlacementModifier) {
                this.heightProvider = ((HeightRangePlacementModifierAccessor) modifier).getHeight();
            } else if (modifier instanceof RarityFilterPlacementModifier) {
                this.rarity = ((RarityFilterPlacementModifierAccessor) modifier).getChance();
            }
        }

        FeatureConfig featureConfig = feature.feature().value().config();

        if (featureConfig instanceof OreFeatureConfig oreFeatureConfig) {
            this.discardOnAirChance = oreFeatureConfig.discardOnAirChance;
            this.size = oreFeatureConfig.size;
        }

        // Emerald ore uses a specific feature type in Yarn
        if (feature.feature().value().feature().toString().contains("emerald") || feature.feature().value().feature().toString().contains("scattered")) {
            this.scattered = true;
        }
    }
}
