package com.foxyclient.seedcracker.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ConfigScreen {

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("SeedCracker Configuration"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
        general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Active"), Config.get().active)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> Config.get().active = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Debug"), Config.get().debug)
                .setDefaultValue(false)
                .setSaveConsumer(newValue -> Config.get().debug = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Automatic Database Submits"), Config.get().databaseSubmits)
                .setDefaultValue(false)
                .setSaveConsumer(newValue -> Config.get().databaseSubmits = newValue)
                .build());

        ConfigCategory visual = builder.getOrCreateCategory(Text.literal("Visual"));
        visual.addEntry(entryBuilder.startEnumSelector(Text.literal("Render Mode"), Config.RenderType.class, Config.get().render)
                .setDefaultValue(Config.RenderType.XRAY)
                .setSaveConsumer(newValue -> Config.get().render = newValue)
                .build());

        builder.setSavingRunnable(Config::save);

        return builder.build();
    }
}
