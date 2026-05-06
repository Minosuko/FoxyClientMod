package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.AttackEntityEvent;
import com.foxyclient.event.events.EntityDeathEvent;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ColorSetting;
import com.foxyclient.util.RenderUtil;
import com.foxyclient.util.RenderLayers;
import com.foxyclient.util.FoxySounds;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.math.ColorHelper;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * KillEffect - Visual effects on entity kill.
 * Supports: Lightning, Particles, Totem, and custom effects.
 */
public class KillEffect extends Module {
    private static final SoundEvent SOUND_EXPLODE = SoundEvent.of(Identifier.of("minecraft", "entity.generic.explode"));
    private static final SoundEvent SOUND_SPLASH = SoundEvent.of(Identifier.of("minecraft", "entity.generic.splash"));
    private static final SoundEvent SOUND_RAIN = SoundEvent.of(Identifier.of("minecraft", "weather.rain"));
    private static final SoundEvent SOUND_UNDERWATER = SoundEvent.of(Identifier.of("minecraft", "ambient.underwater.enter"));
    private static final SoundEvent SOUND_THUNDER = SoundEvent.of(Identifier.of("minecraft", "entity.lightning_bolt.thunder"));
    private static final SoundEvent SOUND_TOTEM = SoundEvent.of(Identifier.of("minecraft", "item.totem.use"));
    private static final SoundEvent SOUND_ANVIL = SoundEvent.of(Identifier.of("minecraft", "block.anvil.land"));

    private final ModeSetting effect = addSetting(new ModeSetting("Effect", "Kill effect", "Lightning", "Lightning", "Particles", "Totem", "Kuronami", "Champion2023", "Aemondir", "Mystbloom", "NeoFrontier", "None"));
    private final ModeSetting particle = addSetting(new ModeSetting("Particle", "Particle type", "Crit", "Heart", "Flame", "Smoke", "Magic", "Crit", "Snow", "Slime", "Totem"));
    private final BoolSetting playersOnly = addSetting(new BoolSetting("PlayersOnly", "Only affect players", false));
    private final BoolSetting selfKill = addSetting(new BoolSetting("SelfKill", "Show effect when you kill something", true));
    private final BoolSetting othersKill = addSetting(new BoolSetting("OthersKill", "Show effect when others kill", false));

    private int lastAttackedId = -1;
    private long lastAttackTime = 0;

    private static class ActiveKuronami {
        public final Vec3d center;
        public final double bottomY;
        public final long startTime;
        public long lastRainSoundTime;
        public boolean exploded;

        public ActiveKuronami(Vec3d center, double bottomY) {
            this.center = center;
            this.bottomY = bottomY;
            this.startTime = System.currentTimeMillis();
            this.lastRainSoundTime = this.startTime;
            this.exploded = false;
        }
    }

    private final java.util.List<ActiveKuronami> activeKuronamis = new CopyOnWriteArrayList<>();
    
    public static class ActiveChampion {
        public final Vec3d center;
        public final double bottomY;
        public final long startTime;
        public boolean enteredZone;
        public boolean musicStarted;
        public com.foxyclient.util.FadingSoundInstance music1;
        public com.foxyclient.util.FadingSoundInstance music2;

        public ActiveChampion(Vec3d center, double bottomY) {
            this.center = center;
            this.bottomY = bottomY;
            this.startTime = System.currentTimeMillis();
            this.music1 = null;
            this.music2 = null;
            this.enteredZone = false;
            this.musicStarted = false;
        }
    }

    public static final java.util.List<ActiveChampion> activeChampions = new CopyOnWriteArrayList<>();
    
    private static class ActiveAemondir {
        public final Vec3d center;
        public final double bottomY;
        public final long startTime;

        // Timing constants (ms) — matched to Valorant Aemondir finisher
        public static final int SWORD_COUNT = 11;
        public static final long[] SWORD_SPAWN_TIMES = {1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 2000};
        public static final long SWORD_FALL_MS = 300;
        public static final long GREATSWORD_START = 2900;
        public static final long GREATSWORD_FALL_MS = 600;
        public static final long TOTAL_DURATION = 7000;

        // Per-sword data
        public final double[][] swordOffsets; // [i]{x,z}
        public final double[] swordRotations;
        public final double[] swordTilts;
        public final boolean[] swordImpacted;
        public boolean greatswordImpacted;
        public final float greatswordYaw;

        public ActiveAemondir(Vec3d center, double bottomY, float playerYaw) {
            this.center = center;
            this.bottomY = bottomY;
            this.startTime = System.currentTimeMillis();
            this.greatswordImpacted = false;
            // Native items display edge-on if yRot=0. We rotate it to face the player.
            this.greatswordYaw = (float)Math.toRadians(-playerYaw);

            swordOffsets = new double[SWORD_COUNT][2];
            swordRotations = new double[SWORD_COUNT];
            swordTilts = new double[SWORD_COUNT];
            swordImpacted = new boolean[SWORD_COUNT];
            java.util.Random rng = new java.util.Random(System.nanoTime());
            for (int i = 0; i < SWORD_COUNT; i++) {
                double ang = (i * Math.PI * 2.0 / SWORD_COUNT) + (rng.nextDouble() * 0.3 - 0.15);
                double dist = 1.8 + rng.nextDouble() * 1.5;
                swordOffsets[i][0] = Math.cos(ang) * dist;
                swordOffsets[i][1] = Math.sin(ang) * dist;
                // Compute rotation so blade tip points from start toward center
                // Flight direction: (-offset.x * 3.5, -25, -offset.z * 3.5)
                double dx = -swordOffsets[i][0] * 3.5;
                double dz = -swordOffsets[i][1] * 3.5;
                double dy = -25.0;
                swordRotations[i] = Math.atan2(dx, dz);
                double horiz = Math.sqrt(dx * dx + dz * dz);
                swordTilts[i] = Math.atan2(horiz, -dy); // pitch angle from vertical
                swordImpacted[i] = false;
            }
        }
    }

    public static final java.util.List<ActiveAemondir> activeAemondirs = new java.util.concurrent.CopyOnWriteArrayList<>();
    
    private static final Identifier CHERRY_LEAVES_TEXTURE = Identifier.of("minecraft", "textures/block/cherry_leaves.png");
    
    private static final RenderLayer CHERRY_LEAVES_LAYER = RenderLayer.of("foxyclient_mystbloom_petals", 
        RenderSetup.builder(net.minecraft.client.gl.RenderPipelines.ENTITY_CUTOUT)
            .texture("Sampler0", CHERRY_LEAVES_TEXTURE)
            .useLightmap()
            .useOverlay()
            .outlineMode(RenderSetup.OutlineMode.AFFECTS_OUTLINE)
            .build()
    );
    
    private static final Identifier NETHERITE_SWORD_TEXTURE = Identifier.of("minecraft", "textures/item/netherite_sword.png");
    
    // 1.21 Item Rendering States
    private static final ItemRenderState SWORD_STATE = new ItemRenderState();
    private static final ItemRenderState TULIP_STATE = new ItemRenderState();
    private static final ItemRenderState POSTER_STATE = new ItemRenderState();
    private static final ItemRenderState DOOR_STATE = new ItemRenderState();
    private static final ItemRenderState FENCE_STATE = new ItemRenderState();
    private static final ItemRenderState HAY_STATE = new ItemRenderState();
    private static final ItemRenderState DEAD_BUSH_STATE = new ItemRenderState();
    private static boolean itemsInitialized = false;

    private void ensureItemsInitialized() {
        if (itemsInitialized) return;
        var imm = mc.getItemModelManager();
        if (imm == null) return;
        
        imm.clearAndUpdate(SWORD_STATE, new ItemStack(Items.NETHERITE_SWORD), ItemDisplayContext.FIXED, mc.world, null, 0);
        imm.clearAndUpdate(TULIP_STATE, new ItemStack(Items.PINK_TULIP), ItemDisplayContext.FIXED, mc.world, null, 0);
        imm.clearAndUpdate(POSTER_STATE, new ItemStack(Items.PAPER), ItemDisplayContext.FIXED, mc.world, null, 0);
        imm.clearAndUpdate(DOOR_STATE, new ItemStack(Items.DARK_OAK_DOOR), ItemDisplayContext.FIXED, mc.world, null, 0);
        imm.clearAndUpdate(FENCE_STATE, new ItemStack(Items.DARK_OAK_FENCE), ItemDisplayContext.FIXED, mc.world, null, 0);
        imm.clearAndUpdate(HAY_STATE, new ItemStack(Items.HAY_BLOCK), ItemDisplayContext.FIXED, mc.world, null, 0);
        imm.clearAndUpdate(DEAD_BUSH_STATE, new ItemStack(Items.DEAD_BUSH), ItemDisplayContext.FIXED, mc.world, null, 0);
        itemsInitialized = true;
    }

    private void renderItemState(ItemRenderState state, MatrixStack matrices, VertexConsumerProvider vcp, int light, int overlay, float alpha) {
        if (state.isEmpty()) return;
        
        // We iterate through exposed layers from our AccessWidener
        for (int i = 0; i < state.layerCount; i++) {
            ItemRenderState.LayerRenderState layer = state.layers[i];
            if (layer.renderLayer == null) continue;
            
            // If alpha is not 1.0, we need to apply it to all tints
            int[] tints = layer.tints;
            if (alpha < 0.99f) {
                int[] alphaTints = new int[tints.length];
                for (int j = 0; j < tints.length; j++) {
                    int c = tints[j];
                    int a = (int)(ColorHelper.getAlpha(c) * alpha);
                    alphaTints[j] = ColorHelper.getArgb(a, ColorHelper.getRed(c), ColorHelper.getGreen(c), ColorHelper.getBlue(c));
                }
                tints = alphaTints;
            }

            net.minecraft.client.render.item.ItemRenderer.renderItem(
                ItemDisplayContext.FIXED,
                matrices,
                vcp,
                light,
                overlay,
                tints,
                layer.quads,
                layer.renderLayer,
                layer.glint
            );
        }
    }
    
    private static class ActiveMystbloom {
        public final Vec3d center;
        public final double bottomY;
        public final long startTime;
        public boolean exploded;
        public final double[][] smallFlowers; // [i]{x, z, scale, rotation}
        public static final int SMALL_COUNT = 30;

        public ActiveMystbloom(Vec3d center, double bottomY) {
            this.center = center;
            this.bottomY = bottomY;
            this.startTime = System.currentTimeMillis();
            this.exploded = false;
            
            this.smallFlowers = new double[SMALL_COUNT][4];
            java.util.Random rng = new java.util.Random(System.nanoTime());
            for (int i = 0; i < SMALL_COUNT; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = 1.0 + Math.sqrt(rng.nextDouble()) * 5.0; // Distribution up to 6 blocks
                smallFlowers[i][0] = center.x + Math.cos(angle) * dist;
                smallFlowers[i][1] = center.z + Math.sin(angle) * dist;
                smallFlowers[i][2] = 0.4 + rng.nextDouble() * 0.4; // scale
                smallFlowers[i][3] = rng.nextDouble() * Math.PI * 2; // initial Y rotation
            }
        }
    }

