package com.rockbite.bongo.engine.gltf.scene.shader;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;

public class PBRMaterialAttribute extends Attribute {

	public final static String BaseColourTextureAlias = "baseColourTexture";
	public final static long BaseColourTexture = register(BaseColourTextureAlias);

	public final static String EmissiveTextureAlias = "emissiveTexture";
	public final static long EmissiveTexture = register(EmissiveTextureAlias);

	public final static String OcclusionTextureAlias = "occlusionTexture";
	public final static long OcclusionTexture = register(OcclusionTextureAlias);

	public final static String NormalTextureAlias = "normalTexture";
	public final static long NormalTexture = register(NormalTextureAlias);


	public final static String MetalRoughnessTextureAlias = "metalRoughnessTexture";
	public final static long MetalRoughnessTexture = register(MetalRoughnessTextureAlias);


	protected static long Mask = BaseColourTexture | EmissiveTexture | OcclusionTexture | NormalTexture | MetalRoughnessTexture;

	public final TextureDescriptor<Texture> textureDescriptor;
	public final int texCoordIndex;

	public PBRMaterialAttribute (long type, int texCoordIndex) {
		super(type);
		textureDescriptor = new TextureDescriptor<Texture>();
		this.texCoordIndex = texCoordIndex;
	}

	public PBRMaterialAttribute (long type, int texCoordIndex, Texture texture) {
		this(type, texCoordIndex);
		textureDescriptor.texture = texture;
	}

	public PBRMaterialAttribute (PBRMaterialAttribute pbrMaterialAttribute) {
		this(pbrMaterialAttribute.type, pbrMaterialAttribute.texCoordIndex);
	}

	public static PBRMaterialAttribute createBaseColourTexture (Texture texture, int texCoordIndex) {
		return new PBRMaterialAttribute(BaseColourTexture, texCoordIndex, texture);
	}

	public static PBRMaterialAttribute createEmissiveTexture (Texture texture, int texCoordIndex) {
		return new PBRMaterialAttribute(EmissiveTexture, texCoordIndex, texture);
	}

	public static PBRMaterialAttribute createOcclusionTexture (Texture texture, int texCoordIndex) {
		return new PBRMaterialAttribute(OcclusionTexture, texCoordIndex, texture);
	}

	public static PBRMaterialAttribute createNormalTexture (Texture texture, int texCoordIndex) {
		return new PBRMaterialAttribute(NormalTexture, texCoordIndex, texture);
	}

	public static PBRMaterialAttribute createMetalRoughnessTexture (Texture texture, int texCoordIndex) {
		return new PBRMaterialAttribute(MetalRoughnessTexture, texCoordIndex, texture);
	}

	@Override
	public Attribute copy () {
		return new PBRMaterialAttribute(this);
	}

	@Override
	public int compareTo (Attribute o) {
		if (type != o.type) return type < o.type ? -1 : 1;
		PBRMaterialAttribute other = (PBRMaterialAttribute)o;
		final int c = textureDescriptor.compareTo(other.textureDescriptor);
		if (c != 0) return c;
		if (texCoordIndex != other.texCoordIndex) return texCoordIndex - other.texCoordIndex;

		return 0;
	}

	@Override
	public int hashCode () {
		int result = super.hashCode();
		result = 991 * result + textureDescriptor.hashCode();
		result = 991 * result + texCoordIndex;
		return result;
	}
}
