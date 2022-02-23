package com.rockbite.bongo.engine.gltf.scene.shader;

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.utils.NumberUtils;
import lombok.Getter;

public class WaterAttribute extends Attribute {

	public final static String WaterAlias = "water";
	public final static long Water = register(WaterAlias);

	public final static WaterAttribute createWaterAttribute (ShaderWaveInfo[] shaderWaveInfos) {
		return new WaterAttribute(Water, shaderWaveInfos);
	}

	@Getter
	private ShaderWaveInfo[] waves;

	@Getter
	private float[] foamFalloff = new float[]{3};

	@Getter
	private float[] foamScrolling = new float[]{-0.988f};
	@Getter
	private float[] foamScrolling2 = new float[]{-1.578f};

	public WaterAttribute (final long type, ShaderWaveInfo[] shaderWaveInfos) {
		super(type);
		this.waves = shaderWaveInfos;
	}


	public WaterAttribute (final WaterAttribute copyFrom) {
		this(copyFrom.type, copyFrom.waves);
	}

	@Override
	public Attribute copy () {
		return new WaterAttribute(this);
	}

	@Override
	public int hashCode () {
		int result = super.hashCode();
		result = 953 * result + waves.hashCode();
		return result;
	}

	@Override
	public int compareTo (Attribute o) {
		if (type != o.type) return (int)(type - o.type);
		return 0;
	}
}
