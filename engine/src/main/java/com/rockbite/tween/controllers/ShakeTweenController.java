package com.rockbite.tween.controllers;

import com.rockbite.tween.TweenAttribute;
import com.rockbite.tween.TweenController;
import com.rockbite.tween.TweenData;
import com.rockbite.tween.TweenMask;
import com.rockbite.tween.interpolations.ShakeInterpolation;
import lombok.Data;

public class ShakeTweenController extends TweenController<ShakeTweenController.ShakeConfiguration> {

    @Data
    public static class ShakeConfiguration {

        public ShakeConfiguration() {

        }

        private float xOffset;
        private float yOffset;
        private float sizeOffset;
        private float rotationOffset;

        public void reset() {
            xOffset = 0;
            yOffset = 0;
            sizeOffset = 0;
            rotationOffset = 0;
        }
    }

    public static final ShakeTweenMask DEFAULT_SHAKE = new ShakeTweenMask();


    /**
     * MUST BE CALLED WITH LINEAR INTERPOLATION, but I mean if you are feeling adventurous be my guest.
     */
    public static class ShakeTweenMask extends TweenMask<ShakeConfiguration> {

        ShakeInterpolation shakeInterpolation;


        public ShakeTweenMask() {
            shakeInterpolation = new ShakeInterpolation(10f, (float) Math.random());
        }

        public ShakeTweenMask setConfig(float frequency) {
            shakeInterpolation.set(frequency);

            return this;
        }

        @Override
        public String getDisplayString () {
            return "Shake";
        }

        @Override
        public int getMask () {
            return 0;
        }

        @Override
        public TweenAttribute[] attributes () {
            return new TweenAttribute[] { TweenAttribute.create("X:"), TweenAttribute.create("Y:"), TweenAttribute.create("SIZE:"), TweenAttribute.create("ROTATION:")};
        }

        @Override
        public void mapDataToTarget (ShakeConfiguration target, TweenData data) {
            target.xOffset = shakeInterpolation.apply(data.getValue()[0], 1f);
            target.yOffset = shakeInterpolation.apply(data.getValue()[1], -1f);
            target.sizeOffset = shakeInterpolation.apply(data.getValue()[2], 3f);
            target.rotationOffset = shakeInterpolation.apply(data.getValue()[3], 1.5f);
        }

        @Override
        public void mapTargetToData (ShakeConfiguration target, TweenData data) {
            data.getValue()[0] = 0;
            data.getValue()[1] = 0;
            data.getValue()[2] = 0;
            data.getValue()[3] = 0;
        }
    }
}
