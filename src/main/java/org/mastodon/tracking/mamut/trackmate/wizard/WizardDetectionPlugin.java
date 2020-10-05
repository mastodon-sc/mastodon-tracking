package org.mastodon.tracking.mamut.trackmate.wizard;

import java.util.ArrayList;

import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.WindowManager;
import org.mastodon.mamut.plugin.MamutPluginAppModel;
import org.mastodon.tracking.mamut.trackmate.Settings;
import org.mastodon.tracking.mamut.trackmate.TrackMate;
import org.mastodon.ui.keymap.CommandDescriptionProvider;
import org.mastodon.ui.keymap.CommandDescriptions;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.scijava.plugin.Plugin;

import bdv.viewer.SourceAndConverter;

@Plugin( type = WizardDetectionPlugin.class )
public class WizardDetectionPlugin extends WizardPlugin
{
	private static final String RUN_WIZARD = "run spot detection wizard";

	private static final String[] RUN_WIZARD_KEYS = new String[] { "not mapped" };

	private static final Settings settings = new Settings();

	public WizardDetectionPlugin()
	{
		super( RUN_WIZARD, "Detection...", "Plugins > Tracking", RUN_WIZARD_KEYS );
	}

	@Override
	public WizardSequence getWizardSequence( final MamutPluginAppModel pluginAppModel, final Wizard wizard )
	{
		final MamutAppModel appModel = pluginAppModel.getAppModel();
		final WindowManager windowManager = pluginAppModel.getWindowManager();
		/*
		 * TODO Resolve later: SharedBdvData has AbstractSpimData<?>, we need
		 * SpimDataMinimal here. Using AbstractSpimData<?> everywhere should
		 * work, but requires additional casting when instantiation ops.
		 */
		final ArrayList< SourceAndConverter< ? > > sources = appModel.getSharedBdvData().getSources();
		settings.sources( sources );
		final TrackMate trackmate = new TrackMate( settings, appModel.getModel(), appModel.getSelectionModel() );
		getContext().inject( trackmate );
		trackmate.setLogger( wizard.getLogService() );
		trackmate.setStatusService( wizard.getLogService() );
		return new DetectionSequence( trackmate, windowManager, wizard.getLogService() );
	}

	/*
	 * Command descriptions for the detection wizard.
	 */
	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigContexts.MASTODON );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( RUN_WIZARD, RUN_WIZARD_KEYS, "Launch the detection wizard." );
		}
	}
}
