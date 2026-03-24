package com.foxyclient.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks left and right mouse click timestamps
 * to compute clicks-per-second (CPS).
 */
public class CPSTracker {
    public static final CPSTracker INSTANCE = new CPSTracker();

    private final List<Long> leftClicks = new ArrayList<>();
    private final List<Long> rightClicks = new ArrayList<>();

    public void recordLeft() {
        leftClicks.add(System.currentTimeMillis());
    }

    public void recordRight() {
        rightClicks.add(System.currentTimeMillis());
    }

    public int getLeftCPS() {
        return getCPS(leftClicks);
    }

    public int getRightCPS() {
        return getCPS(rightClicks);
    }

    private int getCPS(List<Long> clicks) {
        long now = System.currentTimeMillis();
        clicks.removeIf(t -> now - t > 1000);
        return clicks.size();
    }
}