    public static final java.util.List<ActiveMystbloom> activeMystblooms = new java.util.concurrent.CopyOnWriteArrayList<>();

    private static ItemStack getHeadForEntity(LivingEntity victim) {
        if (victim instanceof net.minecraft.entity.mob.ZombieEntity) return new ItemStack(Items.ZOMBIE_HEAD);
        if (victim instanceof net.minecraft.entity.mob.SkeletonEntity) return new ItemStack(Items.SKELETON_SKULL);
        if (victim instanceof net.minecraft.entity.mob.WitherSkeletonEntity) return new ItemStack(Items.WITHER_SKELETON_SKULL);
        if (victim instanceof net.minecraft.entity.mob.CreeperEntity) return new ItemStack(Items.CREEPER_HEAD);
        if (victim instanceof net.minecraft.entity.mob.PiglinEntity) return new ItemStack(Items.PIGLIN_HEAD);
        if (victim instanceof net.minecraft.entity.boss.dragon.EnderDragonEntity) return new ItemStack(Items.DRAGON_HEAD);
        if (victim instanceof net.minecraft.entity.player.PlayerEntity pe) {
            ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
            stack.set(net.minecraft.component.DataComponentTypes.PROFILE, net.minecraft.component.type.ProfileComponent.ofStatic(pe.getGameProfile()));
            return stack;
        }
        return new ItemStack(Items.PLAYER_HEAD);
    }
    
    private static class ActiveNeoFrontier {
        public final Vec3d center;
        public final double bottomY;
        public final long startTime;
        public final float playerYaw;
        public final float playerYawRad;
        public final ItemRenderState headState;
        public final net.minecraft.entity.decoration.ArmorStandEntity cowboy;

        // Timing constants (ms) - matched to Valorant Neo Frontier finisher
        public static final long DISSOLVE_START = 0;
        public static final long DISSOLVE_END = 1500;
        public static final long DUST_STORM_START = 800;
        public static final long DUST_STORM_END = 5500;
        public static final long TUMBLEWEED_START = 1200;
        public static final long TUMBLEWEED_END = 3500;
        public static final long GUNSLINGER_START = 2000;
        public static final long GUNSLINGER_END = 4200;
        public static final long FASTDRAW_TIME = 3400;
        public static final long GUNSHOT_TIME = 3600;
        public static final long POSTER_START = 3800;
        public static final long POSTER_LAND = 4300;
        public static final long GLITCH_START = 6000;
        public static final long TOTAL_DURATION = 7500;

        // Tumbleweed data
        public final double tumbleweedStartX, tumbleweedStartZ;
        public final double tumbleweedEndX, tumbleweedEndZ;
        public final float tumbleweedScale;

        // Environment props randomized positions
        public final double[][] hayBalePositions; // [i]{x, z, yRot}
        public final double[][] deadBushPositions; // [i]{x, z, yRot}

        public boolean gunshotFired;
        public boolean posterLanded;

        public ActiveNeoFrontier(net.minecraft.client.world.ClientWorld world, Vec3d center, double bottomY, float playerYaw, LivingEntity victim) {
            this.center = center;
            this.bottomY = bottomY;
            this.startTime = System.currentTimeMillis();
            this.playerYaw = playerYaw;
            this.playerYawRad = (float) Math.toRadians(playerYaw);
            this.gunshotFired = false;
            this.posterLanded = false;
            
            this.headState = new ItemRenderState();
            var imm = net.minecraft.client.MinecraftClient.getInstance().getItemModelManager();
            if (imm != null) {
                imm.clearAndUpdate(headState, getHeadForEntity(victim), ItemDisplayContext.FIXED, world, null, 0);
            }

            // Tumbleweed rolls perpendicular to player facing direction
            double perpX = Math.cos(Math.toRadians(playerYaw));
            double perpZ = Math.sin(Math.toRadians(playerYaw));
            this.tumbleweedStartX = center.x - perpX * 8.0;
            this.tumbleweedStartZ = center.z - perpZ * 8.0;
            this.tumbleweedEndX = center.x + perpX * 8.0;
            this.tumbleweedEndZ = center.z + perpZ * 8.0;
            this.tumbleweedScale = 0.6f;

            // Random hay bales and dead bushes for atmosphere
            java.util.Random rng = new java.util.Random(System.nanoTime());
            this.hayBalePositions = new double[4][3];
            for (int i = 0; i < 4; i++) {
                double ang = rng.nextDouble() * Math.PI * 2;
                double dist = 3.0 + rng.nextDouble() * 4.0;
                hayBalePositions[i][0] = center.x + Math.cos(ang) * dist;
                hayBalePositions[i][1] = center.z + Math.sin(ang) * dist;
                hayBalePositions[i][2] = rng.nextDouble() * Math.PI * 2;
            }
            this.deadBushPositions = new double[6][3];
            for (int i = 0; i < 6; i++) {
                double ang = rng.nextDouble() * Math.PI * 2;
                double dist = 2.0 + rng.nextDouble() * 5.0;
                deadBushPositions[i][0] = center.x + Math.cos(ang) * dist;
                deadBushPositions[i][1] = center.z + Math.sin(ang) * dist;
                deadBushPositions[i][2] = rng.nextDouble() * Math.PI * 2;
            }

            // Phase 3 Cowboy - holographic gunslinger
            cowboy = new net.minecraft.entity.decoration.ArmorStandEntity(world, center.x, bottomY, center.z);
            cowboy.setCustomNameVisible(false);
            cowboy.setNoGravity(true);
            cowboy.setShowArms(true);
            cowboy.setInvisible(true);
            cowboy.setGlowing(true);
            
            cowboy.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            cowboy.equipStack(net.minecraft.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            
            // Position the gunslinger 5 blocks away facing the victim
            double offsetX = Math.sin(Math.toRadians(playerYaw)) * 5.0;
            double offsetZ = -Math.cos(Math.toRadians(playerYaw)) * 5.0;
            cowboy.setPosition(center.x + offsetX, bottomY, center.z + offsetZ);
            
            cowboy.setYaw(playerYaw + 180f);
            cowboy.setHeadYaw(playerYaw + 180f);
            cowboy.setBodyYaw(playerYaw + 180f);
            
            cowboy.setRightArmRotation(new net.minecraft.util.math.EulerAngle(0f, 0f, 0f));
            world.addEntity(cowboy);
        }
    }
    public static final java.util.List<ActiveNeoFrontier> activeNeoFrontiers = new java.util.concurrent.CopyOnWriteArrayList<>();
    
    public static int currentBlackoutType = 0; // 1 = Champion, 2 = Aemondir
    
    public static float getKillEffectSkyIntensity(net.minecraft.client.render.Camera camera) {
        float maxIntensity = 0.0F;
        currentBlackoutType = 0;
        
        Vec3d pos = camera.getCameraPos();
        
        // Champion Proximity
        for (ActiveChampion c : activeChampions) {
            double dist = pos.distanceTo(c.center);
            if (dist < 5.0) {
                float intensity = 1.0F - (float) ((dist - 3.5) / (5.0 - 3.5));
                intensity = Math.max(0.0F, Math.min(1.0F, intensity));
                if (intensity > maxIntensity) {
                    maxIntensity = intensity;
                    currentBlackoutType = 1;
                }
            }
        }
        
        // Aemondir Proximity (builds intensity with greatsword phase)
        for (ActiveAemondir a : activeAemondirs) {
            long aElapsed = System.currentTimeMillis() - a.startTime;
            double dist = pos.distanceTo(a.center);
            // Expand blackout radius during greatsword phase
            double maxDist = aElapsed >= ActiveAemondir.GREATSWORD_START ? 8.0 : 6.0;
            double innerDist = aElapsed >= ActiveAemondir.GREATSWORD_START ? 3.0 : 4.5;
            if (dist < maxDist) {
                float intensity = 1.0F - (float) ((dist - innerDist) / (maxDist - innerDist));
                intensity = Math.max(0.0F, Math.min(1.0F, intensity));
                if (intensity > maxIntensity) {
                    maxIntensity = intensity;
                    currentBlackoutType = 2;
                }
            }
        }
        
        return maxIntensity;
    }
    
    private long lastParticleTime = 0;

    public KillEffect() {
        super("KillEffect", "Visual effects on kills", Category.RENDER);
    }

