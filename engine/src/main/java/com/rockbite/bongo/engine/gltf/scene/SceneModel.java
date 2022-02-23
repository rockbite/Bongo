package com.rockbite.bongo.engine.gltf.scene;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.rockbite.bongo.engine.gltf.scene.animation.SceneAnimation;
import com.rockbite.bongo.engine.gltf.scene.animation.SceneAnimationData;
import com.rockbite.bongo.engine.gltf.scene.animation.SceneAnimationSampler;
import com.rockbite.bongo.engine.gltf.scene.animation.SceneNodeAnimation;
import lombok.Getter;

public class SceneModel {


	@Getter
	protected final String name;

	public Array<SceneAnimation> sceneAnimations = new Array<>();

	/*
	root nodes
	 */
	public Array<SceneNode> nodes = new Array<>();


	public SceneModel (String name) {
		this.name = name;
	}

	public void createAnimation (SceneResourceContext resourceContext) {

		//Collect every animation
		final ObjectSet<SceneAnimationData> objects = new ObjectSet<>();
		extractAnimation(resourceContext, nodes, objects);

		for (SceneAnimationData object : objects) {
			SceneAnimation sceneAnimation = new SceneAnimation(object.getAnimationName());
			final Array<SceneNodeAnimation> sceneNodeAnimationArray = sceneAnimation.getSceneNodeAnimationArray();

			final IntMap<Array<SceneAnimationSampler<?>>> nodesWithData = object.getNodesWithData();
			for (IntMap.Entry<Array<SceneAnimationSampler<?>>> nodesWithDatum : nodesWithData) {
				final int nodeId = nodesWithDatum.key;
				final Array<SceneAnimationSampler<?>> value = nodesWithDatum.value;
				final SceneNode sceneNodeForAnimationID = findNode(nodeId);

				SceneNodeAnimation sceneNodeAnimation = new SceneNodeAnimation();
				sceneNodeAnimation.setSceneNode(sceneNodeForAnimationID);
				sceneNodeAnimation.setSamplers(value);

				sceneNodeAnimationArray.add(sceneNodeAnimation);
			}

			sceneAnimation.calculateMaxTime();
			sceneAnimations.add(sceneAnimation);
		}

	}

	private void extractAnimation (SceneResourceContext resourceContext, Array<SceneNode> sceneNodeArray, ObjectSet<SceneAnimationData> animationGather) {
		for (int i = 0; i < sceneNodeArray.size; i++) {
			final SceneNode sceneNode = sceneNodeArray.get(i);

			final int nodeIndex = sceneNode.nodeIndex;

			final Array<SceneAnimationData> sceneAnimationData = resourceContext.gatherAnimations(nodeIndex);
			if (sceneAnimationData != null) {
				animationGather.addAll(sceneAnimationData);
			}

			extractAnimation(resourceContext, sceneNode.getChildren(), animationGather);
		}
	}

	public void createSkin (SceneResourceContext resourceContext) {
		createSkin(resourceContext, nodes);
	}

	private void createSkin (SceneResourceContext resourceContext, Array<SceneNode> nodeArray) {
		for (int i = 0; i < nodeArray.size; i++) {
			final SceneNode sceneNode = nodeArray.get(i);

			if (sceneNode.skin == -1 || !sceneNode.isHasMesh()) {

			} else {
				final SceneSkin skin = resourceContext.getSkin(sceneNode.skin);

				final Array<Matrix4> ibms = skin.getIbms();
				final int[] joints = skin.getJoints();

				final SceneMesh sceneMesh = sceneNode.getSceneMesh();

				for (SceneMeshPrimtive child : sceneMesh.getSceneMeshPrimtiveArray()) {
					child.bones = new Matrix4[ibms.size];
					child.invBoneBindTransforms = new ArrayMap<SceneNode, Matrix4>(SceneNode.class, Matrix4.class);
					for (int n = 0; n < joints.length; n++) {
						child.bones[n] = new Matrix4().idt();
						int nodeIndex = joints[n];
						SceneNode key = findNode(nodeIndex);
						if (key == null) {
							throw new GdxRuntimeException("node not found for bone: " + nodeIndex);
						}
						child.invBoneBindTransforms.put(key, ibms.get(n));
					}
				}
			}

			createSkin(resourceContext, sceneNode.getChildren());
		}
	}

	private SceneNode findNode (Array<SceneNode> sceneNodeArray, String nodeName) {
		for (int i = 0; i < sceneNodeArray.size; i++) {
			final SceneNode sceneNode = sceneNodeArray.get(i);
			if (sceneNode.name.equalsIgnoreCase(nodeName)) return sceneNode;

			SceneNode found = findNode(sceneNode.getChildren(), nodeName);
			if (found != null) return found;
		}

		return null;
	}

	public SceneNode findNode (String nodeName) {
		return findNode(nodes, nodeName);
	}

	private SceneNode findNode (Array<SceneNode> sceneNodeArray, int nodeIndex) {
		for (int i = 0; i < sceneNodeArray.size; i++) {
			final SceneNode sceneNode = sceneNodeArray.get(i);
			if (sceneNode.nodeIndex == nodeIndex) return sceneNode;

			SceneNode found = findNode(sceneNode.getChildren(), nodeIndex);
			if (found != null) return found;
		}

		return null;
	}

	private SceneNode findNode (int nodeIndex) {
		return findNode(nodes, nodeIndex);
	}

	public void calculateTransforms () {
		final int n = nodes.size;
		for (int i = 0; i < n; i++) {
			nodes.get(i).calculateTransforms(true);
		}
		for (int i = 0; i < n; i++) {
			nodes.get(i).calculateBoneTransforms(true);
		}
	}

	@Override
	public String toString () {
		return "SceneModel{" + "name='" + name + '\'' + '}';
	}


}
