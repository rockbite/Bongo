package com.rockbite.bongo.engine.components.render;

import com.artemis.Component;
import com.badlogic.gdx.math.Vector3;
//import com.talosvfx.talos.runtime.ParticleEffectInstance;
import lombok.Data;

@Data
public class Particle extends Component {

	private Vector3 position = new Vector3();
//	private ParticleEffectInstance particleEffectInstance;

}
