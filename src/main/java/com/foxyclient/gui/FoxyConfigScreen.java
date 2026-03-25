package com.foxyclient.gui;

import com.foxyclient.FoxyClient;
import com.foxyclient.module.Module;
import com.foxyclient.module.ui.CustomKeybinds;
import com.foxyclient.util.FoxyConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

public class FoxyConfigScreen extends Screen {
    private final Screen parent;
    private Tab currentTab = Tab.GENERAL;
    private Module listeningModule = null;

    // Staggered slide-in animation
    private long tabSwitchTime = 0;
    private static final float STAGGER_DELAY_MS = 40f;   // delay between each item
    private static final float ITEM_ANIM_DURATION_MS = 300f; // duration per item
    private static final int SLIDE_DISTANCE = 60;

    // Track content widgets with their index for stagger
    private final List<ClickableWidget> contentWidgets = new ArrayList<>();
    private final List<Integer> contentWidgetBaseX = new ArrayList<>();
    private final List<ClickableWidget> sidebarWidgets = new ArrayList<>();

    // Macro management state
    private TextFieldWidget macroCommandField;
    private int macroListeningKey = -1;
    private int macroPendingKey = -1;
    private int macroScrollOffset = 0;

    // Keybind scroll
    private double targetScrollPixels = 0;
    private double currentScrollPixels = 0;

    // Skin preview drag state
    private float skinYaw = 0;
    private float skinPitch = 0;
    private boolean isDraggingSkin = false;
    private java.util.function.Supplier<net.minecraft.client.texture.PlayerSkinCache.Entry> skinEntrySupplier = null;

    private enum Tab {
        GENERAL("General"),
        KEYBINDS("Keybinds"),
        MACROS("Macros"),
        COSMETICS("Cosmetics");

        final String name;
        Tab(String name) { this.name = name; }
    }

    public FoxyConfigScreen(Screen parent) {
        super(Text.literal("FoxyClient Config"));
        this.parent = parent;
        this.tabSwitchTime = System.currentTimeMillis();
    }

    private <T extends ClickableWidget> T addContent(T widget) {
        contentWidgets.add(widget);
        contentWidgetBaseX.add(widget.getX());
        this.addDrawableChild(widget);
        return widget;
    }

