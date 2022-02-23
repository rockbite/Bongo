package com.rockbite.tween;

public abstract class TweenMask<T> {

	private TweenAttribute[] cachedAttributes;

	public abstract String getDisplayString ();
	public abstract int getMask ();
	protected abstract TweenAttribute[] attributes ();

	public TweenAttribute[] cachedAttributes () {
		if (cachedAttributes == null) {
			cachedAttributes = attributes();
		}
		return cachedAttributes;
	}

	public abstract void mapDataToTarget (T target, TweenData data);
	public abstract void mapTargetToData (T target, TweenData data);

	@Override
	public String toString () {
		return getDisplayString();
	}
}
