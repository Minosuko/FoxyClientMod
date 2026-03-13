package com.foxyclient.module.ui;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.Render2DEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.WaypointManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.world.Heightmap;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.gui.ScreenRect;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

/**
 * Premium Minimap HUD module.
 * Features: glassmorphic background, glow border, height-shaded terrain,
 * triangular player arrow, distinct entity markers, styled compass, info panel.
 */
public class Minimap extends Module {
    // --- Settings ---
    private final NumberSetting zoom = addSetting(new NumberSetting("Zoom", "Zoom level", 1.0, 0.5, 5.0));
    private final NumberSetting scale = addSetting(new NumberSetting("Scale", "Size of the map", 120.0, 50.0, 300.0));
    private final NumberSetting xOffset = addSetting(new NumberSetting("X Offset", "X position", 10.0, 0.0, 1000.0));
    private final NumberSetting yOffset = addSetting(new NumberSetting("Y Offset", "Y position", 10.0, 0.0, 1000.0));
    private final NumberSetting mapAlpha = addSetting(new NumberSetting("Alpha", "Map transparency", 0.75, 0.1, 1.0));
    private final NumberSetting markerScale = addSetting(new NumberSetting("Marker Scale", "Size of entities/waypoints", 1.5, 0.5, 3.0));
    private final BoolSetting northLock = addSetting(new BoolSetting("North Lock", "Lock map rotation to North", false));
    private final BoolSetting circular = addSetting(new BoolSetting("Circular", "Circular minimap shape", true));
    private final BoolSetting entities = addSetting(new BoolSetting("Entities", "Show nearby entities", true));
    private final BoolSetting waypoints = addSetting(new BoolSetting("Waypoints", "Show registered waypoints", true));
    private final BoolSetting terrain = addSetting(new BoolSetting("Terrain", "Show basic terrain colors", true));
    private final BoolSetting info = addSetting(new BoolSetting("Info", "Show coordinates and biome", true));
    private final ModeSetting borderColor = addSetting(new ModeSetting("Border Color", "Border accent color", "Cyan", "Cyan", "Purple", "Rainbow", "White"));
    private final ModeSetting markerStyle = addSetting(new ModeSetting("Marker Style", "Entity marker shape", "Diamonds", "Dots", "Diamonds"));

    // --- Terrain cache ---
    private static final int CACHE_SIZE = 512;
    private final int[][] terrainCache = new int[CACHE_SIZE][CACHE_SIZE];
    private final int[][] terrainHeightCache = new int[CACHE_SIZE][CACHE_SIZE];
    private final long[][] terrainCachePos = new long[CACHE_SIZE][CACHE_SIZE];

    // Border color constants
    private static final int GLOW_LAYERS = 6;

    public Minimap() {
        super("Minimap", "Displays a premium minimap on your HUD", Category.UI);
    }

    // =========================================================================
    // Main render entry
    // =========================================================================

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (nullCheck()) return;

        DrawContext ctx = event.getContext();
        float size = scale.get().floatValue();
        float x = xOffset.get().floatValue();
        float y = yOffset.get().floatValue();
        float centerX = x + size / 2f;
        float centerY = y + size / 2f;
        float radius = size / 2f;

        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();
        float yaw = northLock.get() ? 0 : mc.player.getYaw();

        // 1) Outer glow
        drawGlow(ctx, centerX, centerY, radius);

        // 2) Background
        int bgAlpha = (int) (mapAlpha.get().floatValue() * 255);
        int bgColor = (bgAlpha << 24) | 0x0C0C14; // dark navy tint
        if (circular.get()) {
            fillCircle(ctx, centerX, centerY, radius, bgColor);
        } else {
            ctx.fill((int) x, (int) y, (int) (x + size), (int) (y + size), bgColor);
        }

        // 3) Terrain
        if (terrain.get()) {
            drawTerrain(ctx, centerX, centerY, radius, playerX, playerZ, yaw);
        }