    @Override
    protected void init() {
        contentWidgets.clear();
        contentWidgetBaseX.clear();
        sidebarWidgets.clear();
        macroCommandField = null;

        int sidebarWidth = 100;
        int y = 40;

        // Tab Buttons (not animated — part of sidebar)
        for (Tab tab : Tab.values()) {
            sidebarWidgets.add(this.addDrawableChild(ButtonWidget.builder(Text.literal(tab.name), b -> {
                if (this.currentTab != tab) {
                    this.currentTab = tab;
                    this.tabSwitchTime = System.currentTimeMillis();
                    this.targetScrollPixels = 0;
                    this.currentScrollPixels = 0;
                    this.clearAndInit();
                }
            }).dimensions(10, y, sidebarWidth - 20, 20).build()));
            y += 25;
        }

        // Content Area
        int contentX = sidebarWidth + 20;
        int contentY = 40;

        if (currentTab == Tab.GENERAL) {
            int currentY = contentY;
            addContent(CyclingButtonWidget.onOffBuilder(FoxyConfig.INSTANCE.transitionsEnabled.get())
                .build(contentX, currentY, 200, 20, Text.literal("Screen Transitions"), (button, value) -> {
                    FoxyConfig.INSTANCE.transitionsEnabled.set((Boolean)value);
                    FoxyConfig.INSTANCE.save();
                }));
            currentY += 25;
            
            addContent(CyclingButtonWidget.onOffBuilder(FoxyConfig.INSTANCE.inGameTransitions.get())
                .build(contentX, currentY, 200, 20, Text.literal("In-Game Transitions (ESC)"), (button, value) -> {
                    FoxyConfig.INSTANCE.inGameTransitions.set((Boolean)value);
                    FoxyConfig.INSTANCE.save();
                }));
            currentY += 35; // Add extra padding before new section

            // Music Section
            addContent(new com.foxyclient.gui.widget.FoxyLabelWidget(textRenderer, contentX, currentY, "§bBackground Music"));
            currentY += 15;
            
            addContent(CyclingButtonWidget.onOffBuilder(FoxyConfig.INSTANCE.menuMusicEnabled.get())
                .build(contentX, currentY, 200, 20, Text.literal("Enable Music"), (button, value) -> {
                    FoxyConfig.INSTANCE.menuMusicEnabled.set((Boolean)value);
                    FoxyConfig.INSTANCE.save();
                    com.foxyclient.util.FoxyMusicManager.play(); // Auto-restart or stop based on new state
                }));
            currentY += 25;
            
            String[] musicOptions = {"Default", "Custom"};
            addContent(CyclingButtonWidget.builder((String s) -> Text.literal(s), FoxyConfig.INSTANCE.bgMusicType.get())
                .values(musicOptions)
                .build(contentX, currentY, 200, 20, Text.literal("Source"), (button, value) -> {
                    FoxyConfig.INSTANCE.bgMusicType.set(value);
                    FoxyConfig.INSTANCE.save();
                    com.foxyclient.util.FoxyMusicManager.play();
                }));
            currentY += 25;

            addContent(ButtonWidget.builder(Text.literal("§dSelect Custom Music..."), b -> {
                new Thread(() -> {
                    org.lwjgl.PointerBuffer filters = org.lwjgl.system.MemoryUtil.memAllocPointer(3);
                    filters.put(org.lwjgl.system.MemoryUtil.memAddress(org.lwjgl.system.MemoryUtil.memUTF8("*.wav")));
                    filters.put(org.lwjgl.system.MemoryUtil.memAddress(org.lwjgl.system.MemoryUtil.memUTF8("*.mp3")));
                    filters.put(org.lwjgl.system.MemoryUtil.memAddress(org.lwjgl.system.MemoryUtil.memUTF8("*.ogg")));
                    filters.flip();

                    String startPath = System.getProperty("user.home") + java.io.File.separator;
                    String result = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog("Select Music File", startPath, filters, "Audio Files (.wav, .mp3, .ogg)", false);
                    org.lwjgl.system.MemoryUtil.memFree(filters);

                    if (result != null) {
                        java.io.File selected = new java.io.File(result);
                        if (selected.exists() && selected.isFile()) {
                            client.execute(() -> {
                                try {
                                    com.foxyclient.util.FoxyMusicManager.stop();
                                    
                                    String ext = "";
                                    String name = selected.getName();
                                    int i = name.lastIndexOf('.');
                                    if (i > 0) ext = name.substring(i);
                                    
                                    java.nio.file.Path configDir = client.runDirectory.toPath().resolve("config").resolve("foxyclient");
                                    java.nio.file.Files.createDirectories(configDir);
                                    
                                    java.nio.file.Files.deleteIfExists(configDir.resolve("background_music.wav"));
                                    java.nio.file.Files.deleteIfExists(configDir.resolve("background_music.mp3"));
                                    java.nio.file.Files.deleteIfExists(configDir.resolve("background_music.ogg"));
                                    
                                    java.nio.file.Path destFile = configDir.resolve("background_music" + ext);
                                    java.nio.file.Files.copy(selected.toPath(), destFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                                    FoxyConfig.INSTANCE.customMusicPath.set(destFile.getFileName().toString());
                                    FoxyConfig.INSTANCE.customMusicName.set(selected.getName());
                                    FoxyConfig.INSTANCE.bgMusicType.set("Custom");
                                    FoxyConfig.INSTANCE.save();
                                    FoxyClient.LOGGER.info("Configured Custom Music Path: " + destFile.toAbsolutePath().toString());
                                    this.clearAndInit();
                                    com.foxyclient.util.FoxyMusicManager.play(); // Play selected
                                } catch (java.io.IOException e) {
                                    FoxyClient.LOGGER.error("Failed to cache custom background music", e);
                                }
                            });
                        }
                    }
                }, "MusicFileBrowser").start();
            }).dimensions(contentX, currentY, 200, 20).build());
            
            // Show current name under music button if custom
            if ("Custom".equals(FoxyConfig.INSTANCE.bgMusicType.get())) {
                String n = FoxyConfig.INSTANCE.customMusicName.get();
                if (!n.isEmpty()) {
                    addContent(new com.foxyclient.gui.widget.FoxyLabelWidget(textRenderer, contentX, currentY + 25, "§7Selected: §f" + n));
                }
            }
            currentY += 40; // Prevent collision with Visuals
            
            addContent(new com.foxyclient.gui.widget.FoxyLabelWidget(textRenderer, contentX, currentY, "§bVisuals"));
            currentY += 15;

            // Background Type Selection
            String[] bgOptions = {"Default", "FoxyClient", "Custom"};
            addContent(CyclingButtonWidget.builder((String s) -> Text.literal(s), FoxyConfig.INSTANCE.customBackgroundType.get())
                .values(bgOptions)
                .build(contentX, currentY, 200, 20, Text.literal("Background"), (button, value) -> {
                    FoxyConfig.INSTANCE.customBackgroundType.set(value);
                    FoxyConfig.INSTANCE.save();
                    com.foxyclient.util.VideoHelper.initBackground();
                    this.clearAndInit();
                }));
            currentY += 25;

            if ("Custom".equals(FoxyConfig.INSTANCE.customBackgroundType.get())) {
                // Background File Selection
                addContent(ButtonWidget.builder(Text.literal("§dSelect Background..."), b -> {
                    new Thread(() -> {
                        org.lwjgl.PointerBuffer filters = org.lwjgl.system.MemoryUtil.memAllocPointer(2);
                        filters.put(org.lwjgl.system.MemoryUtil.memAddress(org.lwjgl.system.MemoryUtil.memUTF8("*.png")));
                        filters.put(org.lwjgl.system.MemoryUtil.memAddress(org.lwjgl.system.MemoryUtil.memUTF8("*.jpg")));
                        filters.flip();

                        String startPath = System.getProperty("user.home") + java.io.File.separator;
                        String result = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog("Select Custom Background", startPath, filters, "Image Files (*.png, *.jpg)", false);
                        org.lwjgl.system.MemoryUtil.memFree(filters);

                        if (result != null) {
                            java.io.File selected = new java.io.File(result);
                            if (selected.exists() && selected.isFile()) {
                                client.execute(() -> {
                                    try {
                                        com.foxyclient.util.VideoHelper.stopVideo();
                                        
                                        String ext = "";
                                        String name = selected.getName();
                                        int i = name.lastIndexOf('.');
                                        if (i > 0) ext = name.substring(i);
                                        
                                        java.nio.file.Path configDir = client.runDirectory.toPath().resolve("config").resolve("foxyclient");
                                        java.nio.file.Files.createDirectories(configDir);
                                        
                                        java.nio.file.Files.deleteIfExists(configDir.resolve("background.png"));
                                        java.nio.file.Files.deleteIfExists(configDir.resolve("background.jpg"));
                                        java.nio.file.Files.deleteIfExists(configDir.resolve("background.jpeg"));
                                        
                                        java.nio.file.Path destFile;
                                        if (".jpg".equals(ext) || ".jpeg".equals(ext)) {
                                            destFile = configDir.resolve("background.png");
                                            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(selected);
                                            if (img != null) {
                                                javax.imageio.ImageIO.write(img, "png", destFile.toFile());
                                            } else {
                                                throw new java.io.IOException("Failed to decode JPG for PNG conversion");
                                            }
                                        } else {
                                            destFile = configDir.resolve("background" + ext);
                                            java.nio.file.Files.copy(selected.toPath(), destFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                        }

                                        FoxyConfig.INSTANCE.customBackgroundPath.set(destFile.getFileName().toString());
                                        FoxyConfig.INSTANCE.customBackgroundName.set(selected.getName());
                                        FoxyConfig.INSTANCE.save();
                                        com.foxyclient.util.VideoHelper.initBackground(); // Reload background
                                        this.clearAndInit();
                                    } catch (java.io.IOException e) {
                                        FoxyClient.LOGGER.error("Failed to cache custom background", e);
                                    }
                                });
                            }
                        }
                    }, "BackgroundBrowser").start();
                }).dimensions(contentX, currentY, 200, 20).build());
                
                String bgName = FoxyConfig.INSTANCE.customBackgroundName.get();
                if (!bgName.isEmpty()) {
                    addContent(new com.foxyclient.gui.widget.FoxyLabelWidget(textRenderer, contentX, currentY + 25, "§7Selected: §f" + bgName));
                }
                currentY += 40;
            } else {
                currentY += 10;
            }

            // Font Type Selection
            String[] fontOptions = {"Default", "FoxyClient", "Custom"};
            addContent(CyclingButtonWidget.builder((String s) -> Text.literal(s), FoxyConfig.INSTANCE.customFontType.get())
                .values(fontOptions)
                .build(contentX, currentY, 200, 20, Text.literal("Font"), (button, value) -> {
                    FoxyConfig.INSTANCE.customFontType.set(value);
                    FoxyConfig.INSTANCE.save();
                    client.reloadResources();
                    this.clearAndInit();
                }));
            currentY += 25;

            if ("Custom".equals(FoxyConfig.INSTANCE.customFontType.get())) {
                // Font File Selection
                addContent(ButtonWidget.builder(Text.literal("§dSelect Custom Font..."), b -> {
                    new Thread(() -> {
                        org.lwjgl.PointerBuffer filters = org.lwjgl.system.MemoryUtil.memAllocPointer(1);
                        filters.put(org.lwjgl.system.MemoryUtil.memAddress(org.lwjgl.system.MemoryUtil.memUTF8("*.ttf")));
                        filters.flip();

                        String startPath = System.getProperty("user.home") + java.io.File.separator;
                        String result = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog("Select TrueType Font", startPath, filters, "Font Files (*.ttf)", false);
                        org.lwjgl.system.MemoryUtil.memFree(filters);

                        if (result != null) {
                            java.io.File selected = new java.io.File(result);
                            if (selected.exists() && selected.isFile()) {
                                client.execute(() -> {
                                    try {
                                        java.nio.file.Path configDir = client.runDirectory.toPath().resolve("config").resolve("foxyclient");
                                        java.nio.file.Files.createDirectories(configDir);
                                        
                                        java.nio.file.Path destFile = configDir.resolve("CustomFont.ttf");
                                        java.nio.file.Files.copy(selected.toPath(), destFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                                        FoxyConfig.INSTANCE.customFontPath.set(destFile.getFileName().toString());
                                        FoxyConfig.INSTANCE.customFontName.set(selected.getName());
                                        FoxyConfig.INSTANCE.save();
                                        client.reloadResources(); // In-memory mixin will supply the font immediately
                                        this.clearAndInit();
                                    } catch (java.io.IOException e) {
                                        FoxyClient.LOGGER.error("Failed to cache custom font", e);
                                    }
                                });
                            }
                        }
                    }, "FontBrowser").start();
                }).dimensions(contentX, currentY, 200, 20).build());

                String fontName = FoxyConfig.INSTANCE.customFontName.get();
                if (!fontName.isEmpty()) {
                    addContent(new com.foxyclient.gui.widget.FoxyLabelWidget(textRenderer, contentX, currentY + 25, "§7Selected: §f" + fontName));
                }
            }

        } else if (currentTab == Tab.KEYBINDS) {
            // Show ALL modules with smooth scrolling
            List<Module> allModules = FoxyClient.INSTANCE.getModuleManager().getModules();
            int ky = contentY;
            for (Module m : allModules) {
                String keyName = getKeyName(m.getKeybind());
                String label = m.getName() + ": §b" + (listeningModule == m ? "§e..." : keyName);
                addContent(ButtonWidget.builder(Text.literal(label), b -> {
                    listeningModule = m;
                    b.setMessage(Text.literal(m.getName() + ": §ePress a key..."));
                }).dimensions(contentX, ky, 220, 20).build());
                ky += 22;
            }

        } else if (currentTab == Tab.MACROS) {
            String keyLabel = macroPendingKey != -1 ? getKeyName(macroPendingKey) : "Set Key";
            addContent(ButtonWidget.builder(Text.literal(macroListeningKey == 1 ? "§e..." : keyLabel), b -> {
                macroListeningKey = 1;
                b.setMessage(Text.literal("§ePress a key..."));
            }).dimensions(contentX, contentY, 80, 20).build());

            macroCommandField = new TextFieldWidget(this.textRenderer, contentX + 85, contentY, 160, 20, Text.literal("Command"));
            macroCommandField.setPlaceholder(Text.literal("Command or /command"));
            macroCommandField.setMaxLength(256);
            addContent(macroCommandField);

            addContent(ButtonWidget.builder(Text.literal("§a+ Add"), b -> {
                CustomKeybinds ck = getCustomKeybinds();
                if (ck != null && macroPendingKey != -1 && macroCommandField != null) {
                    String cmd = macroCommandField.getText().trim();
                    if (!cmd.isEmpty()) {
                        ck.addMacro(macroPendingKey, cmd);
                        macroPendingKey = -1;
                        macroCommandField.setText("");
                        this.clearAndInit();
                    }
                }
            }).dimensions(contentX + 250, contentY, 50, 20).build());

        } else if (currentTab == Tab.COSMETICS) {
            int currentY = contentY;

            // --- Skin Selection ---
            addContent(new com.foxyclient.gui.widget.FoxyLabelWidget(textRenderer, contentX, currentY, "Select Skin"));
            currentY += 15;
            String[] skinOptions = {"Default", "Steve", "Alex", "Custom"};
            addContent(CyclingButtonWidget.builder((String s) -> Text.literal(s), FoxyConfig.INSTANCE.skinName.get())
                .values(skinOptions)
                .build(contentX, currentY, 150, 20, Text.literal("Skin"), (button, value) -> {
                    FoxyConfig.INSTANCE.skinName.set(value);
                    this.clearAndInit();
                }));
            currentY += 30;

            // --- Cape Selection ---
            addContent(new com.foxyclient.gui.widget.FoxyLabelWidget(textRenderer, contentX, currentY, "Select Cape"));
            currentY += 15;
            String[] capeOptions = {"Default", "None", "Custom"};
            addContent(CyclingButtonWidget.builder((String c) -> Text.literal(c), FoxyConfig.INSTANCE.capeName.get())
                .values(capeOptions)
                .build(contentX, currentY, 150, 20, Text.literal("Cape"), (button, value) -> {
                    FoxyConfig.INSTANCE.capeName.set(value);
                    this.clearAndInit();
                }));
            currentY += 25;

            // --- Model Selection ---
            addContent(CyclingButtonWidget.onOffBuilder(FoxyConfig.INSTANCE.slimModel.get())
                .build(contentX, currentY, 150, 20, Text.literal("Slim Model"), (button, value) -> {
                    FoxyConfig.INSTANCE.slimModel.set(value);
                    this.clearAndInit();
                }));
            currentY += 30;

            if (skinEntrySupplier == null) {
                skinEntrySupplier = client.getPlayerSkinCache().getSupplier(net.minecraft.component.type.ProfileComponent.ofDynamic(client.getSession().getUuidOrNull()));
            }

            // Automatic caching of official cosmetics
            if (skinEntrySupplier != null) {
                net.minecraft.client.texture.PlayerSkinCache.Entry entry = skinEntrySupplier.get();
                if (entry != null) {
                    try {
                        // Use reflection to get textures if possible
                        for (java.lang.reflect.Method m : entry.getClass().getMethods()) {
                            if (m.getParameterCount() == 0 && (m.getName().equals("textures") || m.getName().equals("getSkinTextures"))) {
                                Object textures = m.invoke(entry);
                                if (textures != null) {
                                    com.foxyclient.util.SkinResourceManager.cacheDefaultCosmetics(textures);
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {}
                }
            }

            addContent(ButtonWidget.builder(Text.literal("§bChange Skin..."), b -> {
                new Thread(() -> {
                    org.lwjgl.PointerBuffer filters = org.lwjgl.system.MemoryUtil.memAllocPointer(1);
                    filters.put(org.lwjgl.system.MemoryUtil.memAddress(org.lwjgl.system.MemoryUtil.memUTF8("*.png")));
                    filters.flip();

                    String startPath = System.getProperty("user.home") + java.io.File.separator;
                    String result = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog("Select Skin PNG", startPath, filters, "PNG Images", false);
                    org.lwjgl.system.MemoryUtil.memFree(filters);

                    if (result != null) {
                        java.io.File selected = new java.io.File(result);
                        if (selected.exists() && selected.isFile()) {
                            client.execute(() -> {
                                com.foxyclient.util.SkinResourceManager.setCustomSkin(selected.getAbsolutePath());
                                FoxyConfig.INSTANCE.skinName.set("Custom");
                                FoxyConfig.INSTANCE.save();
                                this.clearAndInit();
                            });
                        }
                    }
                }, "SkinFileBrowser").start();
            }).dimensions(contentX, currentY, 150, 20).build());
            currentY += 25;

            addContent(ButtonWidget.builder(Text.literal("§dChange Cape..."), b -> {
                new Thread(() -> {
                    org.lwjgl.PointerBuffer filters = org.lwjgl.system.MemoryUtil.memAllocPointer(1);
                    filters.put(org.lwjgl.system.MemoryUtil.memAddress(org.lwjgl.system.MemoryUtil.memUTF8("*.png")));
                    filters.flip();

                    String startPath = System.getProperty("user.home") + java.io.File.separator;
                    String result = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog("Select Cape PNG", startPath, filters, "PNG Images", false);
                    org.lwjgl.system.MemoryUtil.memFree(filters);

                    if (result != null) {
                        java.io.File selected = new java.io.File(result);
                        if (selected.exists() && selected.isFile()) {
                            client.execute(() -> {
                                com.foxyclient.util.SkinResourceManager.setCustomCape(selected.getAbsolutePath());
                                FoxyConfig.INSTANCE.capeName.set("Custom");
                                FoxyConfig.INSTANCE.save();
                                this.clearAndInit();
                            });
                        }
                    }
                }, "CapeFileBrowser").start();
            }).dimensions(contentX, currentY, 150, 20).build());
            currentY += 25;

            currentY += 10;

            // --- FoxyClient Account Section ---
            // Only show if the server has verified this as a FoxyClient account
            if (com.foxyclient.FoxyClient.INSTANCE.isFoxyAccount()) {
                addContent(new com.foxyclient.gui.widget.FoxyLabelWidget(textRenderer, contentX, currentY, "§bFoxyClient Account"));
                currentY += 15;
                if (com.foxyclient.FoxyClient.INSTANCE.isFoxyAccount()) {
                    // Logged In Info
                    String status = "§7Account: §f" + client.getSession().getUsername() + " §7(§aFoxyClient Account§7)";
                    addContent(new com.foxyclient.gui.widget.FoxyLabelWidget(textRenderer, contentX, currentY, status));
                    
                    addContent(ButtonWidget.builder(Text.literal("Sync to Server"), b -> {
                        b.setMessage(Text.literal("§eSyncing..."));
                        b.active = false;
                        new Thread(() -> {
                            boolean success = uploadCosmetics();
                            client.execute(() -> {
                                b.setMessage(Text.literal(success ? "§aSynced!" : "§cFailed"));
                                b.active = true;
                                if (success) FoxyConfig.INSTANCE.save();
                            });
                        }).start();
                    }).dimensions(contentX, currentY + 15, 150, 20).build());
                }else{
                    String status = "§7Account: §cNot FoxyClient Account";
                    addContent(new com.foxyclient.gui.widget.FoxyLabelWidget(textRenderer, contentX, currentY, status));
                    
                }
            }
        }

        // Back button (not animated)
        sidebarWidgets.add(this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> this.client.setScreen(parent))
            .dimensions(10, height - 30, sidebarWidth - 20, 20).build()));
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();

        // Macro key listening
        if (macroListeningKey == 1) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                macroListeningKey = -1;
                macroPendingKey = -1;
            } else {
                macroPendingKey = keyCode;
                macroListeningKey = -1;
            }
            this.clearAndInit();
            return true;
        }

        // Module keybind listening
        if (listeningModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                listeningModule.setKeybind(-1);
            } else {
                listeningModule.setKeybind(keyCode);
            }
            // Save config instantly
            FoxyClient.INSTANCE.getModuleManager().saveConfig();
            listeningModule = null;
            this.clearAndInit();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Interpolate scroll
        if (Math.abs(targetScrollPixels - currentScrollPixels) > 0.1) {
            currentScrollPixels += (targetScrollPixels - currentScrollPixels) * 0.25;
        } else {
            currentScrollPixels = targetScrollPixels;
        }

        long elapsed = System.currentTimeMillis() - tabSwitchTime;

        // Staggered animation: each widget gets its own delayed progress
        for (int i = 0; i < contentWidgets.size(); i++) {
            ClickableWidget widget = contentWidgets.get(i);
            int baseX = contentWidgetBaseX.get(i);

            // Each item starts after a stagger delay
            float itemStart = i * STAGGER_DELAY_MS;
            float itemElapsed = Math.max(0, elapsed - itemStart);
            float itemProgress = Math.min(1.0f, itemElapsed / ITEM_ANIM_DURATION_MS);
            float ease = easeOutCubic(itemProgress);

            int slideOffset = (int)((1.0f - ease) * -SLIDE_DISTANCE);
            widget.setX(baseX + slideOffset);
            widget.setAlpha(ease);

            if (currentTab == Tab.KEYBINDS) {
                // Adjust Y based on scroll
                int baseY = 40 + i * 22;
                widget.setY(baseY - (int)currentScrollPixels);
                // Hide if totally out of view (improves performance and prevents accidental clicks)
                widget.visible = widget.getY() + widget.getHeight() > 35 && widget.getY() < height - 35;
            }
        }

        // Clip the scrollable area
        if (currentTab == Tab.KEYBINDS) {
            context.enableScissor(100, 38, width, height - 38);
        }

        // Render all content widgets manually (prevents scissor from clipping everything out recursively)
        for (ClickableWidget widget : contentWidgets) {
            widget.render(context, mouseX, mouseY, delta);
        }

        if (currentTab == Tab.KEYBINDS) {
            context.disableScissor();
        }
        
        // Sidebar (drawn on top to clip sliding content)
        context.fill(0, 0, 100, height, 0xAA000000);
        context.fill(99, 0, 100, height, 0xFF00FCFC);
        context.drawCenteredTextWithShadow(textRenderer, "§b§lFOXY§fCONFIG", 50, 15, 0xFFFFFFFF);

        // Sidebar widgets drawn at the very top (never scissored)
        for (ClickableWidget widget : sidebarWidgets) {
            widget.render(context, mouseX, mouseY, delta);
        }

        // Custom-drawn content uses the "last" item's animation for overall alpha
        int totalItems = contentWidgets.size();
        float lastItemStart = Math.max(0, (totalItems - 1)) * STAGGER_DELAY_MS;
        float lastElapsed = Math.max(0, elapsed - lastItemStart);
        float overallAlpha = Math.min(1.0f, lastElapsed / ITEM_ANIM_DURATION_MS);
        int alpha = (int)(easeOutCubic(overallAlpha) * 255);
        if (alpha <= 0) return;

        // Macros tab: calculate slide offset
        if (currentTab == Tab.MACROS) {
            // Calculate slide offset for custom drawn content (use first item's stagger)
            float firstElapsed = Math.max(0, elapsed - 0);
            float firstEase = easeOutCubic(Math.min(1.0f, firstElapsed / ITEM_ANIM_DURATION_MS));
            int slideOffset = (int)((1.0f - firstEase) * -SLIDE_DISTANCE);
            renderMacroList(context, mouseX, mouseY, slideOffset, alpha);
        }

        if (currentTab == Tab.COSMETICS) {
            // Use stagger for cosmetics content too
            float cosElapsed = Math.max(0, elapsed - 2 * STAGGER_DELAY_MS);
            float cosEase = easeOutCubic(Math.min(1.0f, cosElapsed / ITEM_ANIM_DURATION_MS));
            int slideOffset = (int)((1.0f - cosEase) * -SLIDE_DISTANCE);

            // Preview panel dimensions
            int panelW = 140;
            int panelH = 200;
            int panelX = width - panelW - 30 + slideOffset;
            int panelY = 40;

            // Panel background (dark, semi-transparent)
            context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xAA111111);

            // Teal border
            int borderColor = (int)(cosEase * 255) << 24 | 0x00FCFC;
            context.fill(panelX, panelY, panelX + panelW, panelY + 1, borderColor);             // top
            context.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, borderColor); // bottom
            context.fill(panelX, panelY, panelX + 1, panelY + panelH, borderColor);             // left
            context.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, borderColor); // right

            // Title label
            int textAlpha = (int)(cosEase * 255);
            int labelColor = (textAlpha << 24) | 0xFFFFFF;
            context.drawCenteredTextWithShadow(textRenderer, "§b§lPreview", panelX + panelW / 2, panelY + 6, labelColor);

            // Skin info under the player model
            String skinLabel = "§7Skin: §f" + FoxyConfig.INSTANCE.skinName.get();
            String modelLabel = "§7Model: §f" + (FoxyConfig.INSTANCE.slimModel.get() ? "Slim" : "Classic");
            context.drawCenteredTextWithShadow(textRenderer, skinLabel, panelX + panelW / 2, panelY + panelH - 28, labelColor);
            context.drawCenteredTextWithShadow(textRenderer, modelLabel, panelX + panelW / 2, panelY + panelH - 16, labelColor);

            // Player entity preview (inline rendering with error handling)
            int entityCenterX = panelX + panelW / 2;
            int entityBottom = panelY + panelH - 34;
            int entityTop = panelY + 20;
            if (client.player == null) {
                try {
                    net.minecraft.client.render.entity.state.PlayerEntityRenderState state = new net.minecraft.client.render.entity.state.PlayerEntityRenderState();
                    if (skinEntrySupplier == null) {
                        skinEntrySupplier = client.getPlayerSkinCache().getSupplier(net.minecraft.component.type.ProfileComponent.ofDynamic(client.getSession().getUuidOrNull()));
                    }
                    net.minecraft.entity.player.SkinTextures baseSkin = skinEntrySupplier.get().getTextures();
                    net.minecraft.util.AssetInfo.TextureAsset skinAsset = baseSkin.body();
                    net.minecraft.entity.player.PlayerSkinType skinModel = baseSkin.model();
                    
                    String skinName = FoxyConfig.INSTANCE.skinName.get();
                    if ("Custom".equals(skinName)) {
                        net.minecraft.util.Identifier customSkinId = com.foxyclient.util.SkinResourceManager.getCustomSkinId();
                        skinAsset = new net.minecraft.util.AssetInfo.TextureAssetInfo(customSkinId, customSkinId);
                    } else if ("Alex".equals(skinName)) {
                        skinAsset = new net.minecraft.util.AssetInfo.TextureAssetInfo(net.minecraft.util.Identifier.ofVanilla("entity/player/slim/alex"));
                        skinModel = net.minecraft.entity.player.PlayerSkinType.SLIM;
                    } else if ("Steve".equals(skinName)) {
                        skinAsset = new net.minecraft.util.AssetInfo.TextureAssetInfo(net.minecraft.util.Identifier.ofVanilla("entity/player/wide/steve"));
                        skinModel = net.minecraft.entity.player.PlayerSkinType.WIDE;
                    }
                    
                    if (FoxyConfig.INSTANCE.slimModel.get() && skinModel != net.minecraft.entity.player.PlayerSkinType.SLIM) {
                        skinModel = net.minecraft.entity.player.PlayerSkinType.SLIM;
                    } else if (!FoxyConfig.INSTANCE.slimModel.get() && skinModel != net.minecraft.entity.player.PlayerSkinType.WIDE) {
                        skinModel = net.minecraft.entity.player.PlayerSkinType.WIDE;
                    }
                    
                    net.minecraft.util.AssetInfo.TextureAsset capeAsset = baseSkin.cape();
                    String capeName = FoxyConfig.INSTANCE.capeName.get();
                    if ("Custom".equals(capeName)) {
                        net.minecraft.util.Identifier customCapeId = com.foxyclient.util.SkinResourceManager.getCustomCapeId();
                        capeAsset = new net.minecraft.util.AssetInfo.TextureAssetInfo(customCapeId, customCapeId);
                    } else if ("None".equals(capeName)) {
                        capeAsset = null;
                    }

                    state.skinTextures = new net.minecraft.entity.player.SkinTextures(skinAsset, capeAsset, baseSkin.elytra(), skinModel, baseSkin.secure());
                    state.capeVisible = true;
                    state.height = 1.8f;
                    state.width = 0.6f;
                    state.entityType = net.minecraft.entity.EntityType.PLAYER;
                    if (state instanceof net.minecraft.client.render.entity.state.LivingEntityRenderState living) {
                        living.baseScale = 1.0f;
                    }
                    
                    int ex1 = entityCenterX - 55;
                    int ey1 = entityTop;
                    int ex2 = entityCenterX + 55;
                    int ey2 = entityBottom;

                    drawEntityStateWithCustomRotation(context, ex1, ey1, ex2, ey2, 55, state);
                } catch (Exception e) {
                    context.drawCenteredTextWithShadow(textRenderer, "§c[Preview Error]", panelX + panelW / 2, panelY + panelH / 2, 0xFFFF5555);
                }
            } else {
                int ex1 = entityCenterX - 55;
                int ey1 = entityTop;
                int ex2 = entityCenterX + 55;
                int ey2 = entityBottom;
                try {
                    net.minecraft.client.render.entity.EntityRenderManager entityRenderManager = client.getEntityRenderDispatcher();
                    net.minecraft.client.render.entity.EntityRenderer<? super net.minecraft.entity.LivingEntity, ?> entityRenderer = entityRenderManager.getRenderer(client.player);
                    net.minecraft.client.render.entity.state.EntityRenderState state = entityRenderer.getAndUpdateRenderState(client.player, 1.0F);
                    drawEntityStateWithCustomRotation(context, ex1, ey1, ex2, ey2, 55, state);
                } catch (Exception e) {
                    // Fallback: show error text if entity rendering fails
                    context.drawCenteredTextWithShadow(textRenderer, "§c[Preview Error]", panelX + panelW / 2, panelY + panelH / 2, 0xFFFF5555);
                }
            }
        }
    }

