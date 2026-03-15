package com.foxyclient.module;

import com.foxyclient.module.combat.*;
import com.foxyclient.module.movement.*;
import com.foxyclient.module.movement.BunnyHop;
import com.foxyclient.module.movement.Strafe;
import com.foxyclient.module.movement.FastFall;
import com.foxyclient.module.movement.NoJumpDelay;
import com.foxyclient.module.movement.EntityControl;
import com.foxyclient.module.movement.Phase;
import com.foxyclient.module.movement.TickShift;
import com.foxyclient.module.render.*;
import com.foxyclient.module.render.HoleESP;
import com.foxyclient.module.render.CityESP;
import com.foxyclient.module.render.BurrowESP;
import com.foxyclient.module.render.LogoutSpots;
import com.foxyclient.module.render.SoundESP;
import com.foxyclient.module.render.BreakIndicators;
import com.foxyclient.module.player.*;
import com.foxyclient.module.player.InventoryCleaner;
import com.foxyclient.module.player.AntiAim;
import com.foxyclient.module.player.PotionSaver;
import com.foxyclient.module.player.SmartEat;
import com.foxyclient.module.world.*;
import com.foxyclient.module.misc.*;
import com.foxyclient.module.misc.Proxy;
import com.foxyclient.module.exploit.*;
import com.foxyclient.module.exploit.PingSpoof;
import com.foxyclient.module.exploit.ServerSync;
import com.foxyclient.module.exploit.FastLatency;
import com.foxyclient.module.seedcracker.*;

import com.foxyclient.module.world.AutoTunnel;
import com.foxyclient.module.world.AutoBuild;
import com.foxyclient.module.world.FoxyBot;
import com.foxyclient.module.world.NoDesync;
import com.foxyclient.module.ui.*;
import com.google.gson.Gson;
import com.foxyclient.module.combat.AutoTrap;
import com.foxyclient.module.combat.HoleFill;
import com.foxyclient.module.combat.SelfTrap;
import com.foxyclient.module.combat.AutoLog;
import com.foxyclient.module.combat.AntiSurround;
import com.foxyclient.module.combat.AutoWeb;
import com.foxyclient.module.combat.AutoCity;
import com.foxyclient.module.combat.AntiBed;
import com.foxyclient.module.combat.AntiAnchor;
import com.foxyclient.module.combat.Burrow;
import com.foxyclient.module.combat.PacketMine;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

/**
 * Manages all modules: registration, keybinds, config save/load.
 */
