package com.rockbite.bongo.engine.console;

import com.artemis.World;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.utils.Array;
import com.rockbite.bongo.engine.events.commands.RawCommandEvent;
import com.rockbite.bongo.engine.events.internal.CustomEventSystem;
import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Console extends Table {

	private static final Logger logger = LoggerFactory.getLogger(Console.class);

	private World world;

	private ScrollPane scrollPane;
	private VerticalGroup history;

	private Array<String> commandHistory = new Array<>();
	private int commandHistoryIndex = -1;

	@Getter
	private TextField textField;

	public Console (World world, Skin skin) {
		super(skin);
		this.world = world;

		history = new VerticalGroup();
		history.top().left().grow();
		scrollPane = new ScrollPane(history);

		textField = new TextField("", skin, "console");
		textField.setMessageText("enter command");
		textField.setTextFieldFilter(new TextField.TextFieldFilter() {
			@Override
			public boolean acceptChar (TextField textField, char c) {
				if (c == '`') {
					return false;
				}
				return true;
			}
		});

		Table textFieldTable = new Table();
		textFieldTable.setBackground(skin.newDrawable("white", 0.1f, 0.1f, 0.1f, 0.9f));
		textFieldTable.defaults().pad(5);
		textFieldTable.add(textField).growX();

		add(textFieldTable).growX();
		row();
		add(scrollPane).grow();

		setBackground(skin.newDrawable("white", 0, 0, 0, 0.9f));

		textFieldTable.addListener(new InputListener() {
			@Override
			public boolean keyDown (InputEvent event, int keycode) {
				if (commandHistory.size > 0) {
					if (keycode == Input.Keys.UP || keycode == Input.Keys.DOWN) {
						if (keycode == Input.Keys.UP) {
							commandHistoryIndex--;
						} else if (keycode == Input.Keys.DOWN) {
							commandHistoryIndex++;
						}
						commandHistoryIndex = MathUtils.clamp(commandHistoryIndex, 0, commandHistory.size - 1);

						textField.setText(commandHistory.get(commandHistoryIndex));
					}
				}

				if (keycode == Input.Keys.ENTER) {
					final String inputText = textField.getText();

					if (!inputText.isEmpty()) {
						handleInputMessage(inputText);
					}

					CustomEventSystem customEventSystem = world.getSystem(CustomEventSystem.class);
					RawCommandEvent rawCommandEvent = customEventSystem.obtainEvent(RawCommandEvent.class);
					rawCommandEvent.setCommandText(inputText);
					customEventSystem.dispatch(rawCommandEvent);



					textField.setText("");
					return true;
				}
				return super.keyDown(event, keycode);
			}
		});
	}

	private void handleInputMessage (String inputText) {
		logger.info("Handling message {}", inputText);


		if (inputText.equals("clear")) {
			history.clearChildren();
			return;
		}


		addMessageToLog(inputText);
		commandHistory.add(inputText);
		commandHistoryIndex = commandHistory.size - 1;
	}

	private void addMessageToLog (String payloadMessage) {
		Table entry = new Table();

		entry.top();
		entry.defaults().top();

		entry.padLeft(1);

		//gwt friendly timedate
		final Date date = new Date();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.UK);

		Label time = new Label("[" + simpleDateFormat.format(date) + "]:", getSkin(), "console-time");

		Table textTable = new Table();

		Label content = new Label(payloadMessage, getSkin(), "console");
		textTable.add(content).growX();

		entry.add(time);
		entry.add(textTable).growX();

		history.addActorAt(0, entry);

	}

}
