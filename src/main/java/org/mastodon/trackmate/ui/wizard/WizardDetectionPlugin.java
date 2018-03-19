package org.mastodon.trackmate.ui.wizard;

import org.mastodon.plugin.MastodonPluginAppModel;
import org.mastodon.revised.mamut.MamutAppModel;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.TrackMate;
import org.scijava.plugin.Plugin;

import bdv.spimdata.SpimDataMinimal;

@Plugin( type = WizardDetectionPlugin.class )
public class WizardDetectionPlugin extends WizardPlugin
{
	private static final String RUN_WIZARD = "[trackmate] run detection wizard";

	private static final String[] RUN_WIZARD_KEYS = new String[] { "not mapped" };

	public WizardDetectionPlugin()
	{
		super( RUN_WIZARD, "Detection...", "Plugins > TrackMate", RUN_WIZARD_KEYS );
	}

	@Override
	public WizardSequence getWizardSequence( final MastodonPluginAppModel pluginAppModel, final Wizard wizard )
	{
		final MamutAppModel appModel = pluginAppModel.getAppModel();
		final WindowManager windowManager = pluginAppModel.getWindowManager();
		/*
		 * TODO Resolve later: SharedBdvData has AbstractSpimData<?>, we need
		 * SpimDataMinimal here. Using AbstractSpimData<?> everywhere should
		 * work, but requires additional casting when instantiation ops.
		 */
		final SpimDataMinimal spimData = ( SpimDataMinimal ) appModel.getSharedBdvData().getSpimData();
		final Settings settings = new Settings().spimData( spimData );
		final TrackMate trackmate = new TrackMate( settings, appModel.getModel() );
		getContext().inject( trackmate );

		return new DetectionSequence( trackmate, windowManager, wizard.getLogService().getPanel() );
	}
}
