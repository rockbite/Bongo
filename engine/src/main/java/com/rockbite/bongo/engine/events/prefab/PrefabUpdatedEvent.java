package com.rockbite.bongo.engine.events.prefab;

import com.rockbite.bongo.engine.prefab.PrefabConfig;
import lombok.Data;
import net.mostlyoriginal.api.event.common.Event;


@Data
public class PrefabUpdatedEvent implements Event {

	private PrefabConfig prefabConfig;

}