    @EventHandler
    public void onAttack(AttackEntityEvent event) {
        lastAttackedId = event.getEntity().getId();
        lastAttackTime = System.currentTimeMillis();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (nullCheck()) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity victim = (LivingEntity) event.getEntity();
        boolean isPlayer = victim instanceof PlayerEntity;

        if (playersOnly.get() && !isPlayer) return;

        // Check if the player killed this entity
        boolean wasKilledByMe = victim.getId() == lastAttackedId && (System.currentTimeMillis() - lastAttackTime) < 2000;

        if (wasKilledByMe) {
            if (!selfKill.get()) return;
        } else {
            if (!othersKill.get()) return;
        }

        String effectName = effect.get();
        switch (effectName) {
            case "Lightning":
                spawnLightning(victim);
                break;
            case "Particles":
                spawnParticles(victim);
                break;
            case "Totem":
                spawnTotem(victim);
                break;
            case "Kuronami":
                spawnKuronami(victim);
                break;
            case "Champion2023":
                spawnChampion(victim);
                break;
            case "Aemondir":
                spawnAemondir(victim);
                break;
            case "Mystbloom":
                spawnMystbloom(victim);
                break;
            case "NeoFrontier":
                spawnNeoFrontier(victim);
                break;
            case "None":
            default:
                break;
        }
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (mc.world == null) {
            activeKuronamis.clear();
            for (ActiveChampion c : activeChampions) {
                if (c.music1 != null) { c.music1.forceStop(); mc.getSoundManager().stop(c.music1); }
                if (c.music2 != null) { c.music2.forceStop(); mc.getSoundManager().stop(c.music2); }
            }
            activeChampions.clear();
            activeAemondirs.clear();
            activeMystblooms.clear();
            for (ActiveNeoFrontier n : activeNeoFrontiers) {
                if (n.cowboy != null) n.cowboy.discard();
            }
            activeNeoFrontiers.clear();
            return;
        }

        long currentTime = System.currentTimeMillis();
        // Kuronami lasts exactly 10s
        activeKuronamis.removeIf(k -> k.exploded || currentTime - k.startTime > 10000);
        
        // Aemondir
        activeAemondirs.removeIf(a -> currentTime - a.startTime > ActiveAemondir.TOTAL_DURATION);
        
        // Mystbloom lasts 6s
        activeMystblooms.removeIf(m -> currentTime - m.startTime > 6000);
        
        // Champion lasts exactly 64s (ACE Intro + 60s Loop)
        activeChampions.removeIf(c -> {
            if (currentTime - c.startTime > 64000) {
                if (c.music1 != null) {
                    c.music1.forceStop();
                    mc.getSoundManager().stop(c.music1);
                }
                if (c.music2 != null) {
                    c.music2.forceStop();
                    mc.getSoundManager().stop(c.music2);
                }
                return true;
            }
            return false;
        });

        // NeoFrontier lasts 7.5s
        activeNeoFrontiers.removeIf(n -> {
            if (currentTime - n.startTime > ActiveNeoFrontier.TOTAL_DURATION) {
                if (mc.world != null && n.cowboy != null) n.cowboy.discard();
                return true;
            }
            return false;
        });

        if (activeKuronamis.isEmpty() && activeChampions.isEmpty() && activeAemondirs.isEmpty() && activeMystblooms.isEmpty() && activeNeoFrontiers.isEmpty()) return;

        // === EVERY-FRAME 3D RENDERING (Aemondir swords) ===
        if (!activeAemondirs.isEmpty()) {
            MatrixStack matrices = event.getMatrices();
            VertexConsumerProvider.Immediate vcp = event.getVertexConsumers();
            if (matrices != null && vcp != null) {
                Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
                for (ActiveAemondir a : activeAemondirs) {
                    renderAemondirSwords3D(matrices, vcp, camPos, a, currentTime);
                }
            }
        }
        
        // === EVERY-FRAME 3D RENDERING (Mystbloom flowers) ===
        if (!activeMystblooms.isEmpty()) {
            MatrixStack matrices = event.getMatrices();
            VertexConsumerProvider.Immediate vcp = event.getVertexConsumers();
            if (matrices != null && vcp != null) {
                Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
                for (ActiveMystbloom m : activeMystblooms) {
                    long elapsed = currentTime - m.startTime;
                    
                    // bloomProgress: Organic smooth curve
                    // 0-3.2s: stays largely closed
                    // 3.2s-3.8s: powerfully blooms out
                    float bloomProgress = 0.0f;
                    if (elapsed < 3200) {
                        bloomProgress = (float)elapsed / 3200.0f * 0.15f; // barely opening
                    } else if (elapsed < 3800) {
                        float t = (elapsed - 3200) / 600.0f;
                        bloomProgress = 0.15f + (float)(1.0 - Math.pow(1.0 - t, 3.0)) * 0.85f; // easeOutCubic
                    } else {
                        bloomProgress = 1.0f;
                    }
                    
                    // Continuous slow rotation while opened
                    float openRot = (elapsed > 3800) ? ((elapsed - 3800) / 2000.0f) : 0.0f;
                    
                    // Waving animation (organic wind simulation)
                    float bigTiltX = (float)(Math.sin(elapsed / 800.0) * 0.05);
                    float bigTiltZ = (float)(Math.cos(elapsed / 700.0) * 0.05);

                    // Render central massive flower
                    renderMystbloomFlower3D(matrices, vcp, camPos, m.center.x, m.bottomY + 1.0, m.center.z, 2.8f, openRot, bigTiltX, bigTiltZ, bloomProgress);
                    
                    // Render scattered small flowers
                    for (int i = 0; i < ActiveMystbloom.SMALL_COUNT; i++) {
                        double sx = m.smallFlowers[i][0];
                        double sz = m.smallFlowers[i][1];
                        float sScale = (float)m.smallFlowers[i][2];
                        float yRotStr = (float)m.smallFlowers[i][3];
                        
                        // Small flowers pop up fully grown over exactly 100ms right after the kill
                        float popProgress = 0.0f;
                        long popTime = (long)(20 * i);
                        if (elapsed < popTime) {
                            popProgress = 0.0f;
                        } else if (elapsed < popTime + 100) {
                            // Transition from 0 to 1 over 100ms
                            popProgress = (elapsed - popTime) / 100.0f;
                        } else {
                            popProgress = 1.0f;
                        }

                        matrices.push();
                        // Put it perfectly at ground level (no +0.1 offset)
                        matrices.translate(sx - camPos.x, m.bottomY - camPos.y, sz - camPos.z);
                        matrices.multiply(new Quaternionf().rotationY(yRotStr + openRot * 0.5f));
                        
                        // Waving animation
                        matrices.multiply(new Quaternionf().rotationX((float)(Math.sin(elapsed / 600.0 + i) * 0.1)));
                        matrices.multiply(new Quaternionf().rotationZ((float)(Math.cos(elapsed / 500.0 + i) * 0.1)));
                        
                        float finalSmallScale = sScale * popProgress * 2.5f;
                        matrices.scale(finalSmallScale, finalSmallScale, finalSmallScale);
                        
                        // Center cross models along X and Z so they wave from their central stem
                        matrices.translate(-0.5f, 0.0f, -0.5f);
                        
                        ensureItemsInitialized();
                        renderItemState(TULIP_STATE, matrices, vcp, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 1.0f);
                        
                        matrices.pop();
                    }
                }
            }
        }

        // === EVERY-FRAME 3D RENDERING (NeoFrontier — Valorant-accurate cinematic sequence) ===
        if (!activeNeoFrontiers.isEmpty()) {
            MatrixStack matrices = event.getMatrices();
            VertexConsumerProvider.Immediate vcp = event.getVertexConsumers();
            if (matrices != null && vcp != null) {
                Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
                for (ActiveNeoFrontier n : activeNeoFrontiers) {
                    long elapsed = currentTime - n.startTime;
                    ensureItemsInitialized();

                    // === Desert environment props (hay bales, dead bushes) ===
                    if (elapsed >= ActiveNeoFrontier.DUST_STORM_START && elapsed < ActiveNeoFrontier.DUST_STORM_END) {
                        float envFadeIn = Math.min(1.0f, (elapsed - ActiveNeoFrontier.DUST_STORM_START) / 800.0f);
                        float envFadeOut = elapsed > ActiveNeoFrontier.DUST_STORM_END - 1000 ? 
                            Math.max(0.0f, (ActiveNeoFrontier.DUST_STORM_END - elapsed) / 1000.0f) : 1.0f;
                        float envAlpha = envFadeIn * envFadeOut * 0.6f;

                        // Hay bales
                        for (int i = 0; i < n.hayBalePositions.length; i++) {
                            matrices.push();
                            matrices.translate(n.hayBalePositions[i][0] - camPos.x, n.bottomY - camPos.y, n.hayBalePositions[i][1] - camPos.z);
                            matrices.multiply(new Quaternionf().rotationY((float) n.hayBalePositions[i][2]));
                            float hScale = 0.8f * envFadeIn;
                            matrices.scale(hScale, hScale, hScale);
                            matrices.translate(-0.5f, 0.0f, -0.5f);
                            renderItemState(HAY_STATE, matrices, vcp, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, envAlpha);
                            matrices.pop();
                        }

                        // Dead bushes
                        for (int i = 0; i < n.deadBushPositions.length; i++) {
                            matrices.push();
                            matrices.translate(n.deadBushPositions[i][0] - camPos.x, n.bottomY - camPos.y, n.deadBushPositions[i][1] - camPos.z);
                            matrices.multiply(new Quaternionf().rotationY((float) n.deadBushPositions[i][2]));
                            // Gentle sway in wind
                            float sway = (float)(Math.sin(currentTime / 300.0 + i) * 0.08);
                            matrices.multiply(new Quaternionf().rotationZ(sway));
                            float bScale = 1.2f * envFadeIn;
                            matrices.scale(bScale, bScale, bScale);
                            matrices.translate(-0.5f, 0.0f, -0.5f);
                            renderItemState(DEAD_BUSH_STATE, matrices, vcp, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, envAlpha);
                            matrices.pop();
                        }
                    }

                    // === Tumbleweed rolling across the scene ===
                    if (elapsed >= ActiveNeoFrontier.TUMBLEWEED_START && elapsed < ActiveNeoFrontier.TUMBLEWEED_END) {
                        float tProgress = (float)(elapsed - ActiveNeoFrontier.TUMBLEWEED_START) / (ActiveNeoFrontier.TUMBLEWEED_END - ActiveNeoFrontier.TUMBLEWEED_START);
                        // Smooth ease-in-out
                        float tEase = tProgress < 0.5f ? 2 * tProgress * tProgress : 1 - (float)Math.pow(-2 * tProgress + 2, 2) / 2;
                        double twX = n.tumbleweedStartX + (n.tumbleweedEndX - n.tumbleweedStartX) * tEase;
                        double twZ = n.tumbleweedStartZ + (n.tumbleweedEndZ - n.tumbleweedStartZ) * tEase;
                        // Bounce height (sinusoidal hops)
                        double bounceH = Math.abs(Math.sin(tProgress * Math.PI * 5)) * 0.4;
                        float twRoll = tProgress * 20.0f; // continuous rolling rotation

                        matrices.push();
                        matrices.translate(twX - camPos.x, n.bottomY + 0.3 + bounceH - camPos.y, twZ - camPos.z);
                        matrices.multiply(new Quaternionf().rotationZ(twRoll));
                        matrices.multiply(new Quaternionf().rotationY(twRoll * 0.3f));
                        float twScale = n.tumbleweedScale;
                        matrices.scale(twScale, twScale, twScale);
                        matrices.translate(-0.5f, -0.5f, -0.5f);
                        renderItemState(DEAD_BUSH_STATE, matrices, vcp, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0.85f);
                        matrices.pop();
                    }

                    // === Holographic Gunslinger (cowboy armor stand) ===
                    if (elapsed >= ActiveNeoFrontier.GUNSLINGER_START && elapsed < ActiveNeoFrontier.GUNSLINGER_END) {
                        if (n.cowboy != null) {
                            // Materialize with holographic flicker
                            float materialize = Math.min(1.0f, (elapsed - ActiveNeoFrontier.GUNSLINGER_START) / 600.0f);
                            boolean flickerVisible = materialize < 0.7f ? 
                                ((elapsed / 80) % 3 != 0) : true; // Holographic flicker during materialize
                            n.cowboy.setInvisible(!flickerVisible);
                            
                            // Fast-draw sequence
                            if (elapsed >= ActiveNeoFrontier.FASTDRAW_TIME && elapsed < ActiveNeoFrontier.GUNSHOT_TIME) {
                                float draw = (elapsed - ActiveNeoFrontier.FASTDRAW_TIME) / (float)(ActiveNeoFrontier.GUNSHOT_TIME - ActiveNeoFrontier.FASTDRAW_TIME);
                                // Snap draw: cubic ease-out for snappy feel
                                draw = 1.0f - (1.0f - draw) * (1.0f - draw) * (1.0f - draw);
                                n.cowboy.setRightArmRotation(new net.minecraft.util.math.EulerAngle(-95f * draw, 0f, -15f * draw));
                            } else if (elapsed >= ActiveNeoFrontier.GUNSHOT_TIME) {
                                n.cowboy.setRightArmRotation(new net.minecraft.util.math.EulerAngle(-95f, 0f, -15f));
                            }
                        }
                    } else if (elapsed >= ActiveNeoFrontier.GUNSLINGER_END) {
                        // Dematerialize cowboy after gunslinger phase
                        if (n.cowboy != null) {
                            n.cowboy.setInvisible(true);
                            if (elapsed >= ActiveNeoFrontier.POSTER_START) {
                                n.cowboy.discard();
                            }
                        }
                    } else if (n.cowboy != null) {
                        n.cowboy.setInvisible(true);
                    }

                    // === WANTED Poster drop from sky ===
                    if (elapsed >= ActiveNeoFrontier.POSTER_START) {
                        float dropDuration = ActiveNeoFrontier.POSTER_LAND - ActiveNeoFrontier.POSTER_START;
                        float dropT = Math.min(1.0f, (elapsed - ActiveNeoFrontier.POSTER_START) / dropDuration);
                        // Dramatic ease-out bounce
                        float eased = dropT < 1.0f ? 1.0f - (1.0f - dropT) * (1.0f - dropT) : 1.0f;
                        float yPoster = 18.0f * (1.0f - eased);
                        // Subtle bounce after landing
                        if (elapsed > ActiveNeoFrontier.POSTER_LAND && elapsed < ActiveNeoFrontier.POSTER_LAND + 200) {
                            float bounce = (elapsed - ActiveNeoFrontier.POSTER_LAND) / 200.0f;
                            yPoster = (float)(Math.sin(bounce * Math.PI) * 0.3);
                        }
                        
                        // Glitch-out fade
                        float posterAlpha = 1.0f;
                        if (elapsed >= ActiveNeoFrontier.GLITCH_START) {
                            float glitchT = (elapsed - ActiveNeoFrontier.GLITCH_START) / (float)(ActiveNeoFrontier.TOTAL_DURATION - ActiveNeoFrontier.GLITCH_START);
                            // Flickering dissolution
                            boolean glitchFlicker = ((elapsed / 60) % 4) != 0;
                            posterAlpha = glitchFlicker ? Math.max(0.0f, 1.0f - glitchT * 1.2f) : 0.0f;
                        }
                        
                        if (posterAlpha > 0.01f) {
                            matrices.push();
                            matrices.translate(n.center.x - camPos.x, n.bottomY + 2.8 + yPoster - camPos.y, n.center.z - camPos.z);
                            matrices.multiply(new Quaternionf().rotationY(n.playerYawRad + (float) Math.PI));
                            
                            // Glitch offset jitter
                            if (elapsed >= ActiveNeoFrontier.GLITCH_START) {
                                float jitter = (float)(Math.sin(elapsed * 0.1) * 0.15);
                                matrices.translate(jitter, jitter * 0.5, 0);
                            }
                            
                            // Poster frame — large paper background
                            matrices.push();
                            matrices.scale(3.5f, 4.5f, 0.15f);
                            matrices.translate(-0.5f, -0.5f, -0.5f);
                            renderItemState(POSTER_STATE, matrices, vcp, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, posterAlpha * 0.95f);
                            matrices.pop();
                            
                            // "WANTED" text bar — dark oak fence at top
                            matrices.push();
                            matrices.translate(0, 1.6f, -0.12f);
                            matrices.scale(2.8f, 0.4f, 0.1f);
                            matrices.translate(-0.5f, -0.5f, -0.5f);
                            renderItemState(FENCE_STATE, matrices, vcp, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, posterAlpha * 0.7f);
                            matrices.pop();
                            
                            // Victim's captured head in poster center
                            matrices.push();
                            matrices.translate(0, 0.2f, -0.16f);
                            matrices.scale(2.0f, 2.0f, 0.5f);
                            matrices.translate(-0.5f, -0.3f, -0.5f);
                            renderItemState(n.headState, matrices, vcp, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, posterAlpha);
                            matrices.pop();
                            
                            // Bottom bounty bar
                            matrices.push();
                            matrices.translate(0, -1.4f, -0.12f);
                            matrices.scale(2.8f, 0.3f, 0.1f);
                            matrices.translate(-0.5f, -0.5f, -0.5f);
                            renderItemState(FENCE_STATE, matrices, vcp, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, posterAlpha * 0.7f);
                            matrices.pop();
                            
                            matrices.pop();
                        }
                    }
                }
            }
        }

        // Run rendering approx 20 times per second to sustain particles steadily
        if (currentTime - lastParticleTime < 50) return;
        lastParticleTime = currentTime;

        for (ActiveKuronami k : activeKuronamis) {
            double cx = k.center.x;
            double cy = k.center.y;
            double cz = k.center.z;
            double bottomY = k.bottomY;

            // Check if player touches the orb
            if (mc.player != null && mc.player.getBoundingBox().getCenter().distanceTo(k.center) < 2.2) {
                k.exploded = true;
                
                // Explode visually and audibly
                mc.world.addParticleClient(ParticleTypes.EXPLOSION_EMITTER, cx, cy, cz, 0, 0, 0);
                mc.world.playSoundClient(cx, cy, cz, SOUND_EXPLODE, SoundCategory.PLAYERS, 1.0F, 1.0F, false);
                
                // Huge splash from the pop
                for(int j = 0; j < 100; j++) {
                    double rdx = mc.world.random.nextGaussian() * 0.8;
                    double rdy = mc.world.random.nextGaussian() * 0.8;
                    double rdz = mc.world.random.nextGaussian() * 0.8;
                    mc.world.addParticleClient(ParticleTypes.SPLASH, cx, cy, cz, rdx, rdy, rdz);
                    if (j % 2 == 0) mc.world.addParticleClient(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, rdx * 0.5, rdy * 0.5, rdz * 0.5);
                }
                
                continue; // Skip rendering the sustained particles since it just exploded
            }

            // Continual rain sound loop every 1.5s
            if (currentTime - k.lastRainSoundTime > 1500) {
                mc.world.playSoundClient(cx, cy, cz, SOUND_RAIN, SoundCategory.PLAYERS, 1.0F, 0.8F, false);
                k.lastRainSoundTime = currentTime;
            }

            // 1. GIGANTIC Storm Cloud
            for (int i = 0; i < 40; i++) {
                double rx = mc.world.random.nextGaussian() * 6.5; // Significantly bigger cloud radius
                double rz = mc.world.random.nextGaussian() * 6.5;
                mc.world.addParticleClient(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, cx + rx, bottomY + 8.5, cz + rz, 0, 0, 0);
                mc.world.addParticleClient(ParticleTypes.CLOUD, cx + rx, bottomY + 8.0, cz + rz, 0, 0.05, 0);
                mc.world.addParticleClient(ParticleTypes.SQUID_INK, cx + rx, bottomY + 8.0, cz + rz, 0, 0, 0);
            }

            // 2. Heavy Rain Torrent continually dripping across the larger cloud
            for (int i = 0; i < 90; i++) {
                double rx = mc.world.random.nextGaussian() * 6.0;
                double rz = mc.world.random.nextGaussian() * 6.0;
                double ry = mc.world.random.nextDouble() * 8.5; // Starts from higher up
                mc.world.addParticleClient(ParticleTypes.FALLING_WATER, cx + rx, bottomY + ry, cz + rz, 0, -0.6, 0);
                mc.world.addParticleClient(ParticleTypes.RAIN, cx + rx, bottomY + ry, cz + rz, 0, -0.8, 0);
            }

            // 3. Bigger Dark Water Orb spinning continuously
            for (int i = 0; i < 40; i++) {
                double theta = mc.world.random.nextDouble() * 2 * Math.PI;
                double phi = Math.acos(2 * mc.world.random.nextDouble() - 1);
                double px = Math.sin(phi) * Math.cos(theta);
                double py = Math.sin(phi) * Math.sin(theta);
                double pz = Math.cos(phi);
                
                double radius = 1.8 + mc.world.random.nextDouble() * 0.3; // Expanded radius
                
                mc.world.addParticleClient(ParticleTypes.SPLASH, cx + px * radius, cy + py * radius, cz + pz * radius, px * 0.1, py * 0.1, pz * 0.1);
                mc.world.addParticleClient(ParticleTypes.SOUL_FIRE_FLAME, cx + px * radius, cy + py * radius, cz + pz * radius, 0, 0, 0);
                if (mc.world.random.nextBoolean()) {
                    mc.world.addParticleClient(ParticleTypes.SQUID_INK, cx + px * radius, cy + py * radius, cz + pz * radius, 0, 0, 0);
                }
            }

            // 4. Chains shifting fluidly around
            double[][] chainDirs = { {1, 1}, {-1, 1}, {1, -1}, {-1, -1} };
            for (double[] dir : chainDirs) {
                for (int c = 0; c < 3; c++) {
                    double dist = mc.world.random.nextDouble() * 6.0;
                    double px = cx + dir[0] * dist;
                    double pz = cz + dir[1] * dist;
                    double py = bottomY - 0.5 + (dist * 0.6); 
                    
                    mc.world.addParticleClient(ParticleTypes.ENCHANTED_HIT, px, py, pz, 0, 0.05, 0);
                    mc.world.addParticleClient(ParticleTypes.SOUL, px, py, pz, 0, 0.05, 0);
                }
            }
            
            // 5. Huge splashing Ground Impact Ring
            for (int i = 0; i < 20; i++) {
                double angle = mc.world.random.nextDouble() * Math.PI * 2;
                double px = Math.cos(angle) * 3.5;
                double pz = Math.sin(angle) * 3.5;
                mc.world.addParticleClient(ParticleTypes.CLOUD, cx + px, bottomY + 0.1, cz + pz, px * 0.05, 0, pz * 0.05);
                mc.world.addParticleClient(ParticleTypes.SPLASH, cx + px, bottomY + 0.2, cz + pz, px * 0.05, 0.1, pz * 0.05);
            }
        }

        for (ActiveChampion c : activeChampions) {
            double cx = c.center.x;
            double cy = c.center.y;
            double cz = c.center.z;
            double bottomY = c.bottomY;

            // Sequence the main tracks exactly 3000ms (3 seconds) after ACE begins
            if (!c.musicStarted && currentTime - c.startTime >= 3000) {
                c.musicStarted = true;
                
                // Start both silent and trigger a fade-in for the correct track
                c.music1 = new com.foxyclient.util.FadingSoundInstance(
                    com.foxyclient.util.FoxySounds.CHAMPIONS_2023_1, SoundCategory.PLAYERS, c.center.x, c.center.y, c.center.z, 0.01F
                );
                c.music2 = new com.foxyclient.util.FadingSoundInstance(
                    com.foxyclient.util.FoxySounds.CHAMPIONS_2023_2, SoundCategory.PLAYERS, c.center.x, c.center.y, c.center.z, 0.01F
                );
                
                if (c.enteredZone) {
                    c.music2.triggerFadeIn();
                } else {
                    c.music1.triggerFadeIn();
                }
                
                mc.getSoundManager().play(c.music1);
                mc.getSoundManager().play(c.music2);
            }

            // Audio Crossfade & Sky Darkening boundary checking
            boolean playerInside = mc.player != null && mc.player.getBoundingBox().getCenter().distanceTo(c.center) < 3.5;

            if (playerInside && !c.enteredZone) {
                c.enteredZone = true;
                if (c.music1 != null) c.music1.triggerFadeOut();
                if (c.music2 != null) c.music2.triggerFadeIn();
                
                // Huge dramatic golden burst to signify the transition inward
                for (int i = 0; i < 200; i++) {
                    double rx = mc.world.random.nextGaussian() * 2.0;
                    double ry = mc.world.random.nextGaussian() * 2.0;
                    double rz = mc.world.random.nextGaussian() * 2.0;
                    mc.world.addParticleClient(ParticleTypes.END_ROD, cx, cy, cz, rx, ry, rz);
                    if (i % 2 == 0) mc.world.addParticleClient(ParticleTypes.GLOW, cx, cy, cz, rx*0.5, ry*0.5, rz*0.5);
                }
            } else if (!playerInside && c.enteredZone) {
                c.enteredZone = false;
                if (c.music2 != null) c.music2.triggerFadeOut();
                if (c.music1 != null) c.music1.triggerFadeIn();
            }

            // Continuous Champion Visuals (Golden Trophy / Beams aesthetic)
            // 1. Central golden multi-beam structure
            for (int i = 0; i < 8; i++) {
                double ry = mc.world.random.nextDouble() * 25.0;
                double offsetX = Math.sin(currentTime / 500.0) * 0.5;
                double offsetZ = Math.cos(currentTime / 500.0) * 0.5;
                mc.world.addParticleClient(ParticleTypes.END_ROD, cx + offsetX, bottomY + ry, cz + offsetZ, 0, 0.4, 0);
                mc.world.addParticleClient(ParticleTypes.GLOW, cx - offsetX, bottomY + ry, cz - offsetZ, 0, 0.4, 0);
            }

            // 2. Triple Swirling Glowing Rings
            double[] ringHeights = {0.5, 2.5, 4.5};
            double[] ringRadii = {3.5, 2.8, 1.5}; // Tapering up like a trophy
            for (int rIdx = 0; rIdx < 3; rIdx++) {
                double h = ringHeights[rIdx];
                double r = ringRadii[rIdx];
                for (int i = 0; i < 10; i++) {
                    double age = (currentTime - c.startTime) / (400.0 + rIdx * 100);
                    double angle = age + (i * (Math.PI / 5.0));
                    double px = Math.cos(angle) * r;
                    double pz = Math.sin(angle) * r;
                    mc.world.addParticleClient(ParticleTypes.GLOW, cx + px, bottomY + h + (mc.world.random.nextDouble() * 0.2), cz + pz, 0, 0.05, 0);
                }
            }
            
            // 3. Falling gold particles from sky
            for (int i = 0; i < 8; i++) {
                double rx = mc.world.random.nextGaussian() * 4.0;
                double rz = mc.world.random.nextGaussian() * 4.0;
                mc.world.addParticleClient(ParticleTypes.GLOW, cx + rx, bottomY + 15.0, cz + rz, 0, -0.3, 0);
            }

            // 4. Stadium Crowd Sky (Massive Uniform Hemisphere Dome)
            if (currentTime - c.startTime > 3000) { 
                for (int i = 0; i < 60; i++) {
                    double theta = mc.world.random.nextDouble() * 2 * Math.PI;
                    double phi = Math.acos(mc.world.random.nextDouble()); 
                    double r = 25.0; 
                    
                    double dx = r * Math.sin(phi) * Math.cos(theta);
                    double dy = r * Math.cos(phi);
                    double dz = r * Math.sin(phi) * Math.sin(theta);
                    
                    if (i % 5 == 0) {
                        mc.world.addParticleClient(ParticleTypes.END_ROD, cx + dx, bottomY + dy + 1.0, cz + dz, 0, 0, 0);
                    } else {
                        mc.world.addParticleClient(ParticleTypes.GLOW, cx + dx, bottomY + dy + 1.0, cz + dz, 0, 0, 0);
                    }
                }
                
                // 5. Vertical "Stadium Beam" Spotlights from the edge
                for (int j = 0; j < 4; j++) {
                    double angle = (currentTime / 2000.0) + (j * Math.PI / 2);
                    double bx = Math.cos(angle) * 20.0;
                    double bz = Math.sin(angle) * 20.0;
                    for (int h = 0; h < 10; h++) {
                        mc.world.addParticleClient(ParticleTypes.END_ROD, cx + bx, bottomY + h * 2.0, cz + bz, 0, 1.0, 0);
                    }
                }
            }
        }

        for (ActiveAemondir a : activeAemondirs) {
            double cx = a.center.x;
            double cz = a.center.z;
            double bottomY = a.bottomY;
            long elapsed = currentTime - a.startTime;

            // Phase 0: Halo forming in sky (0 - first sword)
            if (elapsed < ActiveAemondir.SWORD_SPAWN_TIMES[0]) {
                double progress = elapsed / (double) ActiveAemondir.SWORD_SPAWN_TIMES[0];
                double haloR = 3.5 * progress;
                double haloY = bottomY + 22.0;
                for (int i = 0; i < 20; i++) {
                    double ang = (i / 20.0) * Math.PI * 2 + (currentTime / 400.0);
                    mc.world.addParticleClient(ParticleTypes.END_ROD, cx + Math.cos(ang) * haloR, haloY, cz + Math.sin(ang) * haloR, 0, -0.03, 0);
                    if (i % 3 == 0) mc.world.addParticleClient(ParticleTypes.SOUL_FIRE_FLAME, cx + Math.cos(ang) * haloR * 0.8, haloY - 0.3, cz + Math.sin(ang) * haloR * 0.8, 0, -0.01, 0);
                }
                for (int i = 0; i < 8; i++) {
                    mc.world.addParticleClient(ParticleTypes.SQUID_INK, cx + mc.world.random.nextGaussian() * 1.2, bottomY + 0.5, cz + mc.world.random.nextGaussian() * 1.2, 0, 0.08, 0);
                    mc.world.addParticleClient(ParticleTypes.ENCHANTED_HIT, cx + mc.world.random.nextGaussian() * 1.0, bottomY + 0.3, cz + mc.world.random.nextGaussian() * 1.0, 0, 0.04, 0);
                }
            }

            // Persistent halo while swords rain
            if (elapsed >= ActiveAemondir.SWORD_SPAWN_TIMES[0] && elapsed < ActiveAemondir.GREATSWORD_START) {
                double haloY = bottomY + 22.0;
                for (int i = 0; i < 12; i++) {
                    double ang = (i / 12.0) * Math.PI * 2 + (currentTime / 600.0);
                    mc.world.addParticleClient(ParticleTypes.END_ROD, cx + Math.cos(ang) * 3.5, haloY, cz + Math.sin(ang) * 3.5, 0, -0.02, 0);
                }
            }

            // Sword impact bursts
            for (int i = 0; i < ActiveAemondir.SWORD_COUNT; i++) {
                long sImpactTime = ActiveAemondir.SWORD_SPAWN_TIMES[i] + ActiveAemondir.SWORD_FALL_MS;
                if (a.swordImpacted[i] && elapsed - sImpactTime < 100) {
                    double sx = cx + a.swordOffsets[i][0];
                    double sz = cz + a.swordOffsets[i][1];
                    for (int j = 0; j < 12; j++) {
                        double rx = mc.world.random.nextGaussian() * 0.4;
                        double rz = mc.world.random.nextGaussian() * 0.4;
                        mc.world.addParticleClient(ParticleTypes.ENCHANTED_HIT, sx + rx, bottomY + 0.2, sz + rz, rx * 0.3, 0.15, rz * 0.3);
                        mc.world.addParticleClient(ParticleTypes.SOUL_FIRE_FLAME, sx + rx * 0.5, bottomY + 0.1, sz + rz * 0.5, 0, 0.05, 0);
                    }
                }
            }

            // Energy along embedded swords (only before greatsword)
            if (elapsed < ActiveAemondir.GREATSWORD_START) {
                for (int i = 0; i < ActiveAemondir.SWORD_COUNT; i++) {
                    if (!a.swordImpacted[i]) continue;
                    double sx = cx + a.swordOffsets[i][0];
                    double sz = cz + a.swordOffsets[i][1];
                    mc.world.addParticleClient(ParticleTypes.ENCHANTED_HIT, sx, bottomY + mc.world.random.nextDouble() * 2.0, sz, 0, 0.02, 0);
                }
            }

            // Greatsword impact shockwave
            long gsImpact = ActiveAemondir.GREATSWORD_START + ActiveAemondir.GREATSWORD_FALL_MS;
            if (a.greatswordImpacted && elapsed - gsImpact < 300) {
                double shockProgress = (elapsed - gsImpact) / 300.0;
                double ringR = 1.0 + shockProgress * 5.0;
                for (int i = 0; i < 40; i++) {
                    double ang = mc.world.random.nextDouble() * Math.PI * 2;
                    double px = Math.cos(ang) * ringR;
                    double pz = Math.sin(ang) * ringR;
                    mc.world.addParticleClient(ParticleTypes.SOUL_FIRE_FLAME, cx + px, bottomY + 0.15, cz + pz, px * 0.15, 0.1, pz * 0.15);
                    mc.world.addParticleClient(ParticleTypes.ENCHANTED_HIT, cx + px * 0.9, bottomY + 0.3, cz + pz * 0.9, px * 0.1, 0.2, pz * 0.1);
                    if (i % 4 == 0) mc.world.addParticleClient(ParticleTypes.END_ROD, cx + px * 0.5, bottomY + mc.world.random.nextDouble() * 3.0, cz + pz * 0.5, 0, 0.3, 0);
                }
                for (int i = 0; i < 20; i++) {
                    mc.world.addParticleClient(ParticleTypes.END_ROD, cx, bottomY + mc.world.random.nextDouble() * 15.0, cz, mc.world.random.nextGaussian() * 0.05, 0.5, mc.world.random.nextGaussian() * 0.05);
                }
            }

            // Aftermath embers
            if (elapsed > gsImpact + 300) {
                for (int i = 0; i < 3; i++) {
                    mc.world.addParticleClient(ParticleTypes.SOUL_FIRE_FLAME, cx + mc.world.random.nextGaussian() * 2.0, bottomY + mc.world.random.nextDouble() * 2.0, cz + mc.world.random.nextGaussian() * 2.0, 0, 0.02, 0);
                }
            }
        }

        for (ActiveMystbloom m : activeMystblooms) {
            double cx = m.center.x;
            double cz = m.center.z;
            double bottomY = m.bottomY;
            long elapsed = currentTime - m.startTime;

            if (elapsed < 3800) {
                // Pre-bloom: Mystical energy gathering from the ground up
                if (mc.world.random.nextInt(2) == 0) {
                    double angle = mc.world.random.nextDouble() * Math.PI * 2;
                    double radius = 0.5 + mc.world.random.nextDouble() * 3.0; // Spiraling in
                    double height = mc.world.random.nextDouble() * 4.0;
                    
                    mc.world.addParticleClient(ParticleTypes.CHERRY_LEAVES, 
                        cx + Math.cos(angle) * radius, bottomY + height, cz + Math.sin(angle) * radius, 
                        -Math.cos(angle) * 0.05, 0.02, -Math.sin(angle) * 0.05); // Move inwards
                        
                    if (mc.world.random.nextInt(4) == 0) {
                        mc.world.addParticleClient(ParticleTypes.ENCHANTED_HIT, 
                            cx + Math.cos(angle) * radius, bottomY + height, cz + Math.sin(angle) * radius, 
                            0, 0.1, 0);
                    }
                }
            } else if (!m.exploded) {
                m.exploded = true;
                // Bloom explosion exactly at 3.8s (3800ms)
                
                // 1. The primary flower petals expanding outwards
                for (int i = 0; i < 300; i++) {
                    double angle = mc.world.random.nextDouble() * Math.PI * 2;
                    // Petals mostly flat but with slight upward curve
                    double horizontalSpeed = mc.world.random.nextDouble() * 0.8 + 0.2; 
                    double verticalSpeed = mc.world.random.nextDouble() * 0.2;
                    
                    mc.world.addParticleClient(ParticleTypes.CHERRY_LEAVES, 
                        cx, bottomY + 1.2, cz, 
                        Math.cos(angle) * horizontalSpeed, verticalSpeed, Math.sin(angle) * horizontalSpeed);
                }
                
                // 2. The glowing stamen shooting upwards
                for (int i = 0; i < 100; i++) {
                    double angle = mc.world.random.nextDouble() * Math.PI * 2;
                    double spread = mc.world.random.nextDouble() * 0.2;
                    double verticalSpeed = mc.world.random.nextDouble() * 0.6 + 0.3;
                    
                    mc.world.addParticleClient(ParticleTypes.END_ROD, 
                        cx, bottomY + 1.2, cz, 
                        Math.cos(angle) * spread, verticalSpeed, Math.sin(angle) * spread);
                }
                
                // 3. Magical burst
                for (int i = 0; i < 80; i++) {
                    mc.world.addParticleClient(ParticleTypes.FIREWORK, 
                        cx, bottomY + 1.5, cz, 
                        mc.world.random.nextGaussian() * 0.4, 
                        mc.world.random.nextGaussian() * 0.4, 
                        mc.world.random.nextGaussian() * 0.4);
                }
            } else {
                // Post-bloom: gentle magical aura and drifting petals
                for (int i = 0; i < 6; i++) {
                    double rx = mc.world.random.nextGaussian() * 3.5;
                    double ry = mc.world.random.nextDouble() * 5.0;
                    double rz = mc.world.random.nextGaussian() * 3.5;
                    
                    // Drifting down
                    mc.world.addParticleClient(ParticleTypes.CHERRY_LEAVES, 
                        cx + rx, bottomY + 1.0 + ry, cz + rz, 
                        mc.world.random.nextGaussian() * 0.02, -0.05, mc.world.random.nextGaussian() * 0.02);
                }
            }
        }

        for (ActiveNeoFrontier n : activeNeoFrontiers) {
            long elapsed = currentTime - n.startTime;
            double cx = n.center.x;
            double cy = n.bottomY;
            double cz = n.center.z;
            
            // ========== PHASE 1: Victim Sci-Fi Dissolution (0 - 1.5s) ==========
            if (elapsed < ActiveNeoFrontier.DISSOLVE_END) {
                float dissolveProgress = (float) elapsed / ActiveNeoFrontier.DISSOLVE_END;
                int particleCount = (int)(8 + dissolveProgress * 15);
                
                for (int i = 0; i < particleCount; i++) {
                    // Upward spiral dissolution - body breaking into energy
                    double angle = mc.world.random.nextDouble() * Math.PI * 2;
                    double spiralR = 0.3 + dissolveProgress * 1.5;
                    double px = cx + Math.cos(angle + elapsed / 200.0) * spiralR;
                    double pz = cz + Math.sin(angle + elapsed / 200.0) * spiralR;
                    double py = cy + mc.world.random.nextDouble() * (1.8 * dissolveProgress + 0.5);
                    
                    // Orange-tinted electric sparks (sci-fi tech breakdown)
                    mc.world.addParticleClient(ParticleTypes.ELECTRIC_SPARK, px, py, pz,
                        mc.world.random.nextGaussian() * 0.08, 0.15 + mc.world.random.nextDouble() * 0.2, mc.world.random.nextGaussian() * 0.08);
                    
                    // Cyan-tinted soul flames (holographic dissipation)
                    if (i % 2 == 0) {
                        mc.world.addParticleClient(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz,
                            mc.world.random.nextGaussian() * 0.05, 0.1, mc.world.random.nextGaussian() * 0.05);
                    }
                    
                    // Enchanted sparkles rising up
                    if (i % 3 == 0) {
                        mc.world.addParticleClient(ParticleTypes.ENCHANTED_HIT, 
                            cx + mc.world.random.nextGaussian() * 0.5, cy + mc.world.random.nextDouble() * 2.0, cz + mc.world.random.nextGaussian() * 0.5,
                            0, 0.3, 0);
                    }
                }
                
                // Ground crackling energy at victim's feet
                for (int i = 0; i < 3; i++) {
                    double gx = cx + mc.world.random.nextGaussian() * 0.8;
                    double gz = cz + mc.world.random.nextGaussian() * 0.8;
                    mc.world.addParticleClient(ParticleTypes.END_ROD, gx, cy + 0.05, gz, 0, 0.02, 0);
                }
            }

            // ========== PHASE 2: Desert Dust Storm (0.8s - 5.5s) ==========
            if (elapsed >= ActiveNeoFrontier.DUST_STORM_START && elapsed < ActiveNeoFrontier.DUST_STORM_END) {
                float stormIntensity = Math.min(1.0f, (elapsed - ActiveNeoFrontier.DUST_STORM_START) / 1000.0f);
                if (elapsed > ActiveNeoFrontier.DUST_STORM_END - 1500) {
                    stormIntensity *= Math.max(0.0f, (ActiveNeoFrontier.DUST_STORM_END - elapsed) / 1500.0f);
                }
                
                int dustCount = (int)(5 + stormIntensity * 12);
                // Wind direction (perpendicular to player facing for cinematic side-wind)
                double windX = Math.cos(Math.toRadians(n.playerYaw)) * 0.15;
                double windZ = Math.sin(Math.toRadians(n.playerYaw)) * 0.15;
                
                for (int i = 0; i < dustCount; i++) {
                    double angle = mc.world.random.nextDouble() * Math.PI * 2;
                    double dist = mc.world.random.nextDouble() * 7.0;
                    double px = cx + Math.cos(angle) * dist;
                    double pz = cz + Math.sin(angle) * dist;
                    double py = cy + mc.world.random.nextDouble() * 3.0;
                    
                    // Thick dust clouds blowing in the wind
                    mc.world.addParticleClient(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py, pz,
                        windX + mc.world.random.nextGaussian() * 0.03, 0.02, windZ + mc.world.random.nextGaussian() * 0.03);
                    
                    // Ground-level sand puffs
                    if (i % 2 == 0) {
                        mc.world.addParticleClient(ParticleTypes.SMOKE, 
                            cx + mc.world.random.nextGaussian() * 5, cy + 0.1 + mc.world.random.nextDouble() * 0.5, cz + mc.world.random.nextGaussian() * 5,
                            windX * 2, 0.03, windZ * 2);
                    }
                }
                
                // Swirling dust devils around the scene
                if (mc.world.random.nextInt(3) == 0) {
                    double dAngle = (elapsed / 500.0) + mc.world.random.nextDouble() * Math.PI;
                    double dR = 3.0 + mc.world.random.nextDouble() * 2.0;
                    for (int j = 0; j < 4; j++) {
                        double dpy = cy + j * 0.8 + mc.world.random.nextDouble() * 0.3;
                        mc.world.addParticleClient(ParticleTypes.CLOUD, 
                            cx + Math.cos(dAngle + j * 0.3) * dR, dpy, cz + Math.sin(dAngle + j * 0.3) * dR,
                            -Math.sin(dAngle) * 0.1, 0.05, Math.cos(dAngle) * 0.1);
                    }
                }
            }

            // ========== PHASE 3: Gunslinger Standoff + Gunshot (2.0s - 4.2s) ==========
            // Holographic materialization particles around cowboy
            if (elapsed >= ActiveNeoFrontier.GUNSLINGER_START && elapsed < ActiveNeoFrontier.GUNSLINGER_END) {
                double cowboyX = cx + Math.sin(Math.toRadians(n.playerYaw)) * 5.0;
                double cowboyZ = cz - Math.cos(Math.toRadians(n.playerYaw)) * 5.0;
                
                // Holographic shimmer around cowboy while materializing
                if (elapsed < ActiveNeoFrontier.GUNSLINGER_START + 600) {
                    for (int i = 0; i < 6; i++) {
                        mc.world.addParticleClient(ParticleTypes.END_ROD, 
                            cowboyX + mc.world.random.nextGaussian() * 0.5, cy + mc.world.random.nextDouble() * 2.0, cowboyZ + mc.world.random.nextGaussian() * 0.5,
                            0, 0.05, 0);
                        mc.world.addParticleClient(ParticleTypes.ELECTRIC_SPARK, 
                            cowboyX + mc.world.random.nextGaussian() * 0.3, cy + mc.world.random.nextDouble() * 1.8, cowboyZ + mc.world.random.nextGaussian() * 0.3,
                            mc.world.random.nextGaussian() * 0.02, 0.02, mc.world.random.nextGaussian() * 0.02);
                    }
                }
            }
            
            // Gunshot flash and bullet trail
            if (elapsed >= ActiveNeoFrontier.GUNSHOT_TIME && !n.gunshotFired) {
                n.gunshotFired = true;
                double cowboyX = cx + Math.sin(Math.toRadians(n.playerYaw)) * 5.0;
                double cowboyZ = cz - Math.cos(Math.toRadians(n.playerYaw)) * 5.0;
                
                // Muzzle flash at cowboy's weapon
                mc.world.addParticleClient(ParticleTypes.EXPLOSION, cowboyX, cy + 1.4, cowboyZ, 0, 0, 0);
                for (int i = 0; i < 15; i++) {
                    mc.world.addParticleClient(ParticleTypes.FLAME, cowboyX, cy + 1.4, cowboyZ,
                        mc.world.random.nextGaussian() * 0.15, mc.world.random.nextGaussian() * 0.1, mc.world.random.nextGaussian() * 0.15);
                }
                
                // Bullet trail from cowboy to victim (sonic boom line)
                double dirX = -(Math.sin(Math.toRadians(n.playerYaw)));
                double dirZ = Math.cos(Math.toRadians(n.playerYaw));
                for (int i = 0; i < 8; i++) {
                    double t = i / 8.0;
                    double bx = cowboyX + dirX * t * 5.0;
                    double bz = cowboyZ + dirZ * t * 5.0;
                    mc.world.addParticleClient(ParticleTypes.SONIC_BOOM, bx, cy + 1.4, bz, 0, 0, 0);
                }
                
                // Impact burst at victim location
                for (int i = 0; i < 30; i++) {
                    mc.world.addParticleClient(ParticleTypes.ELECTRIC_SPARK, cx, cy + 1.0, cz,
                        mc.world.random.nextGaussian() * 0.3, mc.world.random.nextGaussian() * 0.3, mc.world.random.nextGaussian() * 0.3);
                    if (i % 3 == 0) {
                        mc.world.addParticleClient(ParticleTypes.END_ROD, cx, cy + 1.0, cz,
                            mc.world.random.nextGaussian() * 0.2, mc.world.random.nextDouble() * 0.4 + 0.1, mc.world.random.nextGaussian() * 0.2);
                    }
                }
                
                // Play gunshot sound
                mc.world.playSoundClient(cowboyX, cy + 1.4, cowboyZ, SOUND_EXPLODE, SoundCategory.PLAYERS, 0.6F, 1.8F, false);
            }

            // ========== PHASE 4: Poster Landing Impact (3.8s - 5.5s) ==========
            if (elapsed >= ActiveNeoFrontier.POSTER_LAND && !n.posterLanded) {
                n.posterLanded = true;
                
                // Ground shockwave on poster impact
                for (int i = 0; i < 30; i++) {
                    double angle = mc.world.random.nextDouble() * Math.PI * 2;
                    double speed = 0.2 + mc.world.random.nextDouble() * 0.3;
                    mc.world.addParticleClient(ParticleTypes.CLOUD, cx, cy + 0.1, cz,
                        Math.cos(angle) * speed, 0.1, Math.sin(angle) * speed);
                    mc.world.addParticleClient(ParticleTypes.CAMPFIRE_COSY_SMOKE, cx, cy + 0.2, cz,
                        Math.cos(angle) * speed * 0.5, 0.15, Math.sin(angle) * speed * 0.5);
                }
                
                // Dramatic impact sound
                mc.world.playSoundClient(cx, cy, cz, SOUND_ANVIL, SoundCategory.PLAYERS, 0.8F, 0.5F, false);
            }
            
            // Ambient poster glow while visible
            if (elapsed >= ActiveNeoFrontier.POSTER_LAND && elapsed < ActiveNeoFrontier.GLITCH_START) {
                // Golden glow emanating from the poster
                if (mc.world.random.nextInt(3) == 0) {
                    mc.world.addParticleClient(ParticleTypes.END_ROD, 
                        cx + mc.world.random.nextGaussian() * 1.5, cy + 2.5 + mc.world.random.nextDouble() * 3.0, cz + mc.world.random.nextGaussian() * 1.5,
                        0, 0.03, 0);
                }
            }

            // ========== PHASE 5: Holographic Glitch Dissolution (6.0s - 7.5s) ==========
            if (elapsed >= ActiveNeoFrontier.GLITCH_START) {
                float glitchProgress = (elapsed - ActiveNeoFrontier.GLITCH_START) / (float)(ActiveNeoFrontier.TOTAL_DURATION - ActiveNeoFrontier.GLITCH_START);
                int glitchCount = (int)(3 + glitchProgress * 10);
                
                for (int i = 0; i < glitchCount; i++) {
                    // Electric sparks as hologram breaks down
                    mc.world.addParticleClient(ParticleTypes.ELECTRIC_SPARK, 
                        cx + mc.world.random.nextGaussian() * (1.5 + glitchProgress * 2), 
                        cy + 2.5 + mc.world.random.nextGaussian() * 2.5, 
                        cz + mc.world.random.nextGaussian() * (1.5 + glitchProgress * 2),
                        mc.world.random.nextGaussian() * 0.1, mc.world.random.nextGaussian() * 0.1, mc.world.random.nextGaussian() * 0.1);
                    
                    // Cyan soul flames (holographic energy dissipating)
                    if (i % 2 == 0) {
                        mc.world.addParticleClient(ParticleTypes.SOUL_FIRE_FLAME, 
                            cx + mc.world.random.nextGaussian() * 1.5, cy + 2.0 + mc.world.random.nextDouble() * 3.0, cz + mc.world.random.nextGaussian() * 1.5,
                            mc.world.random.nextGaussian() * 0.05, 0.1 + mc.world.random.nextDouble() * 0.15, mc.world.random.nextGaussian() * 0.05);
                    }
                    
                    // Dissolving data fragments
                    if (i % 3 == 0) {
                        mc.world.addParticleClient(ParticleTypes.ENCHANTED_HIT, 
                            cx + mc.world.random.nextGaussian() * 2, cy + 1.0 + mc.world.random.nextDouble() * 4.0, cz + mc.world.random.nextGaussian() * 2,
                            0, 0.15, 0);
                    }
                }
                
                // Final burst at the very end
                if (glitchProgress > 0.9f && mc.world.random.nextInt(2) == 0) {
                    for (int i = 0; i < 5; i++) {
                        mc.world.addParticleClient(ParticleTypes.END_ROD, cx, cy + 2.5, cz,
                            mc.world.random.nextGaussian() * 0.3, mc.world.random.nextGaussian() * 0.3, mc.world.random.nextGaussian() * 0.3);
                    }
                }
            }
        }
    }

