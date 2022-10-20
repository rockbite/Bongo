package com.rockbite.bongo.engine.systems;

import com.artemis.BaseSystem;
import com.artemis.Component;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.EntityEdit;
import com.artemis.EntitySubscription;
import com.artemis.annotations.All;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.ObjectMap;
import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import com.rockbite.bongo.engine.components.prefab.Prefab;
import com.rockbite.bongo.engine.events.internal.CustomEventSystem;
import com.rockbite.bongo.engine.events.prefab.PrefabUpdatedEvent;
import com.rockbite.bongo.engine.fileutil.AutoReloadingFileHandle;
import com.rockbite.bongo.engine.fileutil.ReloadUtils;
import com.rockbite.bongo.engine.gltf.scene.SceneModelInstance;
import com.rockbite.bongo.engine.prefab.Marshallable;
import com.rockbite.bongo.engine.prefab.PrefabConfig;
import com.rockbite.bongo.engine.prefab.PrefabReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class PrefabSystem extends BaseSystem {

	private static Logger logger = LoggerFactory.getLogger(PrefabSystem.class);

	//MAPPERS
	private ComponentMapper<Prefab> prefabComponentMapper;

	//SUBSCRIPTIONS
	@All(Prefab.class)
	private EntitySubscription prefabSubscription;

	private ObjectMap<String, PrefabConfig> prefabIdentifierMap = new ObjectMap<>();

	private Array<String> prefabFiles = new Array<>();

	private PrefabReader prefabReader = new PrefabReader();

	public PrefabSystem () {
		registerComponentShortName("prefab", Prefab.class);
		registerComponentShortName("model", SceneModelInstance.class);
	}

	/**
	 * Process the system.
	 */
	@Override
	protected void processSystem () {

	}

	public void registerComponentShortName (String componentShortName, Class<? extends Component> clazz) {
		prefabReader.objectMapper.put(componentShortName, clazz);
	}


	public void registerForPrefabExtraction (FileHandle fileHandle) {
		if (!fileHandle.exists()) return;

		if (fileHandle.isDirectory()) {
			if (fileHandle.type() == Files.FileType.Internal) {
				logger.error("Trying to use list on an internal file handle");
			}
			for (FileHandle handle : fileHandle.list()) {
				registerForPrefabExtraction(handle);
			}
		} else {

			if (!prefabFiles.contains(fileHandle.path(), false)) {
				AutoReloadingFileHandle autoReloadingFileHandle = new AutoReloadingFileHandle(fileHandle, new ReloadUtils.AutoReloadingListener() {
					@Override
					public void onAutoReloadFileChanged () {
						extractPrefabConfig(fileHandle);
					}
				});
				prefabFiles.add(fileHandle.path());
				extractPrefabConfig(fileHandle);
			} else {
				logger.warn("Ignoring {} as it has already been registered", fileHandle);
			}
		}
	}


	private void extractPrefabConfig (FileHandle fileHandle) {
		try {
			final Toml config = new Toml().read(fileHandle.readString());
			final List<Toml> prefabs = config.getTables("prefab");

			for (Toml prefabConfig : prefabs) {

				PrefabConfig preConfig = new PrefabConfig();

				for (Map.Entry<String, Object> stringObjectEntry : prefabConfig.entrySet()) {
					if (stringObjectEntry.getValue() instanceof Toml) {
						String componentName = stringObjectEntry.getKey();
						Toml data = (Toml)stringObjectEntry.getValue();
						Object artemisComponent = null;
						try {
							 artemisComponent = prefabReader.topLevelParseAndRead(componentName, data);
						} catch (PrefabReader.PrefabException pe) {
							final Toml prefab = prefabConfig.getTable("prefab");
							String identifier = null;
							if (prefab != null && prefab.getString("identifier") != null) {
								identifier = prefab.getString("identifier");
							}
							logger.error("Prefab reader problem with prefab {} -> {}", identifier, pe.getMessage());
							continue;
						}
						if (artemisComponent == null) continue; //Handled

						if (artemisComponent instanceof Component) {
							preConfig.getComponents().add((Component)artemisComponent);
						} else {
							logger.error("Object read from config is not a Component {}", artemisComponent);
						}
					}
				}

				final Prefab prefabComponent = preConfig.getPrefabComponent();
				if (prefabComponent == null) {
					TomlWriter writer = new TomlWriter();
					logger.error("No prefab component found for config {}", writer.write(prefabConfig));
				} else {
					prefabIdentifierMap.put(prefabComponent.getIdentifier(), preConfig);


					for (int i = 0; i < prefabSubscription.getEntities().size(); i++) {
						final int entityID = prefabSubscription.getEntities().get(i);
						final Prefab prefab = prefabComponentMapper.get(entityID);

						final String identifier = preConfig.getPrefabComponent().getIdentifier();
						if (prefab.getIdentifier().equals(identifier)) {
							updateEntityFromPrefab(entityID, preConfig);
						}
					}

					final CustomEventSystem system = world.getSystem(CustomEventSystem.class);
					final PrefabUpdatedEvent event = new PrefabUpdatedEvent();
					event.setPrefabConfig(preConfig);
					system.dispatch(event);
				}

			}
		} catch (IllegalStateException e) {
			logger.error("invalid toml {}", fileHandle, e);
		}
	}

	private void updateEntityFromPrefab (int entityID, PrefabConfig prefabConfig) {
		final Entity entity = world.getEntity(entityID);
		final EntityEdit edit = entity.edit();

		if (prefabConfig == null) {
			logger.error("No prefab found");
		}

		for (Component configComponent : prefabConfig.getComponents()) {
			Component componentToEdit;
			if ((componentToEdit = entity.getComponent(configComponent.getClass())) == null) {
				componentToEdit = edit.create(configComponent.getClass());
			}
			if (configComponent instanceof Marshallable && componentToEdit instanceof Marshallable) {
				Marshallable<Component> newMarshal = (Marshallable<Component>)componentToEdit;
				newMarshal.marshallFrom(configComponent);
			}
		}
	}

	private Entity createEntityFromPrefabConfig (PrefabConfig prefabConfig) {
		Entity entity = world.createEntity();

		updateEntityFromPrefab(entity.getId(), prefabConfig);

		return entity;
	}

	public PrefabConfig getConfigForIdentifier (String identifier) {
		final PrefabConfig prefabConfig = prefabIdentifierMap.get(identifier);
		if (prefabConfig == null) {
			logger.warn("No prefab found for identifier {}", identifier);

			logger.warn("Available prefabs => ");

			for (String key : prefabIdentifierMap.keys()) {
				logger.warn("\t\t" + key);
			}

			return null;
		}
		return prefabConfig;
	}

	public @Null Entity createEntityFromPrefabIdentifierOrNull (String identifier) {
		final PrefabConfig configForIdentifier = getConfigForIdentifier(identifier);
		if (configForIdentifier == null) {
			return null;
		}
		return createEntityFromPrefabConfig(configForIdentifier);
	}



}
