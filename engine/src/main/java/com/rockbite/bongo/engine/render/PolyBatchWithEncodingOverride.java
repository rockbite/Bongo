package com.rockbite.bongo.engine.render;

import com.badlogic.gdx.graphics.g2d.PolygonBatch;

public interface PolyBatchWithEncodingOverride extends PolygonBatch {

	void setCustomEncodingColour (float r, float g, float b, float a);
	void setUsingCustomColourEncoding (boolean usingCustomEncoding);

	void setCustomInfo (float customInfo);

    void setIgnoreBlendModeChanges (boolean shouldIgnore);
}