    private void spawnLightning(LivingEntity victim) {
        if (mc.world == null) return;
        
        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
        lightning.refreshPositionAfterTeleport(victim.getEntityPos());
        lightning.setCosmetic(true);
        mc.world.addEntity(lightning);
    }

    private void spawnParticles(LivingEntity victim) {
        if (mc.world == null) return;
        
        net.minecraft.particle.ParticleEffect type = switch (particle.get()) {
            case "Heart" -> ParticleTypes.HEART;
            case "Flame" -> ParticleTypes.FLAME;
            case "Smoke" -> ParticleTypes.SMOKE;
            case "Magic" -> ParticleTypes.ENCHANTED_HIT;
            case "Crit" -> ParticleTypes.CRIT;
            case "Snow" -> ParticleTypes.SNOWFLAKE;
            case "Slime" -> ParticleTypes.ITEM_SLIME;
            case "Totem" -> ParticleTypes.TOTEM_OF_UNDYING;
            default -> ParticleTypes.EXPLOSION;
        };
        
        for (int i = 0; i < 20; i++) {
            mc.world.addParticleClient(type, victim.getX(), victim.getY() + victim.getHeight() / 2, victim.getZ(), 
                mc.world.random.nextGaussian() * 0.1, 
                mc.world.random.nextGaussian() * 0.1, 
                mc.world.random.nextGaussian() * 0.1);
        }
    }

