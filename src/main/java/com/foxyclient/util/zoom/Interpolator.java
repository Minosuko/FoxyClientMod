package com.foxyclient.util.zoom;

import java.util.function.DoubleUnaryOperator;

public interface Interpolator {
    double tickInterpolation(double targetInterpolation, double currentInterpolation, double tickDelta);

    default double modifyInterpolation(double interpolation) {
        return interpolation;
    }

    default double modifyPrevInterpolation(double interpolation) {
        return interpolation;
    }

    boolean isSmooth();
}

enum TransitionType implements DoubleUnaryOperator {
    INSTANT(t -> t),
    LINEAR(t -> t),
    EASE_IN_SINE(t -> 1 - Math.cos((t * Math.PI) / 2)),
    EASE_OUT_SINE(t -> Math.sin((t * Math.PI) / 2)),
    EASE_IN_OUT_SINE(t -> -(Math.cos(Math.PI * t) - 1) / 2),
    EASE_IN_QUAD(t -> t * t),
    EASE_OUT_QUAD(t -> 1 - (1 - t) * (1 - t)),
    EASE_IN_OUT_QUAD(t -> t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2),
    EASE_IN_CUBIC(t -> Math.pow(t, 3)),
    EASE_OUT_CUBIC(t -> 1 - Math.pow(1 - t, 3)),
    EASE_IN_OUT_CUBIC(t -> t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);

    private final DoubleUnaryOperator operator;

    TransitionType(DoubleUnaryOperator operator) {
        this.operator = operator;
    }

    @Override
    public double applyAsDouble(double operand) {
        return operator.applyAsDouble(operand);
    }
}
