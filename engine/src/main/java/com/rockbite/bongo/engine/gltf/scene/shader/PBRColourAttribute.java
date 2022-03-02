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

package com.rockbite.bongo.engine.gltf.scene.shader;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class PBRColourAttribute extends Attribute {

	public final static String BaseColourModifierAlias = "baseColourModifier";
	public final static long BaseColourModifier = register(BaseColourModifierAlias);

	public final static PBRColourAttribute createBaseColourModifier (final Color color) {
		return new PBRColourAttribute(BaseColourModifier, color);
	}

	public final float[] color = new float[]{1f, 1f, 1f, 1f};

	public PBRColourAttribute (final long type) {
		super(type);
	}

	public PBRColourAttribute (final long type, final Color color) {
		this(type);
		if (color != null) {
			this.color[0] = color.r;
			this.color[1] = color.g;
			this.color[2] = color.b;
			this.color[3] = color.a;
		}
	}
	public PBRColourAttribute (final long type, final float[] color) {
		this(type);
		if (color != null) {
			this.color[0] = color[0];
			this.color[1] = color[1];
			this.color[2] = color[2];
			this.color[3] = color[3];
		}
	}

	public PBRColourAttribute (final long type, float r, float g, float b, float a) {
		this(type);
		this.color[0] = r;
		this.color[1] = g;
		this.color[2] = b;
		this.color[3] = a;
	}

	public PBRColourAttribute (final PBRColourAttribute copyFrom) {
		this(copyFrom.type, copyFrom.color);
	}

	@Override
	public Attribute copy () {
		return new PBRColourAttribute(this);
	}

	static Color staticColour = new Color();



	@Override
	public int hashCode () {
		int result = super.hashCode();
		final int intbits = staticColour.set(color[0], color[1], color[2], color[3]).toIntBits();
		result = 953 * result + intbits;
		return result; 
	}
	
	@Override
	public int compareTo (Attribute o) {
		if (type != o.type) return (int)(type - o.type);

		final float[] otherColour = ((PBRColourAttribute)o).color;

		final int intbits = staticColour.set(color[0], color[1], color[2], color[3]).toIntBits();
		final int otherBits = staticColour.set(otherColour[0], otherColour[1], otherColour[2], otherColour[3]).toIntBits();

		return otherBits - intbits;
	}
}
