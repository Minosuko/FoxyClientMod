package com.foxyclient.seedcracker.cracker.storage;

import com.seedfinding.mcbiome.source.OverworldBiomeSource;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.rand.seed.PillarSeed;
import com.seedfinding.mccore.rand.seed.StructureSeed;
import com.seedfinding.mccore.rand.seed.WorldSeed;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.Feature;
import com.seedfinding.mcfeature.structure.OldStructure;
import com.seedfinding.mcfeature.structure.PillagerOutpost;
import com.seedfinding.mcfeature.structure.Shipwreck;
import com.seedfinding.mcfeature.structure.UniformStructure;
import com.seedfinding.mcseed.lcg.LCG;
import com.foxyclient.seedcracker.FoxySeedCracker;
import com.foxyclient.seedcracker.config.Config;
import com.foxyclient.seedcracker.cracker.BiomeData;
import com.foxyclient.seedcracker.cracker.decorator.Decorator;
import com.foxyclient.seedcracker.util.Database;
import com.foxyclient.seedcracker.util.Log;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class TimeMachine {
    private static final Logger logger = LoggerFactory.getLogger("timeMachine");

    public static ExecutorService SERVICE = Executors.newFixedThreadPool(5);

    private final LCG inverseLCG = LCG.JAVA.combine(-2);
    public boolean isRunning = false;
    public boolean shouldTerminate = false;
    public List<Integer> pillarSeeds = null;
    public Set<Long> structureSeeds = new HashSet<>();
    public Set<Long> worldSeeds = new HashSet<>();
    protected DataStorage dataStorage;

    public TimeMachine(DataStorage dataStorage) {
        this.dataStorage = dataStorage;
    }

    public void poke(Phase phase) {
        if (this.worldSeeds.size() == 1) return;
        this.isRunning = true;

        while (phase != null && !this.shouldTerminate) {
            if (phase != Phase.BIOMES && pokeStructureReduce()) {
                phase = Phase.BIOMES;
                continue;

            } else if (phase == Phase.STRUCTURES) {
                if (!pokeStructures()) break;

            } else if (phase == Phase.LIFTING) {
                if (!pokeStructures() && !pokeLifting()) break;

            } else if (phase == Phase.PILLARS) {
                if (!this.pokePillars()) break;

            } else if (phase == Phase.BIOMES) {
                if (!this.pokeBiomes()) break;
            }

            phase = phase.nextPhase();
        }
        if (this.worldSeeds.size() == 1 && !this.shouldTerminate) {
            long seed = worldSeeds.stream().findFirst().get();
            FoxySeedCracker.get().getApi().pushWorldSeed(seed);
            Log.printSeed("Found world seed: ${SEED}", seed);

            MinecraftClient client = MinecraftClient.getInstance();
            if (Config.get().databaseSubmits && client.getNetworkHandler() != null &&
                    client.getNetworkHandler().getPlayerList().size() > 10 &&
                    !client.isInSingleplayer()) {
                try {
                    Database.handleDatabaseCall(seed);
                } catch (Exception e) {
                    Log.error("Database submit failed: " + e.getMessage());
                }
            }
        }
    }

    protected boolean pokePillars() {
        if (this.pillarSeeds != null || this.dataStorage.pillarData == null) return false;
        this.pillarSeeds = new ArrayList<>();

        Log.debug("====================================");
        Log.warn("Looking for pillar seeds...");

        for (int pillarSeed = 0; pillarSeed < 1 << 16 && !this.shouldTerminate; pillarSeed++) {
            if (this.dataStorage.pillarData.test(pillarSeed)) {
                Log.printSeed("Found pillar seed: ${SEED}", pillarSeed);
                this.pillarSeeds.add(pillarSeed);
            }
        }

        if (!this.pillarSeeds.isEmpty()) {
            Log.warn("Pillar seed search complete.");
        } else {
            Log.error("No pillar seeds found.");
        }

        return true;
    }

    protected boolean pokeLifting() {
        if (!this.structureSeeds.isEmpty() || this.dataStorage.getLiftingBits() < 40F) return false;
        List<UniformStructure.Data<?>> dataList = new ArrayList<>();

        for (DataStorage.Entry<Feature.Data<?>> e : this.dataStorage.baseSeedData) {
            if (e.data.feature instanceof OldStructure || e.data.feature instanceof Shipwreck) {
                dataList.add((UniformStructure.Data<?>) e.data);
            }
        }
        List<Feature.Data<?>> cache = new ArrayList<>();

        for (DataStorage.Entry<Feature.Data<?>> entry : this.dataStorage.baseSeedData) {
            if (!(entry.data.feature instanceof Decorator) || entry.data.feature.getVersion().isOlderThan(MCVersion.v1_18)) {
                if (!(entry.data.feature instanceof PillagerOutpost)) {
                    cache.add(entry.data);
                }
            }
        }
        Log.warn("Starting lifting with " + dataList.size() + " structures...");

        Stream<Long> lowerBitsStream = LongStream.range(0, 1L << 19).boxed().filter(lowerBits -> {
            ChunkRand rand = new ChunkRand();
            for (UniformStructure.Data<?> data : dataList) {
                rand.setRegionSeed(lowerBits, data.regionX, data.regionZ, data.feature.getSalt(), Config.get().getVersion());
                if (rand.nextInt(((UniformStructure<?>) data.feature).getOffset()) % 4 != data.offsetX % 4 ||
                        rand.nextInt(((UniformStructure<?>) data.feature).getOffset()) % 4 != data.offsetZ % 4) {
                    return false;
                }
            }
            return true;
        });

        Stream<Long> seedStream = lowerBitsStream.flatMap(lowerBits ->
                LongStream.range(0, 1L << (48 - 19))
                        .boxed()
                        .map(upperBits -> (upperBits << 19) | lowerBits)
        );

        Stream<Long> structureSeedStream = seedStream.filter(seed -> {
            ChunkRand rand = new ChunkRand();
            for (Feature.Data<?> data : cache) {
                if (!data.testStart(seed, rand)) {
                    return false;
                }
            }
            return true;
        });

        this.structureSeeds = structureSeedStream.parallel().collect(Collectors.toSet());

        if (!this.structureSeeds.isEmpty()) {
            Log.warn("Structure seed search complete.");
        } else {
            Log.error("No structure seeds found.");
        }

        return !this.structureSeeds.isEmpty();
    }


    protected boolean pokeStructures() {
        if (this.pillarSeeds == null || !this.structureSeeds.isEmpty() ||
                this.dataStorage.getBaseBits() < this.dataStorage.getWantedBits()) return false;

        List<Feature.Data<?>> cache = new ArrayList<>();

        for (DataStorage.Entry<Feature.Data<?>> entry : this.dataStorage.baseSeedData) {
            if (!(entry.data.feature instanceof Decorator) || entry.data.feature.getVersion().isOlderThan(MCVersion.v1_18)) {
                if (!(entry.data.feature instanceof PillagerOutpost)) {
                    cache.add(entry.data);
                }
            }
        }

        for (int pillarSeed : this.pillarSeeds) {
            Log.debug("====================================");
            Log.warn("Looking for structure seeds (pillar " + pillarSeed + ")...");

            AtomicInteger completion = new AtomicInteger();
            ProgressListener progressListener = new ProgressListener();

            for (int threadId = 0; threadId < 4; threadId++) {
                int fThreadId = threadId;

                SERVICE.submit(() -> {
                    ChunkRand rand = new ChunkRand();

                    long lower = (long) fThreadId * (1L << 30);
                    long upper = (long) (fThreadId + 1) * (1L << 30);

                    for (long partialWorldSeed = lower; partialWorldSeed < upper && !this.shouldTerminate; partialWorldSeed++) {
                        if ((partialWorldSeed & ((1 << 27) - 1)) == 0) {
                            progressListener.addPercent(3.125F, true);
                        }

                        long seed = this.timeMachine(partialWorldSeed, pillarSeed);

                        boolean matches = true;

                        for (Feature.Data<?> baseSeedDatum : cache) {
                            if (!baseSeedDatum.testStart(seed, rand)) {
                                matches = false;
                                break;
                            }
                        }

                        if (matches) {
                            this.structureSeeds.add(seed);
                            Log.printSeed("Found structure seed: ${SEED}", seed);
                        }

                    }

                    completion.getAndIncrement();
                });
            }

            while (completion.get() != 4) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (this.shouldTerminate) {
                    return false;
                }
            }

            progressListener.addPercent(0.0F, true);
        }

        if (!this.structureSeeds.isEmpty()) {
            Log.warn("Structure seed search complete.");
        } else {
            Log.error("No structure seeds found.");
        }

        return true;
    }

    protected boolean pokeBiomes() {
        if (this.structureSeeds.isEmpty() || this.worldSeeds.size() == 1) return false;
        if (this.structureSeeds.size() > 1000) return false;

        Log.debug("====================================");

        worldSeeds.clear();
        dataStorage.baseSeedData.dump();

        if (this.dataStorage.hashedSeedData != null && this.dataStorage.hashedSeedData.getHashedSeed() != 0) {
            Log.warn("Using hashed seed to find world seed...");
            for (long structureSeed : this.structureSeeds) {
                WorldSeed.fromHash(structureSeed, this.dataStorage.hashedSeedData.getHashedSeed()).forEach(worldSeed -> {
                    this.worldSeeds.add(worldSeed);
                    Log.printSeed("Found world seed using hashed seed: ${SEED}", worldSeed);
                });

                if (this.shouldTerminate) {
                    return false;
                }
            }

            if (!this.worldSeeds.isEmpty()) {
                Log.warn("World seed search complete.");
                return true;
            } else {
                this.dataStorage.hashedSeedData = null;
                Log.error("Hashed seed failed, reverting to biomes...");
            }
        }

        this.dataStorage.biomeSeedData.dump();
        if (this.dataStorage.notEnoughBiomeData()) {
            Log.error("Need more biome data (at least 7).");
            return false;
        }

        Log.warn("Searching for world seed using " + this.dataStorage.biomeSeedData.size() + " biome samples...");
        MCVersion version = Config.get().getVersion();
        for (long structureSeed : this.structureSeeds) {
            for (long worldSeed : StructureSeed.toRandomWorldSeeds(structureSeed)) {
                OverworldBiomeSource source = new OverworldBiomeSource(version, worldSeed);

                boolean matches = true;

                for (DataStorage.Entry<BiomeData> e : this.dataStorage.biomeSeedData) {
                    if (!e.data.test(source)) {
                        matches = false;
                        break;
                    }
                }

                if (matches) {
                    this.worldSeeds.add(worldSeed);
                    Log.printSeed("Found world seed: ${SEED}", worldSeed);
                }
                if (this.shouldTerminate) {
                    return false;
                }
            }
        }

        if (!this.worldSeeds.isEmpty()) return true;
        if (this.structureSeeds.size() > 10) return false;

        Log.warn("Deep biome search...");
        for (long structureSeed : this.structureSeeds) {
            for (long upperBits = 0; upperBits < 1 << 16 && !this.shouldTerminate; upperBits++) {
                long worldSeed = (upperBits << 48) | structureSeed;

                OverworldBiomeSource source = new OverworldBiomeSource(version, worldSeed);

                boolean matches = true;

                for (DataStorage.Entry<BiomeData> e : this.dataStorage.biomeSeedData) {
                    if (!e.data.test(source)) {
                        matches = false;
                        break;
                    }
                }

                if (matches) {
                    this.worldSeeds.add(worldSeed);
                    if (this.worldSeeds.size() < 10) {
                        Log.printSeed("Found world seed: ${SEED}", worldSeed);
                    } else {
                        logger.info("Found world seed " + worldSeed);
                    }
                }

                if (this.shouldTerminate) {
                    return false;
                }
            }
        }

        if (!this.worldSeeds.isEmpty()) {
            Log.warn("World seed search complete.");
        } else {
            Log.error("No world seeds found.");
        }

        if (!this.worldSeeds.isEmpty()) return true;

        Log.warn("Outputting random world seeds for structure seeds...");
        for (long structureSeed : this.structureSeeds) {
            StructureSeed.toRandomWorldSeeds(structureSeed).forEach(s ->
                    Log.printSeed("Possible world seed: ${SEED}", s));
        }

        return true;
    }

    protected boolean pokeStructureReduce() {
        if (shouldTerminate) return false;
        if (!this.worldSeeds.isEmpty() || this.structureSeeds.size() < 2) return false;
        if (Config.get().getVersion().isOlderThan(MCVersion.v1_13)) return false;

        Set<Long> result = new HashSet<>();
        Log.debug("====================================");
        Log.warn("Reducing " + this.structureSeeds.size() + " structure seeds...");

        if (this.pillarSeeds != null) {
            structureSeeds.forEach(seed -> {
                if (this.pillarSeeds.contains((int) PillarSeed.fromStructureSeed(seed))) {
                    result.add(seed);
                }
            });
        }

        if (result.size() != 1) {
            this.dataStorage.baseSeedData.dump();
            List<Feature.Data<?>> cache = new ArrayList<>();

            for (DataStorage.Entry<Feature.Data<?>> entry : this.dataStorage.baseSeedData) {
                if (!(entry.data.feature instanceof Decorator) || entry.data.feature.getVersion().isOlderThan(MCVersion.v1_18)) {
                    if (!(entry.data.feature instanceof PillagerOutpost)) {
                        cache.add(entry.data);
                    }
                }
            }
            ChunkRand rand = new ChunkRand();

            for (Long seed : this.structureSeeds) {
                boolean matches = true;

                for (Feature.Data<?> baseSeedDatum : cache) {
                    if (!baseSeedDatum.testStart(seed, rand)) {
                        matches = false;
                        break;
                    }
                }

                if (matches) {
                    result.add(seed);
                }
            }
        }

        if (!result.isEmpty() && this.structureSeeds.size() > result.size()) {
            if (result.size() < 10) {
                result.forEach(seed -> Log.printSeed("Structure seed: ${SEED}", seed));
            } else {
                Log.warn("Reduced to " + result.size() + " structure seeds.");
            }

            this.structureSeeds = result;
            return true;
        } else {
            Log.warn("Could not reduce structure seeds.");
        }
        return false;
    }

    public long timeMachine(long partialWorldSeed, int pillarSeed) {
        long currentSeed = 0L;
        currentSeed |= (partialWorldSeed & 0xFFFF0000L) << 16;
        currentSeed |= (long) pillarSeed << 16;
        currentSeed |= partialWorldSeed & 0xFFFFL;

        currentSeed = this.inverseLCG.nextSeed(currentSeed);
        currentSeed ^= LCG.JAVA.multiplier;
        return currentSeed;
    }

    public enum Phase {
        BIOMES(null), STRUCTURES(BIOMES), LIFTING(BIOMES), PILLARS(STRUCTURES);

        private final Phase nextPhase;

        Phase(Phase nextPhase) {
            this.nextPhase = nextPhase;
        }

        public Phase nextPhase() {
            return this.nextPhase;
        }
    }
}
