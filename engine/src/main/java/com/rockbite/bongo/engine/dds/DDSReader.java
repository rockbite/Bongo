package com.rockbite.bongo.engine.dds;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FacedCubemapData;
import com.badlogic.gdx.graphics.glutils.MipMapTextureData;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntMap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DDSReader {

	private static final int DDPF_ALPHAPIXELS = 0x1;
	private static final int DDPF_ALPHA = 0x2;
	private static final int DDPF_FOURCC = 0x4;
	private static final int DDPF_RGB = 0x40;
	private static final int DDPF_YUV = 0x200;
	private static final int DDPF_LUMINANCE = 0x20000;

	private static final int DDSCAPS_COMPLEX = 0x8;
	private static final int DDSCAPS_MIPMAP = 0x400000;
	private static final int DDSCAPS_TEXTURE = 0x1000;

	private static final int DDSCAPS2_CUBEMAP = 0x200;
	private static final int DDSCAPS2_CUBEMAP_POSITIVEX = 0x400;
	private static final int DDSCAPS2_CUBEMAP_NEGATIVEX = 0x800;
	private static final int DDSCAPS2_CUBEMAP_POSITIVEY = 0x1000;
	private static final int DDSCAPS2_CUBEMAP_NEGATIVEY = 0x2000;
	private static final int DDSCAPS2_CUBEMAP_POSITIVEZ = 0x4000;
	private static final int DDSCAPS2_CUBEMAP_NEGATIVEZ = 0x8000;
	private static final int DDSCAPS2_VOLUME = 0x200000;


	public static Cubemap createCubemapFromDDS (FileHandle handle) {
		final byte[] bytes = handle.readBytes();

		ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.LITTLE_ENDIAN);
		buffer.put(bytes);
		buffer.flip();

		final int magic = buffer.getInt();
		if (magic != 0x20534444) {
			throw new GdxRuntimeException("Not a valid dds file");
		}

		DDSHeader header = new DDSHeader(buffer);
		if (header.getDwFlags() == DDPF_FOURCC) {
			final int dwFourCC = header.getDdspf().getDwFourCC();
			//Dont need for now
		}

		final int dwWidth = header.getDwWidth();
		final int dwHeight = header.getDwHeight();
		final int dwMipMapCount = header.getDwMipMapCount();

		if ((header.getDwCaps() & DDSCAPS_COMPLEX) == DDSCAPS_COMPLEX) {
			System.out.println("complex");


			boolean validCubemap = hasValidCubemap(header.getDwCaps2());

			final int dwPitchOrLinearSize = header.getDwPitchOrLinearSize();

			if ((header.getDdspf().getDwFlags() & DDPF_RGB) == DDPF_RGB) {

				System.out.println("uncompressed RGB");

				final DDSHeader.DDSPixelFormat pixelFormat = header.getDdspf();
				final int dwRGBBitCount = pixelFormat.getDwRGBBitCount();

				final int dwRBitMask = pixelFormat.getDwRBitMask();
				final int dwGBitMask = pixelFormat.getDwGBitMask();
				final int dwBBitMask = pixelFormat.getDwBBitMask();

				IntMap<Array<byte[]>> imageByteArrays = new IntMap<>();

				for (int surface = 0; surface < 6; surface++) {
					imageByteArrays.put(surface, new Array<>());

					for (int mip = 0; mip < dwMipMapCount; mip++) {

						int width = header.getDwWidth() >> mip;
						int height = header.getDwHeight() >> mip;
						int pitch = (width * dwRGBBitCount + 7) / 8;

						pitch *= height;

						byte[] data = new byte[pitch];
						buffer.get(data);

						for (int i = 0; i < data.length; i+=4) {
							final byte origR = data[i];
							final byte origG = data[i + 2];
							data[i] = origG;
							data[i + 2] = origR;
						}

						imageByteArrays.get(surface).add(data);
					}
				}

				final MipMapTextureData posX = createMipMapTextureData(0, header.getDwWidth(), header.getDwHeight(), imageByteArrays);
				final MipMapTextureData negX = createMipMapTextureData(1, header.getDwWidth(), header.getDwHeight(), imageByteArrays);
				final MipMapTextureData posY = createMipMapTextureData(2, header.getDwWidth(), header.getDwHeight(), imageByteArrays);
				final MipMapTextureData negY = createMipMapTextureData(3, header.getDwWidth(), header.getDwHeight(), imageByteArrays);
				final MipMapTextureData posZ = createMipMapTextureData(4, header.getDwWidth(), header.getDwHeight(), imageByteArrays);
				final MipMapTextureData negZ = createMipMapTextureData(5, header.getDwWidth(), header.getDwHeight(), imageByteArrays);
				Cubemap cubemap = new Cubemap(new FacedCubemapData(posX, negX, posY, negY, posZ, negZ));
				cubemap.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);

				return cubemap;
			}
		}
		return null;
	}

	private static MipMapTextureData createMipMapTextureData (int face, int baseWidth, int baseHeight, IntMap<Array<byte[]>> data) {
		final Array<byte[]> entries = data.get(face);
		final int size = entries.size;
		Array<DDSTextureData> textureData = new Array<>();
		for (int mip = 0; mip < size; mip++) {

			int copyWidth = baseWidth;
			int copyHeight = baseHeight;
			int width = copyWidth >> mip;
			int height = copyHeight >> mip;

			textureData.add(new DDSTextureData(width, height, face, mip, entries.get(mip)));
		}

		return new MipMapTextureData(textureData.toArray(DDSTextureData.class));
	}

	private static boolean hasValidCubemap (int dwCaps2) {
		return
			((dwCaps2 & DDSCAPS2_CUBEMAP) == DDSCAPS2_CUBEMAP) &&
			((dwCaps2 & DDSCAPS2_CUBEMAP_POSITIVEX) == DDSCAPS2_CUBEMAP_POSITIVEX) &&
			((dwCaps2 & DDSCAPS2_CUBEMAP_NEGATIVEX) == DDSCAPS2_CUBEMAP_NEGATIVEX) &&
			((dwCaps2 & DDSCAPS2_CUBEMAP_POSITIVEY) == DDSCAPS2_CUBEMAP_POSITIVEY) &&
			((dwCaps2 & DDSCAPS2_CUBEMAP_NEGATIVEY) == DDSCAPS2_CUBEMAP_NEGATIVEY) &&
			((dwCaps2 & DDSCAPS2_CUBEMAP_POSITIVEZ) == DDSCAPS2_CUBEMAP_POSITIVEZ) &&
			((dwCaps2 & DDSCAPS2_CUBEMAP_NEGATIVEZ) == DDSCAPS2_CUBEMAP_NEGATIVEZ);
	}

}
