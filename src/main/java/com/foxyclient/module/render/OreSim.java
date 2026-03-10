package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.ChunkDataEvent;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.FoxyRenderer;
import com.foxyclient.util.Ore;
import com.foxyclient.util.RenderUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * OreSim - Simulates Minecraft ore generation using the world seed.
 * Renders predicted ore positions as highlighted boxes.
 * Ported from meteor-rejects, reimplemented for FoxyClient 1.21.
 */
public class OreSim extends Module {

    private final Map<Long, Map<Ore, Set<BlockPos>>> chunkRenderers = new ConcurrentHashMap<>();
    private long worldSeed = -1;
    private Map<RegistryKey<Biome>, List<Ore>> oreConfig;
    private ExecutorService executor;

    private final NumberSetting horizontalRadius = addSetting(new NumberSetting("Range", "Chunk radius to simulate", 5, 1, 10));
    private final ModeSetting airCheck = addSetting(new ModeSetting("AirCheck", "Check if ore position is solid", "RECHECK", "ON_LOAD", "RECHECK", "OFF"));
    private final BoolSetting filled = addSetting(new BoolSetting("Filled", "Draw filled translucent boxes", true));
    private final BoolSetting tracers = addSetting(new BoolSetting("Tracers", "Draw tracer lines to ores", false));

    public OreSim() {
        super("OreSim", "Xray on crack. Simulates ore gen from seed.", Category.RENDER);
        Ore.ORE_SETTINGS.forEach(this::addSetting);
    }

    public void updateSeed(long seed) {
        this.worldSeed = seed;
        if (isEnabled()) reload();
    }

    @Override
    public void onEnable() {
        if (worldSeed == -1) {
            info("No seed found. Use .seed <seed> to set one.");
            setEnabled(false);
            return;
        }
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        reload();
    }

    @Override
    public void onDisable() {
        chunkRenderers.clear();
        oreConfig = null;
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void reload() {
        if (mc.world == null || worldSeed == -1) return;

        Ore.Dimension dim = Ore.Dimension.Overworld;
        if (mc.world.getRegistryKey() == World.NETHER) dim = Ore.Dimension.Nether;
        else if (mc.world.getRegistryKey() == World.END) dim = Ore.Dimension.End;

        oreConfig = Ore.getRegistry(dim, mc.world.getRegistryManager());
        chunkRenderers.clear();

        if (mc.player != null) {
            int r = horizontalRadius.get().intValue();
            ChunkPos pos = mc.player.getChunkPos();
            int count = 0;
            for (int x = pos.x - r; x <= pos.x + r; x++) {
                for (int z = pos.z - r; z <= pos.z + r; z++) {
                    Chunk chunk = mc.world.getChunk(x, z, ChunkStatus.FULL, false);
                    if (chunk != null) {
                        simulateChunk(chunk);
                        count++;
                    }
                }
            }
            info("§aOreSim loaded — simulating " + count + " chunks (seed: " + worldSeed + ")");
        }
    }

    // ===== Event Handlers =====

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        WorldChunk wc = event.getChunk();
        if (wc != null) simulateChunk(wc);
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (nullCheck() || oreConfig == null) return;

        // In RECHECK mode, re-filter positions against current block state each tick
        // We do this lazily: one chunk per tick to avoid lag spikes
        if (airCheck.get().equals("RECHECK")) {
            recheckOneChunk();
        }
    }

    @EventHandler
    private void onRender(RenderEvent event) {
        if (mc.player == null || oreConfig == null || worldSeed == -1) return;

        int chunkX = mc.player.getChunkPos().x;
        int chunkZ = mc.player.getChunkPos().z;
        int r = horizontalRadius.get().intValue();

        for (int x = chunkX - r; x <= chunkX + r; x++) {
            for (int z = chunkZ - r; z <= chunkZ + r; z++) {
                renderChunk(x, z, event);
            }
        }
    }

    // ===== Rendering =====

