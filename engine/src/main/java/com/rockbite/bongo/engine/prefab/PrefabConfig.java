package com.rockbite.bongo.engine.prefab;

import com.artemis.Component;
import com.badlogic.gdx.utils.Array;
import com.rockbite.bongo.engine.components.prefab.Prefab;
import lombok.Data;

@Data
public class PrefabConfig {

	private Array<Component> components = new Array<>();

	public Prefab getPrefabComponent () {
		return getComponent(Prefab.class);
	}

	public boolean hasComponent (Class<? extends Component> clazz) {
		for (Component component : components) {
			if (component.getClass().equals(clazz)) {
				return true;
			}
		}
		return false;
	}

	public <T extends Component> T getComponent (Class<T> clazz) {
		for (Component component : components) {
			if (component.getClass().equals(clazz)) {
				return (T)component;
			}
		}
		return null;
	}

}
