package org.mastodon.trackmate.ui.wizard;

import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;

import java.util.Map;

import org.mastodon.detection.DetectionUtil;
import org.mastodon.linking.LinkingUtils;
import org.mastodon.plugin.MastodonPluginAppModel;
import org.mastodon.revised.mamut.MamutAppModel;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.TrackMate;
import org.scijava.plugin.Plugin;

import bdv.spimdata.SpimDataMinimal;

@Plugin( type = WizardLinkingPlugin.class )
public class WizardLinkingPlugin extends WizardPlugin
{
	private static final String ACTION_NAME = "[trackmate] run linking wizard";

	private static final String COMMAND_NAME = "Linking...";

	private static final String[] KEYSTROKES = new String[] { "not mapped" };

	private static final String MENU_PATH = "Plugins > TrackMate";

	public WizardLinkingPlugin()
	{
		super( ACTION_NAME, COMMAND_NAME, MENU_PATH, KEYSTROKES );
	}

	@Override
	public WizardSequence getWizardSequence( final MastodonPluginAppModel pluginAppModel, final Wizard wizard )
	{
		final MamutAppModel appModel = pluginAppModel.getAppModel();
		final WindowManager windowManager = pluginAppModel.getWindowManager();
		final SpimDataMinimal spimData = ( SpimDataMinimal ) appModel.getSharedBdvData().getSpimData();
		final int tmax = windowManager.getAppModel().getMaxTimepoint();
		final int tmin = windowManager.getAppModel().getMinTimepoint();

		final Map< String, Object > defaultLinkerSettings = LinkingUtils.getDefaultLAPSettingsMap();
		defaultLinkerSettings.put( KEY_MIN_TIMEPOINT, Integer.valueOf( tmin ) );
		defaultLinkerSettings.put( KEY_MAX_TIMEPOINT, Integer.valueOf( tmax ) );
		final Map< String, Object > defaultDetectorSettings = DetectionUtil.getDefaultDetectorSettingsMap();
		defaultDetectorSettings.put( KEY_MIN_TIMEPOINT, Integer.valueOf( tmin ) );
		defaultDetectorSettings.put( KEY_MAX_TIMEPOINT, Integer.valueOf( tmax ) );

		final Settings settings = new Settings()
				.spimData( spimData )
				.linkerSettings( defaultLinkerSettings )
				.detectorSettings( defaultDetectorSettings );
		final TrackMate trackmate = new TrackMate( settings, appModel.getModel() );
		getContext().inject( trackmate );

		return new LinkingSequence( trackmate, windowManager, wizard.getLogService().getPanel() );
	}
}
