package com.foxybot.api;

import java.util.List;

public interface IFoxyBotProvider {
    IFoxyBot getPrimaryFoxyBot();
    List<IFoxyBot> getAllFoxyBots();
}
