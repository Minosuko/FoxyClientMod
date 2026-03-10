package com.foxybot;

import baritone.api.BaritoneAPI;
import com.foxybot.api.IFoxyBot;
import com.foxybot.api.IFoxyBotProvider;
import java.util.List;
import java.util.stream.Collectors;

public class FoxyBotProviderImpl implements IFoxyBotProvider {
    @Override
    public IFoxyBot getPrimaryFoxyBot() {
        return new FoxyBotImpl(BaritoneAPI.getProvider().getPrimaryBaritone());
    }

    @Override
    public List<IFoxyBot> getAllFoxyBots() {
        return BaritoneAPI.getProvider().getAllBaritones().stream()
            .map(FoxyBotImpl::new)
            .collect(Collectors.toList());
    }
}
