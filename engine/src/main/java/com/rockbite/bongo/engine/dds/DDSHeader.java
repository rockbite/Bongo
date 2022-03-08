package com.rockbite.bongo.engine.dds;

import lombok.Data;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Data
public class DDSHeader {

	private int dwSize;
	private int dwFlags;
	private int dwHeight;
	private int dwWidth;
	private int dwPitchOrLinearSize;
	private int dwDepth;
	private int dwMipMapCount;
	private int[] dwReserved1 = new int[11];
	private DDSPixelFormat ddspf;
	private int dwCaps;
	private int dwCaps2;
	private int dwCaps3;
	private int dwCaps4;
	private int dwReserved2;

	@Data
	public static class DDSPixelFormat {
		private int dwSize;
		private int dwFlags;
		private int dwFourCC;
		private int dwRGBBitCount;
		private int dwRBitMask;
		private int dwGBitMask;
		private int dwBBitMask;
		private int dwABitMask;

		public DDSPixelFormat (ByteBuffer buffer) {
			dwSize = buffer.getInt();
			dwFlags = buffer.getInt();
			dwFourCC = buffer.getInt();
			dwRGBBitCount = buffer.getInt();
			dwRBitMask = buffer.getInt();
			dwGBitMask = buffer.getInt();
			dwBBitMask = buffer.getInt();
			dwABitMask = buffer.getInt();
		}
	}

	public DDSHeader (ByteBuffer buffer) {
		dwSize = buffer.getInt();
		dwFlags = buffer.getInt();
		dwHeight = buffer.getInt();
		dwWidth = buffer.getInt();
		dwPitchOrLinearSize = buffer.getInt();
		dwDepth = buffer.getInt();
		dwMipMapCount = buffer.getInt();
		for (int i = 0; i < dwReserved1.length; i++) {
			dwReserved1[i] = buffer.getInt();
		}
		ddspf = new DDSPixelFormat(buffer);
		dwCaps = buffer.getInt();
		dwCaps2 = buffer.getInt();
		dwCaps3 = buffer.getInt();
		dwCaps4 = buffer.getInt();
		dwReserved2 = buffer.getInt();
	}
}
