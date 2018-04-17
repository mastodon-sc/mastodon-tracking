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

	private final static Settings settings = new Settings();
	static
	{
		final Map< String, Object > defaultLinkerSettings = LinkingUtils.getDefaultLAPSettingsMap();
		final Map< String, Object > defaultDetectorSettings = DetectionUtil.getDefaultDetectorSettingsMap();
		settings.detectorSettings( defaultDetectorSettings )
				.linkerSettings( defaultLinkerSettings );
	}

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

		final Map< String, Object > linkerSettings = settings.values.getLinkerSettings();
		linkerSettings.put( KEY_MIN_TIMEPOINT, Integer.valueOf( tmin ) );
		linkerSettings.put( KEY_MAX_TIMEPOINT, Integer.valueOf( tmax ) );
		final Map< String, Object > detectorSettings = settings.values.getDetectorSettings();
		detectorSettings.put( KEY_MIN_TIMEPOINT, Integer.valueOf( tmin ) );
		detectorSettings.put( KEY_MAX_TIMEPOINT, Integer.valueOf( tmax ) );

		settings.spimData( spimData );
		final TrackMate trackmate = new TrackMate( settings, appModel.getModel(), appModel.getSelectionModel() );
		getContext().inject( trackmate );
		trackmate.setLogger( wizard.getLogService() );
		trackmate.setStatusService( wizard.getLogService() );
		return new LinkingSequence( trackmate, windowManager, wizard.getLogService() );
	}
}
