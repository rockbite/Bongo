package com.rockbite.bongo.engine.components.render;

import com.artemis.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector;
import com.badlogic.gdx.math.Vector3;
import lombok.Data;

@Data
public class PointLight extends Component {

	private float r,g,b,strength;
	private Vector3 worldPosition = new Vector3();

	public void setRadiance (float r, float g, float b, float strength) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.strength = strength;
	}
}
