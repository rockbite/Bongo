package com.rockbite.bongo.engine.dds;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.OrderedMap;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static java.nio.ByteOrder.nativeOrder;

public class DDSTextureData implements TextureData {

	private final int width;
	private final int height;
	private final FloatBuffer data;
	private final int level;
	private final int face;

	public DDSTextureData (int baseWidth, int baseHeight, int face, int level, byte[] data) {
		this.width = baseWidth;
		this.height = baseHeight;
		this.face = face;
		this.level = level;
		final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length);
		byteBuffer.order(ByteOrder.BIG_ENDIAN);
		byteBuffer.put(data);
		byteBuffer.position(0);


		this.data = byteBuffer.asFloatBuffer();

		final int capacity = this.data.capacity();
		for (int i = 0; i <= capacity - 4; i+=4) {
			float a = this.data.get();
			float b = this.data.get();
			float g = this.data.get();
			float r = this.data.get();

			this.data.put(i, b);
			this.data.put(i + 1, g);
			this.data.put(i + 2, r);
			this.data.put(i + 3, 1);
		}

		//16-bit floating-point formats use half-precision (s10e5 format): sign bit, 5-bit biased (15) exponent, and 10-bit mantissa.


		this.data.position(0);

	}

	@Override
	public TextureDataType getType () {
		return TextureDataType.Custom;
	}

	@Override
	public boolean isPrepared () {
		return false;
	}

	@Override
	public void prepare () {

	}

	@Override
	public Pixmap consumePixmap () {
		return null;
	}

	@Override
	public boolean disposePixmap () {
		return false;
	}

	@Override
	public void consumeCustomData (int target) {
		Gdx.gl.glTexImage2D(target, level, GL30.GL_RGB32F, width, height, 0, Gdx.gl.GL_RGBA, GL30.GL_FLOAT, data);
	}

	@Override
	public int getWidth () {
		return width;
	}

	@Override
	public int getHeight () {
		return height;
	}

	@Override
	public Pixmap.Format getFormat () {
		return Pixmap.Format.RGBA8888;
	}

	@Override
	public boolean useMipMaps () {
		return true;
	}

	@Override
	public boolean isManaged () {
		return false;
	}
}