        // 4) Entity markers
        if (entities.get()) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity == mc.player) continue;
                int color;
                if (entity instanceof PlayerEntity) {
                    color = 0xFFFFFFFF; // white for players
                } else if (entity instanceof HostileEntity) {
                    color = 0xFFFF4444; // red for hostiles
                } else {
                    color = 0xFFFF9933; // orange for others
                }
                renderMarker(ctx, entity.getX(), entity.getZ(), playerX, playerZ, yaw, centerX, centerY, radius, color);
            }
        }

        // 5) Waypoint markers
        if (waypoints.get()) {
            String dim = mc.world.getRegistryKey().getValue().toString();
            for (WaypointManager.Waypoint wp : FoxyClient.INSTANCE.getWaypointManager().getForDimension(dim)) {
                renderWaypoint(ctx, wp, playerX, playerZ, yaw, centerX, centerY, radius);
            }
        }

        // 6) Player arrow
        drawPlayerArrow(ctx, centerX, centerY, yaw);

        // 7) Accent border ring
        drawBorder(ctx, centerX, centerY, radius);

        // 8) Compass labels
        drawCompassLabels(ctx, centerX, centerY, radius, yaw);

        // 9) Info panel
        if (info.get()) {
            drawInfoPanel(ctx, centerX, y + size, radius);
        }
    }

    // =========================================================================
    // Glow effect — concentric translucent rings outside the border
    // =========================================================================

    private void drawGlow(DrawContext ctx, float cx, float cy, float radius) {
        int[] accent = getAccentColor();
        for (int i = GLOW_LAYERS; i >= 1; i--) {
            int alpha = Math.max(8, 35 - i * 5);
            int glowColor = (alpha << 24) | (accent[0] << 16) | (accent[1] << 8) | accent[2];
            float r = radius + i * 2f;
            if (circular.get()) {
                drawRing(ctx, cx, cy, r, r + 1.5f, glowColor);
            } else {
                int ix = (int) (cx - r), iy = (int) (cy - r), iw = (int) (r * 2), ih = (int) (r * 2);
                drawRectBorder(ctx, ix, iy, iw, ih, glowColor);
            }
        }
    }

    // =========================================================================
    // Accent border
    // =========================================================================

    private void drawBorder(DrawContext ctx, float cx, float cy, float radius) {
        int[] accent = getAccentColor();
        int borderOuter = 0xFF000000 | (accent[0] << 16) | (accent[1] << 8) | accent[2];
        int borderInner = 0x60FFFFFF;

        if (circular.get()) {
            drawRing(ctx, cx, cy, radius - 1f, radius, borderInner);
            drawRing(ctx, cx, cy, radius, radius + 1.5f, borderOuter);
        } else {
            float x = cx - radius, y = cy - radius, s = radius * 2;
            drawRectBorder(ctx, (int) x, (int) y, (int) s, (int) s, borderOuter);
            drawRectBorder(ctx, (int) x + 1, (int) y + 1, (int) s - 2, (int) s - 2, borderInner);
        }
    }

    private int[] getAccentColor() {
        String mode = borderColor.get();
        return switch (mode) {
            case "Purple" -> new int[]{180, 100, 255};
            case "White" -> new int[]{220, 220, 230};
            case "Rainbow" -> {
                float hue = (System.currentTimeMillis() % 3000) / 3000f;
                int rgb = java.awt.Color.HSBtoRGB(hue, 0.7f, 1.0f);
                yield new int[]{(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF};
            }
            default -> new int[]{0, 210, 255}; // Cyan
        };
    }

    // =========================================================================
    // Terrain rendering with elevation shading
    // =========================================================================

    private void drawTerrain(DrawContext ctx, float cx, float cy, float radius, double px, double pz, float yaw) {
        float zoomVal = zoom.get().floatValue();
        int res = 1;
        int range = (int) (radius / zoomVal) + res;

        float angle = (float) Math.toRadians(yaw);
        float cos = MathHelper.cos(angle);
        float sin = MathHelper.sin(angle);

        float radiusSq = radius * radius;
        int baseX = (int) Math.floor(px);
        int baseZ = (int) Math.floor(pz);

        float pixelSize = Math.max(1, (int) Math.ceil(zoomVal));
        boolean isCircular = circular.get();

        // Populate cache before rendering
        for (int dx = -range; dx <= range; dx += res) {
            for (int dz = -range; dz <= range; dz += res) {
                getCachedColor(baseX + dx, baseZ + dz);
            }
        }

        // Use a custom render state to batch all pixels into one element.
        // This avoids creating thousands of ColoredQuadGuiElementRenderState objects.
        TerrainRenderState terrainState = new TerrainRenderState(
            RenderPipelines.GUI,
            TextureSetup.empty(),
            new Matrix3x2f(ctx.getMatrices()),
            terrainCache,
            terrainHeightCache,
            baseX, baseZ, range, res,
            zoomVal, cos, sin, cx, cy, radius, radiusSq, pixelSize, isCircular,
            null // Scissor area
        );

        ctx.state.addSimpleElement(terrainState);
    }

    private static record TerrainRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        int[][] terrainCache,
        int[][] terrainHeightCache,
        int baseX, int baseZ, int range, int res,
        float zoomVal, float cos, float sin, float cx, float cy, float radius, float radiusSq,
        float pixelSize, boolean isCircular,
        @Nullable ScreenRect scissorArea
    ) implements SimpleGuiElementRenderState {

        @Override
        public void setupVertices(VertexConsumer vertices) {
            for (int dx = -range; dx <= range; dx += res) {
                int worldX = baseX + dx;
                float mapX = dx * zoomVal;
                float rotX_part1 = mapX * cos;
                float rotX_part2 = -mapX * sin;

                for (int dz = -range; dz <= range; dz += res) {
                    float mapZ = dz * zoomVal;
                    float rotX = rotX_part1 + mapZ * sin;
                    float rotZ = mapZ * cos + rotX_part2;

                    if (isCircular) {
                        if (rotX * rotX + rotZ * rotZ > radiusSq) continue;
                    } else {
                        if (Math.abs(rotX) > radius || Math.abs(rotZ) > radius) continue;
                    }

                    int worldZ = baseZ + dz;
                    // Cache is already populated in drawTerrain
                    int ix = Math.floorMod(worldX, CACHE_SIZE);
                    int iz = Math.floorMod(worldZ, CACHE_SIZE);
                    int color = terrainCache[ix][iz];
                    if (color == 0) continue;

                    int thisHeight = terrainHeightCache[ix][iz];
                    int neighborHeight = terrainHeightCache[ix][Math.floorMod(worldZ + 1, CACHE_SIZE)];
                    int delta = thisHeight - neighborHeight;

                    // Manual shading logic (matching shadeColor)
                    int r = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = color & 0xFF;
                    int shift = MathHelper.clamp(delta * 12, -40, 40);
                    r = MathHelper.clamp(r + shift, 0, 255);
                    g = MathHelper.clamp(g + shift, 0, 255);
                    b = MathHelper.clamp(b + shift, 0, 255);
                    
                    float fr = r / 255f;
                    float fg = g / 255f;
                    float fb = b / 255f;

                    float finalX = cx + rotX;
                    float finalY = cy - rotZ;

                    vertices.vertex(pose, finalX, finalY).color(fr, fg, fb, 1.0f);
                    vertices.vertex(pose, finalX, finalY + pixelSize).color(fr, fg, fb, 1.0f);
                    vertices.vertex(pose, finalX + pixelSize, finalY + pixelSize).color(fr, fg, fb, 1.0f);
                    vertices.vertex(pose, finalX + pixelSize, finalY).color(fr, fg, fb, 1.0f);
                }
            }
        }

        @Override
        public @Nullable ScreenRect bounds() {
            // Roughly cover the minimap area
            return new ScreenRect((int)(cx - radius), (int)(cy - radius), (int)(radius * 2), (int)(radius * 2));
        }
    }

    private int shadeColor(int color, int heightDelta) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // Lighten for higher, darken for lower
        int shift = MathHelper.clamp(heightDelta * 12, -40, 40);
        r = MathHelper.clamp(r + shift, 0, 255);
        g = MathHelper.clamp(g + shift, 0, 255);
        b = MathHelper.clamp(b + shift, 0, 255);

        return (r << 16) | (g << 8) | b;
    }

    private int getCachedColor(int x, int z) {
        int ix = Math.floorMod(x, CACHE_SIZE);
        int iz = Math.floorMod(z, CACHE_SIZE);
        long posKey = ((long) x << 32) | (z & 0xFFFFFFFFL);

        if (terrainCachePos[ix][iz] == posKey) {
            return terrainCache[ix][iz];
        }

        // Cache miss
        int topY = mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;
        BlockPos pos = new BlockPos(x, topY, z);
        if (pos.getY() < mc.world.getBottomY()) {
            terrainCache[ix][iz] = 0;
            terrainHeightCache[ix][iz] = 0;
            terrainCachePos[ix][iz] = posKey;
            return 0;
        }

        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) {
            topY = mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
            pos = new BlockPos(x, topY, z);
            state = mc.world.getBlockState(pos);
        }

        int color = getBlockColor(state);
        terrainCache[ix][iz] = color;
        terrainHeightCache[ix][iz] = topY;
        terrainCachePos[ix][iz] = posKey;
        return color;
    }

    private int getCachedHeight(int x, int z) {
        int ix = Math.floorMod(x, CACHE_SIZE);
        int iz = Math.floorMod(z, CACHE_SIZE);
        long posKey = ((long) x << 32) | (z & 0xFFFFFFFFL);

        if (terrainCachePos[ix][iz] == posKey) {
            return terrainHeightCache[ix][iz];
        }
        // Force populate cache
        getCachedColor(x, z);
        return terrainHeightCache[ix][iz];
    }

    private int getBlockColor(BlockState state) {
        MapColor mapColor = state.getMapColor(mc.world, BlockPos.ORIGIN);
        if (mapColor != null && mapColor.color != 0) return mapColor.color;
        if (state.isAir()) return 0;
        return 0x777777;
    }

    // =========================================================================
    // Player arrow — proper filled triangle
    // =========================================================================

    private void drawPlayerArrow(DrawContext ctx, float cx, float cy, float yaw) {
        float arrowSize = 6f;

        // Triangle points (pointing up before rotation):
        // tip (forward), left base, right base
        float[][] tri = {
            { 0, -arrowSize},          // tip
            {-arrowSize * 0.55f, arrowSize * 0.5f}, // left
            { arrowSize * 0.55f, arrowSize * 0.5f}  // right
        };

        // Rotate if north-locked (arrow shows player facing)
        float angle = northLock.get() ? (float) Math.toRadians(mc.player.getYaw() + 180) : 0;
        float cos = MathHelper.cos(angle);
        float sin = MathHelper.sin(angle);

        float[][] rotated = new float[3][2];
        for (int i = 0; i < 3; i++) {
            rotated[i][0] = cx + tri[i][0] * cos - tri[i][1] * sin;
            rotated[i][1] = cy + tri[i][0] * sin + tri[i][1] * cos;
        }

        // Fill triangle using scanline
        fillTriangle(ctx, rotated, 0xFF000000); // shadow offset
        // Shift shadow slightly
        float[][] shadow = new float[3][2];
        for (int i = 0; i < 3; i++) {
            shadow[i][0] = rotated[i][0] + 1;
            shadow[i][1] = rotated[i][1] + 1;
        }
        fillTriangle(ctx, shadow, 0xFF001100);
        fillTriangle(ctx, rotated, 0xFF00DD44); // bright green arrow

        // Small center dot
        ctx.fill((int) cx - 1, (int) cy - 1, (int) cx + 1, (int) cy + 1, 0xFFFFFFFF);
    }

    private void fillTriangle(DrawContext ctx, float[][] pts, int color) {
        // Sort vertices by Y
        float[][] sorted = pts.clone();
        if (sorted[0][1] > sorted[1][1]) { float[] t = sorted[0]; sorted[0] = sorted[1]; sorted[1] = t; }
        if (sorted[1][1] > sorted[2][1]) { float[] t = sorted[1]; sorted[1] = sorted[2]; sorted[2] = t; }
        if (sorted[0][1] > sorted[1][1]) { float[] t = sorted[0]; sorted[0] = sorted[1]; sorted[1] = t; }

        float y0 = sorted[0][1], y1 = sorted[1][1], y2 = sorted[2][1];
        float x0 = sorted[0][0], x1 = sorted[1][0], x2 = sorted[2][0];

        if ((int) y2 == (int) y0) return; // degenerate

        for (int scanY = (int) y0; scanY <= (int) y2; scanY++) {
            float t02 = (y2 - y0) != 0 ? (scanY - y0) / (y2 - y0) : 0;
            float xa = x0 + t02 * (x2 - x0);
            float xb;
            if (scanY < (int) y1 || y1 == y0) {
                float t01 = (y1 - y0) != 0 ? (scanY - y0) / (y1 - y0) : 0;
                t01 = MathHelper.clamp(t01, 0, 1);
                xb = x0 + t01 * (x1 - x0);
            } else {
                float t12 = (y2 - y1) != 0 ? (scanY - y1) / (y2 - y1) : 0;
                t12 = MathHelper.clamp(t12, 0, 1);
                xb = x1 + t12 * (x2 - x1);
            }
            int left = (int) Math.min(xa, xb);
            int right = (int) Math.max(xa, xb);
            if (right > left) {
                ctx.fill(left, scanY, right, scanY + 1, color);
            }
        }
    }

    // =========================================================================
    // Entity / waypoint markers
    // =========================================================================

    private void renderMarker(DrawContext ctx, double wx, double wz, double px, double pz,
                               float yaw, float cx, float cy, float radius, int color) {
        float[] pos = worldToMap(wx, wz, px, pz, yaw, cx, cy, radius);
        if (pos == null) return;

        float pSize = markerScale.get().floatValue();

        if (markerStyle.is("Diamonds")) {
            drawDiamond(ctx, pos[0], pos[1], pSize + 1, color);
        } else {
            // Shadow
            ctx.fill((int) (pos[0] - pSize), (int) (pos[1] - pSize),
                     (int) (pos[0] + pSize + 1), (int) (pos[1] + pSize + 1),
                     darken(color, 80));
            // Dot
            ctx.fill((int) (pos[0] - pSize + 0.5f), (int) (pos[1] - pSize + 0.5f),
                     (int) (pos[0] + pSize + 0.5f), (int) (pos[1] + pSize + 0.5f),
                     color);
        }
    }

    private void renderWaypoint(DrawContext ctx, WaypointManager.Waypoint wp,
                                 double px, double pz, float yaw,
                                 float cx, float cy, float radius) {
        float[] pos = worldToMap(wp.x(), wp.z(), px, pz, yaw, cx, cy, radius);
        if (pos == null) return;

        float pSize = markerScale.get().floatValue() + 0.5f;
        drawDiamond(ctx, pos[0], pos[1], pSize + 1, 0xFF00FFEE);

        // Label
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(0.6f, 0.6f);
        float sx = pos[0] / 0.6f;
        float sy = (pos[1] - pSize - 5) / 0.6f;
        ctx.drawCenteredTextWithShadow(mc.textRenderer, wp.name(), (int) sx, (int) sy, 0xFF00FFEE);
        ctx.getMatrices().popMatrix();
    }

    private void drawDiamond(DrawContext ctx, float x, float y, float size, int color) {
        // Draw a diamond as 4 small rects rotated 45 degrees (approximation)
        int s = (int) size;
        for (int i = 0; i <= s; i++) {
            int w = s - i;
            // top half
            ctx.fill((int) x - w, (int) y - i, (int) x + w + 1, (int) y - i + 1, color);
            // bottom half
            ctx.fill((int) x - w, (int) y + i, (int) x + w + 1, (int) y + i + 1, color);
        }
    }

    private float[] worldToMap(double wx, double wz, double px, double pz,
                                float yaw, float cx, float cy, float radius) {
        double diffX = wx - px;
        double diffZ = wz - pz;

        float mapX = (float) (diffX * zoom.get().doubleValue());
        float mapZ = (float) (diffZ * zoom.get().doubleValue());

        float angle = (float) Math.toRadians(yaw);
        float cos = MathHelper.cos(angle);
        float sin = MathHelper.sin(angle);

        float rotX = mapX * cos + mapZ * sin;
        float rotZ = mapZ * cos - mapX * sin;

        if (circular.get()) {
            if (rotX * rotX + rotZ * rotZ >= radius * radius) return null;
        } else {
            if (Math.abs(rotX) > radius || Math.abs(rotZ) > radius) return null;
        }

        return new float[]{cx + rotX, cy - rotZ};
    }

    // =========================================================================
    // Compass labels
    // =========================================================================

    private void drawCompassLabels(DrawContext ctx, float cx, float cy, float radius, float yaw) {
        float angle = (float) Math.toRadians(yaw);
        float cos = MathHelper.cos(angle);
        float sin = MathHelper.sin(angle);

        String[] labels = {"N", "E", "S", "W"};
        int[] colors = {0xFFFF5555, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF}; // N=red, rest=white
        double[][] dirs = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

        float labelDist = radius + 10;

        for (int i = 0; i < 4; i++) {
            float dx = (float) dirs[i][0];
            float dz = (float) dirs[i][1];

            float nx = dx * cos + dz * sin;
            float nz = dz * cos - dx * sin;

            float dist = (float) Math.sqrt(nx * nx + nz * nz);
            if (dist == 0) continue;
            nx = nx / dist * labelDist;
            nz = nz / dist * labelDist;

            float lx = cx + nx;
            float ly = cy - nz;

            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().scale(0.7f, 0.7f);
            float sx = lx / 0.7f;
            float sy = (ly - 3) / 0.7f;
            ctx.drawCenteredTextWithShadow(mc.textRenderer, labels[i], (int) sx, (int) sy, colors[i]);
            ctx.getMatrices().popMatrix();
        }
    }

    // =========================================================================
    // Info panel — semi-transparent pill below the map
    // =========================================================================

    private void drawInfoPanel(DrawContext ctx, float cx, float topY, float radius) {
        String coords = (int) mc.player.getX() + ", " + (int) mc.player.getY() + ", " + (int) mc.player.getZ();
        String biome = mc.world.getBiome(mc.player.getBlockPos())
                .getKey().map(k -> k.getValue().getPath()).orElse("Unknown");
        biome = formatBiome(biome);

        float panelW = Math.max(radius * 1.4f, 90f);
        float panelH = 22f;
        float panelX = cx - panelW / 2f;
        float panelY = topY + 16f;

        // Panel background
        int panelBg = 0xB0101020;
        ctx.fill((int) panelX, (int) panelY, (int) (panelX + panelW), (int) (panelY + panelH), panelBg);
        // Panel border
        int[] accent = getAccentColor();
        int panelBorder = 0x60000000 | (accent[0] << 16) | (accent[1] << 8) | accent[2];
        drawRectBorder(ctx, (int) panelX, (int) panelY, (int) panelW, (int) panelH, panelBorder);

        // Text
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(0.7f, 0.7f);
        float tx = cx / 0.7f;
        float ty1 = (panelY + 3f) / 0.7f;
        float ty2 = (panelY + 12f) / 0.7f;
        ctx.drawCenteredTextWithShadow(mc.textRenderer, coords, (int) tx, (int) ty1, 0xFFCCCCCC);
        ctx.drawCenteredTextWithShadow(mc.textRenderer, biome, (int) tx, (int) ty2, 0xFF88AAAA);
        ctx.getMatrices().popMatrix();
    }

    private String formatBiome(String raw) {
        if (raw == null || raw.isEmpty()) return "Unknown";
        return raw.substring(0, 1).toUpperCase() + raw.substring(1).replace("_", " ");
    }

    // =========================================================================
    // Drawing primitives
    // =========================================================================

    /** Filled circle via horizontal scanlines. */
    private void fillCircle(DrawContext ctx, float cx, float cy, float radius, int color) {
        int r = (int) radius;
        for (int dy = -r; dy <= r; dy++) {
            int w = (int) Math.sqrt(radius * radius - dy * dy);
            ctx.fill((int) (cx - w), (int) (cy + dy), (int) (cx + w), (int) (cy + dy + 1), color);
        }
    }

    /** Ring between innerRadius and outerRadius using scanlines. */
    private void drawRing(DrawContext ctx, float cx, float cy, float innerR, float outerR, int color) {
        int outer = (int) Math.ceil(outerR);
        float innerSq = innerR * innerR;
        float outerSq = outerR * outerR;

        for (int dy = -outer; dy <= outer; dy++) {
            float dySq = dy * dy;
            if (dySq > outerSq) continue;

            int outerW = (int) Math.sqrt(outerSq - dySq);
            int innerW = dySq < innerSq ? (int) Math.sqrt(innerSq - dySq) : 0;

            // Left slice
            ctx.fill((int) (cx - outerW), (int) (cy + dy), (int) (cx - innerW), (int) (cy + dy + 1), color);
            // Right slice
            ctx.fill((int) (cx + innerW), (int) (cy + dy), (int) (cx + outerW), (int) (cy + dy + 1), color);
        }
    }

    /** 1px rectangle border. */
    private void drawRectBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);             // top
        ctx.fill(x, y + h - 1, x + w, y + h, color);     // bottom
        ctx.fill(x, y, x + 1, y + h, color);              // left
        ctx.fill(x + w - 1, y, x + w, y + h, color);      // right
    }

    /** Darken a color by reducing RGB. */
    private int darken(int color, int amount) {
        int a = (color >> 24) & 0xFF;
        int r = Math.max(0, ((color >> 16) & 0xFF) - amount);
        int g = Math.max(0, ((color >> 8) & 0xFF) - amount);
        int b = Math.max(0, (color & 0xFF) - amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
