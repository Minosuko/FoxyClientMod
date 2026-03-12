package com.foxyclient;

import com.foxyclient.command.CommandManager;
import com.foxyclient.event.EventBus;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.gui.ClickGUI;
import com.foxyclient.module.Module;
import com.foxyclient.module.ModuleManager;
import com.foxyclient.module.ui.HUD;
import com.foxyclient.pathfinding.PathFinder;
import com.foxyclient.util.FriendsManager;
import com.foxyclient.util.WaypointManager;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

/**
 * FoxyClient - A unified Minecraft client mod.
 * Combines features from Meteor Client, Meteor-Rejected, SeedCracker, Baritone, and MeteorPlus.
 */
public class FoxyClient implements ClientModInitializer {
    public static final String NAME = "FoxyClient";
    public static final String VERSION = "1.2.5-beta";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    public static FoxyClient INSTANCE;

    private EventBus eventBus;
    private ModuleManager moduleManager;
    private CommandManager commandManager;
    private PathFinder pathFinder;
    private FriendsManager friendsManager;
    private WaypointManager waypointManager;
    private ClickGUI clickGUI;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        LOGGER.info("§6[FoxyClient]§f Initializing FoxyClient v{}...", VERSION);

        // Core systems
        eventBus = new EventBus();
        friendsManager = new FriendsManager();
        waypointManager = new WaypointManager();
        commandManager = new CommandManager();
        pathFinder = new PathFinder();
        moduleManager = new ModuleManager();
        moduleManager.init();

