package com.foxyclient.module.misc;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Formatting;

/**
 * Teams - Team detection and team-based targeting filtering.
 * Detects whether players are on the same team using scoreboard teams
 * and/or name color matching. Provides utility methods for other modules
 * (like KillAura) to check team membership before targeting.
 */
public class Teams extends Module {
    private final BoolSetting colorBased = addSetting(new BoolSetting("ColorBased", "Detect teams by name color", true));
    private final BoolSetting scoreboardBased = addSetting(new BoolSetting("Scoreboard", "Detect by scoreboard teams", true));
    private final BoolSetting prefixBased = addSetting(new BoolSetting("Prefix", "Detect by team prefix", true));
    private final ModeSetting action = addSetting(new ModeSetting("Action", "What to do with teammates",
        "DontAttack", "DontAttack", "Highlight", "Both"));

    public Teams() {
        super("Teams", "Team detection and filtering", Category.MISC);
    }

    /**
     * Checks if the given player is on the same team as the local player.
     * Used by combat modules to decide whether to target this player.
     */
    public boolean isOnSameTeam(PlayerEntity player) {
        if (!isEnabled() || mc.player == null) return false;

        // Scoreboard team check
        if (scoreboardBased.get()) {
            Scoreboard scoreboard = mc.world.getScoreboard();
            Team myTeam = scoreboard.getScoreHolderTeam(mc.player.getName().getString());
            Team theirTeam = scoreboard.getScoreHolderTeam(player.getName().getString());

            if (myTeam != null && myTeam == theirTeam) {
                return true;
            }
        }

        // Color-based check (same display name color = same team)
        if (colorBased.get()) {
            Formatting myColor = getPlayerColor(mc.player);
            Formatting theirColor = getPlayerColor(player);

            if (myColor != null && myColor == theirColor && myColor != Formatting.WHITE) {
                return true;
            }
        }

        // Prefix-based check (same team prefix in tab list)
        if (prefixBased.get()) {
            String myPrefix = getTeamPrefix(mc.player);
            String theirPrefix = getTeamPrefix(player);

            if (myPrefix != null && !myPrefix.isEmpty() && myPrefix.equals(theirPrefix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Whether the module says to not attack teammates.
     */
    public boolean shouldNotAttack() {
        return isEnabled() && (action.is("DontAttack") || action.is("Both"));
    }

    /**
     * Whether the module says to highlight teammates.
     */
    public boolean shouldHighlight() {
        return isEnabled() && (action.is("Highlight") || action.is("Both"));
    }

    private Formatting getPlayerColor(PlayerEntity player) {
        Scoreboard scoreboard = mc.world.getScoreboard();
        Team team = scoreboard.getScoreHolderTeam(player.getName().getString());
        if (team != null && team.getColor() != Formatting.RESET) {
            return team.getColor();
        }
        // Fallback: check display name styling
        if (player.getDisplayName() != null && player.getDisplayName().getStyle().getColor() != null) {
            String colorName = player.getDisplayName().getStyle().getColor().getName();
            try {
                return Formatting.byName(colorName);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String getTeamPrefix(PlayerEntity player) {
        Scoreboard scoreboard = mc.world.getScoreboard();
        Team team = scoreboard.getScoreHolderTeam(player.getName().getString());
        if (team != null) {
            return team.getPrefix().getString();
        }
        return null;
    }
}
