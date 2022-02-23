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

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.math.Vector3;

public class PBRVec3Attribute extends Attribute {

	public final static String EmissiveFactorAlias = "emissiveFactor";
	public final static long EmissiveFactor = register(EmissiveFactorAlias);

	public final static PBRVec3Attribute createEmmissiveModifier (final Vector3 vec3) {
		return new PBRVec3Attribute(EmissiveFactor, vec3);
	}

	public final Vector3 vec3 = new Vector3();

	public PBRVec3Attribute (final long type) {
		super(type);
	}

	public PBRVec3Attribute (final long type, final Vector3 vec3) {
		this(type);
		if (vec3 != null) this.vec3.set(vec3);
	}


	public PBRVec3Attribute (final PBRVec3Attribute copyFrom) {
		this(copyFrom.type, copyFrom.vec3);
	}

	@Override
	public Attribute copy () {
		return new PBRVec3Attribute(this);
	}

	@Override
	public int hashCode () {
		int result = super.hashCode();
		result = 953 * result + vec3.hashCode();
		return result; 
	}
	
	@Override
	public int compareTo (Attribute o) {
		if (type != o.type) return (int)(type - o.type);
		return ((PBRVec3Attribute)o).vec3.hashCode() - vec3.hashCode();
	}
}
