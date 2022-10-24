package com.rockbite.bongo.engine.events.render;

import lombok.Data;
import net.mostlyoriginal.api.event.common.Event;

@Data
public class WindowResizeEvent implements Event {
	private int width;
	private int height;


}
