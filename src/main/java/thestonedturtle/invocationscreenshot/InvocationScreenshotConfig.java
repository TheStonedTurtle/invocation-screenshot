package thestonedturtle.invocationscreenshot;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("invocationscreenshot")
public interface InvocationScreenshotConfig extends Config
{
	@ConfigItem(
		keyName = "useResourcePack",
		name = "Use Resource Pack",
		description = "Use Resource Pack Theme for screenshot"
	)
	default boolean useResourcePack()
	{
		return true;
	}
}