    private void spawnTotem(LivingEntity victim) {
        if (mc.player == null || mc.world == null) return;
        
        mc.gameRenderer.showFloatingItem(new ItemStack(Items.TOTEM_OF_UNDYING));
        mc.world.playSoundClient(mc.player.getX(), mc.player.getY(), mc.player.getZ(), 
            SOUND_TOTEM, SoundCategory.PLAYERS, 1.0F, 1.0F, false);
    }

    private void spawnKuronami(LivingEntity victim) {
        if (mc.player == null || mc.world == null) return;

        // Spawn a cosmetic lightning bolt at start
        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
        lightning.refreshPositionAfterTeleport(victim.getEntityPos());
        lightning.setCosmetic(true);
        mc.world.addEntity(lightning);

        // Record the event to run continuously in Render hook!
        Vec3d center = victim.getBoundingBox().getCenter();
        double bottomY = victim.getY();
        
        activeKuronamis.add(new ActiveKuronami(center, bottomY));

        // Initial big strike sounds
        mc.world.playSoundClient(mc.player.getX(), mc.player.getY(), mc.player.getZ(), SOUND_SPLASH, SoundCategory.PLAYERS, 2.0F, 0.5F, false);
        mc.world.playSoundClient(mc.player.getX(), mc.player.getY(), mc.player.getZ(), SOUND_RAIN, SoundCategory.PLAYERS, 1.5F, 0.8F, false);
        mc.world.playSoundClient(mc.player.getX(), mc.player.getY(), mc.player.getZ(), SOUND_UNDERWATER, SoundCategory.PLAYERS, 1.5F, 0.5F, false);
        mc.world.playSoundClient(mc.player.getX(), mc.player.getY(), mc.player.getZ(), SOUND_THUNDER, SoundCategory.PLAYERS, 1.0F, 0.6F, false);
        mc.world.playSoundClient(mc.player.getX(), mc.player.getY(), mc.player.getZ(), FoxySounds.KURONAMI_ACE, SoundCategory.PLAYERS, 1.0F, 1.0F, false);
    }

