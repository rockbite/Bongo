package com.rockbite.bongo.engine.components.render;

import com.artemis.Component;
import lombok.Data;

@Data
public class Animation extends Component {

	private String animationName;
	private float track;

}
