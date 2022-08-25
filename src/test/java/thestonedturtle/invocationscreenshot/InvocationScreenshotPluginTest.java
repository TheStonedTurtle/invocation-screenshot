package thestonedturtle.invocationscreenshot;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class InvocationScreenshotPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(InvocationScreenshotPlugin.class);
		RuneLite.main(args);
	}
}