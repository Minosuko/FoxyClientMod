package com.foxyclient.seedcracker.finder.decorator;

import com.foxyclient.seedcracker.FoxySeedCracker;
import com.foxyclient.seedcracker.config.Config;
import com.foxyclient.seedcracker.cracker.DataAddedEvent;
import com.foxyclient.seedcracker.cracker.PillarData;
import com.foxyclient.seedcracker.finder.BlockFinder;
import com.foxyclient.seedcracker.finder.Finder;
import com.foxyclient.seedcracker.render.Cuboid;
import com.seedfinding.mccore.version.MCVersion;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EndPillarsFinder extends Finder {

    private final boolean alreadyFound;
    protected BedrockMarkerFinder[] bedrockMarkers = new BedrockMarkerFinder[10];

    public EndPillarsFinder(World world, ChunkPos chunkPos) {
        super(world, chunkPos);

        this.alreadyFound = !FoxySeedCracker.get().getDataStorage().addPillarData(null, DataAddedEvent.POKE_PILLARS);
        if (this.alreadyFound) return;

        for (int i = 0; i < this.bedrockMarkers.length; i++) {
            double x = 42.0D * Math.cos(2.0D * (-Math.PI + (Math.PI / 10.0D) * (double) i));
            double z = 42.0D * Math.sin(2.0D * (-Math.PI + (Math.PI / 10.0D) * (double) i));
            if (Config.get().getVersion().isOlderThan(MCVersion.v1_14)) {
                x = Math.round(x);
                z = Math.round(z);
            }
            BlockPos targetPos = new BlockPos(MathHelper.floor(x), 0, MathHelper.floor(z));
            this.bedrockMarkers[i] = new BedrockMarkerFinder(this.world, new ChunkPos(targetPos), targetPos);
        }
    }

    public static List<Finder> create(World world, ChunkPos chunkPos) {
        List<Finder> finders = new ArrayList<>();
        finders.add(new EndPillarsFinder(world, chunkPos));
        return finders;
    }

    @Override
    public List<BlockPos> findInChunk() {
        if (this.alreadyFound) return new ArrayList<>();
        List<BlockPos> result = new ArrayList<>();

        for (BedrockMarkerFinder bedrockMarker : this.bedrockMarkers) {
            if (bedrockMarker == null) continue;
            result.addAll(bedrockMarker.findInChunk());
        }

        if (result.size() == this.bedrockMarkers.length) {
            PillarData pillarData = new PillarData(result.stream().map(Vec3i::getY).collect(Collectors.toList()));

            if (FoxySeedCracker.get().getDataStorage().addPillarData(pillarData, DataAddedEvent.POKE_PILLARS)) {
                result.forEach(pos -> this.cuboids.add(new Cuboid(pos, 0xFF800080))); // Purple
            }

        }

        return result;
    }

    @Override
    public boolean isValidDimension(DimensionType dimension) {
        return this.isEnd(dimension);
    }

    public static class BedrockMarkerFinder extends BlockFinder {

        protected static List<BlockPos> SEARCH_POSITIONS;

        public BedrockMarkerFinder(World world, ChunkPos chunkPos, BlockPos xz) {
            super(world, chunkPos, Blocks.BEDROCK);
            this.searchPositions = SEARCH_POSITIONS;
        }

        public static void reloadSearchPositions() {
            SEARCH_POSITIONS = buildSearchPositions(CHUNK_POSITIONS, pos -> {
                if (pos.getY() < 76) return true;
                return pos.getY() > 76 + 3 * 10;
            });
        }

        @Override
        public List<BlockPos> findInChunk() {
            return super.findInChunk();
        }

        @Override
        public boolean isValidDimension(DimensionType dimension) {
            return true;
        }

    }

}
