package com.rockbite.bongo.engine.gltf.scene;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.math.Vector3;
import com.rockbite.bongo.engine.gltf.GLTFDataModel;
import com.rockbite.bongo.engine.gltf.scene.shader.PBRColourAttribute;
import com.rockbite.bongo.engine.gltf.scene.shader.PBRFloatAttribute;
import com.rockbite.bongo.engine.gltf.scene.shader.PBRMaterialAttribute;
import com.rockbite.bongo.engine.gltf.scene.shader.PBRVec3Attribute;
import lombok.Getter;

public class SceneMaterial {

	private final String name;

	@Getter
	private Attributes attributes = new Attributes();

	public static SceneMaterial Empty (String name) {
		return new SceneMaterial(name);
	}

	public static SceneMaterial BasicPBR (String name, Color color, float metal, float roughness) {
		final SceneMaterial sceneMaterial = new SceneMaterial(name);
		sceneMaterial.attributes.set(PBRColourAttribute.createBaseColourModifier(color));
		sceneMaterial.attributes.set(PBRFloatAttribute.createMetallic(metal));
		sceneMaterial.attributes.set(PBRFloatAttribute.createRoughness(roughness));
		sceneMaterial.attributes.set(PBRVec3Attribute.createEmmissiveModifier(new Vector3(0f, 0f, 0f)));
		return sceneMaterial;
	}

	private SceneMaterial (String name) {
		this.name = name;
	}

	public SceneMaterial (SceneResourceContext sceneResourceContext, GLTFDataModel.MaterialData materialData) {
		name = materialData.getName();

		final GLTFDataModel.PBRMetallicRoughnessData pbrMetallicRoughness = materialData.getPbrMetallicRoughness();
		final GLTFDataModel.BaseColorTextureData baseColorTexture = pbrMetallicRoughness.getBaseColorTexture();

		final float[] colourFactor = pbrMetallicRoughness.getBaseColorFactor();
		attributes.set(
			PBRColourAttribute.createBaseColourModifier(
				new Color(colourFactor[0], colourFactor[1], colourFactor[2], colourFactor[3])
			)
		);


		if (baseColorTexture != null) {

			final int index = baseColorTexture.getIndex();
			final int texCoord = baseColorTexture.getTexCoord();

			attributes.set(
				PBRMaterialAttribute.createBaseColourTexture(sceneResourceContext.getTexture(index), texCoord)
			);

			if (pbrMetallicRoughness.getMetallicRoughnessTexture() != null) {
				final GLTFDataModel.MetallicRoughnessTexture metallicRoughnessTexture = pbrMetallicRoughness.getMetallicRoughnessTexture();
				final int metalRoughnessIndex = metallicRoughnessTexture.getIndex();
				final int metalRoughnessTexCoord = metallicRoughnessTexture.getTexCoord();

				attributes.set(
					PBRMaterialAttribute.createMetalRoughnessTexture(sceneResourceContext.getTexture(metalRoughnessIndex), metalRoughnessTexCoord)
				);
			}

		}
		attributes.set(PBRFloatAttribute.createMetallic(pbrMetallicRoughness.getMetallicFactor()));
		attributes.set(PBRFloatAttribute.createRoughness(pbrMetallicRoughness.getRoughnessFactor()));

		if (materialData.getNormalTexture() != null) {
			final GLTFDataModel.NormalTexture normalTexture = materialData.getNormalTexture();

			final int index = normalTexture.getIndex();
			final float scale = normalTexture.getScale();
			final int texCoord = normalTexture.getTexCoord();

			attributes.set(
				PBRMaterialAttribute.createNormalTexture(sceneResourceContext.getTexture(index), texCoord)
			);

			attributes.set(
				PBRFloatAttribute.createNormalTextureScale(scale)
			);

		}

		if (materialData.getOcclusionTexture() != null) {

			final GLTFDataModel.OcclusionTexture occlusionTexture = materialData.getOcclusionTexture();
			final int index = occlusionTexture.getIndex();
			final int texCoord = occlusionTexture.getTexCoord();
			final float strength = occlusionTexture.getStrength();

			attributes.set(
				PBRMaterialAttribute.createOcclusionTexture(sceneResourceContext.getTexture(index), texCoord)
			);

			attributes.set(
				PBRFloatAttribute.createOcclusionStrength(strength)
			);
		}

		if (materialData.getEmissiveTexture() != null) {
			final int index = materialData.getEmissiveTexture().getIndex();
			final int texCoord = materialData.getEmissiveTexture().getTexCoord();
			attributes.set(
				PBRMaterialAttribute.createEmissiveTexture(sceneResourceContext.getTexture(index), texCoord)
			);

		}
		attributes.set(
			PBRVec3Attribute.createEmmissiveModifier(new Vector3(materialData.getEmissiveFactor()))
		);

	}

	public long getMask () {
		return attributes.getMask();
	}



}
