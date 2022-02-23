package com.rockbite.bongo.engine.gltf.scene;

import com.badlogic.gdx.utils.Array;
import com.rockbite.bongo.engine.gltf.GLTFDataModel;
import lombok.Getter;

public class SceneMesh {

	private String name;
	private int numPrimitives;

	@Getter
	private Array<SceneMeshPrimtive> sceneMeshPrimtiveArray = new Array();

	public SceneMesh (GLTFDataModel.MeshData meshData, GLTFDataModel dataModel) {
		name = meshData.getName();
		numPrimitives = meshData.getPrimitives().length;
	}

	public SceneMesh (String name, SceneMeshPrimtive... primitives) {
		this.name = name;
		numPrimitives = primitives.length;
		sceneMeshPrimtiveArray.addAll(primitives);
	}

	private SceneMesh () {

	}

	public void setAllPrimitivesMaterial (SceneMaterial sceneMaterial) {
		for (SceneMeshPrimtive sceneMeshPrimtive : sceneMeshPrimtiveArray) {
			sceneMeshPrimtive.sceneMaterial = sceneMaterial;
		}
	}

	public SceneMesh copy () {
		SceneMesh sceneMesh = new SceneMesh();

		sceneMesh.name = name;
		sceneMesh.numPrimitives = numPrimitives;

		for (SceneMeshPrimtive sceneMeshPrimtive : sceneMeshPrimtiveArray) {
			sceneMesh.sceneMeshPrimtiveArray.add(sceneMeshPrimtive.copy());
		}


		return sceneMesh;
	}
}
