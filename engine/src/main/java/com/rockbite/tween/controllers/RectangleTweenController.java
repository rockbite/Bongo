package com.rockbite.tween.controllers;

import com.badlogic.gdx.math.Rectangle;
import com.rockbite.tween.TweenAttribute;
import com.rockbite.tween.TweenController;
import com.rockbite.tween.TweenData;
import com.rockbite.tween.TweenMask;

public class RectangleTweenController extends TweenController<Rectangle> {

	public static final TweenMask<Rectangle> MOVE_TO = new TweenMask<Rectangle>() {
		@Override
		public String getDisplayString () {
			return "Combined move rectangle to X:Y:W:H";
		}

		@Override
		public int getMask () {
			return 0;
		}

		@Override
		public TweenAttribute[] attributes () {
			return new TweenAttribute[] { TweenAttribute.create("Move to X:"), TweenAttribute.create("Move to Y:"), TweenAttribute.create("Move to W:"), TweenAttribute.create("Move to H:")};
		}

		@Override
		public void mapDataToTarget (Rectangle target, TweenData data) {
			target.set(data.getValue()[0], data.getValue()[1], data.getValue()[2], data.getValue()[3]);
		}

		@Override
		public void mapTargetToData (Rectangle target, TweenData data) {
			data.getValue()[0] = target.getX();
			data.getValue()[1] = target.getY();
			data.getValue()[2] = target.getWidth();
			data.getValue()[3] = target.getHeight();
		}
	};

}