    private void spawnChampion(LivingEntity victim) {
        if (mc.player == null || mc.world == null) return;

        Vec3d center = victim.getBoundingBox().getCenter();
        double bottomY = victim.getY();

        // Add to active loop (Looping tracks spawned via delay)
        activeChampions.add(new ActiveChampion(center, bottomY));

        // Blast the un-looped legendary ACE Intro track absolutely first!
        mc.world.playSoundClient(center.x, center.y, center.z, com.foxyclient.util.FoxySounds.CHAMPIONS_2023_ACE, SoundCategory.PLAYERS, 1.0F, 1.0F, false);

        // Initial pop visuals
        for (int i = 0; i < 150; i++) {
            mc.world.addParticleClient(ParticleTypes.END_ROD, center.x, center.y, center.z, mc.world.random.nextGaussian() * 0.5, mc.world.random.nextGaussian() * 0.5, mc.world.random.nextGaussian() * 0.5);
            mc.world.addParticleClient(ParticleTypes.GLOW, center.x, center.y, center.z, mc.world.random.nextGaussian() * 0.2, mc.world.random.nextGaussian() * 0.2, mc.world.random.nextGaussian() * 0.2);
        }
    }

    private void spawnAemondir(LivingEntity victim) {
        if (mc.player == null || mc.world == null) return;

        Vec3d center = victim.getBoundingBox().getCenter();
        double bottomY = victim.getY();

        activeAemondirs.add(new ActiveAemondir(center, bottomY, mc.player.getYaw()));

        // Play finisher sound
        mc.world.playSoundClient(center.x, center.y, center.z, com.foxyclient.util.FoxySounds.AEMONDIR_FINISHER, SoundCategory.PLAYERS, 1.0F, 1.0F, false);

        // Initial dark energy burst (victim dissolves into energy)
        for (int i = 0; i < 100; i++) {
            double rx = mc.world.random.nextGaussian() * 0.8;
            double ry = mc.world.random.nextGaussian() * 0.8;
            double rz = mc.world.random.nextGaussian() * 0.8;
            mc.world.addParticleClient(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z, rx, ry, rz);
            mc.world.addParticleClient(ParticleTypes.ENCHANTED_HIT, center.x, center.y, center.z, rx * 0.5, ry * 0.5, rz * 0.5);
            if (i % 5 == 0) mc.world.addParticleClient(ParticleTypes.END_ROD, center.x, center.y, center.z, rx * 0.3, Math.abs(ry) * 0.5 + 0.2, rz * 0.3);
        }
    }