    private void renderChunk(int cx, int cz, RenderEvent event) {
        long key = ChunkPos.toLong(cx, cz);
        Map<Ore, Set<BlockPos>> chunk = chunkRenderers.get(key);
        if (chunk == null) return;

        for (Map.Entry<Ore, Set<BlockPos>> entry : chunk.entrySet()) {
            Ore ore = entry.getKey();
            if (!ore.active.get()) continue;

            for (BlockPos pos : entry.getValue()) {
                if (filled.get()) {
                    // Filled box + wireframe (matches OreFinder style)
                    RenderUtil.drawBlockBox(event.getMatrices(), pos, ore.color, 1.0f, event.getVertexConsumers());
                } else {
                    // Wireframe only
                    Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
                    Box box = new Box(
                        pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z,
                        pos.getX() + 1 - camPos.x, pos.getY() + 1 - camPos.y, pos.getZ() + 1 - camPos.z
                    );
                    FoxyRenderer.drawLines(event.getMatrices(), event.getVertexConsumers(), box, ore.color);
                }

                if (tracers.get()) {
                    RenderUtil.drawBlockTracer(event.getMatrices(), pos, ore.color, event.getVertexConsumers());
                }
            }
        }
    }

    // ===== Simulation =====

    private void simulateChunk(Chunk chunk) {
        if (executor == null || executor.isShutdown()) return;
        var chunkPos = chunk.getPos();
        long chunkKey = chunkPos.toLong();
        if (chunkRenderers.containsKey(chunkKey) || mc.world == null) return;

        executor.execute(() -> {
            try {
                Map<Ore, Set<BlockPos>> result = runSimulation(chunk, chunkPos);
                chunkRenderers.put(chunkKey, result);
            } catch (Exception e) {
                // Silently handle — chunk may have been unloaded mid-simulation
            }
        });
    }

    private Map<Ore, Set<BlockPos>> runSimulation(Chunk chunk, ChunkPos chunkPos) {
        // Collect biomes present in the chunk
        Set<RegistryKey<Biome>> biomes = new HashSet<>();
        for (int x = 0; x < 16; x += 4) {
            for (int z = 0; z < 16; z += 4) {
                for (int y = mc.world.getBottomY(); y < mc.world.getHeight() + mc.world.getBottomY(); y += 4) {
                    var biomeHolder = chunk.getBiomeForNoiseGen(x >> 2, y >> 2, z >> 2);
                    if (biomeHolder != null && biomeHolder.getKey().isPresent()) {
                        biomes.add(biomeHolder.getKey().get());
                    }
                }
            }
        }

        Set<Ore> oreSet = biomes.stream()
            .flatMap(b -> getOresForBiome(b).stream())
            .collect(Collectors.toSet());

        int chunkX = chunkPos.getStartX();
        int chunkZ = chunkPos.getStartZ();
        ChunkRandom random = new ChunkRandom(net.minecraft.util.math.random.Random.create());

        long populationSeed = random.setPopulationSeed(worldSeed, chunkX, chunkZ);
        Map<Ore, Set<BlockPos>> result = new HashMap<>();

        boolean doAirCheck = !airCheck.get().equals("OFF");

        for (Ore ore : oreSet) {
            Set<BlockPos> ores = new HashSet<>();
            random.setDecoratorSeed(populationSeed, ore.index, ore.step);
            int repeat = ore.count.get(random);

            for (int i = 0; i < repeat; i++) {
                if (ore.rarity != 1F && random.nextFloat() >= 1 / ore.rarity) continue;

                int x = random.nextInt(16) + chunkX;
                int z = random.nextInt(16) + chunkZ;
                int y = ore.heightProvider.get(random, ore.heightContext);
                BlockPos origin = new BlockPos(x, y, z);

                List<BlockPos> generated;
                if (ore.scattered) {
                    generated = generateHidden(random, origin, ore.size, doAirCheck);
                } else {
                    generated = generateNormal(random, origin, ore.size, ore.discardOnAirChance, doAirCheck);
                }
                ores.addAll(generated);
            }
            if (!ores.isEmpty()) result.put(ore, ores);
        }
        return result;
    }

