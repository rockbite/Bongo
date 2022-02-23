package com.rockbite.bongo.engine.gltf.scene.shader;

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.math.collision.BoundingBox;
import lombok.Getter;

public class VolumetricCloudAttribute extends Attribute {

	public final static String BoxCloudAlias = "BoxCloud";
	public final static long BoxCloud = register(BoxCloudAlias);

	@Getter
	private BoundingBox boundingBox;

	public final static VolumetricCloudAttribute createBoxCloud (BoundingBox boundingBox) {
		return new VolumetricCloudAttribute(BoxCloud, boundingBox);
	}

	public VolumetricCloudAttribute (final long type, BoundingBox boundingBox) {
		super(type);
		this.boundingBox = boundingBox;
	}


	public VolumetricCloudAttribute (final VolumetricCloudAttribute copyFrom) {
		this(copyFrom.type, copyFrom.boundingBox);
	}

	@Override
	public Attribute copy () {
		return new VolumetricCloudAttribute(this);
	}

	@Override
	public int hashCode () {
		int result = super.hashCode();
		return result;
	}

	@Override
	public int compareTo (Attribute o) {
		if (type != o.type) return (int)(type - o.type);
		return 0;
	}
}
