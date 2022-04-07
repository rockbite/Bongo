package com.rockbite.bongo.engine.systems.render;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.EntitySubscription;
import com.artemis.annotations.AspectDescriptor;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.rockbite.bongo.engine.components.render.ShaderControlResource;
import com.rockbite.bongo.engine.components.singletons.Cameras;
import com.rockbite.bongo.engine.render.PolygonSpriteBatchMultiTextureMULTIBIND;
import com.rockbite.bongo.engine.render.ShaderFlags;
import com.rockbite.bongo.engine.render.ShaderSourceProvider;
import com.rockbite.bongo.engine.render.SpriteShaderCompiler;
import com.rockbite.bongo.engine.systems.RenderPassSystem;
import lombok.Getter;
import lombok.Setter;

public class EngineDebugSystem extends BaseSystem {


	private Cameras cameras;
	private ShapeRenderer shapeRenderer;
	private PolygonSpriteBatchMultiTextureMULTIBIND spriteBatch;

	private Plane plane = new Plane(new Vector3(0, 1, 0), 0f);
	private Vector3 intersectionOut = new Vector3();

	@AspectDescriptor(all = ShaderControlResource.class)
	private EntitySubscription shaderResourceSubscription;

	private ComponentMapper<ShaderControlResource> shaderResourceMapper;


	@Setter
	private boolean drawAxis = true;
	@Setter
	private boolean drawUnitSquare = true;

	@Getter
	private Array<ShaderControlResource> liveShaderResources = new Array<>();


	public EngineDebugSystem () {

		String shapeVertexSource = ShaderSourceProvider.resolveVertex("core/shape", Files.FileType.Classpath).readString();
		String shapeFragmentSource = ShaderSourceProvider.resolveFragment("core/shape", Files.FileType.Classpath).readString();

		shapeRenderer = new ShapeRenderer(5000,
			SpriteShaderCompiler.getOrCreateShader("core/shape", shapeVertexSource, shapeFragmentSource, new ShaderFlags())
		);

		spriteBatch = new PolygonSpriteBatchMultiTextureMULTIBIND(1000);

	}

	@Override
	protected void initialize () {
		super.initialize();
	}

	/**
	 * Process the system.
	 */
	@Override
	protected void processSystem () {
		liveShaderResources.clear();
		final IntBag entities = shaderResourceSubscription.getEntities();
		for (int i = 0; i < entities.size(); i++) {
			final int e = entities.get(i);
			liveShaderResources.add(shaderResourceMapper.get(e));
		}

		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

		shapeRenderer.setProjectionMatrix(cameras.getGameCamera().combined);

		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
		shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1f);

		if (drawAxis) {

			float length = 100;

			shapeRenderer.setColor(Color.GREEN);
			shapeRenderer.line(0, 0, 0, length, 0, 0);
			shapeRenderer.setColor(Color.RED);
			shapeRenderer.line(0, 0, 0, 0, length, 0);
			shapeRenderer.setColor(Color.BLUE);
			shapeRenderer.line(0, 0, 0, 0, 0, length);
		}

		if (drawUnitSquare) {
			final Ray pickRay = cameras.getGameCamera().getPickRay(Gdx.input.getX(), Gdx.input.getY(), RenderPassSystem.glViewport.x, RenderPassSystem.glViewport.y, RenderPassSystem.glViewport.width, RenderPassSystem.glViewport.height);
			Intersector.intersectRayPlane(pickRay, plane, intersectionOut);
			int selectedX = MathUtils.floor(intersectionOut.x);
			int selectedZ = MathUtils.floor(intersectionOut.z);

			shapeRenderer.box(selectedX, 0, selectedZ + 1, 1f, 0.1f, 1f);
		}

		shapeRenderer.end();

		Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

	}
}