    private List<Ore> getOresForBiome(RegistryKey<Biome> biome) {
        if (oreConfig.containsKey(biome)) return oreConfig.get(biome);
        return oreConfig.values().stream().findAny().orElse(Collections.emptyList());
    }

    // ===== Air Check Recheck =====

    private Iterator<Long> recheckIterator;

    private void recheckOneChunk() {
        if (mc.world == null) return;

        // Reset iterator if needed
        if (recheckIterator == null || !recheckIterator.hasNext()) {
            recheckIterator = new ArrayList<>(chunkRenderers.keySet()).iterator();
        }
        if (!recheckIterator.hasNext()) return;

        long chunkKey = recheckIterator.next();
        Map<Ore, Set<BlockPos>> chunkData = chunkRenderers.get(chunkKey);
        if (chunkData == null) return;

        // Filter out positions that now have air
        for (Map.Entry<Ore, Set<BlockPos>> entry : chunkData.entrySet()) {
            entry.getValue().removeIf(pos -> {
                if (pos.getY() < mc.world.getBottomY() || pos.getY() >= mc.world.getBottomY() + mc.world.getHeight()) {
                    return false;
                }
                return !mc.world.getBlockState(pos).isSolidBlock(mc.world, pos);
            });
        }
    }

    // ===== Mojang Ore Generation Math (Yarn mappings) =====

