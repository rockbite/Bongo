package com.rockbite.bongo.engine.console;

import com.artemis.World;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.rockbite.bongo.engine.events.commands.RawCommandEvent;
import lombok.Getter;
import net.mostlyoriginal.api.event.common.EventSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class Console extends Table {

	private static final Logger logger = LoggerFactory.getLogger(Console.class);

	private World world;

	private ScrollPane scrollPane;
	private VerticalGroup history;

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
				if (keycode == Input.Keys.ENTER) {
					final String inputText = textField.getText();

					if (!inputText.isEmpty()) {
						handleInputMessage(inputText);
					}

					RawCommandEvent rawCommandEvent = new RawCommandEvent(inputText);
					world.getSystem(EventSystem.class).dispatch(rawCommandEvent);



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
	}

	private void addMessageToLog (String payloadMessage) {
		Table entry = new Table();

		entry.top();
		entry.defaults().top();

		entry.padLeft(1);

		//gwt friendly timedate
		final Date date = new Date();
		final int day = date.getDay();
		final int hours = date.getHours();
		final int minutes = date.getMinutes();
		final int seconds = date.getSeconds();

		String getFriendlyBasicTime = hours + ":" + minutes + ":" + seconds;

		Label time = new Label("[" + getFriendlyBasicTime + "]:", getSkin(), "console-time");

		Table textTable = new Table();

		Label content = new Label(payloadMessage, getSkin(), "console");
		textTable.add(content).growX();

		entry.add(time).width(70);
		entry.add(textTable).growX();

		history.addActorAt(0, entry);

	}

}
