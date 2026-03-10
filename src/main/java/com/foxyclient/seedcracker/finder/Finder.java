package com.foxyclient.seedcracker.finder;

import com.foxyclient.seedcracker.config.Config;
import com.foxyclient.seedcracker.finder.decorator.DesertWellFinder;
import com.foxyclient.seedcracker.finder.decorator.DungeonFinder;
import com.foxyclient.seedcracker.finder.decorator.EndGatewayFinder;
import com.foxyclient.seedcracker.finder.decorator.EndPillarsFinder;
import com.foxyclient.seedcracker.finder.decorator.WarpedFungusFinder;
import com.foxyclient.seedcracker.finder.decorator.ore.EmeraldOreFinder;
import com.foxyclient.seedcracker.finder.structure.*;
import com.foxyclient.seedcracker.render.Cuboid;
import com.foxyclient.seedcracker.util.FeatureToggle;
import com.foxyclient.seedcracker.util.HeightContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class Finder {

    protected static final List<BlockPos> CHUNK_POSITIONS = new ArrayList<>();
    protected static final List<BlockPos> SUB_CHUNK_POSITIONS = new ArrayList<>();
    protected static HeightContext heightContext;

    static {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 16; y++) {
                    SUB_CHUNK_POSITIONS.add(new BlockPos(x, y, z));
                }
            }
        }
    }

    protected MinecraftClient mc = MinecraftClient.getInstance();
    protected final List<Cuboid> cuboids = new ArrayList<>();
    protected World world;
    protected ChunkPos chunkPos;

    public Finder(World world, ChunkPos chunkPos) {
        this.world = world;
        this.chunkPos = chunkPos;
    }

    public static List<BlockPos> buildSearchPositions(List<BlockPos> base, Predicate<BlockPos> removeIf) {
        List<BlockPos> newList = new ArrayList<>();

        for (BlockPos pos : base) {
            if (!removeIf.test(pos)) {
                newList.add(pos);
            }
        }

        return newList;
    }

    public World getWorld() {
        return this.world;
    }

    public ChunkPos getChunkPos() {
        return this.chunkPos;
    }

    public abstract List<BlockPos> findInChunk();

    public boolean shouldRender() {
        DimensionType finderDim = this.world.getDimensionEntry().value();
        DimensionType playerDim = mc.world.getDimensionEntry().value();

        if (finderDim != playerDim) return false;

        int renderDistance = mc.options.getClampedViewDistance() * 16 + 16;
        Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());

        for (Cuboid cuboid : this.cuboids) {
            BlockPos pos = cuboid.getPos(); // Note: SeedCrackerX uses getCenterPos(), our Cuboid uses getPos()
            double distance = playerPos.squaredDistanceTo(pos.getX(), playerPos.y, pos.getZ());
            if (distance <= renderDistance * renderDistance + 32) return true;
        }

        return false;
    }

    public boolean isUseless() {
        return this.cuboids.isEmpty();
    }

    public abstract boolean isValidDimension(DimensionType dimension);

    public boolean isOverworld(DimensionType dimension) {
        return dimension.hasSkyLight() && !dimension.hasCeiling();
    }

    public boolean isNether(DimensionType dimension) {
        return !dimension.hasSkyLight() && dimension.hasCeiling();
    }

    public boolean isEnd(DimensionType dimension) {
        return !dimension.hasSkyLight() && !dimension.hasCeiling();
    }

    public static String inferDimension(DimensionType dimension) {
        if (dimension.hasSkyLight() && !dimension.hasCeiling()) return "overworld";
        if (!dimension.hasSkyLight() && dimension.hasCeiling()) return "the_nether";
        return "the_end";
    }

    public enum Category {
        STRUCTURES,
        DECORATORS,
        BIOMES,
    }

    public enum Type {
        BURIED_TREASURE(BuriedTreasureFinder::create, Category.STRUCTURES, Config.get().buriedTreasure, "finder.buriedTreasures"),
        DESERT_TEMPLE(DesertPyramidFinder::create, Category.STRUCTURES, Config.get().desertTemple, "finder.desertTemples"),
        END_CITY(EndCityFinder::create, Category.STRUCTURES, Config.get().endCity, "finder.endCities"),
        JUNGLE_TEMPLE(JunglePyramidFinder::create, Category.STRUCTURES, Config.get().jungleTemple, "finder.jungleTemples"),
        MONUMENT(MonumentFinder::create, Category.STRUCTURES, Config.get().monument, "finder.monuments"),
        SWAMP_HUT(SwampHutFinder::create, Category.STRUCTURES, Config.get().swampHut, "finder.swampHuts"),
        SHIPWRECK(ShipwreckFinder::create, Category.STRUCTURES, Config.get().shipwreck, "finder.shipwrecks"),
        PILLAGER_OUTPOST(OutpostFinder::create, Category.STRUCTURES, Config.get().outpost, "finder.outposts"),
        IGLOO(IglooFinder::create, Category.STRUCTURES, Config.get().igloo, "finder.igloo"),
        TRIAL_CHAMBERS(TrialChambersFinder::create, Category.STRUCTURES, Config.get().trialChambers, "finder.trialChambers"),

        END_PILLARS(EndPillarsFinder::create, Category.DECORATORS, Config.get().endPillars, "finder.endPillars"),
        END_GATEWAY(EndGatewayFinder::create, Category.DECORATORS, Config.get().endGateway, "finder.endGateways"),
        DUNGEON(DungeonFinder::create, Category.DECORATORS, Config.get().dungeon, "finder.dungeons"),
        EMERALD_ORE(EmeraldOreFinder::create, Category.DECORATORS, Config.get().emeraldOre, "finder.emeraldOres"),
        DESERT_WELL(DesertWellFinder::create, Category.DECORATORS, Config.get().desertWell, "finder.desertWells"),
        WARPED_FUNGUS(WarpedFungusFinder::create, Category.DECORATORS, Config.get().warpedFungus, "finder.warpedFungus"),

        BIOME(BiomeFinder::create, Category.BIOMES, Config.get().biome, "finder.biomes");

        public final FinderBuilder finderBuilder;
        public final String nameKey;
        private final Category category;
        public FeatureToggle enabled;

        Type(FinderBuilder finderBuilder, Category category, FeatureToggle enabled, String nameKey) {
            this.finderBuilder = finderBuilder;
            this.category = category;
            this.enabled = enabled;
            this.nameKey = nameKey;
        }

        public static List<Type> getForCategory(Category category) {
            return Arrays.stream(values()).filter(type -> type.category == category).collect(Collectors.toList());
        }
    }
}
