package thestonedturtle.invocationscreenshot;

import com.google.inject.Provides;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.SpriteID;
import net.runelite.api.SpritePixels;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageCapture;
import net.runelite.client.util.ImageUploadStyle;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Invocation Screenshot"
)
public class InvocationScreenshotPlugin extends Plugin
{
	private static final int INVOCATION_GROUP_ID = 774;
	private static final int INVOCATION_TITLE_CHILD_ID = 3;
	private static final int INVOCATION_CONTAINER_CHILD_ID = 52;
	private static final int INVOCATION_REWARDS_BUTTON_CHILD_ID = 70;

	private static final int TOA_PARTY_WIDGET_SCRIPT_ID = 6617;

	private static final BufferedImage CAMERA_IMG;
	private static final int CAMERA_OVERRIDE_SPRITE_IDX = -420;
	private static final int CAMERA_HOVER_OVERRIDE_SPRITE_IDX = -421;
	static
	{
		CAMERA_IMG = ImageUtil.loadImageResource(InvocationScreenshotPlugin.class, "camera.png");
	}

	private static final int BUTTON_UNSELECTED_SPRITE_ID = 1040;

	@Inject
	private Client client;

	@Inject
	private InvocationScreenshotConfig config;

	@Inject
	private ClientThread clientThread;

	@Inject
	private SpriteManager spriteManager;

	@Inject
	private ImageCapture imageCapture;

	private Widget button = null;

	@Provides
	InvocationScreenshotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(InvocationScreenshotConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		clientThread.invokeLater(this::createButton);
		addCameraIconOverride();
	}

	@Override
	protected void shutDown() throws Exception
	{
		button = null;
		removeCameraIconOverride();
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired e)
	{
		if (e.getScriptId() != TOA_PARTY_WIDGET_SCRIPT_ID)
		{
			return;
		}

		createButton();
	}

	private void removeCameraIconOverride()
	{
		client.getWidgetSpriteCache().reset();
		client.getSpriteOverrides().remove(CAMERA_OVERRIDE_SPRITE_IDX);
		client.getSpriteOverrides().remove(CAMERA_HOVER_OVERRIDE_SPRITE_IDX);
	}

	private void addCameraIconOverride()
	{
		client.getWidgetSpriteCache().reset();
		// Add images to a sprite background so it works with resource packs
		spriteManager.getSpriteAsync(SpriteID.EQUIPMENT_SLOT_TILE, 0, (img) -> {
			final BufferedImage cameraImg = overlapImages(CAMERA_IMG, img);
			client.getSpriteOverrides().put(CAMERA_OVERRIDE_SPRITE_IDX, ImageUtil.getImageSpritePixels(cameraImg, client));

		});
		spriteManager.getSpriteAsync(SpriteID.EQUIPMENT_SLOT_SELECTED, 0, (img) -> {
			final BufferedImage cameraImg = overlapImages(CAMERA_IMG, img);
			client.getSpriteOverrides().put(CAMERA_HOVER_OVERRIDE_SPRITE_IDX, ImageUtil.getImageSpritePixels(cameraImg, client));
		});
	}

	private void createButton()
	{
		final Widget parent = client.getWidget(INVOCATION_GROUP_ID, INVOCATION_TITLE_CHILD_ID);
		if (parent == null)
		{
			return;
		}

		final Widget[] children = parent.getDynamicChildren();
		if (children == null || children.length == 0)
		{
			return;
		}

		for (Widget child : children)
		{
			// Button already exists
			if (child.equals(button))
			{
				return;
			}
		}
		addCameraIconOverride();

		button = parent.createChild(-1, WidgetType.GRAPHIC);
		button.setOriginalHeight(20);
		button.setOriginalWidth(20);
		button.setOriginalX(430);
		button.setOriginalY(8);
		button.setSpriteId(CAMERA_OVERRIDE_SPRITE_IDX);
		button.setAction(0, "Invocation Screenshot");
		button.setOnOpListener((JavaScriptCallback) (e) -> clientThread.invokeLater(this::screenshot));
		button.setHasListener(true);
		button.revalidate();

		button.setOnMouseOverListener((JavaScriptCallback) (e) ->  button.setSpriteId(CAMERA_HOVER_OVERRIDE_SPRITE_IDX));
		button.setOnMouseLeaveListener((JavaScriptCallback) (e) -> button.setSpriteId(CAMERA_OVERRIDE_SPRITE_IDX));
	}

	private void screenshot()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		client.getWidgetSpriteCache().reset();

		final Widget container = client.getWidget(INVOCATION_GROUP_ID, INVOCATION_CONTAINER_CHILD_ID);
		if (container == null)
		{
			return;
		}

		final Widget[] children = container.getDynamicChildren();
		if (children.length == 0)
		{
			return;
		}

		final Widget rewardButton = client.getWidget(INVOCATION_GROUP_ID, INVOCATION_REWARDS_BUTTON_CHILD_ID);

		int width = 287; // Hardcoded width of the interface so that it can be drawn correctly even when on another tab
		if (rewardButton != null && rewardButton.getDynamicChildren().length > 0 && rewardButton.getDynamicChildren()[0].getSpriteId() == BUTTON_UNSELECTED_SPRITE_ID)
		{
			// If the reward button isn't selected then don't use the hardcoded value regardless of the tab that's selected
			width = container.getWidth();
		}
		int height = children[0].getHeight() + children[0].getRelativeY();
		int y = 0;
		for (Widget invocation : children)
		{
			if (invocation.getRelativeY() > y)
			{
				y = invocation.getRelativeY() + 2;
				height = y + invocation.getHeight();
			}
		}

