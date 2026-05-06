package com.foxyclient.util.zoom;

import java.util.function.Supplier;

public class SmoothInterpolator extends LinearInterpolator {
    private final Supplier<Double> smoothness;

    public SmoothInterpolator(Supplier<Double> smoothness) {
        this.smoothness = smoothness;
    }

    @Override
    protected double getTimeIncrement(boolean zoomingOut, double tickDelta, double targetInterpolation, double currentInterpolation) {
        double diff = !zoomingOut ? targetInterpolation - currentInterpolation : currentInterpolation - targetInterpolation;
        return diff * smoothness.get() / 0.05 * tickDelta;
    }

    @Override
    public double tickInterpolation(double targetInterpolation, double currentInterpolation, double tickDelta) {
        if (!isSmooth()) return targetInterpolation;
        return super.tickInterpolation(targetInterpolation, currentInterpolation, tickDelta);
    }

    @Override
    public boolean isSmooth() {
        return smoothness.get() != 1.0;
    }
}
