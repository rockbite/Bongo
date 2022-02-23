package com.rockbite.tween;

import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntMap;

public abstract class TweenController<T> {

	protected IntMap<TweenMask<T>> masks = new IntMap<>();

	void setValues (T target, TweenData tweenData, int mask) {
		TweenMask<T> tweenMask = masks.get(mask);
		if (tweenMask == null) {
			throw new GdxRuntimeException("No tween mask found for Controller: " + getClass() + " with mask: " + mask);
		}

		tweenMask.mapDataToTarget(target, tweenData);
	}

	int getValues (T target, TweenData tweenData, int mask) {
		TweenMask<T> tweenMask = masks.get(mask);
		if (tweenMask == null) {
			throw new GdxRuntimeException("No tween mask found for Controller: " + getClass() + " with mask: " + mask);
		}

		tweenMask.mapTargetToData(target, tweenData);
		return tweenMask.cachedAttributes().length;
	}

	@Override
	public String toString () {
		return getClass().getSimpleName();
	}
}
