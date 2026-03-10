package com.foxyclient.module.ui;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.Render2DEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.WaypointManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;

import java.awt.Color;

/**
 * Minimap module to display nearby entities and waypoints.
 */
public class Minimap extends Module {
    private final NumberSetting zoom = addSetting(new NumberSetting("Zoom", "Zoom level", 1.0, 0.5, 5.0));
    private final NumberSetting scale = addSetting(new NumberSetting("Scale", "Size of the map", 100.0, 50.0, 300.0));
    private final NumberSetting xOffset = addSetting(new NumberSetting("X Offset", "X position", 10.0, 0.0, 1000.0));
    private final NumberSetting yOffset = addSetting(new NumberSetting("Y Offset", "Y position", 10.0, 0.0, 1000.0));
    private final NumberSetting mapAlpha = addSetting(new NumberSetting("Alpha", "Map transparency", 0.7, 0.1, 1.0));
    private final NumberSetting markerScale = addSetting(new NumberSetting("Marker Scale", "Size of entities/waypoints", 1.5, 0.5, 3.0));
    private final BoolSetting northLock = addSetting(new BoolSetting("North Lock", "Lock map rotation to North", false));
    private final BoolSetting circular = addSetting(new BoolSetting("Circular", "Circular minimap shape", true));
    private final BoolSetting entities = addSetting(new BoolSetting("Entities", "Show nearby entities", true));
    private final BoolSetting waypoints = addSetting(new BoolSetting("Waypoints", "Show registered waypoints", true));
    private final BoolSetting terrain = addSetting(new BoolSetting("Terrain", "Show basic terrain colors", true));
    private final BoolSetting info = addSetting(new BoolSetting("Info", "Show coordinates and biome", true));

    // Cache for terrain colors: [x][z]
    private final int[][] terrainCache = new int[512][512];
    private final long[][] terrainCachePos = new long[512][512]; // stores (x << 32) | z
    private int lastPlayerX = Integer.MAX_VALUE;
    private int lastPlayerZ = Integer.MAX_VALUE;

    public Minimap() {
        super("Minimap", "Displays a minimap on your HUD", Category.UI);
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (nullCheck()) return;

        DrawContext context = event.getContext();
        float size = scale.get().floatValue();
        float x = xOffset.get().floatValue();
        float y = yOffset.get().floatValue();
        float centerX = x + size / 2f;
        float centerY = y + size / 2f;
        float radius = size / 2f;

        float tickDelta = event.getTickDelta();
        // Use getX() / getZ() for simplicity in 2D, or accessors if lerping is needed
        double lerpX = mc.player.getX();
        double lerpZ = mc.player.getZ();
        float yaw = northLock.get() ? 0 : mc.player.getYaw();

        // Draw Background
        int bgAlpha = (int) (mapAlpha.get().floatValue() * 255);
        int bgColor = (bgAlpha << 24);
        
        if (circular.get()) {
            drawCircle(context, centerX, centerY, radius, bgColor);
            drawCircleBorder(context, centerX, centerY, radius, 0xFF444444);
            drawCircleBorder(context, centerX, centerY, radius - 1, 0xFFFFFFFF);
        } else {
            context.fill((int) x, (int) y, (int) (x + size), (int) (y + size), bgColor);
            drawRectBorder(context, (int) x, (int) y, (int) size, (int) size, 0xFF444444);
            drawRectBorder(context, (int) x + 1, (int) y + 1, (int) size - 2, (int) size - 2, 0xFFFFFFFF);
        }

        // Draw Terrain
        if (terrain.get()) {
            drawTerrain(context, centerX, centerY, radius, lerpX, lerpZ, yaw);
        }

        // Draw Player Arrow
        drawPlayerArrow(context, centerX, centerY, yaw);

        // Draw Entities
        if (entities.get()) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity == mc.player) continue;
                