    private void spawnMystbloom(LivingEntity victim) {
        if (mc.player == null || mc.world == null) return;

        Vec3d center = victim.getBoundingBox().getCenter();
        double bottomY = victim.getY();

        activeMystblooms.add(new ActiveMystbloom(center, bottomY));

        mc.world.playSoundClient(center.x, bottomY, center.z, com.foxyclient.util.FoxySounds.MYSTBLOOM_FINISHER, SoundCategory.PLAYERS, 1.0F, 1.0F, false);
    }

    private void spawnNeoFrontier(LivingEntity victim) {
        if (mc.player == null || mc.world == null) return;

        Vec3d center = victim.getBoundingBox().getCenter();
        double bottomY = victim.getY();

        activeNeoFrontiers.add(new ActiveNeoFrontier(mc.world, center, bottomY, mc.player.getYaw(), victim));

        // Play finisher sound
        mc.world.playSoundClient(center.x, bottomY, center.z, com.foxyclient.util.FoxySounds.NEOFRONTIER_FINISHER, SoundCategory.PLAYERS, 1.0F, 1.0F, false);

        // Initial sci-fi dissolution burst at kill moment
        for (int i = 0; i < 60; i++) {
            double rx = mc.world.random.nextGaussian() * 0.5;
            double ry = mc.world.random.nextGaussian() * 0.8;
            double rz = mc.world.random.nextGaussian() * 0.5;
            mc.world.addParticleClient(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, rx, ry, rz);
            if (i % 2 == 0) {
                mc.world.addParticleClient(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z, rx * 0.5, Math.abs(ry) * 0.3 + 0.1, rz * 0.5);
            }
            if (i % 4 == 0) {
                mc.world.addParticleClient(ParticleTypes.END_ROD, center.x, center.y, center.z, rx * 0.3, Math.abs(ry) * 0.5 + 0.2, rz * 0.3);
            }
        }

        // Desert wind ambiance
        mc.world.playSoundClient(center.x, bottomY, center.z, SOUND_EXPLODE, SoundCategory.PLAYERS, 0.3F, 0.4F, false);
    }

