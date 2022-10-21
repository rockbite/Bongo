/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.rockbite.bongo.engine.render;


import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Mesh.VertexDataType;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.NumberUtils;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;
import java.util.Arrays;

import static com.rockbite.bongo.engine.render.SpriteShaderCompiler.UNROLL_TEXTURE_ARRAY;

/** A PolygonSpriteBatch is used to draw 2D polygons that reference a texture (region). The class will batch the drawing commands
 * and optimize them for processing by the GPU.
 * <p>
 * To draw something with a PolygonSpriteBatch one has to first call the {@link com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch#begin()} method which will
 * setup appropriate render states. When you are done with drawing you have to call {@link com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch#end()} which will
 * actually draw the things you specified.
 * <p>
 * All drawing commands of the PolygonSpriteBatch operate in screen coordinates. The screen coordinate system has an x-axis
 * pointing to the right, an y-axis pointing upwards and the origin is in the lower left corner of the screen. You can also
 * provide your own transformation and projection matrices if you so wish.
 * <p>
 * A PolygonSpriteBatch is managed. In case the OpenGL context is lost all OpenGL resources a PolygonSpriteBatch uses internally
 * get invalidated. A context is lost when a user switches to another application or receives an incoming call on Android. A
 * SpritPolygonSpriteBatcheBatch will be automatically reloaded after the OpenGL context is restored.
 * <p>
 * A PolygonSpriteBatch is a pretty heavy object so you should only ever have one in your program.
 * <p>
 * A PolygonSpriteBatch works with OpenGL ES 1.x and 2.0. In the case of a 2.0 context it will use its own custom shader to draw
 * all provided sprites. You can set your own custom shader via {@link #setShader(ShaderProgram)}.
 * <p>
 * A PolygonSpriteBatch has to be disposed if it is no longer used.
 * @author mzechner
 * @author Stefan Bachmann
 * @author Nathan Sweet */
public class PolygonSpriteBatchMultiTextureMULTIBIND implements PolyBatchWithEncodingOverride {

	private static final Logger logger = LoggerFactory.getLogger(PolygonSpriteBatchMultiTextureMULTIBIND.class);

	/** The current number of textures in the LFU cache. Gets reset when calling {@link#begin()} **/
	private int currentTextureLFUSize = 0;

	/** The current number of texture swaps in the LFU cache. Gets reset when calling {@link#begin()} **/
	private int currentTextureLFUSwaps = 0;


	/** The maximum number of available texture units for the fragment shader */
	@Getter
	public static int maxTextureUnits;

	/** Textures in use (index: Texture Unit, value: Texture) */
	private Texture[] usedTextures;

	/** LFU Array (index: Texture Unit Index - value: Access frequency) */
	private int[] usedTexturesLFU;

	/** Gets sent to the fragment shader as an uniform "uniform sampler2d[X] u_textures" */
	private IntBuffer textureUnitIndicesBuffer;

	static final int POSITION_COMPONENTS = 2;
	static final int COLOR_COMPONENTS = 1;
	static final int UV_COMPONENTS = 2;
	static final int TEXTURE_INDEX_COMPONENTS = 1;
	static final int CUSTOM_INFO_COMPONENTS = 3;

	private float[] customInfoArray = new float[CUSTOM_INFO_COMPONENTS];

	{
		for (int i = 0; i < customInfoArray.length; i++) {
			customInfoArray[i] = 69f;
		}
	}

	static final int VERTEX_SIZE = POSITION_COMPONENTS + COLOR_COMPONENTS + UV_COMPONENTS + TEXTURE_INDEX_COMPONENTS + CUSTOM_INFO_COMPONENTS;
	static final int SPRITE_SIZE = 4 * VERTEX_SIZE;

	private Mesh mesh;

	public static boolean ALLOWED_TO_HACK = false;

	private float[] vertices;
	private short[] triangles;
	private int vertexIndex, triangleIndex;
	private Texture lastTexture;
	private Texture lastEmissiveTexture;
	private float invTexWidth = 0, invTexHeight = 0;
	private boolean drawing;

	private final Matrix4 transformMatrix = new Matrix4();
	private final Matrix4 projectionMatrix = new Matrix4();
	private final Matrix4 combinedMatrix = new Matrix4();

	private boolean blendingDisabled;
	private int blendSrcFunc = GL20.GL_SRC_ALPHA;
	private int blendDstFunc = GL20.GL_ONE_MINUS_SRC_ALPHA;
	private int blendSrcFuncAlpha = GL20.GL_SRC_ALPHA;
	private int blendDstFuncAlpha = GL20.GL_ONE_MINUS_SRC_ALPHA;

	private ShaderProgram shader;
	private ShaderProgram customShader;
	private boolean ownsShader;

	float color = Color.WHITE.toFloatBits();
	private Color tempColor = new Color(1, 1, 1, 1);

	private boolean sampleEmissive;
	private boolean shouldIgnoreBlendChanges;

	public interface EmissiveTextureProvider {
		Texture getEmissiveTexture (Texture diffuseTexture);
	}

	@Setter
	private EmissiveTextureProvider emissiveTextureProvider;

	private boolean customColourEncoding;
	private float customColourBitsToBeEncoded;

	/** Number of render calls since the last {@link #begin()}. **/
	public int renderCalls = 0;

	/** Number of rendering calls, ever. Will not be reset unless set manually. **/
	public static int totalRenderCalls = 0;

	/** The maximum number of triangles rendered in one batch so far. **/
	public int maxTrianglesInBatch = 0;

	/** Constructs a PolygonSpriteBatch with the default shader, 2000 vertices, and 4000 triangles.
	 * @see #PolygonSpriteBatchMultiTextureMULTIBIND(int, int, ShaderProgram) */
	public PolygonSpriteBatchMultiTextureMULTIBIND () {
		this(2000, null);
	}

	/** Constructs a PolygonSpriteBatch with the default shader, size vertices, and size * 2 triangles.
	 * @param size The max number of vertices and number of triangles in a single batch. Max of 32767.
	 * @see #PolygonSpriteBatchMultiTextureMULTIBIND(int, int, ShaderProgram) */
	public PolygonSpriteBatchMultiTextureMULTIBIND (int size) {
		this(size, size * 2, null);
	}

