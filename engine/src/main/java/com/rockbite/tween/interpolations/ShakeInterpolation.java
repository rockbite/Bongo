package com.rockbite.tween.interpolations;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;

public class ShakeInterpolation extends Interpolation {

    private float frequency;
    private float seed;

    /**
     * @param frequency amount of shakes till complete
     * @param seed please pass seed from 0 - 1
     */
    public ShakeInterpolation(float frequency, float seed) {
        this.frequency = frequency;
        this.seed = seed;
    }

    public float apply(float a, float seed) {
        this.seed = this.seed * seed;
        float result = apply(a);
        this.seed = this.seed / seed;

        return result;
    }

    @Override
    public float apply(float a) {
        float shake = 1f - a; // basically it shakes a lot at the beginning and then slows down to 0;
        float perlinValue = perlin(frequency, seed, a); // this is from 0 to 1, and dies of at 0.
        // we need to change it so shake value is from -1 to 1, and dues off at 0;

        float finalValue = (perlinValue - 0.5f) * 2f;

        float offset = shake * finalValue; // passing frequency as a scale of perlin map
        return offset;
    }

    /**
     *
     * @param scale
     * @param seed
     * @param time
     * @return value from 0 to 1
     */
    private float perlin(float scale, float seed, float time) {
        return MathUtils.random(0, 1f) * time; //THis is not perlin :D
    }

    public void set(float frequency) {
        seed = (float) Math.random();
        this.frequency = frequency;
    }
}