		final BufferedImage screenshot = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics graphics = screenshot.getGraphics();

		final BufferedImage background = getSprite(297);
		int x = screenshot.getWidth() / background.getWidth() + 1;
		y = screenshot.getHeight() / background.getHeight() + 1;
		for (int i = 0; i < x; i++)
		{
			for (int z = 0; z < y; z++)
			{
				graphics.drawImage(background, i * background.getWidth(), z * background.getHeight(), null);
			}
		}

		for (Widget invocation : children)
		{
			drawWidget(graphics, invocation, invocation.getRelativeX(), invocation.getRelativeY());
		}

		// Convert from ARGB to RGB so it can be stored on the clipboard
		BufferedImage out = toBufferedImageOfType(screenshot, BufferedImage.TYPE_INT_RGB);

		imageCapture.takeScreenshot(out, "invocationscreenshot", "invocations", true, ImageUploadStyle.CLIPBOARD);
		final String message = new ChatMessageBuilder()
			.append(ChatColorType.HIGHLIGHT)
			.append("A screenshot of your current invocations was saved and inserted into your clipboard!")
			.build();
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
	}

	private void drawWidget(Graphics graphics, Widget child, int x, int y)
	{
		if (child == null || child.getType() == 0)
		{
			return;
		}

		int width = child.getWidth();
		int height = child.getHeight();

		if (child.getSpriteId() > 0)
		{
			SpritePixels sp = getPixels(child.getSpriteId());
			assert sp != null;
			BufferedImage childImage = sp.toBufferedImage();

			if (width == childImage.getWidth() && height == childImage.getHeight())
			{
				drawAt(graphics, childImage, x, y);
			}
			else
			{
				drawScaled(graphics, childImage, x, y, width, height);
			}
		}
		else if (child.getType() == WidgetType.TEXT)
		{
			final String text = Text.removeTags(child.getText());
			Font font = FontManager.getRunescapeSmallFont();

			x = child.getRelativeX();
			y = child.getRelativeY();
			width = child.getWidth();
			height = child.getHeight();

			final Graphics textLayer = graphics.create(x, y, width, height);
			textLayer.setFont(font);

			int xPos = 0;
			int yPos = 0;

			int textWidth = textLayer.getFontMetrics().stringWidth(text);

			if (child.getXTextAlignment() == 1)
			{
				xPos = (width - textWidth) / 2 + 1;
			}

			if (child.getYTextAlignment() == 0)
			{
				yPos = font.getSize() - 3;
			}
			else if (child.getYTextAlignment() == 1)
			{
				yPos = (height + font.getSize()) / 2 - 1;
			}
			else if (child.getYTextAlignment() == 2)
			{
				yPos = height;
			}

			if (child.getTextShadowed())
			{
				textLayer.setColor(Color.BLACK);
				textLayer.drawString(text, xPos, yPos);
				xPos -= 1;
				yPos -= 1;
			}

			textLayer.setColor(new Color(child.getTextColor()));
			textLayer.drawString(text, xPos, yPos);
			textLayer.dispose();
		}
		else if (child.getType() == WidgetType.RECTANGLE)
		{
			// Rectangles are purposely not drawn as they are just for helping to layout other components and are not needed.
			return;
		}
	}

	@Nullable
	private SpritePixels getPixels(int archive)
	{
		if (config.useResourcePack())
		{
			SpritePixels pixels = client.getSpriteOverrides().get(archive);
			if (pixels != null)
			{
				return pixels;
			}
		}

		SpritePixels[] sp = client.getSprites(client.getIndexSprites(), archive, 0);
		assert sp != null;
		return sp[0];
	}

	private BufferedImage getSprite(int id)
	{
		SpritePixels sp = getPixels(id);
		assert sp != null;
		return sp.toBufferedImage();
	}

	private void drawScaled(Graphics graphics, BufferedImage image, int x, int y, int width, int height)
	{
		image = ImageUtil.resizeCanvas(image, width, height);
		graphics.drawImage(image, x, y, null);
	}

	private void drawAt(Graphics graphics, BufferedImage image, int x, int y)
	{
		graphics.drawImage(image, x, y, null);
	}


	private static BufferedImage overlapImages(final BufferedImage foreground, final BufferedImage background)
	{
		final int centeredX = background.getWidth() / 2 - foreground.getWidth() / 2;
		final int centeredY = background.getHeight() / 2 - foreground.getHeight() / 2;

		BufferedImage combined = new BufferedImage(background.getWidth(), background.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = combined.createGraphics();
		g2d.drawImage(background, 0, 0, null);

		g2d.drawImage(foreground, centeredX, centeredY, null);

		g2d.dispose();

		return combined;
	}

	private static BufferedImage toBufferedImageOfType(BufferedImage original, int type)
	{
		if (original == null || original.getType() == type)
		{
			return original;
		}

		BufferedImage out = new BufferedImage(original.getWidth(), original.getHeight(), type);
		Graphics2D g = out.createGraphics();
		g.setComposite(AlphaComposite.Src);
		g.drawImage(original, 0, 0, null);
		g.dispose();

		return out;
	}
}
