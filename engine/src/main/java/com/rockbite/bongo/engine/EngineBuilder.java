package com.rockbite.bongo.engine;

import com.artemis.BaseSystem;
import com.artemis.SystemInvocationStrategy;
import com.artemis.World;
import com.artemis.WorldConfiguration;
import com.artemis.WorldConfigurationBuilder;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.Array;
import com.rockbite.bongo.engine.input.InputProvider;
import com.rockbite.bongo.engine.systems.GameLoopSystemInvocationStrategy;
import com.rockbite.bongo.engine.systems.render.EngineDebugEndSystem;
import com.rockbite.bongo.engine.systems.render.EngineDebugStartSystem;
import net.mostlyoriginal.api.SingletonPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EngineBuilder {

	private static Logger logger = LoggerFactory.getLogger(EngineBuilder.class);

	public static World buildWorld (BaseSystem[] userSystems) {
		return buildWorld(userSystems, new GameLoopSystemInvocationStrategy(16));
	}

	public static World buildWorld (BaseSystem[] userSystems, SystemInvocationStrategy invocationStrategy) {

		Array<BaseSystem> prefixSystems = new Array<>(BaseSystem.class);
		Array<BaseSystem> suffixSystems = new Array<>(BaseSystem.class);
		Array<BaseSystem> finalSystemsList = new Array<>(BaseSystem.class);

		//Do start
		if (Bongo.DEBUG) {

			//first
			prefixSystems.add(new EngineDebugStartSystem());

			logger.info("Running bongo in debug mode");
		} else {
			logger.info("Running bongo in release mode");
		}

		//Do End
		if (Bongo.DEBUG) {

			//Last of end
			suffixSystems.add(new EngineDebugEndSystem());
		} else {
		}


		finalSystemsList.addAll(prefixSystems);
		finalSystemsList.addAll(userSystems);
		finalSystemsList.addAll(suffixSystems);

		WorldConfiguration basicWorldConfig = new WorldConfigurationBuilder().
				dependsOn(SingletonPlugin.class).with(
				finalSystemsList.toArray(BaseSystem.class)
			)
			.register(invocationStrategy).build();


		final World world = new World(basicWorldConfig);

		if (Bongo.DEBUG) {
			world.process();
			world.getSystem(EngineDebugStartSystem.class).postInit();
		}

		//Setup input

		Array<InputProcessor> inputProcessors = new Array<>(InputProcessor.class);
		for (BaseSystem system : world.getSystems()) {
			if (system instanceof InputProvider) {
				inputProcessors.add(((InputProvider)system).getInputProcessor());
			}
		}

		Gdx.input.setInputProcessor(new InputMultiplexer(inputProcessors.toArray(InputProcessor.class)));

		return world;
	}
}
