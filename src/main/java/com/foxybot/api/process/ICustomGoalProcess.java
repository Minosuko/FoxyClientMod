package com.foxybot.api.process;

import com.foxybot.api.pathing.goals.Goal;

public interface ICustomGoalProcess extends IBaritoneProcess {
    void setGoal(Goal goal);
    void setGoalAndPath(Goal goal);
    Goal getGoal();
}
