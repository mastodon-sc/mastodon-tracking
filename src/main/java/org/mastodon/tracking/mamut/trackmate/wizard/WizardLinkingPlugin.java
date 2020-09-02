package org.mastodon.tracking.mamut.trackmate.wizard;

import static org.mastodon.tracking.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_MIN_TIMEPOINT;

import java.util.ArrayList;
import java.util.Map;

import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.WindowManager;
import org.mastodon.mamut.plugin.MamutPluginAppModel;
import org.mastodon.tracking.detection.DetectionUtil;
import org.mastodon.tracking.linking.LinkingUtils;
import org.mastodon.tracking.mamut.trackmate.Settings;
import org.mastodon.tracking.mamut.trackmate.TrackMate;
import org.scijava.plugin.Plugin;

import bdv.viewer.SourceAndConverter;

@Plugin( type = WizardLinkingPlugin.class )
public class WizardLinkingPlugin extends WizardPlugin
{
	private static final String ACTION_NAME = "run spot linking wizard";

	private static final String COMMAND_NAME = "Linking...";

	private static final String[] KEYSTROKES = new String[] { "not mapped" };

	private static final String MENU_PATH = "Plugins > Tracking";

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
	public WizardSequence getWizardSequence( final MamutPluginAppModel pluginAppModel, final Wizard wizard )
	{
		final MamutAppModel appModel = pluginAppModel.getAppModel();
		final WindowManager windowManager = pluginAppModel.getWindowManager();
		final int tmax = windowManager.getAppModel().getMaxTimepoint();
		final int tmin = windowManager.getAppModel().getMinTimepoint();

		final Map< String, Object > linkerSettings = settings.values.getLinkerSettings();
		linkerSettings.put( KEY_MIN_TIMEPOINT, Integer.valueOf( tmin ) );
		linkerSettings.put( KEY_MAX_TIMEPOINT, Integer.valueOf( tmax ) );
		final Map< String, Object > detectorSettings = settings.values.getDetectorSettings();
		detectorSettings.put( KEY_MIN_TIMEPOINT, Integer.valueOf( tmin ) );
		detectorSettings.put( KEY_MAX_TIMEPOINT, Integer.valueOf( tmax ) );

		final ArrayList< SourceAndConverter< ? > > sources = appModel.getSharedBdvData().getSources();
		settings.sources( sources );
		final TrackMate trackmate = new TrackMate( settings, appModel.getModel(), appModel.getSelectionModel() );
		getContext().inject( trackmate );
		trackmate.setLogger( wizard.getLogService() );
		trackmate.setStatusService( wizard.getLogService() );
		return new LinkingSequence( trackmate, windowManager, wizard.getLogService() );
	}
}
