package com.rockbite.bongo.engine.events.commands;

import lombok.Data;
import net.mostlyoriginal.api.event.common.Event;

@Data
public class RawCommandEvent implements Event {

	private final String commandText;

	public RawCommandEvent (String commandText) {
		this.commandText = commandText;
	}
}
