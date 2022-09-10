package com.rockbite.bongo.engine.systems;

import com.artemis.BaseSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.rockbite.bongo.engine.console.Console;
import com.rockbite.bongo.engine.events.asset.AssetsEndLoadEvent;
import com.rockbite.bongo.engine.events.render.WindowResizeEvent;
import com.rockbite.bongo.engine.input.InputProvider;
import com.rockbite.bongo.engine.render.PolygonSpriteBatchMultiTextureMULTIBIND;
import com.rockbite.bongo.engine.systems.assets.AssetSystem;
import lombok.Getter;
import net.mostlyoriginal.api.event.common.Subscribe;

public class InGameConsoleSystem extends BaseSystem implements InputProvider {

	@Getter
	private Stage stage;
	private Table consoleTable;
	private Console console;

	@Override
	protected void initialize () {
		super.initialize();
		stage = new Stage(new ScreenViewport(), new PolygonSpriteBatchMultiTextureMULTIBIND());
	}

	@Subscribe
	public void onAssetLoaded (AssetsEndLoadEvent assetsEndLoadEvent) {

		consoleTable = new Table();
		consoleTable.setFillParent(true);
		stage.addActor(consoleTable);

		console = new Console(world, world.getSystem(AssetSystem.class).getSkin());

		stage.addListener(new InputListener() {
			@Override
			public boolean keyTyped (InputEvent event, char character) {
				if (character == '`') {
					event.stop();
					event.cancel();
					toggleConsole();
					return true;
				}
				return super.keyTyped(event, character);
			}
		});


	}

	private void toggleConsole () {
		if (console.getParent() != null) {
			console.remove();
			stage.setKeyboardFocus(null);
		} else {
			consoleTable.clearChildren();
			consoleTable.add(console).grow();
			stage.setKeyboardFocus(console.getTextField());
		}
	}

	/**
	 * Process the system.
	 */
	@Override
	protected void processSystem () {
		stage.act();
		stage.draw();
	}

	@Override
	protected void dispose () {
		super.dispose();
		stage.dispose();
	}

	@Subscribe
	public void windowResizeEvent (WindowResizeEvent windowResizeEvent) {
		stage.getViewport().update(windowResizeEvent.getWidth(), windowResizeEvent.getHeight(), true);
	}

	@Override
	public InputProcessor getInputProcessor () {
		return stage;
	}
}
