package com.rockbite.bongo.engine.prefab;

import com.artemis.Component;

public interface Marshallable<T extends Component> {

	void marshallFrom (T other);

}
