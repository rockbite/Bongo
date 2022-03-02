package com.rockbite.bongo.engine.gltf.scene.shader;

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.NumberUtils;

public class PBRFloatAttribute extends Attribute {

	public static final String RoughnessAlias = "roughness";
	public static final long Roughness = register(RoughnessAlias);

	public static final String MetallicAlias = "metallic";
	public static final long Metallic = register(MetallicAlias);

	public static final String NormalMaterialScaleAlias = "normalMaterialScale";
	public static final long NormalMaterialScale = register(NormalMaterialScaleAlias);


	public static final String OcclusionStrengthAlias = "occlusionStrengthAlias";
	public static final long OcclusionStrength = register(OcclusionStrengthAlias);


	public static PBRFloatAttribute createRoughness (float value) {
		return new PBRFloatAttribute(Roughness, value);
	}

	public static PBRFloatAttribute createMetallic (float value) {
		return new PBRFloatAttribute(Metallic, value);
	}

	public static PBRFloatAttribute createNormalTextureScale (float value) {
		return new PBRFloatAttribute(NormalMaterialScale, value);
	}

	public static PBRFloatAttribute createOcclusionStrength (float value) {
		return new PBRFloatAttribute(OcclusionStrength, value);
	}

	public float[] value = {0};

	public PBRFloatAttribute (long type) {
		super(type);
	}

	public PBRFloatAttribute (long type, float value) {
		super(type);
		this.value[0] = value;
	}



	@Override
	public Attribute copy () {
		return new PBRFloatAttribute(type, value[0]);
	}

	@Override
	public int hashCode () {
		int result = super.hashCode();
		result = 977 * result + NumberUtils.floatToRawIntBits(value[0]);
		return result;
	}

	@Override
	public int compareTo (Attribute o) {
		if (type != o.type) return (int)(type - o.type);
		final float v = ((PBRFloatAttribute)o).value[0];
		return MathUtils.isEqual(value[0], v) ? 0 : value[0] < v ? -1 : 1;
	}
}
