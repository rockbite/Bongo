package com.rockbite.bongo.engine.gltf.scene.animation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.util.Map;
import java.util.TreeMap;

public abstract class SceneAnimationSampler<T> {

	private static final int LINEAR = 0, STEP = 1, CUBICSPLINE = 2;


	//"translation"	"VEC3"	5126 (FLOAT)	XYZ translation vector
	//"rotation"	"VEC4"	5126 (FLOAT)
	//5120 (BYTE) normalized
	//5121 (UNSIGNED_BYTE) normalized
	//5122 (SHORT) normalized
	//5123 (UNSIGNED_SHORT) normalized	XYZW rotation quaternion
	//"scale"	"VEC3"	5126 (FLOAT)	XYZ scale vector
	//"weights"	"SCALAR"	5126 (FLOAT)
	//5120 (BYTE) normalized
	//5121 (UNSIGNED_BYTE) normalized
	//5122 (SHORT) normalized
	//5123 (UNSIGNED_SHORT) normalized	Weights of
	int INTERPOLATION_MODE;
	private TreeMap<Float, T> data = new TreeMap<>();

	float maxInputTime;

	public SceneAnimationSampler (float[] inputData, T[] outputData, int interpolation) {
		INTERPOLATION_MODE = interpolation;

		maxInputTime = inputData[inputData.length - 1];

		for (int index = 0; index < outputData.length; index++) {
			data.put(inputData[index], outputData[index++]);
		}
	}

	protected abstract T interpolate (T floor, T ceil, float alpha);

	public static SceneAnimationSampler create (float[] inputs, float[] outputs, String path, String interpolation) {
		if (path.equalsIgnoreCase("translation") || path.equalsIgnoreCase("scale")) {
			Vector3[] vecs = new Vector3[outputs.length/3];
			int idx = 0;
			for (int i = 0; i < outputs.length; i+=3) {
				Vector3 vector3 = new Vector3(outputs[i], outputs[i + 1], outputs[i + 2]);
				vecs[idx++] = vector3;
			}
			if (path.equalsIgnoreCase("scale")) {
				return new ScaleSampler(inputs, vecs, parseInterpolation(interpolation));
			} else {
				return new TranslationSampler(inputs, vecs, parseInterpolation(interpolation));
			}
		} else if (path.equalsIgnoreCase("rotation")) {
			Quaternion[] quaternions = new Quaternion[outputs.length/4];
			int idx = 0;
			for (int i = 0; i < outputs.length; i+=4) {
				Quaternion quaternion = new Quaternion(outputs[i], outputs[i + 1], outputs[i + 2], outputs[i + 3]);
				quaternions[idx++] = quaternion;
			}
			return new RotationSampler(inputs, quaternions, parseInterpolation(interpolation));
		} else if (path.equalsIgnoreCase("weights")) {
			Float[] data = new Float[outputs.length];
			int idx = 0;
			for (float output : outputs) {
				data[idx++] = output;
			}
			return new WeightsSampler(inputs, data, parseInterpolation(interpolation));
		} else {
			throw new GdxRuntimeException("No valid interpolation found for " + path);
		}
	}

	private static int parseInterpolation (String interpolation) {

		if (interpolation.equalsIgnoreCase("LINEAR")) {
			return LINEAR;
		} else if (interpolation.equalsIgnoreCase("STEP")) {
			return STEP;
		} else if (interpolation.equalsIgnoreCase("CUBICSPLINE")) {
			return CUBICSPLINE;
		}

		throw new GdxRuntimeException("Invalid interpolation " + interpolation);
	}

	public T getInterpolatedValueForTime (float time) {
		Map.Entry<Float, T> floor = data.floorEntry(time);
		Map.Entry<Float, T> ceil = data.ceilingEntry(time);

		if (floor == null) {
			floor = data.firstEntry();
		}
		if (ceil == null) {
			ceil = data.lastEntry();
		}

		final Float floorFloat = floor.getKey();
		final Float ceilFloat = ceil.getKey();


		float interval = ceilFloat - floorFloat;
		float alpha = floorFloat;
		if (MathUtils.isZero(interval)) {
			alpha = 1f;
		} else {
			time -= alpha;
			time /= interval;
			alpha = time;
		}

		final T interpolate = interpolate(floor.getValue(), ceil.getValue(), alpha);
		return interpolate;
	}


	public static class TranslationSampler extends SceneAnimationSampler<Vector3> {

		private Vector3 temp = new Vector3();

		public TranslationSampler (float[] inputData, Vector3[] outputData, int interpolation) {
			super(inputData, outputData, interpolation);
		}

		@Override
		protected Vector3 interpolate (Vector3 floor, Vector3 ceil, float alpha) {

			if (INTERPOLATION_MODE == LINEAR) {
				temp.set(floor);
				temp.lerp(ceil, alpha);
				return temp;
			} else if (INTERPOLATION_MODE == STEP) {
				return floor;
			} else if (INTERPOLATION_MODE == CUBICSPLINE) {
				temp.set(floor);
				temp.lerp(ceil, alpha);
				return temp;
			}

			throw new GdxRuntimeException("Invalid interpolation");

		}
	}

	public static class RotationSampler extends SceneAnimationSampler<Quaternion> {

		private Quaternion temp = new Quaternion();

		public RotationSampler (float[] inputData, Quaternion[] outputData, int interpolation) {
			super(inputData, outputData, interpolation);
		}

		@Override
		protected Quaternion interpolate (Quaternion floor, Quaternion ceil, float alpha) {

			if (INTERPOLATION_MODE == LINEAR) {
				temp.set(floor);
				temp.slerp(ceil, alpha);
				return temp;
			} else if (INTERPOLATION_MODE == STEP) {
				return floor;
			} else if (INTERPOLATION_MODE == CUBICSPLINE) {
				temp.set(floor);
				temp.slerp(ceil, alpha);
				return temp;
			}

			throw new GdxRuntimeException("Invalid interpolation");
		}
	}

	public static class ScaleSampler extends SceneAnimationSampler<Vector3> {

		private Vector3 temp = new Vector3();

		public ScaleSampler (float[] inputData, Vector3[] outputData, int interpolation) {
			super(inputData, outputData, interpolation);
		}

		@Override
		protected Vector3 interpolate (Vector3 floor, Vector3 ceil, float alpha) {

			if (INTERPOLATION_MODE == LINEAR) {
				temp.set(floor);
				temp.lerp(ceil, alpha);
				return temp;
			} else if (INTERPOLATION_MODE == STEP) {
				return floor;
			} else if (INTERPOLATION_MODE == CUBICSPLINE) {
				temp.set(floor);
				temp.lerp(ceil, alpha);
				return temp;
			}

			throw new GdxRuntimeException("Invalid interpolation");
		}
	}

	public static class WeightsSampler extends SceneAnimationSampler<Float> {

		public WeightsSampler (float[] inputData, Float[] outputData, int interpolation) {
			super(inputData, outputData, interpolation);
		}

		@Override
		protected Float interpolate (Float floor, Float ceil, float alpha) {

			if (INTERPOLATION_MODE == LINEAR) {
				return Interpolation.linear.apply(floor, ceil, alpha);
			} else if (INTERPOLATION_MODE == STEP) {
				return floor;
			} else if (INTERPOLATION_MODE == CUBICSPLINE) {
				return Interpolation.linear.apply(floor, ceil, alpha);

			}

			throw new GdxRuntimeException("Invalid interpolation");
		}
	}

}