    private void drawEntityWithCustomRotation(DrawContext context, int x1, int y1, int x2, int y2, int size, net.minecraft.entity.LivingEntity entity) {
        net.minecraft.client.render.entity.EntityRenderManager entityRenderManager = client.getEntityRenderDispatcher();
        net.minecraft.client.render.entity.EntityRenderer<? super net.minecraft.entity.LivingEntity, ?> entityRenderer = entityRenderManager.getRenderer(entity);
        net.minecraft.client.render.entity.state.EntityRenderState state = entityRenderer.getAndUpdateRenderState(entity, 1.0F);
        drawEntityStateWithCustomRotation(context, x1, y1, x2, y2, size, state);
    }

    private void drawEntityStateWithCustomRotation(DrawContext context, int x1, int y1, int x2, int y2, int size, net.minecraft.client.render.entity.state.EntityRenderState state) {
        org.joml.Quaternionf quaternionf = (new org.joml.Quaternionf()).rotateZ(3.1415927F);
        org.joml.Quaternionf quaternionf2 = (new org.joml.Quaternionf()).rotateX(skinPitch * 0.017453292F);
        quaternionf.mul(quaternionf2);

        state.light = 15728880;
        state.shadowPieces.clear();
        state.outlineColor = 0;

        if (state instanceof net.minecraft.client.render.entity.state.LivingEntityRenderState) {
            net.minecraft.client.render.entity.state.LivingEntityRenderState livingState = (net.minecraft.client.render.entity.state.LivingEntityRenderState)state;
            livingState.bodyYaw = 180.0F + skinYaw;
            livingState.relativeHeadYaw = 0; // Head moves with body nicely when user drags
            livingState.pitch = -skinPitch;
            
            livingState.width /= livingState.baseScale;
            livingState.height /= livingState.baseScale;
            livingState.baseScale = 1.0F;
        }

        if (state instanceof net.minecraft.client.render.entity.state.PlayerEntityRenderState playerState) {
            playerState.capeVisible = true; // Fix not showing cape in preview if user hasn't explicitly enabled it in vanilla
        }

        float scale = 0.0625F;
        org.joml.Vector3f vector3f = new org.joml.Vector3f(0.0F, state.height / 2.0F + scale, 0.0F);
        context.addEntity(state, size, vector3f, quaternionf, null, x1, y1, x2, y2);
    }

