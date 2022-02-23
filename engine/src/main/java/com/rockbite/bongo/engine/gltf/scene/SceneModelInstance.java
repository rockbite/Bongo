package com.rockbite.bongo.engine.gltf.scene;

import com.artemis.Component;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.rockbite.bongo.engine.gltf.scene.animation.SceneAnimation;
import com.rockbite.bongo.engine.gltf.scene.animation.SceneNodeAnimation;
import com.rockbite.bongo.engine.prefab.Marshallable;
import lombok.Getter;

@Getter
public class SceneModelInstance extends Component implements Marshallable<SceneModelInstance> {

	protected Matrix4 transform;

	private String modelName;

	private SceneModel model;

	private Array<SceneNode> nodes = new Array<>();
	private Array<SceneAnimation> animations = new Array<>();

	public SceneModelInstance () {
	}

	public SceneModelInstance (SceneModel sceneNode) {
		this(sceneNode, null, (String[])null);
	}

	public SceneModelInstance (SceneModel model, Matrix4 transform, String... rootNodeIds) {
		this.model = model;

		this.transform = transform == null ? new Matrix4() : transform;

		if (rootNodeIds == null) {
			copyNodes(model.nodes);
		} else {
			copyNodes(model.nodes, rootNodeIds);
		}
		copyAnimations(model.sceneAnimations);

		calculateTransforms();
	}

	private void copyAnimations (Array<SceneAnimation> sceneAnimations) {
		for (SceneAnimation sceneAnimation : sceneAnimations) {
			SceneAnimation copy = new SceneAnimation(sceneAnimation);
			final Array<SceneNodeAnimation> copyArray = copy.getSceneNodeAnimationArray();

			for (SceneNodeAnimation sceneNodeAnimation : sceneAnimation.getSceneNodeAnimationArray()) {
				final SceneNode copyReferenceNode = findNode(sceneNodeAnimation.getSceneNode().nodeIndex);
				if (copyReferenceNode == null) {
					throw new GdxRuntimeException("No node found for animation : " + sceneAnimation.getName() + " " + sceneNodeAnimation.getSceneNode().nodeIndex);
				}
				SceneNodeAnimation copySceneNodeAnimation = new SceneNodeAnimation(copyReferenceNode, sceneNodeAnimation);
				copyArray.add(copySceneNodeAnimation);
			}
			animations.add(copy);
		}
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

	private void invalidate (SceneNode node) {
		final SceneMesh sceneMesh = node.getSceneMesh();
		if (sceneMesh != null) {
			final Array<SceneMeshPrimtive> sceneMeshPrimtiveArray = sceneMesh.getSceneMeshPrimtiveArray();
			for (SceneMeshPrimtive sceneMeshPrimtive : sceneMeshPrimtiveArray) {
				final ArrayMap<SceneNode, Matrix4> invBoneBindTransforms = sceneMeshPrimtive.invBoneBindTransforms;
				if (invBoneBindTransforms != null) {
					for (int i = 0; i < invBoneBindTransforms.size; i++) {
						final SceneNode node1 = findNode(invBoneBindTransforms.keys[i].name);

						if (node1 == null) {
							throw new GdxRuntimeException("No bound found");
						}

						invBoneBindTransforms.keys[i] = node1;

					}
				}
			}
		}

		for (int i = 0; i < node.getChildren().size; i++) {
			final SceneNode sceneNode = node.getChildren().get(i);
			invalidate(sceneNode);
		}

	}

	/** Makes sure that each {@link NodePart} of each {@link Node} doesn't reference a node outside this node tree and that all
	 * materials are listed in the {@link #materials} array. */
	private void invalidate () {
		for (int i = 0; i < nodes.size; i++) {
			final SceneNode node = nodes.get(i);
			invalidate(node);
		}
	}

	private void copyNodes (Array<SceneNode> nodes) {
		for (int i = 0, n = nodes.size; i < n; ++i) {
			final SceneNode node = nodes.get(i);
			this.nodes.add(node.copy());
		}
		invalidate();
	}

	private void copyNodes (Array<SceneNode> nodes, final String... nodeIds) {
		for (int i = 0, n = nodes.size; i < n; ++i) {
			final SceneNode node = nodes.get(i);
			for (final String nodeId : nodeIds) {
				if (nodeId.equals(node.name)) {
					this.nodes.add(node.copy());
					break;
				}
			}
		}
		invalidate();

	}


	@Override
	public void marshallFrom (SceneModelInstance other) {
		this.modelName = other.modelName;
	}

	public SceneAnimation findAnimation (String animationName) {
		for (SceneAnimation animation : animations) {
			if (animation.getName().equals(animationName)) {
				return animation;
			}
		}
		return null;
	}


	public SceneNode findNode (int nodeIndex) {
		for (SceneNode node : nodes) {
			SceneNode found = findNode(node, nodeIndex);
			if (found != null) return found;
		}

		return null;
	}

	public SceneNode findNode (SceneNode node, int nodeIndex) {
		if (node.nodeIndex == nodeIndex) {
			return node;
		}
		for (SceneNode child : node.getChildren()) {
			SceneNode foundItem = findNode(child, nodeIndex);
			if (foundItem != null) {
				return foundItem;
			}
		}

		return null;
	}

	public SceneNode findNode (String name) {
		for (SceneNode node : nodes) {
			SceneNode found = findNode(node, name);
			if (found != null) return found;
		}

		return null;
	}

	public SceneNode findNode (SceneNode node, String name) {
		if (node.name.equals(name)) {
			return node;
		}
		for (SceneNode child : node.getChildren()) {
			SceneNode foundItem = findNode(child, name);
			if (foundItem != null) {
				return foundItem;
			}
		}

		return null;
	}
}
