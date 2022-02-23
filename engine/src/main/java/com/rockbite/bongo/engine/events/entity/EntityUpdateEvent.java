package com.rockbite.bongo.engine.events.entity;

import lombok.Data;
import net.mostlyoriginal.api.event.common.Event;

@Data
public class EntityUpdateEvent implements Event {

	private int entityID;

	public EntityUpdateEvent (int entityID) {
		this.entityID = entityID;
	}

}
