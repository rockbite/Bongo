package com.rockbite.bongo.engine.dds;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.TextureData;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DDSTextureData implements TextureData {

	private final int width;
	private final int height;
	private final ByteBuffer data;
	private final int level;
	private final int face;

	public DDSTextureData (int baseWidth, int baseHeight, int face, int level, byte[] data) {
		this.width = baseWidth;
		this.height = baseHeight;
		this.face = face;
		this.level = level;
		this.data = ByteBuffer.allocateDirect(data.length);
		this.data.order(ByteOrder.LITTLE_ENDIAN);
		this.data.put(data);
		this.data.flip();
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
		System.out.println("Uploading to " + target  + " face"  + face + " " + level +" " + width + " " + height) ;
		Gdx.gl.glTexImage2D(target, level, Gdx.gl30.GL_RGB16F, width, height, 0, Gdx.gl.GL_RGBA, Gdx.gl.GL_UNSIGNED_BYTE, data);
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
