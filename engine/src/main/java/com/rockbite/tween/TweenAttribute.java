package com.rockbite.tween;

import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import lombok.Data;

@Data
public class TweenAttribute {

	private String displayName;
	private TextField.TextFieldFilter filter;

	TweenAttribute (String displayName) {
		this.displayName = displayName;
	}

	public static TweenAttribute create (String displayName) {
		return new TweenAttribute(displayName);
	}

}
