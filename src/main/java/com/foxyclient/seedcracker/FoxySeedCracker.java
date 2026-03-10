package com.foxyclient.seedcracker;

import com.foxyclient.seedcracker.api.SeedCrackerAPI;
import com.foxyclient.seedcracker.config.Config;
import com.foxyclient.seedcracker.cracker.storage.DataStorage;
import com.foxyclient.seedcracker.util.Database;
import com.foxyclient.seedcracker.util.Log;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FoxySeedCracker implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("seedcracker");
    private static FoxySeedCracker INSTANCE;

    private final DataStorage dataStorage = new DataStorage();
    private SeedCrackerAPI api = seed -> {}; // Default no-op API

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        Config.load();
        Features.init(Config.get().getVersion());
        
        // Initialize search positions for all structure finders
        com.foxyclient.seedcracker.finder.ReloadFinders.reloadHeight(-64, 320);
        
        if (Config.get().databaseSubmits) {
            Database.fetchSeeds();
        }

        LOGGER.info("FoxySeedCracker initialized.");
    }

    public static FoxySeedCracker get() {
        return INSTANCE;
    }

    public DataStorage getDataStorage() {
        return this.dataStorage;
    }

    public SeedCrackerAPI getApi() {
        return api;
    }

    public void registerApi(SeedCrackerAPI api) {
        this.api = api;
    }

    public void reset() {
        this.dataStorage.clear();
        Log.warn("data.clearData");
    }
}