public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;
    private Module lastToggledModule;

    public ModuleManager() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("foxyclient");
        try { Files.createDirectories(configPath); } catch (IOException ignored) {}
    }

    public void init() {
        register(new KillAura());
        register(new CrystalAura());
        register(new AutoTotem());
        register(new BowAimbot());
        register(new Criticals());
        register(new Velocity());
        register(new AntiBot());
        register(new Surround());
        register(new AnchorAura());
        register(new BedAura());
        register(new Offhand());
        register(new AutoTrap());
        register(new HoleFill());
        register(new SelfTrap());
        register(new AutoLog());
        register(new AntiSurround());
        register(new AutoWeb());
        register(new AutoCity());
        register(new AntiBed());
        register(new AntiAnchor());
        register(new Burrow());
        register(new PacketMine());
        register(new AimAssist());
        register(new KnockbackPlus());
        register(new AutoPot());
        register(new AutoSoup());
        register(new ChestAura());
        register(new TreeAura());
        register(new VehicleOneHit());
        register(new ShieldBypass());
        register(new Hunt());
        register(new TriggerBot());
        register(new TPAura());
        register(new ExplosionAura());
        register(new BoatKill());
        register(new MaceKill());
        register(new LavaAura());

        // ====== Movement (11) ======
        register(new Fly());
        register(new Speed());
        register(new Step());
        register(new NoFall());
        register(new Sprint());
        register(new Scaffold());
        register(new Jesus());
        register(new NoSlow());
        register(new BoatFly());
        register(new ReverseStep());
        register(new ElytraFly());
        register(new BunnyHop());
        register(new Strafe());
        register(new FastFall());
        register(new NoJumpDelay());
        register(new EntityControl());
        register(new Phase());
        register(new TickShift());
        register(new BoatPhase());
        register(new Boost());
        register(new FullFlight());
        register(new Glide());
        register(new Jetpack());
        register(new RoboWalk());
        register(new FastLadderPlus());
        register(new SpeedPlus());
        register(new FlyPlus());
        register(new SpiderPlus());
        register(new JesusPlus());
        register(new ElytraFlyPlus());
        register(new NoSlowPlus());
        register(new BoatNoclip());
        register(new TPFly());
        register(new AirJump());

        // ====== Render (12) ======
        register(new ESP());
        register(new Tracers());
        register(new Fullbright());
        register(new Freecam());
        register(new Nametags());
        register(new StorageESP());
        register(new BowIndicator());
        register(new Freelook());
        register(new WallHack());
        register(new NoRender());
        register(new Hitboxes());
        register(new BlockESP());
        register(new Zoom());
        register(new OreFinder());
        register(new HoleESP());
        register(new CityESP());
        register(new BurrowESP());
        register(new LogoutSpots());
        register(new SoundESP());
        register(new BreakIndicators());
        register(new GhostMode());
        register(new NewChunks());
        register(new SkeletonESP());
        register(new Painter());
        register(new OreSim());
        register(new ItemFrameESP());
        register(new KillEffect());
        register(new EyeFinder());
        register(new CustomBlocks());
        register(new MobGearESP());
        register(new Hologram());
        register(new TrailMaker());
        register(new NewerNewChunks());
        register(new PotESP());
        register(new CollectibleESP());
        register(new XRay());

        // ====== Player (11) ======
        register(new AutoEat());
        register(new AutoTool());
        register(new AutoArmor());
        register(new FastPlace());
        register(new Reach());
        register(new AntiHunger());
        register(new AutoFish());
        register(new AutoRespawn());
        register(new FakePlayer());
        register(new FastUse());
        register(new XCarry());
        register(new InventoryCleaner());
        register(new AntiAim());
        register(new PotionSaver());
        register(new SmartEat());
        register(new AntiVanish());
        register(new AutoCraft());
        register(new AutoEnchant());
        register(new AutoExtinguish());
        register(new AutoGrind());
        register(new ColorSigns());
        register(new Confuse());
        register(new SilentDisconnect());
        register(new AutoRename());
        register(new AutoDropPlus());
        register(new Freeze());
        register(new NbtEditor());
        register(new AutoCommand());

        // ====== World (7) ======
        register(new Nuker());
        register(new Timer());
        register(new AutoSign());
        register(new AutoMine());
        register(new HighwayBuilder());
        register(new NoGhostBlocks());
        register(new LiquidInteract());
        register(new AutoTunnel());
        register(new AutoBuild());
        register(new NoDesync());
        register(new AutoFarm());
        register(new LawnBot());
        register(new MossBot());
        register(new GhostBlockFixer());
        register(new SafeMine());
        register(new AutoStaircase());
        register(new AutoMountain());
        register(new AutoLavaCaster());
        register(new RedstoneNuker());
        register(new StorageLooter());
        register(new BetterScaffold());
        register(new FoxyBot());

        // ====== Misc (13) ======
        register(new AutoReconnect());
        register(new AntiAFK());
        register(new Spam());
        register(new BetterChat());
        register(new Spinbot());
        register(new AutoGG());
        register(new ChatTweaks());
        register(new MessageLogger());
        register(new PacketCanceller());
        register(new AutoLogin());
        register(new InventoryTweaks());
        register(new ItemSearch());
        register(new ChatBot());
        register(new CoordLogger());
        register(new GamemodeNotifier());
        register(new InteractionMenu());
        register(new ServerFinder());
        register(new SoundLocator());
        register(new ExtraElytra());
        register(new Teams());
        register(new ChatPrefix());
        register(new AutoLeave());
        register(new AutoAccept());
        register(new PlayerAlarms());
        register(new Proxy());

        // ====== Exploit (4) ======
        register(new PacketFly());
        register(new ServerSync());
        register(new FastLatency());
        register(new PingSpoof());
        register(new AutoBedTrap());
        register(new AntiCrash());
        register(new ArrowDmg());
        register(new AutoTNT());
        register(new AutoWither());
        register(new BlockIn());
        register(new BungeeCordSpoof());
        register(new ChorusExploit());
        register(new CustomPackets());
        register(new InstaMine());
        register(new ItemGenerator());
        register(new Lavacast());
        register(new ObsidianFarm());
        register(new XrayBruteforce());
        register(new BedrockStorageBruteforce());
        register(new PortalGodMode());
        register(new BookAndQuillDupe());
        register(new BoomPlus());
        register(new ForceOPBook());
        register(new ForceOPSign());
        register(new InfiniteReach());
        register(new InfiniteTools());
        register(new InfiniteElytra());
        register(new LecternCrash());
        register(new ShulkerDupe());
        register(new PacketDelay());
        register(new FlightAntikick());
        register(new MultiverseAnnihilator());

        // ====== UI (6) ======
        register(new TotemCounter());
        register(new CombatHUD());
        register(new CrystalStats());
        register(new TargetInfoHUD());
        register(new ArmorHUD());
        register(new HUD());
        register(new CustomKeybinds());
        register(new ExtendedConfig());
        register(new BoatGlitch());
        register(new AntiSpawnpoint());
        register(new ShulkerView());
        register(new Minimap());
        register(new StorageView());

        // ====== SeedCracker (1) ======
        register(new SeedCrackerModule()).setVisible(false);

        loadConfig();
    }

    private Module register(Module module) {
        modules.add(module);
        return module;
    }

    public List<Module> getModules() { return modules; }

    public Module getLastToggledModule() { return lastToggledModule; }
    public void setLastToggledModule(Module module) { this.lastToggledModule = module; }

    public Module getModule(String name) {
        for (Module m : modules) {
            if (m.getName().equalsIgnoreCase(name)) return m;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> clazz) {
        for (Module m : modules) {
            if (m.getClass().equals(clazz)) return (T) m;
        }
        return null;
    }

    public List<Module> getModulesByCategory(Category category) {
        return modules.stream()
            .filter(m -> m.getCategory() == category && m.isVisible())
            .sorted(Comparator.comparing(Module::getName))
            .toList();
    }

    public void onKey(int key, int action) {
        if (action == 2) return; // Ignore repeat
        
        for (Module m : modules) {
            if (m.getKeybind() == key) {
                if (m.getName().equalsIgnoreCase("Freelook")) {
                    if (action == 1) m.setEnabled(true);
                    else if (action == 0) m.setEnabled(false);
                } else if (m.getName().equalsIgnoreCase("Zoom")) {
                    if (action == 1) m.setEnabled(true);
                    // Zoom now handles its own release animation and disablement via KeyEvent handler
                } else if (action == 1) { // Normal toggle on press
                    m.toggle();
                }
            }
        }
    }

    public void saveConfig() {
        JsonObject root = new JsonObject();
        for (Module module : modules) {
            try {
                JsonObject obj = new JsonObject();
                obj.addProperty("enabled", module.isEnabled());
                obj.addProperty("keybind", module.getKeybind());

                JsonObject settings = new JsonObject();
                for (var setting : module.getSettings()) {
                    settings.add(setting.getName(), setting.toJson());
                }
                obj.add("settings", settings);

                root.add(module.getName(), obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Path file = configPath.resolve("foxyclient.json");
        try (Writer writer = Files.newBufferedWriter(file)) {
            gson.toJson(root, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadConfig() {
        // Try loading consolidated config first
        Path consolidatedFile = configPath.resolve("foxyclient.json");
        if (Files.exists(consolidatedFile)) {
            try (Reader reader = Files.newBufferedReader(consolidatedFile)) {
                JsonObject root = gson.fromJson(reader, JsonObject.class);
                if (root != null) {
                    for (Module module : modules) {
                        try {
                            JsonElement el = root.get(module.getName());
                            if (el == null || !el.isJsonObject()) continue;
                            JsonObject obj = el.getAsJsonObject();
                            loadModuleFromJson(module, obj);
                        } catch (Exception e) {
                            System.err.println("[FoxyClient] Failed to load config for " + module.getName());
                            e.printStackTrace();
                        }
                    }
                    return; // Done, skip legacy loading
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Fallback: load from individual files (legacy format)
        for (Module module : modules) {
            Path file = configPath.resolve(module.getName().toLowerCase().replace(" ", "_") + ".json");
            if (!Files.exists(file)) continue;

            try (Reader reader = Files.newBufferedReader(file)) {
                JsonObject obj = gson.fromJson(reader, JsonObject.class);
                if (obj == null) continue;
                loadModuleFromJson(module, obj);
            } catch (Exception e) {
                System.err.println("[FoxyClient] Failed to load legacy config for " + module.getName());
                e.printStackTrace();
            }
        }

        // Migrate: save as consolidated format
        saveConfig();
    }

    private void loadModuleFromJson(Module module, JsonObject obj) {
        if (obj.has("keybind")) {
            module.setKeybind(obj.get("keybind").getAsInt());
        }

        if (obj.has("settings")) {
            JsonObject settings = obj.getAsJsonObject("settings");
            for (var setting : module.getSettings()) {
                JsonElement el = settings.get(setting.getName());
                if (el != null) setting.fromJson(el);
            }
        }

        if (obj.has("enabled") && obj.get("enabled").getAsBoolean()) {
            module.setEnabled(true);
        }
    }

}
