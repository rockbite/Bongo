package com.rockbite.bongo.engine.gltf.scene;

import com.artemis.ComponentMapper;
import com.artemis.EntitySubscription;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.rockbite.bongo.engine.gltf.scene.shader.BaseSceneShader;
import com.rockbite.bongo.engine.gltf.scene.shader.SceneShaderProvider;

import java.util.Comparator;

public class SceneRenderableProvider {

	private Pool<SceneRenderable> sceneRenderablePool = new Pool<SceneRenderable>() {
		@Override
		protected SceneRenderable newObject () {
			return new SceneRenderable();
		}
	};

	private Comparator<? super SceneRenderable> shaderSorter = new Comparator<SceneRenderable>() {
		@Override
		public int compare (SceneRenderable o1, SceneRenderable o2) {
			return o1.shader.compareTo(o2.shader);
		}
	};


	public void obtainSceneRenderables (
		EntitySubscription nodeInstancesSubscription,
		ComponentMapper<SceneModelInstance> sceneNodeInstanceMapper,
		SceneShaderProvider shaderProvider,
		Array<SceneRenderable> out) {
		final IntBag entities = nodeInstancesSubscription.getEntities();
		for (int i = 0; i < entities.size(); i++) {
			final int e = entities.get(i);
			final SceneModelInstance sceneNodeInstance = sceneNodeInstanceMapper.get(e);
			gatherRenderables(sceneNodeInstance, shaderProvider, out);
		}
	}

	//	public Renderable getRenderable (final Renderable out, final Node node, final NodePart nodePart) {
	//		nodePart.setRenderable(out);
	//		if (nodePart.bones == null && transform != null)
	//			out.worldTransform.set(transform).mul(node.globalTransform);
	//		else if (transform != null)
	//			out.worldTransform.set(transform);
	//		else
	//			out.worldTransform.idt();
	//		out.userData = userData;
	//		return out;
	//	}

	private void gatherRenderables (SceneModelInstance instance, SceneNode sceneNode, SceneShaderProvider sceneShaderProvider, Array<SceneRenderable> out) {
		if (sceneNode.isHasMesh()) {
			final SceneMesh sceneMesh = sceneNode.getSceneMesh();
			for (SceneMeshPrimtive sceneMeshPrimtive : sceneMesh.getSceneMeshPrimtiveArray()) {

				final SceneMaterial material = sceneMeshPrimtive.sceneMaterial;

				//pool
				SceneRenderable sceneRenderable = sceneRenderablePool.obtain();
				sceneRenderable.setSceneMesh(sceneMeshPrimtive);
				sceneRenderable.setMaterial(material);
				sceneRenderable.setReferenceSceneNode(sceneNode);

				sceneRenderable.setBones(sceneMeshPrimtive.bones);

				if (sceneMeshPrimtive.bones == null && instance.transform != null) {
					sceneRenderable.worldTransform.set(instance.transform).mul(sceneNode.globalTransform);
				} else if (instance.transform != null) {
					sceneRenderable.worldTransform.set(instance.transform);
				} else {
					sceneRenderable.worldTransform.idt();
				}

				BaseSceneShader shader = sceneShaderProvider.getShader(sceneRenderable);

				sceneRenderable.setShader(shader);

				out.add(sceneRenderable);

			}
		}

		for (SceneNode child : sceneNode.getChildren()) {
			gatherRenderables(instance, child, sceneShaderProvider, out);
		}
	}

	private void gatherRenderables (SceneModelInstance nodeInstance, SceneShaderProvider shaderProvider, Array<SceneRenderable> out) {
		for (SceneNode node : nodeInstance.getNodes()) {
			gatherRenderables(nodeInstance, node, shaderProvider, out);
		}
	}


	public void freeAll (Array<SceneRenderable> sceneRenderables) {
		sceneRenderablePool.freeAll(sceneRenderables);
		sceneRenderables.clear();
	}

	public void sort (Array<SceneRenderable> sceneRenderables) {
		sceneRenderables.sort(shaderSorter);
	}
}
