package com.rockbite.bongo.engine.components.prefab;

import com.artemis.Component;
import com.rockbite.bongo.engine.prefab.Marshallable;
import lombok.Data;

@Data
public class Prefab extends Component implements Marshallable<Prefab> {

	private String identifier;

	@Override
	public void marshallFrom (Prefab other) {
		this.identifier = other.identifier;
	}
}