        LOGGER.info("§6[FoxyClient]§f Loaded {} modules.", moduleManager.getModules().size());
        LOGGER.info("§6[FoxyClient]§f {} friends loaded.", friendsManager.getFriends().size());
        LOGGER.info("§6[FoxyClient]§f {} waypoints loaded.", waypointManager.getAll().size());
        LOGGER.info("§6[FoxyClient]§a FoxyClient initialized successfully!");
    }

    /**
     * Called every client tick from MixinMinecraftClient.
     */
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        // Open ClickGUI on Right Shift
        if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) {
            if (mc.currentScreen == null) {
                if (clickGUI == null) clickGUI = new ClickGUI();
                mc.setScreen(clickGUI);
            }
        }

        // Tick pathfinder
        pathFinder.tick();

        // Tick SeedCracker data storage to commit pending data
        if (com.foxyclient.seedcracker.FoxySeedCracker.get() != null) {
            com.foxyclient.seedcracker.FoxySeedCracker.get().getDataStorage().tick();
        }
    }

    /**
     * Renders the HUD overlay (called from MixinInGameHud).
     */
    public void renderHUD(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (mc.options.hudHidden) return;

        var textRenderer = mc.textRenderer;
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        HUD hud = moduleManager.getModule(HUD.class);

        // ===== Watermark =====
        if (hud.watermark.get()) {
            String watermark = "§bFoxyClient §7v" + VERSION;
            context.fill(2, 2, textRenderer.getWidth("FoxyClient v" + VERSION) + 8, 14, 0x88000000);
            context.fill(2, 2, textRenderer.getWidth("FoxyClient v" + VERSION) + 8, 3, 0xFF00E5CC);
            context.drawTextWithShadow(textRenderer, watermark, 4, 4, 0xFFFFFFFF);
        }

        // ===== Active Modules ArrayList =====
        if (hud.cheatList.get()) {
            List<Module> active = moduleManager.getModules().stream()
                .filter(m -> m.isEnabled() && m.isVisible())
                .sorted(Comparator.comparingInt(m -> -textRenderer.getWidth(m.getName())))
                .toList();

            int y = 4;
            for (Module module : active) {
                String name = module.getName();
                int nameWidth = textRenderer.getWidth(name);
                int x = screenWidth - nameWidth - 4;

                context.fill(x - 2, y - 1, screenWidth, y + 10, 0x88000000);
                context.fill(screenWidth - 1, y - 1, screenWidth, y + 10, 0xFF00E5CC);
                int hue = (y * 3) % 360;
                float[] rgb = java.awt.Color.getHSBColor(hue / 360f, 0.8f, 1.0f).getRGBColorComponents(null);
                int color = 0xFF000000 | ((int)(rgb[0] * 255) << 16) | ((int)(rgb[1] * 255) << 8) | (int)(rgb[2] * 255);
                context.drawTextWithShadow(textRenderer, name, x, y, color);
                y += 11;
            }
        }

        // ===== Bottom Right Info (Dynamic Stacking) =====
        int bottomRightY = screenHeight - 12;

        // Coordinates
        if (hud.coordinates.get()) {
            String coords = String.format("§7XYZ: §f%.1f §7/ §f%.1f §7/ §f%.1f",
                mc.player.getX(), mc.player.getY(), mc.player.getZ());
            int coordsWidth = textRenderer.getWidth(coords.replaceAll("§.", ""));
            context.fill(screenWidth - coordsWidth - 6, bottomRightY - 2, screenWidth - 2, bottomRightY + 10, 0x88000000);
            context.drawTextWithShadow(textRenderer, coords, screenWidth - coordsWidth - 4, bottomRightY, 0xFFFFFFFF);
            bottomRightY -= 12;
        }

        // Facing
        if (hud.facing.get()) {
            net.minecraft.util.math.Direction dir = mc.player.getHorizontalFacing();
            String axis = switch (dir) {
                case NORTH -> "-Z";
                case SOUTH -> "+Z";
                case WEST -> "-X";
                case EAST -> "+X";
                default -> "";
            };
            String facingStr = String.format("§7Facing: §f%s §7(%s)", 
                dir.getId().substring(0, 1).toUpperCase() + dir.getId().substring(1), axis);
            int facingWidth = textRenderer.getWidth(facingStr.replaceAll("§.", ""));
            context.fill(screenWidth - facingWidth - 6, bottomRightY - 2, screenWidth - 2, bottomRightY + 10, 0x88000000);
            context.drawTextWithShadow(textRenderer, facingStr, screenWidth - facingWidth - 4, bottomRightY, 0xFFFFFFFF);
            bottomRightY -= 12;
        }

        // FPS
        if (hud.fps.get()) {
            String fpsStr = "§7FPS: §a" + mc.getCurrentFps();
            int fpsWidth = textRenderer.getWidth(fpsStr.replaceAll("§.", ""));
            context.fill(screenWidth - fpsWidth - 6, bottomRightY - 2, screenWidth - 2, bottomRightY + 10, 0x88000000);
            context.drawTextWithShadow(textRenderer, fpsStr, screenWidth - fpsWidth - 4, bottomRightY, 0xFFFFFFFF);
            bottomRightY -= 12;
        }

        // Ping
        if (hud.ping.get()) {
            int p = 0;
            if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null) {
                p = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency();
            }
            String pingStr = "§7Ping: §e" + p + "ms";
            int pingWidth = textRenderer.getWidth(pingStr.replaceAll("§.", ""));
            context.fill(screenWidth - pingWidth - 6, bottomRightY - 2, screenWidth - 2, bottomRightY + 10, 0x88000000);
            context.drawTextWithShadow(textRenderer, pingStr, screenWidth - pingWidth - 4, bottomRightY, 0xFFFFFFFF);
            bottomRightY -= 12;
        }

        // Speed
        if (hud.speed.get()) {
            double speed = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x +
                mc.player.getVelocity().z * mc.player.getVelocity().z) * 20;
            String speedText = String.format("§7Speed: §f%.1f b/s", speed);
            int speedWidth = textRenderer.getWidth(speedText.replaceAll("§.", ""));
            context.fill(screenWidth - speedWidth - 6, bottomRightY - 2, screenWidth - 2, bottomRightY + 10, 0x88000000);
            context.drawTextWithShadow(textRenderer, speedText, screenWidth - speedWidth - 4, bottomRightY, 0xFFFFFFFF);
            bottomRightY -= 12;
        }

        // TPS
        if (hud.tps.get()) {
            String tpsStr = String.format("§7TPS: §a%.1f", com.foxyclient.util.TickRateManager.INSTANCE.getTPS());
            int tpsWidth = textRenderer.getWidth(tpsStr.replaceAll("§.", ""));
            context.fill(screenWidth - tpsWidth - 6, bottomRightY - 2, screenWidth - 2, bottomRightY + 10, 0x88000000);
            context.drawTextWithShadow(textRenderer, tpsStr, screenWidth - tpsWidth - 4, bottomRightY, 0xFFFFFFFF);
            bottomRightY -= 12;
        }

        // ===== Bottom Left Info =====
        int leftY = screenHeight - 12;

        // Server Address
        if (hud.server.get() && mc.getCurrentServerEntry() != null) {
            String server = "§7Server: §f" + mc.getCurrentServerEntry().address;
            context.drawTextWithShadow(textRenderer, server, 4, leftY, 0xFFFFFFFF);
            leftY -= 12;
        }

        // Baritone status (on top of SC if both active)
        if (pathFinder.isActive()) {
            String process = pathFinder.getCurrentProcessName();
            String pathStatus = "§7[§6Baritone§7] §e" + process;
            if (pathFinder.isPaused()) pathStatus += " §c(PAUSED)";
            context.drawTextWithShadow(textRenderer, pathStatus, 4, leftY, 0xFFFFFFFF);
            leftY -= 12;
        }

        // SeedCracker Detailed Progress
        var sc = com.foxyclient.seedcracker.FoxySeedCracker.get();
        if (sc != null && sc.getDataStorage() != null && com.foxyclient.seedcracker.config.Config.get().active) {
            var storage = sc.getDataStorage();
            var tm = storage.getTimeMachine();
            int structures = storage.baseSeedData.size();
            double baseBits = storage.getBaseBits();
            double wantedBits = storage.getWantedBits();

            String bitsColor = baseBits >= wantedBits ? "§a" : "§e";
            String line1 = String.format("§7[§6SC§7] §fStructures: §a%d §7| Bits: %s%.1f§7/§a%.0f", structures, bitsColor, baseBits, wantedBits);
            context.drawTextWithShadow(textRenderer, line1, 4, leftY, 0xFFFFFFFF);
            leftY -= 12;

            String phase = "Collecting...";
            if (tm.worldSeeds.size() == 1) phase = "§a§lSeed Found: " + tm.worldSeeds.iterator().next();
            else if (!tm.worldSeeds.isEmpty()) phase = "§eWorld seeds: " + tm.worldSeeds.size();
            else if (!tm.structureSeeds.isEmpty()) phase = "§eStructure seeds: " + tm.structureSeeds.size() + " §7→ Biomes";
            else if (baseBits >= wantedBits) phase = "§eCracking...";
            
            context.drawTextWithShadow(textRenderer, "§7[§6SC§7] " + phase, 4, leftY, 0xFFFFFFFF);
        }

        // Dispatch Render2DEvent for modules
        // Dispatch Render2DEvent for modules
        eventBus.post(new com.foxyclient.event.events.Render2DEvent(context, mc.getRenderTickCounter().getTickProgress(false)));
    }

    // Getters
    public EventBus getEventBus() { return eventBus; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public CommandManager getCommandManager() { return commandManager; }
    public PathFinder getPathFinder() { return pathFinder; }
    public FriendsManager getFriendsManager() { return friendsManager; }
    public WaypointManager getWaypointManager() { return waypointManager; }
}
