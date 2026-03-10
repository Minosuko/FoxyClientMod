package com.foxybot.api;

import com.foxybot.api.process.*;
import java.util.Optional;

public interface IFoxyBot {
    ICustomGoalProcess getCustomGoalProcess();
    IFollowProcess getFollowProcess();
    IMineProcess getMineProcess();
    IExploreProcess getExploreProcess();
    IFarmProcess getFarmProcess();
    
    // Behaviors and Managers
    IPathingBehavior getPathingBehavior();
    IPathingControlManager getPathingControlManager();
    ICommandManager getCommandManager();
}
