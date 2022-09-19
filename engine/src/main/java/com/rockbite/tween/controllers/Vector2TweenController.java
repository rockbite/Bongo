package com.rockbite.tween.controllers;

import com.badlogic.gdx.math.Vector2;
import com.rockbite.tween.TweenAttribute;
import com.rockbite.tween.TweenController;
import com.rockbite.tween.TweenData;
import com.rockbite.tween.TweenMask;

public class Vector2TweenController extends TweenController<Vector2> {

	public static final TweenMask<Vector2> MOVE_TO_X = new TweenMask<Vector2>() {
		@Override
		public String getDisplayString () {
			return "Move to X";
		}

		@Override
		public int getMask () {
			return 0;
		}

		@Override
		public TweenAttribute[] attributes () {
			return new TweenAttribute[] { TweenAttribute.create("Move to X:")};
		}

		@Override
		public void mapDataToTarget (Vector2 target, TweenData data) {
			target.x = data.getValue()[0];
		}

		@Override
		public void mapTargetToData (Vector2 target, TweenData data) {
			data.getValue()[0] = target.x;
		}
	};

	public static final TweenMask<Vector2> MOVE_TO_Y = new TweenMask<Vector2>() {
		@Override
		public String getDisplayString () {
			return "Move to Y";
		}

		@Override
		public int getMask () {
			return 1;
		}

		@Override
		public TweenAttribute[] attributes () {
			return new TweenAttribute[] { TweenAttribute.create("Move to Y:")};
		}

		@Override
		public void mapDataToTarget (Vector2 target, TweenData data) {
			target.y = data.getValue()[0];
		}

		@Override
		public void mapTargetToData (Vector2 target, TweenData data) {
			data.getValue()[0] = target.y;
		}
	};


	public static final TweenMask<Vector2> MOVE_TO_XY = new TweenMask<Vector2>() {
		@Override
		public String getDisplayString () {
			return "Combined move to X, Y ";
		}

		@Override
		public int getMask () {
			return 3;
		}

		@Override
		public TweenAttribute[] attributes () {
			return new TweenAttribute[] { TweenAttribute.create("Move to X:"), TweenAttribute.create("Move to Y:")};
		}

		@Override
		public void mapDataToTarget (Vector2 target, TweenData data) {
			target.set(data.getValue()[0], data.getValue()[1]);
		}

		@Override
		public void mapTargetToData (Vector2 target, TweenData data) {
			data.getValue()[0] = target.x;
			data.getValue()[1] = target.y;
		}
	};

}
