package com.foxybot;

import baritone.api.IBaritone;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.command.manager.ICommandManager;
import baritone.api.pathing.calc.IPathingControlManager;
import baritone.api.process.*;
import com.foxybot.api.IFoxyBot;
import com.foxybot.api.pathing.goals.Goal;
import com.foxybot.api.pathing.goals.GoalBlock;
import com.foxybot.api.pathing.goals.GoalXZ;
import java.util.function.Predicate;
import java.util.Optional;
import net.minecraft.entity.Entity;

public class FoxyBotImpl implements IFoxyBot {
    private final IBaritone baritone;

    public FoxyBotImpl(IBaritone baritone) {
        this.baritone = baritone;
    }

    @Override
    public com.foxybot.api.process.ICustomGoalProcess getCustomGoalProcess() {
        return new CustomGoalProcessImpl(baritone.getCustomGoalProcess());
    }

    @Override
    public com.foxybot.api.process.IFollowProcess getFollowProcess() {
        return new FollowProcessImpl(baritone.getFollowProcess());
    }

    @Override
    public com.foxybot.api.process.IMineProcess getMineProcess() {
        return new MineProcessImpl(baritone.getMineProcess());
    }

    @Override
    public com.foxybot.api.process.IExploreProcess getExploreProcess() {
        return new ExploreProcessImpl(baritone.getExploreProcess());
    }

    @Override
    public com.foxybot.api.process.IFarmProcess getFarmProcess() {
        return new FarmProcessImpl(baritone.getFarmProcess());
    }

    @Override
    public com.foxybot.api.IPathingBehavior getPathingBehavior() {
        IPathingBehavior behavior = baritone.getPathingBehavior();
        return new com.foxybot.api.IPathingBehavior() {
            @Override public boolean isPathing() { return behavior.isPathing(); }
            @Override public void cancelEverything() { behavior.cancelEverything(); }
        };
    }

    @Override
    public com.foxybot.api.IPathingControlManager getPathingControlManager() {
        IPathingControlManager manager = baritone.getPathingControlManager();
        return new com.foxybot.api.IPathingControlManager() {
            @Override public Optional<Object> mostRecentInControl() { 
                return manager.mostRecentInControl().map(p -> p); 
            }
        };
    }

    @Override
    public com.foxybot.api.ICommandManager getCommandManager() {
        ICommandManager manager = baritone.getCommandManager();
        return cmd -> manager.execute(cmd);
    }

    private static class CustomGoalProcessImpl implements com.foxybot.api.process.ICustomGoalProcess {
        private final ICustomGoalProcess baritoneProcess;
        CustomGoalProcessImpl(ICustomGoalProcess p) { this.baritoneProcess = p; }
        @Override public boolean isActive() { return baritoneProcess.isActive(); }
        @Override public void setGoal(Goal goal) { baritoneProcess.setGoal(wrapGoal(goal)); }
        @Override public void setGoalAndPath(Goal goal) { baritoneProcess.setGoalAndPath(wrapGoal(goal)); }
        @Override public Goal getGoal() { return unwrapGoal(baritoneProcess.getGoal()); }
        
        private baritone.api.pathing.goals.Goal wrapGoal(Goal goal) {
            if (goal instanceof GoalBlock g) return new baritone.api.pathing.goals.GoalBlock(g.getGoalPos());
            if (goal instanceof GoalXZ g) return new baritone.api.pathing.goals.GoalXZ(g.getX(), g.getZ());
            return null;
        }
        private Goal unwrapGoal(baritone.api.pathing.goals.Goal goal) {
            if (goal instanceof baritone.api.pathing.goals.GoalBlock g) return new GoalBlock(g.getGoalPos());
            if (goal instanceof baritone.api.pathing.goals.GoalXZ g) return new GoalXZ(g.getX(), g.getZ());
            return null;
        }
    }

    private static class FollowProcessImpl implements com.foxybot.api.process.IFollowProcess {
        private final IFollowProcess baritoneProcess;
        FollowProcessImpl(IFollowProcess p) { this.baritoneProcess = p; }
        @Override public boolean isActive() { return baritoneProcess.isActive(); }
        @Override public void follow(Predicate<Entity> filter) { baritoneProcess.follow(filter); }
    }

    private static class MineProcessImpl implements com.foxybot.api.process.IMineProcess {
        private final IMineProcess baritoneProcess;
        MineProcessImpl(IMineProcess p) { this.baritoneProcess = p; }
        @Override public boolean isActive() { return baritoneProcess.isActive(); }
        @Override public void mineByName(String... names) { baritoneProcess.mineByName(names); }
        @Override public void mineByName(int quantity, String... names) { baritoneProcess.mineByName(quantity, names); }
    }

    private static class ExploreProcessImpl implements com.foxybot.api.process.IExploreProcess {
        private final IExploreProcess baritoneProcess;
        ExploreProcessImpl(IExploreProcess p) { this.baritoneProcess = p; }
        @Override public boolean isActive() { return baritoneProcess.isActive(); }
        @Override public void explore(int x, int z) { baritoneProcess.explore(x, z); }
    }

    private static class FarmProcessImpl implements com.foxybot.api.process.IFarmProcess {
        private final IFarmProcess baritoneProcess;
        FarmProcessImpl(IFarmProcess p) { this.baritoneProcess = p; }
        @Override public boolean isActive() { return baritoneProcess.isActive(); }
        @Override public void farm() { baritoneProcess.farm(); }
    }
}
