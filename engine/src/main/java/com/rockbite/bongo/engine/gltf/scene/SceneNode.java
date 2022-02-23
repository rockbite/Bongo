package com.rockbite.bongo.engine.gltf.scene;

import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntMap;
import com.rockbite.bongo.engine.gltf.GLTFDataModel;
import lombok.Getter;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class SceneNode {


	@Getter
	protected final String name;
	int nodeIndex = -1;


	public boolean inheritTransform = true;
	/** Whether this node is currently being animated, if so the translation, rotation and scale values are not used. */
	public boolean isAnimated = false;
	/** the translation, relative to the parent, not modified by animations **/
	public final Vector3 translation = new Vector3();
	/** the rotation, relative to the parent, not modified by animations **/
	public final Quaternion rotation = new Quaternion(0, 0, 0, 1);
	/** the scale, relative to the parent, not modified by animations **/
	public final Vector3 scale = new Vector3(1, 1, 1);
	/** the local transform, based on translation/rotation/scale ({@link #calculateLocalTransform()}) or any applied animation **/
	public final Matrix4 localTransform = new Matrix4();
	/** the global transform, product of local transform and transform of the parent node, calculated via
	 * {@link #calculateWorldTransform()} **/
	public final Matrix4 globalTransform = new Matrix4();


	@Getter
	private SceneNode parent;


	@Getter
	private Array<SceneNode> children = new Array<>();

	@Getter
	private SceneMesh sceneMesh;

	private int cameraID;
	@Getter
	private boolean hasMesh;
	@Getter
	private boolean hasCamera;

	int skin = -1;


	public SceneNode (String name) {
		this.name = name;
	}

	public SceneNode (int nodeIndex, GLTFDataModel.NodeData nodeData, SceneResourceContext sceneResourceContext) {
		this.nodeIndex = nodeIndex;

		name = nodeData.getName();

		final int mesh = nodeData.getMesh();
		if (mesh != -1) {
			sceneMesh = sceneResourceContext.getMesh(mesh);
			hasMesh = true;
		} else {
			cameraID = nodeData.getCamera();
			hasCamera = true;
		}

		if (nodeData.getMatrix() != null) {
			localTransform.set(nodeData.getMatrix());

			localTransform.getTranslation(this.translation);
			localTransform.getRotation(this.rotation);
			localTransform.getScale(this.scale);
		} else {
			final float[] scale = nodeData.getScale();
			final float[] rotation = nodeData.getRotation();
			final float[] translation = nodeData.getTranslation();

			this.translation.set(translation);
			this.rotation.set(rotation[0], rotation[1], rotation[2], rotation[3]);
			this.scale.set(scale);
		}

		if (nodeData.getSkin() != -1) {
			this.skin = nodeData.getSkin();
		}
	}


	public boolean hasParent () {
		return parent != null;
	}

	public void setSceneMesh (SceneMesh sceneMesh) {
		hasMesh = true;
		this.sceneMesh = sceneMesh;
	}



	/** Calculates the local transform based on the translation, scale and rotation
	 * @return the local transform */
	public Matrix4 calculateLocalTransform () {
		if (!isAnimated) {
			localTransform.set(translation, rotation, scale);
		}
		return localTransform;
	}


	/** Calculates the world transform; the product of local transform and the parent's world transform.
	 * @return the world transform */
	public Matrix4 calculateWorldTransform () {
		if (inheritTransform && parent != null) {
			globalTransform.set(parent.globalTransform).mul(localTransform);
		} else {
			globalTransform.set(localTransform);
		}
		return globalTransform;
	}


	/** Calculates the local and world transform of this node and optionally all its children.
	 *
	 * @param recursive whether to calculate the local/world transforms for children. */
	public void calculateTransforms (boolean recursive) {
		calculateLocalTransform();
		calculateWorldTransform();

		if (recursive) {
			for (SceneNode child : children) {
				child.calculateTransforms(true);
			}
		}
	}

	public void calculateBoneTransforms (boolean recursive) {
		if (hasMesh) {
			final Array<SceneMeshPrimtive> sceneMeshPrimtiveArray = sceneMesh.getSceneMeshPrimtiveArray();
			for (final SceneMeshPrimtive prim : sceneMeshPrimtiveArray) {
				if (prim.invBoneBindTransforms == null || prim.bones == null || prim.invBoneBindTransforms.size != prim.bones.length)
					continue;
				final int n = prim.invBoneBindTransforms.size;
				for (int i = 0; i < n; i++) {
					final SceneNode nodeForBoneTransform = prim.invBoneBindTransforms.keys[i];
					final Matrix4 boneMatrix = prim.invBoneBindTransforms.values[i];

					prim.bones[i].set(nodeForBoneTransform.globalTransform).mul(boneMatrix);
				}
			}
		}
		if (recursive) {
			for (SceneNode child : children) {
				child.calculateBoneTransforms(true);
			}
		}
	}

	public SceneNode copy () {
		SceneNode node = new SceneNode(this.name);
		node.nodeIndex = nodeIndex;
		node.hasMesh = hasMesh;
		node.isAnimated = isAnimated;
		node.inheritTransform = inheritTransform;

		node.translation.set(translation);
		node.rotation.set(rotation);
		node.scale.set(scale);

		node.localTransform.set(localTransform);
		node.globalTransform.set(globalTransform);

		if (hasMesh) {
			node.sceneMesh = sceneMesh.copy();
		}

		for (SceneNode child : children) {
			node.addChild(child.copy());
		}
		return node;
	}

	void addChild (SceneNode child) {
		children.add(child);
		child.parent = this;
	}

	@Override
	public String toString () {
		return "SceneNode{" + "name='" + name + '\'' + ", hasMesh=" + hasMesh + '}';
	}
}
