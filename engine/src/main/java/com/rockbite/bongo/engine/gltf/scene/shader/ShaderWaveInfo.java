package com.rockbite.bongo.engine.gltf.scene.shader;

import com.badlogic.gdx.math.Vector2;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class ShaderWaveInfo {

	public float[] direction;
	public Vector2 normedDirection = new Vector2();
	public float[] amplitude;
	public float[] steepness;
	public float[] frequency;
	public float[] speed;

	public ShaderWaveInfo (float[] direction, float amplitude, float steepness, float frequency, float speed) {
		this.direction = direction;
		this.amplitude = new float[]{amplitude};
		this.steepness = new float[]{steepness};
		this.frequency = new float[]{frequency};
		this.speed = new float[]{speed};
	}

}
