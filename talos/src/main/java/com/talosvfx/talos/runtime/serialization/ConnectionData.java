/*******************************************************************************
 * Copyright 2019 See AUTHORS file.
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

package com.talosvfx.talos.runtime.serialization;

public class ConnectionData {

	public int moduleFrom;
	public int moduleTo;
	public int slotFrom;
	public int slotTo;

	public ConnectionData () {

	}

	public ConnectionData (int moduleFrom, int moduleTo, int slotFrom, int slotTo) {
		this.moduleFrom = moduleFrom;
		this.moduleTo = moduleTo;
		this.slotFrom = slotFrom;
		this.slotTo = slotTo;
	}

	@Override
	public String toString () {
		return moduleFrom + " -> " + moduleTo + " : " + slotFrom + " -> " + slotTo;
	}
}
