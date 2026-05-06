package com.foxyclient.util.zoom;

import net.minecraft.util.math.MathHelper;
import java.util.function.Supplier;

public class ZoomHelper {
    private final Interpolator initialInterpolator;
    private final Interpolator scrollInterpolator;

    private final Supplier<Integer> initialZoom;
    private final Supplier<Integer> zoomPerStep;
    private final Supplier<Integer> maxScrollTiers;

    private double prevInitialInterpolation = 0.0;
    private double initialInterpolation = 0.0;
    private boolean zoomingLastTick = false;

    private double prevScrollInterpolation = 0.0;
    private double scrollInterpolation = 0.0;
    private int lastScrollTier = 0;

    private boolean resetting = false;
    private double resetMultiplier = 0.0;

    public ZoomHelper(Interpolator initialInterpolator, Interpolator scrollInterpolator, 
                      Supplier<Integer> initialZoom, Supplier<Integer> zoomPerStep, Supplier<Integer> maxScrollTiers) {
        this.initialInterpolator = initialInterpolator;
        this.scrollInterpolator = scrollInterpolator;
        this.initialZoom = initialZoom;
        this.zoomPerStep = zoomPerStep;
        this.maxScrollTiers = maxScrollTiers;
    }

    public void tick(boolean zooming, int scrollTiers, double lastFrameDuration) {
        tickInitial(zooming, lastFrameDuration);
        tickScroll(scrollTiers, lastFrameDuration);
    }

    private void tickInitial(boolean zooming, double lastFrameDuration) {
        if (zooming && !zoomingLastTick) resetting = false;

        double targetZoom = zooming ? 1.0 : 0.0;
        prevInitialInterpolation = initialInterpolation;
        initialInterpolation = initialInterpolator.tickInterpolation(targetZoom, initialInterpolation, lastFrameDuration);
        prevInitialInterpolation = initialInterpolator.modifyPrevInterpolation(prevInitialInterpolation);
        
        if (!initialInterpolator.isSmooth()) prevInitialInterpolation = initialInterpolation;
        zoomingLastTick = zooming;
    }

    private void tickScroll(int scrollTiers, double lastFrameDuration) {
        if (scrollTiers > lastScrollTier) resetting = false;

        double targetZoom = maxScrollTiers.get() > 0 ? (double) scrollTiers / maxScrollTiers.get() : 0.0;

        prevScrollInterpolation = scrollInterpolation;
        scrollInterpolation = scrollInterpolator.tickInterpolation(targetZoom, scrollInterpolation, lastFrameDuration);
        prevScrollInterpolation = scrollInterpolator.modifyPrevInterpolation(prevScrollInterpolation);
        
        if (!scrollInterpolator.isSmooth()) prevScrollInterpolation = scrollInterpolation;
        lastScrollTier = scrollTiers;
    }

    public double getZoomDivisor(float tickDelta) {
        double initialMultiplier = getInitialZoomMultiplier(tickDelta);
        double baseDivisor = 1.0 / initialMultiplier;

        double scrollT = resetting ? 0.0 : (scrollInterpolator.isSmooth() ? 
                scrollInterpolator.modifyInterpolation(MathHelper.lerp(tickDelta, prevScrollInterpolation, scrollInterpolation)) : 
                scrollInterpolation);

        double stepMultiplier = zoomPerStep.get() / 100.0;
        int maxSteps = maxScrollTiers.get();
        double currentStep = scrollT * maxSteps;

        double rawDivisor = baseDivisor * Math.pow(stepMultiplier, currentStep);
        double finalDivisor = MathHelper.clamp(rawDivisor, 0.5, 500.0);

        if (initialInterpolation == 0.0 && scrollInterpolation == 0.0) resetting = false;
        if (!resetting) resetMultiplier = 1.0 / finalDivisor;

        return finalDivisor;
    }

    private double getInitialZoomMultiplier(float tickDelta) {
        double interpolation = initialInterpolator.isSmooth() ? 
                initialInterpolator.modifyInterpolation(MathHelper.lerp(tickDelta, prevInitialInterpolation, initialInterpolation)) : 
                initialInterpolation;
        
        return MathHelper.lerp(interpolation, 1.0, resetting ? resetMultiplier : 1.0 / initialZoom.get());
    }

    public void reset() {
        if (!resetting && scrollInterpolation > 0.0) {
            resetting = true;
            scrollInterpolation = 0.0;
            prevScrollInterpolation = 0.0;
        }
    }
}