    private List<BlockPos> generateNormal(ChunkRandom random, BlockPos pos, int veinSize, float discardOnAir, boolean checkAir) {
        float f = random.nextFloat() * (float) Math.PI;
        float g = (float) veinSize / 8.0F;
        int i = MathHelper.ceil(((float) veinSize / 16.0F * 2.0F + 1.0F) / 2.0F);
        double d = (double) pos.getX() + Math.sin(f) * (double) g;
        double e = (double) pos.getX() - Math.sin(f) * (double) g;
        double h = (double) pos.getZ() + Math.cos(f) * (double) g;
        double j = (double) pos.getZ() - Math.cos(f) * (double) g;
        double l = pos.getY() + random.nextInt(3) - 2;
        double m = pos.getY() + random.nextInt(3) - 2;
        int n = pos.getX() - MathHelper.ceil(g) - i;
        int o = pos.getY() - 2 - i;
        int p = pos.getZ() - MathHelper.ceil(g) - i;
        int q = 2 * (MathHelper.ceil(g) + i);
        int r = 2 * (2 + i);

        for (int s = n; s <= n + q; ++s) {
            for (int t = p; t <= p + q; ++t) {
                if (o <= mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING, s, t)) {
                    return generateVeinPart(random, veinSize, d, e, h, j, l, m, n, o, p, q, r, discardOnAir, checkAir);
                }
            }
        }
        return Collections.emptyList();
    }

    private List<BlockPos> generateVeinPart(ChunkRandom random, int veinSize,
            double startX, double endX, double startZ, double endZ,
            double startY, double endY, int x, int y, int z, int size, int i,
            float discardOnAir, boolean checkAir) {

        BitSet bitSet = new BitSet(size * i * size);
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        double[] ds = new double[veinSize * 4];
        List<BlockPos> poses = new ArrayList<>();

        for (int n = 0; n < veinSize; ++n) {
            float f = (float) n / (float) veinSize;
            double p = MathHelper.lerp(f, startX, endX);
            double q = MathHelper.lerp(f, startY, endY);
            double r = MathHelper.lerp(f, startZ, endZ);
            double s = random.nextDouble() * (double) veinSize / 16.0D;
            double m = ((double) (MathHelper.sin((float) Math.PI * f) + 1.0F) * s + 1.0D) / 2.0D;
            ds[n * 4] = p;
            ds[n * 4 + 1] = q;
            ds[n * 4 + 2] = r;
            ds[n * 4 + 3] = m;
        }

        for (int n = 0; n < veinSize - 1; ++n) {
            if (!(ds[n * 4 + 3] <= 0.0D)) {
                for (int o = n + 1; o < veinSize; ++o) {
                    if (!(ds[o * 4 + 3] <= 0.0D)) {
                        double p = ds[n * 4] - ds[o * 4];
                        double q = ds[n * 4 + 1] - ds[o * 4 + 1];
                        double r = ds[n * 4 + 2] - ds[o * 4 + 2];
                        double s = ds[n * 4 + 3] - ds[o * 4 + 3];
                        if (s * s > p * p + q * q + r * r) {
                            if (s > 0.0D) ds[o * 4 + 3] = -1.0D;
                            else ds[n * 4 + 3] = -1.0D;
                        }
                    }
                }
            }
        }

        for (int n = 0; n < veinSize; ++n) {
            double u = ds[n * 4 + 3];
            if (!(u < 0.0D)) {
                double v = ds[n * 4];
                double w = ds[n * 4 + 1];
                double aa = ds[n * 4 + 2];
                int ab = Math.max(MathHelper.floor(v - u), x);
                int ac = Math.max(MathHelper.floor(w - u), y);
                int ad = Math.max(MathHelper.floor(aa - u), z);
                int ae = Math.max(MathHelper.floor(v + u), ab);
                int af = Math.max(MathHelper.floor(w + u), ac);
                int ag = Math.max(MathHelper.floor(aa + u), ad);

                for (int ah = ab; ah <= ae; ++ah) {
                    double ai = ((double) ah + 0.5D - v) / u;
                    if (ai * ai < 1.0D) {
                        for (int aj = ac; aj <= af; ++aj) {
                            double ak = ((double) aj + 0.5D - w) / u;
                            if (ai * ai + ak * ak < 1.0D) {
                                for (int al = ad; al <= ag; ++al) {
                                    double am = ((double) al + 0.5D - aa) / u;
                                    if (ai * ai + ak * ak + am * am < 1.0D) {
                                        int an = ah - x + (aj - y) * size + (al - z) * size * i;
                                        if (!bitSet.get(an)) {
                                            bitSet.set(an);
                                            mutable.set(ah, aj, al);
                                            if (aj >= mc.world.getBottomY() && aj < mc.world.getBottomY() + mc.world.getHeight()) {
                                                if (!checkAir || mc.world.getBlockState(mutable).isSolidBlock(mc.world, mutable)) {
                                                    if (shouldPlace(mutable, discardOnAir, random, checkAir)) {
                                                        poses.add(new BlockPos(ah, aj, al));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return poses;
    }

    private boolean shouldPlace(BlockPos pos, float discardOnAir, ChunkRandom random, boolean checkAir) {
        if (discardOnAir == 0F || (discardOnAir != 1F && random.nextFloat() >= discardOnAir)) return true;
        if (!checkAir) return true;
        for (Direction d : Direction.values()) {
            if (!mc.world.getBlockState(pos.offset(d)).isSolidBlock(mc.world, pos.offset(d)) && discardOnAir != 1F) {
                return false;
            }
        }
        return true;
    }

    private List<BlockPos> generateHidden(ChunkRandom random, BlockPos pos, int size, boolean checkAir) {
        List<BlockPos> poses = new ArrayList<>();
        int i = random.nextInt(size + 1);
        for (int j = 0; j < i; ++j) {
            int s = Math.min(j, 7);
            int x = Math.round((random.nextFloat() - random.nextFloat()) * (float) s) + pos.getX();
            int y = Math.round((random.nextFloat() - random.nextFloat()) * (float) s) + pos.getY();
            int z = Math.round((random.nextFloat() - random.nextFloat()) * (float) s) + pos.getZ();
            BlockPos orePos = new BlockPos(x, y, z);
            if (!checkAir || mc.world.getBlockState(orePos).isSolidBlock(mc.world, orePos)) {
                if (shouldPlace(orePos, 1F, random, checkAir)) {
                    poses.add(orePos);
                }
            }
        }
        return poses;
    }
}
