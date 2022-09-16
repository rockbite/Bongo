package com.rockbite.bongo.engine.systems.assets;

import com.artemis.BaseSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.rockbite.bongo.engine.events.asset.AssetsEndLoadEvent;
import lombok.Getter;
import net.mostlyoriginal.api.event.common.EventSystem;

import static com.rockbite.bongo.engine.systems.assets.AssetSystem.FontSize.*;

public class AssetSystem extends BaseSystem {

	private AssetManager assetManager;

	@Getter
	private TextureAtlas gameAtlas;
	private Skin skin;

	private PixmapPacker packer;

	public Skin getSkin () {
		return skin;
	}

	public enum FontSize {
		H1(0.05f, "font/Questrian.otf", 2, false),
		H2(0.04f, "font/Questrian.otf", 2, false),
		P1(0.025f, "font/Questrian.otf", 2, false),
		P2(0.018f, "font/Questrian.otf", 2, false),
		P3(0.015f, "font/Questrian.otf", 1, false),
		CONSOLE(0.0325f, "font/Questrian.otf", 0, true);

		public String path;
		private float pixelPercent;
		private final int outline;
		private final boolean mono;

		FontSize (float pixelPercent, String path, int outline, boolean mono) {
			this.pixelPercent = pixelPercent;
			this.path = path;
			this.outline = outline;
			this.mono = mono;
		}
	}

	public AssetSystem () {
		assetManager = new AssetManager();
		assetManager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(assetManager.getFileHandleResolver()));
		assetManager.setLoader(BitmapFont.class, new FreetypeFontLoader(assetManager.getFileHandleResolver()));

		skin = new Skin();
	}

	/**
	 * Process the system.
	 */
	@Override
	protected void processSystem () {

	}


	public void startLoad () {
		assetManager.load("gameassets/gameatlas.atlas", TextureAtlas.class);

		packer = new PixmapPacker(2048, 2048, Pixmap.Format.RGBA8888, 2, true, new PixmapPacker.SkylineStrategy());

		for (FontSize value : FontSize.values()) {
			genFont(packer, value);
		}


	}

	private void genFont (PixmapPacker packer, FontSize size) {
		FreetypeFontLoader.FreeTypeFontLoaderParameter param = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
		param.fontFileName = size.path;
		param.fontParameters.size = (int)(size.pixelPercent * Gdx.graphics.getHeight());
		param.fontParameters.borderColor = Color.BLACK;
		param.fontParameters.color = Color.WHITE;
		param.fontParameters.packer = packer;
		param.fontParameters.mono = size.mono;
		param.fontParameters.borderWidth = (size.outline * ((float)Gdx.graphics.getHeight()/1080f));
		param.fontParameters.magFilter = Texture.TextureFilter.Nearest;
		param.fontParameters.minFilter = Texture.TextureFilter.Nearest;
		param.fontParameters.hinting = FreeTypeFontGenerator.Hinting.AutoFull;

		assetManager.load(size + "", BitmapFont.class, param);
	}

	public void blockUntilLoaded () {
		assetManager.finishLoading();
	}

	public void endLoad () {
		gameAtlas = assetManager.get("gameassets/gameatlas.atlas", TextureAtlas.class);


		for (FontSize value : FontSize.values()) {
			final BitmapFont font = assetManager.get(value + "", BitmapFont.class);
			font.setUseIntegerPositions(true);
			if (value.mono) {
				font.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
				font.setFixedWidthGlyphs("0123456789[]:");
			}
			skin.add(value.toString().toLowerCase(), font);
		}

		skin.addRegions(gameAtlas);
		addStyles();

		final EventSystem eventSystem = world.getSystem(EventSystem.class);
		eventSystem.dispatch(new AssetsEndLoadEvent());
	}

	private void addStyles () {
		labelStyles();
		textFieldStyles();
		textButtonStyles();
	}

	private void textButtonStyles () {
		for (FontSize value : FontSize.values()) {
			{
				TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
				textButtonStyle.font = getFont(value);
				textButtonStyle.down = skin.newDrawable("white", 1f, 1f, 1f, 1f);
				textButtonStyle.up = skin.newDrawable("white", 0.8f, 0.8f, 0.8f, 1f);
				textButtonStyle.disabled = skin.newDrawable("white", 0.4f, 0.2f, 0.2f, 1f);
				skin.add(value.name().toLowerCase(), textButtonStyle);
			}
			{
				TextButton.TextButtonStyle textButtonStyleWithCheck = new TextButton.TextButtonStyle();
				textButtonStyleWithCheck.font = getFont(value);
				textButtonStyleWithCheck.down = skin.newDrawable("white", 1f, 1f, 1f, 1f);
				textButtonStyleWithCheck.checked = skin.newDrawable("white", 1f, 1f, 1f, 1f);
				textButtonStyleWithCheck.up = skin.newDrawable("white", 0.8f, 0.8f, 0.8f, 1f);
				textButtonStyleWithCheck.disabled = skin.newDrawable("white", 0.4f, 0.2f, 0.2f, 1f);
				skin.add(value.name().toLowerCase() + "-checked", textButtonStyleWithCheck);
			}
		}
	}

	private void labelStyles () {
		for (FontSize value : FontSize.values()) {
			Label.LabelStyle labelStyle = new Label.LabelStyle();
			labelStyle.font = getFont(value);
			skin.add(value.name().toLowerCase(), labelStyle);
		}

		Label.LabelStyle console = new Label.LabelStyle();
		console.font = getFont(CONSOLE);
		skin.add("console", console);

		Label.LabelStyle consoleTime = new Label.LabelStyle();
		consoleTime.font = getFont(CONSOLE);
		consoleTime.fontColor = Color.valueOf("00df3f");
		skin.add("console-time", consoleTime);
	}

	private void textFieldStyles () {
		TextField.TextFieldStyle consoleStyle = new TextField.TextFieldStyle();
		consoleStyle.font = getFont(CONSOLE);
		consoleStyle.fontColor = Color.WHITE;
		skin.add("console", consoleStyle);
	}

	public BitmapFont getFont (FontSize fontSize) {
		return skin.getFont(fontSize.toString().toLowerCase());
	}

	@Override
	protected void dispose () {
		super.dispose();
		gameAtlas.dispose();
	}

	public TextureAtlas.AtlasSprite atlasSprite (String region) {
		return new TextureAtlas.AtlasSprite(gameAtlas.findRegion(region));
	}
}
