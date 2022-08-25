package thestonedturtle.invocationscreenshot;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Invocation Screenshot"
)
public class InvocationScreenshotPlugin extends Plugin
{
	@Inject
	private InvocationScreenshotConfig config;

	@Provides
	InvocationScreenshotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(InvocationScreenshotConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		log.info("Invocation Screenshot started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Invocation Screenshot stopped!");
	}
}
