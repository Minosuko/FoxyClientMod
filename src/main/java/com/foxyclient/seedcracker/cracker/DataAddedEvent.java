package com.foxyclient.seedcracker.cracker;

import com.foxyclient.seedcracker.cracker.storage.DataStorage;
import com.foxyclient.seedcracker.cracker.storage.TimeMachine;

@FunctionalInterface
public interface DataAddedEvent {

    DataAddedEvent POKE_PILLARS = s -> s.getTimeMachine().poke(TimeMachine.Phase.PILLARS);
    DataAddedEvent POKE_STRUCTURES = s -> s.getTimeMachine().poke(TimeMachine.Phase.STRUCTURES);
    DataAddedEvent POKE_LIFTING = s -> s.getTimeMachine().poke(TimeMachine.Phase.LIFTING);
    DataAddedEvent POKE_BIOMES = s -> s.getTimeMachine().poke(TimeMachine.Phase.BIOMES);

    void onDataAdded(DataStorage dataStorage);

}
