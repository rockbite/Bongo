package com.rockbite.bongo.engine.gltf.scene;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.rockbite.bongo.engine.gltf.GLTFDataModel;
import com.rockbite.bongo.engine.gltf.scene.animation.SceneAnimationData;
import lombok.Getter;

public class SceneGraph {

	private final String name;

	@Getter
	private Array<SceneModel> rootModels = new Array<>();
	@Getter
	private ObjectMap<String, SceneModel> nameModelMap = new ObjectMap<>();


	public SceneGraph (GLTFDataModel.SceneData sceneData, GLTFDataModel dataModel, SceneResourceContext resourceContext) {
		name = sceneData.getName();

		for (int node : sceneData.getNodes()) {
			final GLTFDataModel.NodeData nodeData = dataModel.getNodes()[node];
			SceneModel sceneModel = new SceneModel(nodeData.getName());

			createChildrenForModel(sceneModel, nodeData, node, dataModel, resourceContext);

			rootModels.add(sceneModel);
			nameModelMap.put(sceneModel.getName(), sceneModel);

			sceneModel.createSkin(resourceContext);
			sceneModel.createAnimation(resourceContext);
			sceneModel.calculateTransforms();
		}

	}


	public SceneModel getSceneNode (String name) {
		return nameModelMap.get(name);
	}

	private void createChildrenForModel (SceneModel parent, GLTFDataModel.NodeData nodeData, int nodeIndex, GLTFDataModel dataModel, SceneResourceContext resourceContext) {

		//Create the first node
		SceneNode node = new SceneNode(nodeIndex, nodeData, resourceContext);
		parent.nodes.add(node);

		createChildrenForNode(node, nodeData, dataModel, resourceContext);
	}

	private void createChildrenForNode (SceneNode parent, GLTFDataModel.NodeData parentNodeData, GLTFDataModel dataModel, SceneResourceContext resourceContext) {
		if (parentNodeData.getChildren() != null) {
			final int[] children = parentNodeData.getChildren();
			for (int child : children) {

				final GLTFDataModel.NodeData childNodeData = dataModel.getNodes()[child];
				SceneNode childSceneNode = new SceneNode(child, childNodeData, resourceContext);
				parent.addChild(childSceneNode);


				createChildrenForNode(childSceneNode, childNodeData, dataModel, resourceContext);
			}
		}
	}

}