    private void renderMacroList(DrawContext context, int mouseX, int mouseY, int slideOffset, int alpha) {
        CustomKeybinds ck = getCustomKeybinds();
        if (ck == null) {
            context.drawTextWithShadow(textRenderer, "§cCustomKeybinds module not found.", 120 + slideOffset, 70, 0xFFFF5555);
            return;
        }

        Map<Integer, String> macros = ck.getMacros();
        int contentX = 120 + slideOffset;
        int listY = 70;
        int col = (alpha << 24) | 0xFFFFFF;

        context.drawTextWithShadow(textRenderer, "§7Key", contentX, listY, col);
        context.drawTextWithShadow(textRenderer, "§7Command", contentX + 80, listY, col);
        listY += 14;
        context.fill(contentX, listY - 2, contentX + 250, listY - 1, (alpha << 24) | 0x444444);

        if (macros.isEmpty()) {
            context.drawTextWithShadow(textRenderer, "§8No macros defined. Add one above!", contentX, listY + 4, (alpha << 24) | 0x888888);
        } else {
            List<Map.Entry<Integer, String>> entries = new ArrayList<>(macros.entrySet());
            for (int i = macroScrollOffset; i < entries.size() && listY < height - 50; i++) {
                Map.Entry<Integer, String> entry = entries.get(i);
                String keyName = getKeyName(entry.getKey());
                String command = entry.getValue();
                if (command.length() > 30) command = command.substring(0, 27) + "...";

                boolean hover = mouseX >= contentX && mouseX <= contentX + 290 &&
                                mouseY >= listY && mouseY <= listY + 14;
                if (hover) {
                    context.fill(contentX - 2, listY - 1, contentX + 290, listY + 13, (alpha / 4 << 24) | 0xFFFFFF);
                }

                context.drawTextWithShadow(textRenderer, "§b" + keyName, contentX, listY + 2, col);
                context.drawTextWithShadow(textRenderer, "§f" + command, contentX + 80, listY + 2, col);

                int delX = contentX + 260;
                boolean delHover = mouseX >= delX && mouseX <= delX + 20 && mouseY >= listY && mouseY <= listY + 14;
                context.drawTextWithShadow(textRenderer, delHover ? "§c[X]" : "§8[X]", delX, listY + 2, col);

                listY += 16;
            }
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean bl) {
        double mx = click.x();
        double my = click.y();

        if (currentTab == Tab.COSMETICS) {
            int panelW = 140;
            int panelH = 200;
            int panelX = width - panelW - 30;
            int panelY = 40;
            if (mx >= panelX && mx <= panelX + panelW && my >= panelY && my <= panelY + panelH) {
                isDraggingSkin = true;
                return true; // Consume event
            }
        }

        if (currentTab == Tab.MACROS) {
            CustomKeybinds ck = getCustomKeybinds();
            if (ck != null) {
                long elapsed = System.currentTimeMillis() - tabSwitchTime;
                float firstEase = easeOutCubic(Math.min(1.0f, Math.max(0, elapsed) / ITEM_ANIM_DURATION_MS));
                int slideOffset = (int)((1.0f - firstEase) * -SLIDE_DISTANCE);
                int contentX = 120 + slideOffset;
                int listY = 70 + 14;

                List<Map.Entry<Integer, String>> entries = new ArrayList<>(ck.getMacros().entrySet());
                for (int i = macroScrollOffset; i < entries.size() && listY < height - 50; i++) {
                    int delX = contentX + 260;
                    if (mx >= delX && mx <= delX + 20 && my >= listY && my <= listY + 14) {
                        ck.removeMacro(entries.get(i).getKey());
                        this.clearAndInit();
                        return true;
                    }
                    listY += 16;
                }
            }
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.gui.Click click, double deltaX, double deltaY) {
        if (isDraggingSkin && currentTab == Tab.COSMETICS) {
            skinYaw -= (float)deltaX * 2.5f;
            skinPitch -= (float)deltaY * 2.5f;
            skinPitch = net.minecraft.util.math.MathHelper.clamp(skinPitch, -90f, 90f);
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.gui.Click click) {
        if (isDraggingSkin) {
            isDraggingSkin = false;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (currentTab == Tab.KEYBINDS) {
            List<Module> allModules = FoxyClient.INSTANCE.getModuleManager().getModules();
            int totalHeight = allModules.size() * 22;
            int viewHeight = height - 80;
            double maxScroll = Math.max(0, totalHeight - viewHeight);
            targetScrollPixels = Math.max(0, Math.min(maxScroll, targetScrollPixels - verticalAmount * 22 * 2.5)); // faster scroll speed
            return true;
        }
        if (currentTab == Tab.MACROS) {
            CustomKeybinds ck = getCustomKeybinds();
            if (ck != null) {
                int maxScroll = Math.max(0, ck.getMacros().size() - 10);
                macroScrollOffset = Math.max(0, Math.min(maxScroll, macroScrollOffset - (int)verticalAmount));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private CustomKeybinds getCustomKeybinds() {
        return FoxyClient.INSTANCE.getModuleManager().getModule(CustomKeybinds.class);
    }

    static String getKeyName(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN || keyCode == -1 || keyCode == 0) return "NONE";
        String name = GLFW.glfwGetKeyName(keyCode, 0);
        if (name != null) return name.toUpperCase();
        return switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "CAPS";
            case GLFW.GLFW_KEY_INSERT -> "INS";
            case GLFW.GLFW_KEY_DELETE -> "DEL";
            case GLFW.GLFW_KEY_HOME -> "HOME";
            case GLFW.GLFW_KEY_END -> "END";
            case GLFW.GLFW_KEY_PAGE_UP -> "PGUP";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PGDN";
            case GLFW.GLFW_KEY_F1 -> "F1";
            case GLFW.GLFW_KEY_F2 -> "F2";
            case GLFW.GLFW_KEY_F3 -> "F3";
            case GLFW.GLFW_KEY_F4 -> "F4";
            case GLFW.GLFW_KEY_F5 -> "F5";
            case GLFW.GLFW_KEY_F6 -> "F6";
            case GLFW.GLFW_KEY_F7 -> "F7";
            case GLFW.GLFW_KEY_F8 -> "F8";
            case GLFW.GLFW_KEY_F9 -> "F9";
            case GLFW.GLFW_KEY_F10 -> "F10";
            case GLFW.GLFW_KEY_F11 -> "F11";
            case GLFW.GLFW_KEY_F12 -> "F12";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            default -> "KEY " + keyCode;
        };
    }

    private float easeOutCubic(float t) {
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private boolean uploadCosmetics() {
        com.foxyclient.FoxyClient.LOGGER.info("[FoxyClient] Starting cosmetics sync to server...");
        try {
            String token = client.getSession().getAccessToken();
            if (token == null || token.isEmpty()) {
                com.foxyclient.FoxyClient.LOGGER.error("[FoxyClient] Sync failed: No access token found.");
                return false;
            }

            String charset = "UTF-8";
            String boundary = Long.toHexString(System.currentTimeMillis());
            String CRLF = "\r\n";

            URL url = new URL("https://foxyclient.qzz.io/api/profiles/foxyclient/");
            com.foxyclient.FoxyClient.LOGGER.info("[FoxyClient] Syncing to URL: {}", url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (
                OutputStream output = connection.getOutputStream();
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true)
            ) {
                // accesstoken
                writer.append("--" + boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"accesstoken\"").append(CRLF);
                writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
                writer.append(CRLF).append(token).append(CRLF).flush();

                // variant
                String variant = FoxyConfig.INSTANCE.slimModel.get() ? "slim" : "default";
                writer.append("--" + boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"variant\"").append(CRLF);
                writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
                writer.append(CRLF).append(variant).append(CRLF).flush();

                Path configDir = client.runDirectory.toPath().resolve("config").resolve("foxyclient");
                // skin
                String skinName = FoxyConfig.INSTANCE.skinName.get();
                if (skinName.equals("Custom") || skinName.equals("Default")) {
                    String fileName = skinName.equals("Custom") ? "custom_skin.png" : "default_skin.png";
                    Path skinFile = configDir.resolve(fileName);
                    if (Files.exists(skinFile)) {
                        com.foxyclient.FoxyClient.LOGGER.info("[FoxyClient] Syncing {} skin: {}", skinName, skinFile.getFileName());
                        writer.append("--" + boundary).append(CRLF);
                        writer.append("Content-Disposition: form-data; name=\"skin\"; filename=\"skin.png\"").append(CRLF);
                        writer.append("Content-Type: image/png").append(CRLF);
                        writer.append(CRLF).flush();
                        Files.copy(skinFile, output);
                        output.flush();
                        writer.append(CRLF).flush();
                    } else {
                        com.foxyclient.FoxyClient.LOGGER.warn("[FoxyClient] {} skin selected but file not found: {}", skinName, skinFile);
                    }
                }

                // cape
                String capeName = FoxyConfig.INSTANCE.capeName.get();
                if (capeName.equals("Custom") || capeName.equals("Default")) {
                    String fileName = capeName.equals("Custom") ? "custom_cape.png" : "default_cape.png";
                    Path capeFile = configDir.resolve(fileName);
                    if (Files.exists(capeFile)) {
                        com.foxyclient.FoxyClient.LOGGER.info("[FoxyClient] Syncing {} cape: {}", capeName, capeFile.getFileName());
                        writer.append("--" + boundary).append(CRLF);
                        writer.append("Content-Disposition: form-data; name=\"cape\"; filename=\"cape.png\"").append(CRLF);
                        writer.append("Content-Type: image/png").append(CRLF);
                        writer.append(CRLF).flush();
                        Files.copy(capeFile, output);
                        output.flush();
                        writer.append(CRLF).flush();
                    } else {
                        com.foxyclient.FoxyClient.LOGGER.warn("[FoxyClient] {} cape selected but file not found: {}", capeName, capeFile);
                    }
                }

                writer.append("--" + boundary + "--").append(CRLF).flush();
            }

            int responseCode = connection.getResponseCode();
            com.foxyclient.FoxyClient.LOGGER.info("[FoxyClient] Sync response code: {}", responseCode);
            
            if (responseCode == 200 || responseCode == 204) {
                return true;
            } else {
                // Read error stream for detailed server message
                try (InputStream es = connection.getErrorStream()) {
                    if (es != null) {
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(es, charset))) {
                            StringBuilder errorResponse = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) errorResponse.append(line);
                            
                            com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(errorResponse.toString(), com.google.gson.JsonObject.class);
                            if (json != null && json.has("error") && json.has("errorMessage")) {
                                com.foxyclient.FoxyClient.LOGGER.error("[FoxyClient] Sync failed: {} - {}", 
                                    json.get("error").getAsString(), json.get("errorMessage").getAsString());
                            } else {
                                com.foxyclient.FoxyClient.LOGGER.error("[FoxyClient] Sync failed with response: {}", errorResponse.toString());
                            }
                        }
                    } else {
                        com.foxyclient.FoxyClient.LOGGER.error("[FoxyClient] Sync failed with code {} (no error stream)", responseCode);
                    }
                } catch (Exception ex) {
                    com.foxyclient.FoxyClient.LOGGER.error("[FoxyClient] Sync failed with code {} (could not read error stream)", responseCode);
                }
                return false;
            }
        } catch (Exception e) {
            com.foxyclient.FoxyClient.LOGGER.error("[FoxyClient] Sync failed with exception", e);
            return false;
        }
    }

    private static String truncatePath(String path, int maxLen) {
        if (path.length() <= maxLen) return path;
        // Show "...\filename.ext" when the path is too long
        int lastSep = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String filename = lastSep >= 0 ? path.substring(lastSep + 1) : path;
        if (filename.length() >= maxLen - 3) return "..." + filename.substring(filename.length() - (maxLen - 3));
        return "..." + path.substring(path.length() - maxLen + 3);
    }

    @Override
    public void close() {
        FoxyConfig.INSTANCE.save();
        client.setScreen(parent);
    }
}
