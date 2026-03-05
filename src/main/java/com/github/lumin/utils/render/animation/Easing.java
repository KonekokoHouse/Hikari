package com.github.lumin.utils.render.animation;

import net.minecraft.util.Mth;

import java.util.function.Function;

public enum Easing {

    LINEAR(x -> x),
    DECELERATE(x -> 1 - ((x - 1) * (x - 1))),
    SMOOTH_STEP(x -> (float) (-2 * Math.pow(x, 3) + (3 * Math.pow(x, 2)))),
    EASE_IN_QUAD(x -> x * x),
    EASE_OUT_QUAD(x -> x * (2 - x)),
    EASE_IN_OUT_QUAD(x -> x < 0.5 ? 2 * x * x : -1 + (4 - 2 * x) * x),
    EASE_IN_CUBIC(x -> x * x * x),
    EASE_OUT_CUBIC(x -> (--x) * x * x + 1),
    EASE_IN_OUT_CUBIC(x -> x < 0.5 ? 4 * x * x * x : (x - 1) * (2 * x - 2) * (2 * x - 2) + 1),
    EASE_IN_QUART(x -> x * x * x * x),
    EASE_OUT_QUART(x -> 1 - (--x) * x * x * x),
    EASE_IN_OUT_QUART(x -> x < 0.5 ? 8 * x * x * x * x : 1 - 8 * (--x) * x * x * x),
    EASE_IN_QUINT(x -> x * x * x * x * x),
    EASE_OUT_QUINT(x -> 1 + (--x) * x * x * x * x),
    EASE_IN_OUT_QUINT(x -> x < 0.5 ? 16 * x * x * x * x * x : 1 + 16 * (--x) * x * x * x * x),
    EASE_IN_SINE(x -> 1 - Mth.cos((float) (x * Math.PI * 0.5D))),
    EASE_OUT_SINE(x -> Mth.sin((float) (x * Math.PI * 0.5D))),
    EASE_IN_OUT_SINE(x -> 1 - Mth.cos((float) (Math.PI * x * 0.5D))),
    EASE_IN_EXPO(x -> x == 0 ? 0 : (float) Math.pow(2, 10 * x - 10)),
    EASE_OUT_EXPO(x -> x == 1 ? 1 : 1 - (float) Math.pow(2, -10 * x)),
    EASE_IN_OUT_EXPO(x -> x == 0 ? 0 : x == 1 ? 1 : x < 0.5 ? (float) Math.pow(2, 20 * x - 10) * 0.5F : (2 - (float) Math.pow(2, -20 * x + 10)) * 0.5F),
    EASE_IN_CIRC(x -> 1 - (float) Math.sqrt(1 - x * x)),
    EASE_OUT_CIRC(x -> (float) Math.sqrt(1 - (--x) * x)),
    EASE_IN_OUT_CIRC(x -> x < 0.5 ? (1 - (float) Math.sqrt(1 - 4 * x * x)) * 0.5F : ((float) Math.sqrt(1 - 4 * (x - 1) * x) + 1) * 0.5F),
    SIGMOID(x -> 1 / (1 + (float) Math.exp(-x))),
    EASE_OUT_ELASTIC(x -> x == 0 ? 0 : x == 1 ? 1 : (float) (Math.pow(2, -10 * x) * Math.sin((x * 10 - 0.75) * ((2 * Math.PI) / 3)) * 0.5F + 1)),
    EASE_IN_BACK(x -> (1.70158F + 1.0F) * x * x * x - 1.70158F * x * x),
    DYNAMIC_ISLAND(x -> {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }),
    DYNAMIC_ISLAND_SMOOTH(x -> {
        float t = x;
        float t2 = t * t;
        float t3 = t2 * t;
        return 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t) * (1.0f - t) * (1.0f - t);
    }),
    APPLE_SPRING(x -> {
        float stiffness = 180.0f;
        float damping = 12.0f;
        float velocity = 0.0f;
        float dt = x;
        float decay = (float) Math.exp(-damping * dt);
        float oscillation = (float) Math.cos(stiffness * dt);
        return 1.0f - decay * (1.0f - x);
    }),
    ISLAND_IN(x -> {
        float c = 2.5949095f;
        float t = x;
        return (c + 1.0f) * t * t * t - c * t * t;
    }),
    ISLAND_OUT(x -> {
        float c = 2.5949095f;
        float t = x - 1.0f;
        return 1.0f + (c + 1.0f) * t * t * t + c * t * t;
    }),
    ISLAND_IN_OUT(x -> {
        float c = 2.5949095f;
        float t = x;
        if (t < 0.5f) {
            return ((c + 1.0f) * (2.0f * t) * (2.0f * t) * (2.0f * t) - c * (2.0f * t) * (2.0f * t)) * 0.5f;
        } else {
            float t2 = 2.0f * t - 2.0f;
            return ((c + 1.0f) * t2 * t2 * t2 + c * t2 * t2) * 0.5f + 1.0f;
        }
    }),
    SILK(x -> {
        return x < 0.5f
                ? 4.0f * x * x * x * x * x
                : 1.0f - (float) Math.pow(-2.0f * x + 2.0f, 5) / 2.0f;
    });

    private final Function<Float, Float> function;

    Easing(final Function<Float, Float> function) {
        this.function = function;
    }

    public Function<Float, Float> getFunction() {
        return function;
    }

}