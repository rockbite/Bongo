package com.rockbite.tween;

import com.badlogic.gdx.utils.Pool;
import lombok.Data;

import java.util.Arrays;

@Data
public class TweenData implements Pool.Poolable  {

	private static final int BUFFER_SIZE = 5;

	private float[] value = new float[BUFFER_SIZE];

	@Override
	public void reset () {
		Arrays.fill(value, 0);
	}
}