	/** Constructs a PolygonSpriteBatch with the specified shader, size vertices and size * 2 triangles.
	 * @param size The max number of vertices and number of triangles in a single batch. Max of 32767.
	 * @see #PolygonSpriteBatchMultiTextureMULTIBIND(int, int, ShaderProgram) */
	public PolygonSpriteBatchMultiTextureMULTIBIND (int size, ShaderProgram defaultShader) {
		this(size, size * 2, defaultShader);
	}

	/** Constructs a new PolygonSpriteBatch. Sets the projection matrix to an orthographic projection with y-axis point upwards,
	 * x-axis point to the right and the origin being in the bottom left corner of the screen. The projection will be pixel perfect
	 * with respect to the current screen resolution.
	 * <p>
	 * The defaultShader specifies the shader to use. Note that the names for uniforms for this default shader are different than
	 * the ones expect for shaders set with {@link #setShader(ShaderProgram)}. See {@link SpriteBatch#createDefaultShader()}.
	 * @param maxVertices The max number of vertices in a single batch. Max of 32767.
	 * @param maxTriangles The max number of triangles in a single batch.
	 * @param defaultShader The default shader to use. This is not owned by the PolygonSpriteBatch and must be disposed separately.
	 *           May be null to use the default shader. */
	public PolygonSpriteBatchMultiTextureMULTIBIND (int maxVertices, int maxTriangles, ShaderProgram defaultShader) {
		// 32767 is max vertex index.
		if (maxVertices > 32767)
			throw new IllegalArgumentException("Can't have more than 32767 vertices per batch: " + maxVertices);


		// Query the number of available texture units and decide on a safe number of texture units to use
		IntBuffer texUnitsQueryBuffer = BufferUtils.newIntBuffer(32);

		Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_IMAGE_UNITS, texUnitsQueryBuffer);

		maxTextureUnits = texUnitsQueryBuffer.get() - 2;

		if (maxTextureUnits <= 0) {
			maxTextureUnits = 8; //Conservative guess if gl fucks up
		}

		int cacheMaxTexUnits = maxTextureUnits;


		// Some OpenGL drivers (I'm looking at you, Intel!) do not report the right values,
		// so we take caution and test it first, reducing the number of slots if needed.
		if (defaultShader == null) {
			// Will try to create a shader with a lower amount of texture units if creation fails.

			shader = tryToCompile(false);

			if (shader == null) { //We failed, last try, use unrolling
				UNROLL_TEXTURE_ARRAY = true;
				maxTextureUnits = cacheMaxTexUnits;

				shader = tryToCompile(true);
				logger.trace("Using unrolled shader");
			} else {
				logger.trace("Using rolled shader");
			}

			if (shader == null) {
				throw new GdxRuntimeException("Couldn't compile core shader");
			}

			ownsShader = true;
		} else {
			shader = defaultShader;
			ownsShader = false;
		}

		// System.out.println("Using " + maxTextureUnits + " texture units.");

		usedTextures = new Texture[maxTextureUnits];
		usedTexturesLFU = new int[maxTextureUnits];

		// This contains the numbers 0 ... maxTextureUnits - 1. We send these to the shader as an uniform.
		textureUnitIndicesBuffer = BufferUtils.newIntBuffer(maxTextureUnits);
		for (int i = 0; i < maxTextureUnits; i++) {
			textureUnitIndicesBuffer.put(i);
		}
		textureUnitIndicesBuffer.flip();

		VertexDataType vertexDataType = VertexDataType.VertexArray;
		if (Gdx.gl30 != null) {
			vertexDataType = VertexDataType.VertexBufferObjectWithVAO;
		}
		mesh = new Mesh(vertexDataType, false, maxVertices, maxTriangles * 3,
			new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
			new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
			new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"),
			new VertexAttribute(Usage.Generic, 1, "texture_index"),
			new VertexAttribute(Usage.Generic, CUSTOM_INFO_COMPONENTS, "custom_info")
		);

		vertices = new float[maxVertices * VERTEX_SIZE];
		triangles = new short[maxTriangles * 3];



		projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}

	private ShaderProgram tryToCompile (boolean unrolling) {
		while (maxTextureUnits > 0) {
			try {
				ShaderProgram internalShader = createDefaultShader();

				if (internalShader.isCompiled()) {
					return internalShader;
				} else {
					logger.info(internalShader.getLog());
				}

				//break;

			} catch (Exception e) {

			}

			if (maxTextureUnits == 1) {
				//failed
				return null;
			}

			maxTextureUnits /= 2;
		}

		return null;
	}

	/** Returns a new instance of the default shader used by TextureArraySpriteBatch for GL2 when no shader is specified. */
	private ShaderProgram createDefaultShader () {

		String vertexSource = ShaderSourceProvider.resolveVertex("core/defaultSprite", Files.FileType.Classpath).readString();
		String fragmentSource = ShaderSourceProvider.resolveFragment("core/defaultSprite", Files.FileType.Classpath).readString();

		final ShaderProgram shader = SpriteShaderCompiler.getOrCreateShader("core/defaultSprite", vertexSource, fragmentSource, new ShaderFlags());

		return shader;
	}

	@Override
	public void begin () {
		if (drawing) throw new IllegalStateException("PolygonSpriteBatch.end must be called before begin.");
		renderCalls = 0;


		currentTextureLFUSize = 0;
		currentTextureLFUSwaps = 0;


		Arrays.fill(usedTextures, null);
		Arrays.fill(usedTexturesLFU, 0);

		Gdx.gl.glDepthMask(false);
		if (customShader != null)
			customShader.begin();
		else
			shader.begin();
		setupMatrices();

		drawing = true;
	}

	/** Assigns Texture units and manages the LFU cache.
	 * @param texture The texture that shall be loaded into the cache, if it is not already loaded.
	 * @return The texture slot that has been allocated to the selected texture */
	private int activateTexture (Texture texture) {

		if (sampleEmissive) {
			texture = emissiveTextureProvider.getEmissiveTexture(texture);
		}

		invTexWidth = 1.0f / texture.getWidth();
		invTexHeight = 1.0f / texture.getHeight();

		// This is our identifier for the textures
		final int textureHandle = texture.getTextureObjectHandle();

		// First try to see if the texture is already cached
		for (int i = 0; i < currentTextureLFUSize; i++) {

			// getTextureObjectHandle() just returns an int,
			// it's fine to call this method instead of caching the value.
			if (textureHandle == usedTextures[i].getTextureObjectHandle()) {

				// Increase the access counter.
				usedTexturesLFU[i]++;

				return i;
			}
		}

		// If a free texture unit is available we just use it
		// If not we have to flush and then throw out the least accessed one.
		if (currentTextureLFUSize < maxTextureUnits) {

			// System.out.println("Adding new Texture " + textureHandle + " to slot " + currentTextureLFUSize);

			// Put the texture into the next free slot
			usedTextures[currentTextureLFUSize] = texture;

			// Increase the access counter.
			usedTexturesLFU[currentTextureLFUSize]++;

			return currentTextureLFUSize++;

		} else {

			// We have to flush if there is something in the pipeline already,
			// otherwise the texture index of previously rendered sprites gets invalidated
			if (this.vertexIndex > 0) {
				flush();
			}

			// System.out.println("LFU BEFORE: " + Arrays.toString(usedTexturesLFU));

			int slot = 0;
			int slotVal = usedTexturesLFU[0];

			int max = 0;
			int average = 0;

			// We search for the best candidate for a swap (least accessed) and collect some data
			for (int i = 0; i < maxTextureUnits; i++) {

				final int val = usedTexturesLFU[i];

				max = Math.max(val, max);

				average += val;

				if (val <= slotVal) {
					slot = i;
					slotVal = val;
				}
			}

			// The LFU weights will be normalized to the range 0...100
			final int normalizeRange = 100;

			for (int i = 0; i < maxTextureUnits; i++) {
				usedTexturesLFU[i] = usedTexturesLFU[i] * normalizeRange / max;
			}

			average = (average * normalizeRange) / (max * maxTextureUnits);

			// Give the new texture a fair (average) chance of staying.
			usedTexturesLFU[slot] = average;

			// System.out.println("LFU AFTER: " + Arrays.toString(usedTexturesLFU) + " - Swapped: " + slot);

			// System.out.println("Kicking out Texture from slot " + slot + " for texture " + textureHandle);

			usedTextures[slot] = texture;


			return slot;
		}
	}


	@Override
	public void end () {
		if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before end.");
		if (vertexIndex > 0) flush();
		lastTexture = null;
		lastEmissiveTexture = null;
		drawing = false;

		GL20 gl = Gdx.gl;
		gl.glDepthMask(true);
		if (isBlendingEnabled()) gl.glDisable(GL20.GL_BLEND);

		if (customShader != null)
			customShader.end();
		else
			shader.end();
	}

	@Override
	public void setColor (Color tint) {
		color = tint.toFloatBits();
	}

	@Override
	public void setColor (float r, float g, float b, float a) {
		int intBits = (int)(255 * a) << 24 | (int)(255 * b) << 16 | (int)(255 * g) << 8 | (int)(255 * r);
		color = NumberUtils.intToFloatColor(intBits);
	}

	@Override
	public Color getColor () {
		int intBits = NumberUtils.floatToIntColor(color);
		Color color = this.tempColor;
		color.r = (intBits & 0xff) / 255f;
		color.g = ((intBits >>> 8) & 0xff) / 255f;
		color.b = ((intBits >>> 16) & 0xff) / 255f;
		color.a = ((intBits >>> 24) & 0xff) / 255f;
		return color;
	}

	@Override
	public float getPackedColor () {
		return color;
	}

	@Override
	public void setPackedColor(float color) {
		this.color = color;
	}

	private void flushIfFull () {

	}

	/** Draws a polygon region with the bottom left corner at x,y having the width and height of the region. */
	public void draw (PolygonRegion region, float x, float y) {
		if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final short[] regionTriangles = region.getTriangles();
		final int regionTrianglesLength = regionTriangles.length;
		final float[] regionVertices = region.getVertices();
		final int regionVerticesLength = regionVertices.length;

		if (triangleIndex + regionTrianglesLength > triangles.length
			|| vertexIndex + regionVerticesLength * VERTEX_SIZE / 2 > vertices.length) {
			flush();
		}

		final Texture texture = region.getRegion().getTexture();

		final float ti = activateTexture(texture);

		int triangleIndex = this.triangleIndex;
		int vertexIndex = this.vertexIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;

		for (int i = 0; i < regionTrianglesLength; i++)
			triangles[triangleIndex++] = (short)(regionTriangles[i] + startVertex);
		this.triangleIndex = triangleIndex;

		final float[] vertices = this.vertices;
		float color = customColourEncoding ? customColourBitsToBeEncoded : this.color;
		final float[] textureCoords = region.getTextureCoords();

		for (int i = 0; i < regionVerticesLength; i += 2) {
			vertices[vertexIndex++] = regionVertices[i] + x;
			vertices[vertexIndex++] = regionVertices[i + 1] + y;
			vertices[vertexIndex++] = color;
			vertices[vertexIndex++] = textureCoords[i];
			vertices[vertexIndex++] = textureCoords[i + 1];
			vertices[vertexIndex++] = ti;
			for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
				vertices[vertexIndex++] = customInfoArray[j];
			}
		}
		this.vertexIndex = vertexIndex;
	}

	/** Draws a polygon region with the bottom left corner at x,y and stretching the region to cover the given width and height. */
	public void draw (PolygonRegion region, float x, float y, float width, float height) {
		if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final short[] regionTriangles = region.getTriangles();
		final int regionTrianglesLength = regionTriangles.length;
		final float[] regionVertices = region.getVertices();
		final int regionVerticesLength = regionVertices.length;
		final TextureRegion textureRegion = region.getRegion();

		final Texture texture = textureRegion.getTexture();

		final float ti = activateTexture(texture);

		if (triangleIndex + regionTrianglesLength > triangles.length
			|| vertexIndex + regionVerticesLength * VERTEX_SIZE / 2 > vertices.length) {
			flush();
		}

		int triangleIndex = this.triangleIndex;
		int vertexIndex = this.vertexIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;

		for (int i = 0, n = regionTriangles.length; i < n; i++)
			triangles[triangleIndex++] = (short)(regionTriangles[i] + startVertex);
		this.triangleIndex = triangleIndex;

		final float[] vertices = this.vertices;
		float color = customColourEncoding ? customColourBitsToBeEncoded : this.color;
		final float[] textureCoords = region.getTextureCoords();
		final float sX = width / textureRegion.getRegionWidth();
		final float sY = height / textureRegion.getRegionHeight();

		for (int i = 0; i < regionVerticesLength; i += 2) {
			vertices[vertexIndex++] = regionVertices[i] * sX + x;
			vertices[vertexIndex++] = regionVertices[i + 1] * sY + y;
			vertices[vertexIndex++] = color;
			vertices[vertexIndex++] = textureCoords[i];
			vertices[vertexIndex++] = textureCoords[i + 1];
			vertices[vertexIndex++] = ti;
			for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
				vertices[vertexIndex++] = customInfoArray[j];
			}
		}
		this.vertexIndex = vertexIndex;
	}

	/** Draws the polygon region with the bottom left corner at x,y and stretching the region to cover the given width and height.
	 * The polygon region is offset by originX, originY relative to the origin. Scale specifies the scaling factor by which the
	 * polygon region should be scaled around originX, originY. Rotation specifies the angle of counter clockwise rotation of the
	 * rectangle around originX, originY. */
	public void draw (PolygonRegion region, float x, float y, float originX, float originY, float width, float height,
		float scaleX, float scaleY, float rotation) {
		if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final short[] regionTriangles = region.getTriangles();
		final int regionTrianglesLength = regionTriangles.length;
		final float[] regionVertices = region.getVertices();
		final int regionVerticesLength = regionVertices.length;
		final TextureRegion textureRegion = region.getRegion();

		Texture texture = textureRegion.getTexture();

		final float ti = activateTexture(texture);

		if (triangleIndex + regionTrianglesLength > triangles.length
			|| vertexIndex + regionVerticesLength * VERTEX_SIZE / 2 > vertices.length) {
			flush();
		}

		int triangleIndex = this.triangleIndex;
		int vertexIndex = this.vertexIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;

		for (int i = 0; i < regionTrianglesLength; i++)
			triangles[triangleIndex++] = (short)(regionTriangles[i] + startVertex);
		this.triangleIndex = triangleIndex;

		final float[] vertices = this.vertices;
		float color = customColourEncoding ? customColourBitsToBeEncoded : this.color;
		final float[] textureCoords = region.getTextureCoords();

		final float worldOriginX = x + originX;
		final float worldOriginY = y + originY;
		final float sX = width / textureRegion.getRegionWidth();
		final float sY = height / textureRegion.getRegionHeight();
		final float cos = MathUtils.cosDeg(rotation);
		final float sin = MathUtils.sinDeg(rotation);

		float fx, fy;
		for (int i = 0; i < regionVerticesLength; i += 2) {
			fx = (regionVertices[i] * sX - originX) * scaleX;
			fy = (regionVertices[i + 1] * sY - originY) * scaleY;
			vertices[vertexIndex++] = cos * fx - sin * fy + worldOriginX;
			vertices[vertexIndex++] = sin * fx + cos * fy + worldOriginY;
			vertices[vertexIndex++] = color;
			vertices[vertexIndex++] = textureCoords[i];
			vertices[vertexIndex++] = textureCoords[i + 1];
			vertices[vertexIndex++] = ti;
			for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
				vertices[vertexIndex++] = customInfoArray[j];
			}
		}
		this.vertexIndex = vertexIndex;
	}

	/** Draws the polygon using the given vertices and triangles. Each vertices must be made up of 5 elements in this order: x, y,
	 * color, u, v. */
	@Override
	public void draw (Texture texture, float[] polygonVertices, int verticesOffset, int verticesCount, short[] polygonTriangles,
		int trianglesOffset, int trianglesCount) {
		if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final float[] vertices = this.vertices;

		final int triangleCount = polygonTriangles.length;

		float calculatedTriangleCount = (verticesCount/5);
		float calculatedVertexCount = calculatedTriangleCount * VERTEX_SIZE;

		if (triangleIndex + trianglesCount > triangles.length || vertexIndex + calculatedVertexCount > vertices.length) {
			flush();
		}

		final float ti = activateTexture(texture);

		int triangleIndex = this.triangleIndex;
		final int vertexIndex = this.vertexIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;

		for (int i = trianglesOffset, n = i + trianglesCount; i < n; i++)
			triangles[triangleIndex++] = (short)(polygonTriangles[i] + startVertex);
		this.triangleIndex = triangleIndex;

		int triangle = 0;
		int vertexOffset = this.vertexIndex;
		for (int i = 0; i < verticesCount; i+= 5) { //copy and fake

			int rootOffset = triangle * VERTEX_SIZE;

//			if (vertexOffset + (rootOffset + 0) >= vertices.length) {
//				System.out.println();
//			}
			vertices[vertexOffset + (rootOffset + 0)] = polygonVertices[i]; //x
			vertices[vertexOffset + (rootOffset + 1)] = polygonVertices[i + 1];//y
			vertices[vertexOffset + (rootOffset + 2)] = customColourEncoding ? customColourBitsToBeEncoded : polygonVertices[i + 2];//colour
			vertices[vertexOffset + (rootOffset + 3)] = polygonVertices[i + 3];//u
			vertices[vertexOffset + (rootOffset + 4)] = polygonVertices[i + 4];//v
			vertices[vertexOffset + (rootOffset + 5)] = ti;//v
			for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
				vertices[vertexOffset + (rootOffset + 6 + j)] = customInfoArray[j];
			}

			triangle++;

		}

		this.vertexIndex += calculatedVertexCount;
	}

	@Override
	public void draw (Texture texture, float x, float y, float originX, float originY, float width, float height, float scaleX,
		float scaleY, float rotation, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
		if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final float[] vertices = this.vertices;

		final float ti = activateTexture(texture);

		if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) {//
			flush();
		}

		int triangleIndex = this.triangleIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;
		triangles[triangleIndex++] = (short)startVertex;
		triangles[triangleIndex++] = (short)(startVertex + 1);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 3);
		triangles[triangleIndex++] = (short)startVertex;
		this.triangleIndex = triangleIndex;

		// bottom left and top right corner points relative to origin
		final float worldOriginX = x + originX;
		final float worldOriginY = y + originY;
		float fx = -originX;
		float fy = -originY;
		float fx2 = width - originX;
		float fy2 = height - originY;

		// scale
		if (scaleX != 1 || scaleY != 1) {
			fx *= scaleX;
			fy *= scaleY;
			fx2 *= scaleX;
			fy2 *= scaleY;
		}

		// construct corner points, start from top left and go counter clockwise
		final float p1x = fx;
		final float p1y = fy;
		final float p2x = fx;
		final float p2y = fy2;
		final float p3x = fx2;
		final float p3y = fy2;
		final float p4x = fx2;
		final float p4y = fy;

		float x1;
		float y1;
		float x2;
		float y2;
		float x3;
		float y3;
		float x4;
		float y4;

		// rotate
		if (rotation != 0) {
			final float cos = MathUtils.cosDeg(rotation);
			final float sin = MathUtils.sinDeg(rotation);

			x1 = cos * p1x - sin * p1y;
			y1 = sin * p1x + cos * p1y;

			x2 = cos * p2x - sin * p2y;
			y2 = sin * p2x + cos * p2y;

			x3 = cos * p3x - sin * p3y;
			y3 = sin * p3x + cos * p3y;

			x4 = x1 + (x3 - x2);
			y4 = y3 - (y2 - y1);
		} else {
			x1 = p1x;
			y1 = p1y;

			x2 = p2x;
			y2 = p2y;

			x3 = p3x;
			y3 = p3y;

			x4 = p4x;
			y4 = p4y;
		}

		x1 += worldOriginX;
		y1 += worldOriginY;
		x2 += worldOriginX;
		y2 += worldOriginY;
		x3 += worldOriginX;
		y3 += worldOriginY;
		x4 += worldOriginX;
		y4 += worldOriginY;

		float u = srcX * invTexWidth;
		float v = (srcY + srcHeight) * invTexHeight;
		float u2 = (srcX + srcWidth) * invTexWidth;
		float v2 = srcY * invTexHeight;

		if (flipX) {
			float tmp = u;
			u = u2;
			u2 = tmp;
		}

		if (flipY) {
			float tmp = v;
			v = v2;
			v2 = tmp;
		}

		float color = customColourEncoding ? customColourBitsToBeEncoded : this.color;
		int idx = this.vertexIndex;
		vertices[idx++] = x1;
		vertices[idx++] = y1;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = x2;
		vertices[idx++] = y2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = x3;
		vertices[idx++] = y3;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = x4;
		vertices[idx++] = y4;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		this.vertexIndex = idx;
	}

	@Override
	public void draw (Texture texture, float x, float y, float width, float height, int srcX, int srcY, int srcWidth,
		int srcHeight, boolean flipX, boolean flipY) {
		if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final float[] vertices = this.vertices;

		final float ti = activateTexture(texture);

		if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length){
			flush();
		}

		int triangleIndex = this.triangleIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;
		triangles[triangleIndex++] = (short)startVertex;
		triangles[triangleIndex++] = (short)(startVertex + 1);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 3);
		triangles[triangleIndex++] = (short)startVertex;
		this.triangleIndex = triangleIndex;

		float u = srcX * invTexWidth;
		float v = (srcY + srcHeight) * invTexHeight;
		float u2 = (srcX + srcWidth) * invTexWidth;
		float v2 = srcY * invTexHeight;
		final float fx2 = x + width;
		final float fy2 = y + height;

		if (flipX) {
			float tmp = u;
			u = u2;
			u2 = tmp;
		}

		if (flipY) {
			float tmp = v;
			v = v2;
			v2 = tmp;
		}

		float color = customColourEncoding ? customColourBitsToBeEncoded : this.color;
		int idx = this.vertexIndex;
		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		this.vertexIndex = idx;
	}

	@Override
	public void draw (Texture texture, float x, float y, int srcX, int srcY, int srcWidth, int srcHeight) {
		if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final float[] vertices = this.vertices;


		float ti = activateTexture(texture);

		if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) {
			flush();
		}

		int triangleIndex = this.triangleIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;
		triangles[triangleIndex++] = (short)startVertex;
		triangles[triangleIndex++] = (short)(startVertex + 1);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 3);
		triangles[triangleIndex++] = (short)startVertex;
		this.triangleIndex = triangleIndex;

		final float u = srcX * invTexWidth;
		final float v = (srcY + srcHeight) * invTexHeight;
		final float u2 = (srcX + srcWidth) * invTexWidth;
		final float v2 = srcY * invTexHeight;
		final float fx2 = x + srcWidth;
		final float fy2 = y + srcHeight;

		float color = customColourEncoding ? customColourBitsToBeEncoded : this.color;
		int idx = this.vertexIndex;
		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		this.vertexIndex = idx;
	}

	@Override
	public void draw (Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
		if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final float[] vertices = this.vertices;

		float ti = activateTexture(texture);

		if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) {
			flush();
		}

		int triangleIndex = this.triangleIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;
		triangles[triangleIndex++] = (short)startVertex;
		triangles[triangleIndex++] = (short)(startVertex + 1);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 3);
		triangles[triangleIndex++] = (short)startVertex;
		this.triangleIndex = triangleIndex;

		final float fx2 = x + width;
		final float fy2 = y + height;

		float color = customColourEncoding ? customColourBitsToBeEncoded : this.color;
		int idx = this.vertexIndex;
		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		this.vertexIndex = idx;
	}

	@Override
	public void draw (Texture texture, float x, float y) {
		draw(texture, x, y, texture.getWidth(), texture.getHeight());
	}

	@Override
	public void draw (Texture texture, float x, float y, float width, float height) {
		if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final float[] vertices = this.vertices;

		float ti = activateTexture(texture);

		if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) {
			flush();
		}

		int triangleIndex = this.triangleIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;
		triangles[triangleIndex++] = (short)startVertex;
		triangles[triangleIndex++] = (short)(startVertex + 1);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 3);
		triangles[triangleIndex++] = (short)startVertex;
		this.triangleIndex = triangleIndex;

		final float fx2 = x + width;
		final float fy2 = y + height;
		final float u = 0;
		final float v = 1;
		final float u2 = 1;
		final float v2 = 0;

		float color = customColourEncoding ? customColourBitsToBeEncoded : this.color;
		int idx = this.vertexIndex;
		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		this.vertexIndex = idx;
	}

	@Override
	public void draw (Texture texture, float[] spriteVertices, int offset, int count) {
		if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final float[] vertices = this.vertices;

		final int triangleCount = count / 20 * 6;

		int vertCount = triangleCount * SPRITE_SIZE;

		int calculatedSize = triangleCount * SPRITE_SIZE / 6;

		float ti = activateTexture(texture);


		float calculatedTriangleCount = (count/5);
		float calculatedVertexCount = calculatedTriangleCount * VERTEX_SIZE;

		if (triangleIndex + triangleCount > triangles.length || vertexIndex + calculatedVertexCount > vertices.length) {
			flush();
		}

		final int vertexIndex = this.vertexIndex;
		int triangleIndex = this.triangleIndex;
		short vertex = (short)(vertexIndex / VERTEX_SIZE);
		for (int n = triangleIndex + triangleCount; triangleIndex < n; triangleIndex += 6, vertex += 4) {
			triangles[triangleIndex] = vertex;
			triangles[triangleIndex + 1] = (short)(vertex + 1);
			triangles[triangleIndex + 2] = (short)(vertex + 2);
			triangles[triangleIndex + 3] = (short)(vertex + 2);
			triangles[triangleIndex + 4] = (short)(vertex + 3);
			triangles[triangleIndex + 5] = vertex;
		}
		this.triangleIndex = triangleIndex;

		int triangle = 0;
		int vertexOffset = this.vertexIndex;
		for (int i = 0; i < spriteVertices.length; i+= 5) { //copy and fake

			int rootOffset = triangle * VERTEX_SIZE;

			vertices[vertexOffset + (rootOffset + 0)] = spriteVertices[i]; //x
			vertices[vertexOffset + (rootOffset + 1)] = spriteVertices[i + 1];//y
			vertices[vertexOffset + (rootOffset + 2)] = customColourEncoding ? customColourBitsToBeEncoded : spriteVertices[i + 2];//colour
			vertices[vertexOffset + (rootOffset + 3)] = spriteVertices[i + 3];//u
			vertices[vertexOffset + (rootOffset + 4)] = spriteVertices[i + 4];//v
			vertices[vertexOffset + (rootOffset + 5)] = ti;//v
			for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
				vertices[vertexOffset + (rootOffset + 6 + j)] = customInfoArray[j];//v
			}

			triangle++;

		}

		this.vertexIndex += calculatedVertexCount;
	}

	@Override
	public void draw (TextureRegion region, float x, float y) {
		draw(region, x, y, region.getRegionWidth(), region.getRegionHeight());
	}

	@Override
	public void draw (TextureRegion region, float x, float y, float width, float height) {
		if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final float[] vertices = this.vertices;

		Texture texture = region.getTexture();

		int ti = activateTexture(texture);

		if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) {
			flush();
		}

		int triangleIndex = this.triangleIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;
		triangles[triangleIndex++] = (short)startVertex;
		triangles[triangleIndex++] = (short)(startVertex + 1);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 3);
		triangles[triangleIndex++] = (short)startVertex;
		this.triangleIndex = triangleIndex;

		final float fx2 = x + width;
		final float fy2 = y + height;
		final float u = region.getU();
		final float v = region.getV2();
		final float u2 = region.getU2();
		final float v2 = region.getV();

		float color = customColourEncoding ? customColourBitsToBeEncoded : this.color;
		int idx = this.vertexIndex;
		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		this.vertexIndex = idx;
	}

	@Override
	public void draw (TextureRegion region, float x, float y, float originX, float originY, float width, float height,
		float scaleX, float scaleY, float rotation) {
		if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final float[] vertices = this.vertices;

		Texture texture = region.getTexture();
		int ti = activateTexture(texture);

		if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) {
			flush();
		}

		int triangleIndex = this.triangleIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;
		triangles[triangleIndex++] = (short)startVertex;
		triangles[triangleIndex++] = (short)(startVertex + 1);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 3);
		triangles[triangleIndex++] = (short)startVertex;
		this.triangleIndex = triangleIndex;

		// bottom left and top right corner points relative to origin
		final float worldOriginX = x + originX;
		final float worldOriginY = y + originY;
		float fx = -originX;
		float fy = -originY;
		float fx2 = width - originX;
		float fy2 = height - originY;

		// scale
		if (scaleX != 1 || scaleY != 1) {
			fx *= scaleX;
			fy *= scaleY;
			fx2 *= scaleX;
			fy2 *= scaleY;
		}

		// construct corner points, start from top left and go counter clockwise
		final float p1x = fx;
		final float p1y = fy;
		final float p2x = fx;
		final float p2y = fy2;
		final float p3x = fx2;
		final float p3y = fy2;
		final float p4x = fx2;
		final float p4y = fy;

		float x1;
		float y1;
		float x2;
		float y2;
		float x3;
		float y3;
		float x4;
		float y4;

		// rotate
		if (rotation != 0) {
			final float cos = MathUtils.cosDeg(rotation);
			final float sin = MathUtils.sinDeg(rotation);

			x1 = cos * p1x - sin * p1y;
			y1 = sin * p1x + cos * p1y;

			x2 = cos * p2x - sin * p2y;
			y2 = sin * p2x + cos * p2y;

			x3 = cos * p3x - sin * p3y;
			y3 = sin * p3x + cos * p3y;

			x4 = x1 + (x3 - x2);
			y4 = y3 - (y2 - y1);
		} else {
			x1 = p1x;
			y1 = p1y;

			x2 = p2x;
			y2 = p2y;

			x3 = p3x;
			y3 = p3y;

			x4 = p4x;
			y4 = p4y;
		}

		x1 += worldOriginX;
		y1 += worldOriginY;
		x2 += worldOriginX;
		y2 += worldOriginY;
		x3 += worldOriginX;
		y3 += worldOriginY;
		x4 += worldOriginX;
		y4 += worldOriginY;

		final float u = region.getU();
		final float v = region.getV2();
		final float u2 = region.getU2();
		final float v2 = region.getV();

		float color = customColourEncoding ? customColourBitsToBeEncoded : this.color;
		int idx = this.vertexIndex;
		vertices[idx++] = x1;
		vertices[idx++] = y1;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = x2;
		vertices[idx++] = y2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = x3;
		vertices[idx++] = y3;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = x4;
		vertices[idx++] = y4;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		this.vertexIndex = idx;
	}

	@Override
	public void draw (TextureRegion region, float x, float y, float originX, float originY, float width, float height,
		float scaleX, float scaleY, float rotation, boolean clockwise) {
		if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final float[] vertices = this.vertices;

		Texture texture = region.getTexture();
		int ti = activateTexture(texture);

		if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) {
			flush();
		}

		int triangleIndex = this.triangleIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;
		triangles[triangleIndex++] = (short)startVertex;
		triangles[triangleIndex++] = (short)(startVertex + 1);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 3);
		triangles[triangleIndex++] = (short)startVertex;
		this.triangleIndex = triangleIndex;

		// bottom left and top right corner points relative to origin
		final float worldOriginX = x + originX;
		final float worldOriginY = y + originY;
		float fx = -originX;
		float fy = -originY;
		float fx2 = width - originX;
		float fy2 = height - originY;

		// scale
		if (scaleX != 1 || scaleY != 1) {
			fx *= scaleX;
			fy *= scaleY;
			fx2 *= scaleX;
			fy2 *= scaleY;
		}

		// construct corner points, start from top left and go counter clockwise
		final float p1x = fx;
		final float p1y = fy;
		final float p2x = fx;
		final float p2y = fy2;
		final float p3x = fx2;
		final float p3y = fy2;
		final float p4x = fx2;
		final float p4y = fy;

		float x1;
		float y1;
		float x2;
		float y2;
		float x3;
		float y3;
		float x4;
		float y4;

		// rotate
		if (rotation != 0) {
			final float cos = MathUtils.cosDeg(rotation);
			final float sin = MathUtils.sinDeg(rotation);

			x1 = cos * p1x - sin * p1y;
			y1 = sin * p1x + cos * p1y;

			x2 = cos * p2x - sin * p2y;
			y2 = sin * p2x + cos * p2y;

			x3 = cos * p3x - sin * p3y;
			y3 = sin * p3x + cos * p3y;

			x4 = x1 + (x3 - x2);
			y4 = y3 - (y2 - y1);
		} else {
			x1 = p1x;
			y1 = p1y;

			x2 = p2x;
			y2 = p2y;

			x3 = p3x;
			y3 = p3y;

			x4 = p4x;
			y4 = p4y;
		}

		x1 += worldOriginX;
		y1 += worldOriginY;
		x2 += worldOriginX;
		y2 += worldOriginY;
		x3 += worldOriginX;
		y3 += worldOriginY;
		x4 += worldOriginX;
		y4 += worldOriginY;

		float u1, v1, u2, v2, u3, v3, u4, v4;
		if (clockwise) {
			u1 = region.getU2();
			v1 = region.getV2();
			u2 = region.getU();
			v2 = region.getV2();
			u3 = region.getU();
			v3 = region.getV();
			u4 = region.getU2();
			v4 = region.getV();
		} else {
			u1 = region.getU();
			v1 = region.getV();
			u2 = region.getU2();
			v2 = region.getV();
			u3 = region.getU2();
			v3 = region.getV2();
			u4 = region.getU();
			v4 = region.getV2();
		}

		float color = customColourEncoding ? customColourBitsToBeEncoded : this.color;
		int idx = this.vertexIndex;
		vertices[idx++] = x1;
		vertices[idx++] = y1;
		vertices[idx++] = color;
		vertices[idx++] = u1;
		vertices[idx++] = v1;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = x2;
		vertices[idx++] = y2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = x3;
		vertices[idx++] = y3;
		vertices[idx++] = color;
		vertices[idx++] = u3;
		vertices[idx++] = v3;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = x4;
		vertices[idx++] = y4;
		vertices[idx++] = color;
		vertices[idx++] = u4;
		vertices[idx++] = v4;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		this.vertexIndex = idx;
	}

	@Override
	public void draw (TextureRegion region, float width, float height, Affine2 transform) {
		if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final float[] vertices = this.vertices;

		Texture texture = region.getTexture();
		float ti = activateTexture(texture);

		if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) {
			flush();
		}

		int triangleIndex = this.triangleIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;
		triangles[triangleIndex++] = (short)startVertex;
		triangles[triangleIndex++] = (short)(startVertex + 1);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 3);
		triangles[triangleIndex++] = (short)startVertex;
		this.triangleIndex = triangleIndex;

		// construct corner points
		float x1 = transform.m02;
		float y1 = transform.m12;
		float x2 = transform.m01 * height + transform.m02;
		float y2 = transform.m11 * height + transform.m12;
		float x3 = transform.m00 * width + transform.m01 * height + transform.m02;
		float y3 = transform.m10 * width + transform.m11 * height + transform.m12;
		float x4 = transform.m00 * width + transform.m02;
		float y4 = transform.m10 * width + transform.m12;

		float u = region.getU();
		float v = region.getV2();
		float u2 = region.getU2();
		float v2 = region.getV();

		float color = customColourEncoding ? customColourBitsToBeEncoded : this.color;
		int idx = this.vertexIndex;
		vertices[idx++] = x1;
		vertices[idx++] = y1;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = x2;
		vertices[idx++] = y2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = x3;
		vertices[idx++] = y3;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}

		vertices[idx++] = x4;
		vertices[idx++] = y4;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		vertices[idx++] = ti;
		for (int j = 0; j < CUSTOM_INFO_COMPONENTS; j++) {
			vertices[idx++] = customInfoArray[j];
		}


		this.vertexIndex = idx;
	}

	@Override
	public void flush () {
		if (vertexIndex == 0) return;

		renderCalls++;
		totalRenderCalls++;
		int trianglesInBatch = triangleIndex;
		if (trianglesInBatch > maxTrianglesInBatch) maxTrianglesInBatch = trianglesInBatch;

		// Bind the textures
		for (int i = 0; i < currentTextureLFUSize; i++) {
			usedTextures[i].bind(i);
		}

		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

		Mesh mesh = this.mesh;
		mesh.setVertices(vertices, 0, vertexIndex);
		mesh.setIndices(triangles, 0, triangleIndex);
		if (blendingDisabled) {
			Gdx.gl.glDisable(GL20.GL_BLEND);
		} else {
			Gdx.gl.glEnable(GL20.GL_BLEND);
			if (blendSrcFunc != -1) Gdx.gl.glBlendFuncSeparate(blendSrcFunc, blendDstFunc, blendSrcFuncAlpha, blendDstFuncAlpha);
		}

		mesh.render(customShader != null ? customShader : shader, GL20.GL_TRIANGLES, 0, trianglesInBatch);

		vertexIndex = 0;
		triangleIndex = 0;
	}

	@Override
	public void disableBlending () {
		flush();
		blendingDisabled = true;
	}

	@Override
	public void enableBlending () {
		flush();
		blendingDisabled = false;
	}

	@Override
	public void setBlendFunction (int srcFunc, int dstFunc) {
		if (shouldIgnoreBlendChanges) return;
		setBlendFunctionSeparate(srcFunc, dstFunc, srcFunc, dstFunc);
	}

	@Override
	public void setBlendFunctionSeparate(int srcFuncColor, int dstFuncColor, int srcFuncAlpha, int dstFuncAlpha) {
		if (shouldIgnoreBlendChanges) return;
		if (blendSrcFunc == srcFuncColor && blendDstFunc == dstFuncColor && blendSrcFuncAlpha == srcFuncAlpha && blendDstFuncAlpha == dstFuncAlpha) return;
		flush();
		blendSrcFunc = srcFuncColor;
		blendDstFunc = dstFuncColor;
		blendSrcFuncAlpha = srcFuncAlpha;
		blendDstFuncAlpha = dstFuncAlpha;
	}

	@Override
	public int getBlendSrcFunc () {
		return blendSrcFunc;
	}

	@Override
	public int getBlendDstFunc () {
		return blendDstFunc;
	}

	@Override
	public int getBlendSrcFuncAlpha() {
		return blendSrcFuncAlpha;
	}

	@Override
	public int getBlendDstFuncAlpha() {
		return blendDstFuncAlpha;
	}

	@Override
	public void dispose () {
		mesh.dispose();
		if (ownsShader && shader != null) shader.dispose();
	}

	@Override
	public Matrix4 getProjectionMatrix () {
		return projectionMatrix;
	}

	@Override
	public Matrix4 getTransformMatrix () {
		return transformMatrix;
	}

	@Override
	public void setProjectionMatrix (Matrix4 projection) {
		if (drawing) flush();
		projectionMatrix.set(projection);
		if (drawing) setupMatrices();
	}

	@Override
	public void setTransformMatrix (Matrix4 transform) {
		if (drawing) flush();
		transformMatrix.set(transform);
		if (drawing) setupMatrices();
	}

	private void setupMatrices () {
		combinedMatrix.set(projectionMatrix).mul(transformMatrix);
		if (customShader != null) {
			customShader.setUniformMatrix("u_projTrans", combinedMatrix);
			Gdx.gl20.glUniform1iv(customShader.fetchUniformLocation("u_textures", true), maxTextureUnits, textureUnitIndicesBuffer);

		} else {
			shader.setUniformMatrix("u_projTrans", combinedMatrix);
			Gdx.gl20.glUniform1iv(shader.fetchUniformLocation("u_textures", true), maxTextureUnits, textureUnitIndicesBuffer);
		}
	}

	private void switchTexture (Texture texture) {
		flush();
		lastTexture = texture;
		if (emissiveTextureProvider != null) {
			lastEmissiveTexture = emissiveTextureProvider.getEmissiveTexture(texture);
		}
		invTexWidth = 1.0f / texture.getWidth();
		invTexHeight = 1.0f / texture.getHeight();
	}

	@Override
	public void setShader (ShaderProgram shader) {

		if (drawing) {
			flush();
			if (customShader != null)
				customShader.end();
			else
				this.shader.end();
		}
		customShader = shader;
		if (drawing) {
			if (customShader != null)
				customShader.begin();
			else
				this.shader.begin();
			setupMatrices();
		}
	}

	@Override
	public ShaderProgram getShader () {
		if (customShader == null) {
			return shader;
		}
		return customShader;
	}

	@Override
	public boolean isBlendingEnabled () {
		return !blendingDisabled;
	}

	@Override
	public boolean isDrawing () {
		return drawing;
	}

	@Override
	public void setCustomEncodingColour (float r, float g, float b, float a) {
		customColourBitsToBeEncoded = tempColor.set(r, g, b, a).toFloatBits();
	}

	@Override
	public void setUsingCustomColourEncoding (boolean usingCustomEncoding) {
		customColourEncoding = usingCustomEncoding;
	}

	@Override
	public void setCustomInfo (float customInfo) {
		this.customInfoArray[0] = customInfo;
	}

	@Override
	public void setCustomInfo (float customInfo, float customInfo2) {
		this.customInfoArray[0] = customInfo;
		this.customInfoArray[1] = customInfo2;
	}

	@Override
	public void setCustomInfo (float customInfo, float customInfo2, float customInfo3) {
		this.customInfoArray[0] = customInfo;
		this.customInfoArray[1] = customInfo2;
		this.customInfoArray[2] = customInfo3;
	}

	@Override
	public void setIgnoreBlendModeChanges (boolean shouldIgnore) {
		this.shouldIgnoreBlendChanges = shouldIgnore;
	}
}
