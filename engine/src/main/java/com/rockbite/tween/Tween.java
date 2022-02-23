package com.rockbite.tween;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Pool;
import lombok.AccessLevel;
import lombok.Getter;

public class Tween<T> implements Pool.Poolable {

	@Getter(AccessLevel.PACKAGE)
	private T target;

	@Getter
	private float duration;
	private float trackTime;

	private boolean clamp = true;

	@Getter
	private TweenSystem.TweenGroup group = TweenSystem.DEFAULT;

	@Getter
	private Interpolation interpolation = Interpolation.linear;

	private TweenCompletionListener<T> completionListener;

	@Getter
	private TweenController<T> controller;

	@Getter(AccessLevel.PACKAGE)
	private Array<Tween<?>> tweensToRunAfter = new Array<>();

	private TweenData workingData = new TweenData();
	private TweenData startData = new TweenData();
	@Getter
	private TweenData targetData = new TweenData();

	@Getter
	private TweenMask<T> mask;
	private int attributesToTween;

	private TweenSystem tweenManager;

	Tween (TweenSystem tweenManager, TweenController<T> controller) {
		this.tweenManager = tweenManager;
		this.controller = controller;
	}

	public Tween<T> setGroup(TweenSystem.TweenGroup group) {
		this.group = group;
		return this;
	}

	Tween<T> objectTarget (T target) {
		this.target = target;
		if (target instanceof TweenController) {
			controller = (TweenController<T>)target;
		} else if (controller == null) {
			throw new GdxRuntimeException("Invalid state. No controller found, and object does not extend TweenController");
		}
		return this;
	}

	Tween<T> mask (TweenMask<T> mask) {
		if (mask == null) throw new GdxRuntimeException("Mask must not be null");
		this.mask = mask;
		return this;
	}

	public Tween<T> duration (float duration) {
		this.duration = duration;
		return this;
	}

	public Tween<T> interp (Interpolation interpolation) {
		this.interpolation = interpolation;
		return this;
	}

	public Tween<T> clamp (boolean clamp) {
		this.clamp = clamp;
		return this;
	}

	@SuppressWarnings("unchecked")
	public Tween<T> after (Tween<?> tween) {
		tween.getTweensToRunAfter().add(this);
		return this;
	}

	public Tween<T> completion (TweenCompletionListener<T> listener) {
		this.completionListener = listener;
		return this;
	}

	private void maskAttributeCountCheck (int suppliedAttributes) {
		if (suppliedAttributes != mask.cachedAttributes().length) throw new GdxRuntimeException("Using invalid #target method. Please use target with same amount of target attributes as supplied mask");
	}

	public Tween<T> target (float target) {
		maskAttributeCountCheck(1);

		targetData.getValue()[0] = target;
		return this;
	}

	public Tween<T> target (float target1, float target2) {
		maskAttributeCountCheck(2);

		targetData.getValue()[0] = target1;
		targetData.getValue()[1] = target2;
		return this;
	}

	public Tween<T> target (Vector3 vector3) {
		target(vector3.x, vector3.y, vector3.z);
		return this;
	}

	public Tween<T> target (float target1, float target2, float target3) {
		maskAttributeCountCheck(3);

		targetData.getValue()[0] = target1;
		targetData.getValue()[1] = target2;
		targetData.getValue()[2] = target3;
		return this;
	}

	public Tween<T> target (float target1, float target2, float target3, float target4) {
		maskAttributeCountCheck(4);

		targetData.getValue()[0] = target1;
		targetData.getValue()[1] = target2;
		targetData.getValue()[2] = target3;
		targetData.getValue()[3] = target4;
		return this;
	}

	protected void start () {
		attributesToTween = controller.getValues(target, workingData, mask.getMask());
		controller.getValues(target, startData, mask.getMask());
	}

	protected boolean update (float delta) {
		trackTime += delta;

		float percent = MathUtils.isEqual(duration, 0) ? 1f : trackTime / duration;

		for (int i = 0; i < attributesToTween; i++) {
			workingData.getValue()[i] = interpolation.apply(startData.getValue()[i], targetData.getValue()[i], percent);
		}

		controller.setValues(target, workingData, mask.getMask());

		if (trackTime >= duration) {
			trackTime = duration;

			if (clamp) {
				controller.setValues(target, targetData, mask.getMask());
			}
			if (completionListener != null) {
				completionListener.onTweenComplete(this);
			}
			return true;
		} else {
			return false;
		}
	}

	public Tween startNow () {
		tweenManager.startTweenNow(this);
		return this;
	}

	@Override
	public void reset () {
		target = null;
		duration = 0f;
		trackTime = 0f;
		interpolation = Interpolation.linear;
		completionListener = null;
		workingData.reset();
		startData.reset();
		targetData.reset();
		tweensToRunAfter.clear();
		group = TweenSystem.DEFAULT;
	}

}