    // ====================== AEMONDIR 3D SWORD RENDERING ======================

    private void renderAemondirSwords3D(MatrixStack matrices, VertexConsumerProvider.Immediate vcp, Vec3d camPos, ActiveAemondir a, long now) {
        long elapsed = now - a.startTime;

        // Small swords disappear when greatsword hits the ground
        if (!a.greatswordImpacted) {
            for (int i = 0; i < ActiveAemondir.SWORD_COUNT; i++) {
                long sStart = ActiveAemondir.SWORD_SPAWN_TIMES[i];
                long sImpact = sStart + ActiveAemondir.SWORD_FALL_MS;
                if (elapsed < sStart) continue;

                // Landing position: elevated so it doesn't bury fully
                double landX = a.center.x;
                double landZ = a.center.z;
                double landY = a.bottomY + 1.0;

                // Start position: outside from center + high up
                double startX = a.center.x + a.swordOffsets[i][0] * 3.5;
                double startZ = a.center.z + a.swordOffsets[i][1] * 3.5;
                double startY = a.bottomY + 25.0;

                double swordX, swordZ, swordY;

                if (elapsed < sImpact) {
                    float t = (float)(elapsed - sStart) / ActiveAemondir.SWORD_FALL_MS;
                    t = Math.min(1.0f, t);
                    float e = t * t * t; // cubic ease-in (accelerating)
                    swordX = startX + (landX - startX) * e;
                    swordZ = startZ + (landZ - startZ) * e;
                    swordY = startY + (landY - startY) * e;
                } else {
                    swordX = landX;
                    swordZ = landZ;
                    swordY = landY;
                    if (!a.swordImpacted[i]) {
                        a.swordImpacted[i] = true;
                    }
                }
                renderSwordBlade(matrices, vcp, camPos, swordX, swordY, swordZ, (float)a.swordRotations[i], (float)a.swordTilts[i], 1.0f, 1.0f, false);
            }
        }

        // Greatsword
        if (elapsed >= ActiveAemondir.GREATSWORD_START) {
            long gsImpact = ActiveAemondir.GREATSWORD_START + ActiveAemondir.GREATSWORD_FALL_MS;
            double gy = a.bottomY + 2.5; // Elevate so the 4.5x scaled sword sticks out!
            float alpha = 1.0f;
            double swordY;

            if (elapsed < gsImpact) {
                float t = (float)(elapsed - ActiveAemondir.GREATSWORD_START) / ActiveAemondir.GREATSWORD_FALL_MS;
                t = Math.min(1.0f, t);
                float e = t * t;
                swordY = (gy + 40.0) - 40.0 * e;
            } else {
                swordY = gy;
                if (!a.greatswordImpacted) {
                    a.greatswordImpacted = true;
                }
                // Fade out greatsword in last 2 seconds
                long gsAge = elapsed - gsImpact;
                long fadeStart = ActiveAemondir.TOTAL_DURATION - gsImpact - 2000;
                if (gsAge > fadeStart) {
                    alpha = Math.max(0, 1.0f - (float)(gsAge - fadeStart) / 2000.0f);
                }
            }
            if (alpha > 0.01f) {
                renderSwordBlade(matrices, vcp, camPos, a.center.x, swordY, a.center.z, a.greatswordYaw, 0.0f, 3.0f, alpha, true);
            }
        }
    }

    private void renderSwordBlade(MatrixStack matrices, VertexConsumerProvider.Immediate vcp, Vec3d camPos,
                                   double wx, double wy, double wz, float yRot, float tilt, float scale, float alpha, boolean great) {
        if (alpha < 0.01f) return;
        ensureItemsInitialized();
        
        matrices.push();
        matrices.translate(wx - camPos.x, wy - camPos.y, wz - camPos.z);
        matrices.multiply(new Quaternionf().rotationY(yRot));
        if (tilt != 0) matrices.multiply(new Quaternionf().rotationX(-tilt));
        
        // Items in FIXED context usually point top-right natively (45 degrees).
        // Rotate by 135 degrees (Math.PI * 0.75) towards the bottom-right so that the tip points down.
        // Wait, if we want it straight down we rotate by 225 degrees (1.25 * PI)
        matrices.multiply(new Quaternionf().rotationZ((float)(Math.PI * 1.25))); 
        
        float finalScale = (great ? 4.5f : 1.8f) * scale;
        matrices.scale(finalScale, finalScale, finalScale);
        
        // In 1.21 native rendering, the item geometry spans from 0.0 to 1.0. 
        // We translate by -0.5 to center the mesh, ensuring rotations and placements occur exactly relative to the center of the sword!
        matrices.translate(-0.5f, -0.5f, -0.5f);
        
        renderItemState(SWORD_STATE, matrices, vcp, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, alpha);

        matrices.pop();
    }

    // ====================== MYSTBLOOM 3D FLOWER RENDERING ======================

    private void renderMystbloomFlower3D(MatrixStack matrices, VertexConsumerProvider.Immediate vcp, Vec3d camPos,
                                         double wx, double wy, double wz, float scale, float yRot, float tiltX, float tiltZ, float bloomProgress) {
        matrices.push();
        matrices.translate(wx - camPos.x, wy - camPos.y, wz - camPos.z);
        matrices.multiply(new Quaternionf().rotationY(yRot));
        matrices.multiply(new Quaternionf().rotationX(tiltX));
        matrices.multiply(new Quaternionf().rotationZ(tiltZ));
        matrices.scale(scale, scale, scale);
        
        VertexConsumer buf = vcp.getBuffer(CHERRY_LEAVES_LAYER);
        
        // Multi-layered petals
        // Inner layer (6 petals)
        for (int i = 0; i < 6; i++) {
            float angle = (float)(i * Math.PI * 2.0 / 6.0);
            renderPetal(matrices, buf, angle, bloomProgress, 0.8f, 1.0f, 0.4f, 0.7f, 0.9f);
        }
        
        // Outer layer (8 petals)
        for (int i = 0; i < 8; i++) {
            float angle = (float)((i + 0.5) * Math.PI * 2.0 / 8.0);
            // Outer layer blooms wider
            renderPetal(matrices, buf, angle, bloomProgress * 1.15f, 1.3f, 0.9f, 0.2f, 0.8f, 0.5f);
        }
        matrices.pop();
    }
    
    private void renderPetal(MatrixStack matrices, VertexConsumer buf,
                             float yRot, float bloom, float scale, float rIn, float gIn, float bIn, float alpha) {
        matrices.push();
        matrices.multiply(new Quaternionf().rotationY(yRot));
        
        // Pitch mapping from bloom. 
        // bloom = 0 => petal points almost straight up (closed bud)
        // bloom >= 1 => petal lays flat (bloomed flower)
        float clampedBloom = Math.max(0.0f, Math.min(1.0f, bloom));
        float startPitch = -1.4f; // radians (negated to point UP relative to Minecraft's coordinate system)
        float endPitch = 0.1f;    // radians (slightly drooping below horizontal)
        float currentPitch = startPitch - clampedBloom * (startPitch - endPitch);
        
        matrices.multiply(new Quaternionf().rotationX(currentPitch));
        matrices.scale(scale, scale, scale);
        
        Matrix4f m = matrices.peek().getPositionMatrix();
        
        // Minecraft-like chunky blocky petal (simple rectangle)
        // We ignore the rIn,gIn,bIn so the cherry leaf texture is rendered in its true untinted color
        float w = 0.35f; // half-width
        float l = 1.3f;  // length
        float r = 1.0f, g = 1.0f, b = 1.0f;
        
        // Map full texture 0..1 to the quad
        // Top Face (pointing UP natively)
        vertexuv(buf, m, -w, 0, 0, r, g, b, alpha, 0.0f, 1.0f);
        vertexuv(buf, m,  w, 0, 0, r, g, b, alpha, 1.0f, 1.0f);
        vertexuv(buf, m,  w, 0, l, r, g, b, alpha, 1.0f, 0.0f);
        vertexuv(buf, m, -w, 0, l, r, g, b, alpha, 0.0f, 0.0f);
        
        // Bottom Face (Flipped)
        vertexuv(buf, m, -w, 0, l, r*0.8f, g*0.8f, b*0.8f, alpha, 0.0f, 0.0f);
        vertexuv(buf, m,  w, 0, l, r*0.8f, g*0.8f, b*0.8f, alpha, 1.0f, 0.0f);
        vertexuv(buf, m,  w, 0, 0, r*0.8f, g*0.8f, b*0.8f, alpha, 1.0f, 1.0f);
        vertexuv(buf, m, -w, 0, 0, r*0.8f, g*0.8f, b*0.8f, alpha, 0.0f, 1.0f);

        matrices.pop();
    }
    
    private void vertexuv(VertexConsumer buf, Matrix4f m, float x, float y, float z, float r, float g, float b, float a, float u, float v) {
        buf.vertex(m, x, y, z)
           .color(r, g, b, a)
           .texture(u, v)
           .overlay(OverlayTexture.DEFAULT_UV)
           .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
           .normal(0, 1, 0);
    }


    @Override
    public void onEnable() {
        activeKuronamis.clear();
        activeChampions.clear();
        activeAemondirs.clear();
        activeMystblooms.clear();
        for (ActiveNeoFrontier n : activeNeoFrontiers) {
            if (n.cowboy != null) n.cowboy.discard();
        }
        activeNeoFrontiers.clear();
    }

    @Override
    public void onDisable() {
        activeKuronamis.clear();
        for (ActiveChampion c : activeChampions) {
            if (c.music1 != null) { c.music1.forceStop(); mc.getSoundManager().stop(c.music1); }
            if (c.music2 != null) { c.music2.forceStop(); mc.getSoundManager().stop(c.music2); }
        }
        activeChampions.clear();
        activeAemondirs.clear();
        activeMystblooms.clear();
        for (ActiveNeoFrontier n : activeNeoFrontiers) {
            if (n.cowboy != null) n.cowboy.discard();
        }
        activeNeoFrontiers.clear();
    }
}