                double entX = entity.getX();
                double entZ = entity.getZ();
                renderMarker(context, entX, entZ, lerpX, lerpZ, yaw, centerX, centerY, radius, 0xFFFF0000);
            }
        }

        // Draw Waypoints
        if (waypoints.get()) {
            String currentDim = mc.world.getRegistryKey().getValue().toString();
            for (WaypointManager.Waypoint wp : FoxyClient.INSTANCE.getWaypointManager().getForDimension(currentDim)) {
                renderMarker(context, wp.x(), wp.z(), lerpX, lerpZ, yaw, centerX, centerY, radius, 0xFF00FFFF);
            }
        }

        // Draw Compass Labels (N, E, S, W)
        drawCompassLabels(context, centerX, centerY, radius, yaw);

        // Draw Info (Coordinates and Biome)
        if (info.get()) {
            drawInfo(context, x, y, size);
        }
    }

    private void drawInfo(DrawContext context, float x, float y, float size) {
        String coords = (int) mc.player.getX() + ", " + (int) mc.player.getY() + ", " + (int) mc.player.getZ();
        String biome = mc.world.getBiome(mc.player.getBlockPos()).getKey().map(k -> k.getValue().getPath()).orElse("Unknown");
        biome = biome.substring(0, 1).toUpperCase() + biome.substring(1).replace("_", " ");

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x + size / 2f, y + size + 5);
        context.getMatrices().scale(0.8f, 0.8f);
        
        context.drawCenteredTextWithShadow(mc.textRenderer, coords, 0, 0, 0xFFBBBBBB);
        context.drawCenteredTextWithShadow(mc.textRenderer, biome, 0, 10, 0xFF888888);
        
        context.getMatrices().popMatrix();
    }

    private void drawCompassLabels(DrawContext context, float centerX, float centerY, float radius, float yaw) {
        float angle = (float) Math.toRadians(yaw);
        float cos = MathHelper.cos(angle);
        float sin = MathHelper.sin(angle);

        String[] labels = {"N", "E", "S", "W"};
        double[][] worldDirections = {{0, -1000}, {1000, 0}, {0, 1000}, {-1000, 0}};

        for (int i = 0; i < 4; i++) {
            double dx = worldDirections[i][0];
            double dz = worldDirections[i][1];
            
            float nx = (float) (dx * cos + dz * sin);
            float nz = (float) (dz * cos - dx * sin);

            float dist = (float) Math.sqrt(nx * nx + nz * nz);
            nx = nx / dist * (radius + 5);
            nz = nz / dist * (radius + 5);

            float finalX = centerX + nx;
            float finalY = centerY - nz;

            context.drawCenteredTextWithShadow(mc.textRenderer, labels[i], (int)finalX, (int)(finalY - 4), 0xFFFFFFFF);
        }
    }

    private void drawTerrain(DrawContext context, float centerX, float centerY, float radius, double lerpX, double lerpZ, float yaw) {
        float z = zoom.get().floatValue();
        int res = 2; // Sample every 2 blocks for better detail with cache
        int range = (int) (radius / z) + res;
        
        float angle = (float) Math.toRadians(yaw);
        float cos = MathHelper.cos(angle);
        float sin = MathHelper.sin(angle);

        for (int dx = -range; dx <= range; dx += res) {
            for (int dz = -range; dz <= range; dz += res) {
                float mapX = dx * z;
                float mapZ = dz * z;
                
                float rotatedX = mapX * cos + mapZ * sin;
                float rotatedZ = mapZ * cos - mapX * sin;

                if (circular.get()) {
                    if (rotatedX * rotatedX + rotatedZ * rotatedZ > radius * radius) continue;
                } else {
                    if (Math.abs(rotatedX) > radius || Math.abs(rotatedZ) > radius) continue;
                }

                int worldX = (int) Math.floor(lerpX) + dx;
                int worldZ = (int) Math.floor(lerpZ) + dz;
                
                int color = getCachedColor(worldX, worldZ);
                if (color == 0) continue;

                float finalX = centerX + rotatedX;
                float finalY = centerY - rotatedZ;
                int pixelSize = (int) Math.ceil(res * z);
                
                context.fill((int)finalX, (int)finalY, (int)(finalX + pixelSize), (int)(finalY + pixelSize), color | 0xFF000000);
            }
        }
    }

    private int getCachedColor(int x, int z) {
        int ix = Math.floorMod(x, 512);
        int iz = Math.floorMod(z, 512);
        long posKey = ((long) x << 32) | (z & 0xFFFFFFFFL);

        if (terrainCachePos[ix][iz] == posKey) {
            return terrainCache[ix][iz];
        }

        // Cache miss, update
        int color = getBlockColorAt(x, z);
        terrainCache[ix][iz] = color;
        terrainCachePos[ix][iz] = posKey;
        return color;
    }

    private int getBlockColorAt(int x, int z) {
        int topY = mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;
        BlockPos pos = new BlockPos(x, topY, z);
        if (pos.getY() < mc.world.getBottomY()) return 0;

        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) {
            topY = mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
            state = mc.world.getBlockState(new BlockPos(x, topY, z));
        }
        
        return getBlockColor(state);
    }

    private int getBlockColor(BlockState state) {
        MapColor mapColor = state.getMapColor(mc.world, BlockPos.ORIGIN);
        if (mapColor != null) return mapColor.color;

        if (state.isAir()) return 0;
        if (state.isOf(Blocks.GRASS_BLOCK)) return 0x567D46;
        if (state.isOf(Blocks.WATER)) return 0x44AFF5;
        if (state.isOf(Blocks.DIRT)) return 0x9B7653;
        if (state.isOf(Blocks.STONE)) return 0x808080;
        if (state.isOf(Blocks.SAND)) return 0xD2B48C;
        
        return 0x777777;
    }

    private void drawPlayerArrow(DrawContext context, float centerX, float centerY, float yaw) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(centerX, centerY);
        if (northLock.get()) {
            context.getMatrices().rotate((float) Math.toRadians(yaw + 180));
        }

        int color = 0xFF00FF00;
        int shadow = 0xFF000000;
        
        // Better arrow shape
        // Shadow
        context.fill(-1, -6, 2, 7, shadow);
        context.fill(-4, 0, 5, 2, shadow);
        
        // Body
        context.fill(0, -5, 1, 6, color);
        context.fill(-3, 1, 4, 2, color);
        context.fill(-2, 0, 3, 1, color);
        context.fill(-1, -1, 2, 0, color);
        
        context.getMatrices().popMatrix();
    }

    private void renderMarker(DrawContext context, double worldX, double worldZ, double playerX, double playerZ, float yaw, float centerX, float centerY, float radius, int color) {
        double diffX = worldX - playerX;
        double diffZ = worldZ - playerZ;
        
        float mapX = (float) (diffX * zoom.get().doubleValue());
        float mapZ = (float) (diffZ * zoom.get().doubleValue());

        float angle = (float) Math.toRadians(yaw);
        float cos = MathHelper.cos(angle);
        float sin = MathHelper.sin(angle);

        float rotatedX = mapX * cos + mapZ * sin;
        float rotatedZ = mapZ * cos - mapX * sin;

        float finalX = centerX + rotatedX;
        float finalY = centerY - rotatedZ;
        float pSize = markerScale.get().floatValue();

        if (circular.get()) {
            float distSq = rotatedX * rotatedX + rotatedZ * rotatedZ;
            if (distSq < radius * radius) {
                drawPoint(context, finalX, finalY, color, pSize);
            }
        } else {
            if (Math.abs(rotatedX) <= radius && Math.abs(rotatedZ) <= radius) {
                drawPoint(context, finalX, finalY, color, pSize);
            }
        }
    }

    private void drawPoint(DrawContext context, float x, float y, int color, float size) {
        context.fill((int)(x - size), (int)(y - size), (int)(x + size), (int)(y + size), color | 0xFF000000);
    }

    private void drawCircle(DrawContext context, float centerX, float centerY, float radius, int color) {
        for (int i = (int) -radius; i <= radius; i++) {
            int width = (int) Math.sqrt(radius * radius - i * i);
            context.fill((int) (centerX - width), (int) (centerY + i), (int) (centerX + width), (int) (centerY + i + 1), color);
        }
    }

    private void drawCircleBorder(DrawContext context, float centerX, float centerY, float radius, int color) {
        int segments = 90;
        for (int i = 0; i < segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            float nextAngle = (float) ((i + 1) * 2 * Math.PI / segments);
            
            float x1 = (float) (centerX + Math.cos(angle) * radius);
            float y1 = (float) (centerY + Math.sin(angle) * radius);
            float x2 = (float) (centerX + Math.cos(nextAngle) * radius);
            float y2 = (float) (centerY + Math.sin(nextAngle) * radius);
            
            drawLine(context, x1, y1, x2, y2, color);
        }
    }

    private void drawLine(DrawContext context, float x1, float y1, float x2, float y2, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist == 0) return;
        
        float stepX = dx / dist;
        float stepY = dy / dist;
        
        for (int i = 0; i < dist; i++) {
            context.fill((int)(x1 + stepX * i), (int)(y1 + stepY * i), (int)(x1 + stepX * i + 1), (int)(y1 + stepY * i + 1), color);
        }
    }

    private void drawRectBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }
}
