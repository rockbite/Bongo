package com.rockbite.bongo.engine.events.commands;

import lombok.Data;
import lombok.Setter;
import net.mostlyoriginal.api.event.common.Event;

@Data
public class RawCommandEvent implements Event {


	private String commandText;

}
