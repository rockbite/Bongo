package com.rockbite.bongo.engine.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.Buffer;
import java.nio.IntBuffer;

import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE_2D;
import static com.badlogic.gdx.graphics.GL30.*;

public class FrameBufferWithDepthOnly extends FrameBuffer {

	public FrameBufferWithDepthOnly (GLFrameBufferBuilder<? extends GLFrameBuffer<Texture>> bufferBuilder) {
		super(bufferBuilder);
	}

	@Override
	protected void attachFrameBufferColorTexture (Texture texture) {

		Gdx.gl20.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, texture.getTextureObjectHandle(), 0);

		IntBuffer buffer = BufferUtils.newIntBuffer(1);
		for (int i = 0; i < 1; i++) {
			buffer.put(GL30.GL_NONE);
		}
		((Buffer) buffer).position(0);
		Gdx.gl30.glDrawBuffers(1, buffer);

	}
}
