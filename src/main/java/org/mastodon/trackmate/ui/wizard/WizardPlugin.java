package org.mastodon.trackmate.ui.wizard;

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.menu;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.UIManager;

import org.mastodon.app.ui.ViewMenuBuilder;
import org.mastodon.plugin.MastodonPlugin;
import org.mastodon.plugin.MastodonPluginAppModel;
import org.mastodon.revised.mamut.MamutAppModel;
import org.mastodon.revised.mamut.MamutProject;
import org.mastodon.revised.mamut.MamutProjectIO;
import org.mastodon.revised.mamut.Mastodon;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.TrackMate;
import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.RunnableAction;

import bdv.spimdata.SpimDataMinimal;

@Plugin( type = WizardPlugin.class )
public class WizardPlugin extends AbstractContextual implements MastodonPlugin
{
	private static final String RUN_WIZARD = "[trackmate] run wizard";

	private static final String[] RUN_WIZARD_KEYS = new String[] { "not mapped" };

	private static Map< String, String > menuTexts = new HashMap<>();

	static
	{
		menuTexts.put( RUN_WIZARD, "Wizard..." );
	}

	private final AbstractNamedAction runWizardAction;

	private MastodonPluginAppModel pluginAppModel;

	public WizardPlugin()
	{
		runWizardAction = new RunnableAction( RUN_WIZARD, this::runWizard );
		updateEnabledActions();
	}

	@Override
	public void setAppModel( final MastodonPluginAppModel model )
	{
		this.pluginAppModel = model;
		updateEnabledActions();
	}

	@Override
	public List< ViewMenuBuilder.MenuItem > getMenuItems()
	{
		return Arrays.asList(
				menu( "Plugins",
						menu( "TrackMate",
								item( RUN_WIZARD ) ) ) );
	}

	@Override
	public Map< String, String > getMenuTexts()
	{
		return menuTexts;
	}

	@Override
	public void installGlobalActions( final Actions actions )
	{
		actions.namedAction( runWizardAction, RUN_WIZARD_KEYS );
	}

	private void updateEnabledActions()
	{
		final MamutAppModel appModel = ( pluginAppModel == null ) ? null : pluginAppModel.getAppModel();
		runWizardAction.setEnabled( appModel != null );
	}

	private void runWizard()
	{
		if ( pluginAppModel != null )
		{
			final MamutAppModel appModel = pluginAppModel.getAppModel();
			final WindowManager windowManager = pluginAppModel.getWindowManager();

			/*
			 * TODO Resolve later: SharedBdvData has AbstractSpimData<?>, we
			 * need SpimDataMinimal here. Using AbstractSpimData<?> everywhere
			 * should work, but requires additional casting when instantiation
			 * ops.
			 */
			final SpimDataMinimal spimData = ( SpimDataMinimal ) appModel.getSharedBdvData().getSpimData();

			final Settings settings = new Settings().spimData( spimData );

			final TrackMate trackmate = new TrackMate( settings, appModel.getModel() );
			getContext().inject( trackmate );

			final Wizard wizard = new Wizard( trackmate, windowManager );
			getContext().inject( wizard );
			wizard.show();
		}
	}

	public static void main( final String[] args ) throws Exception
	{
		Locale.setDefault( Locale.US );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );

		final Mastodon mastodon = new Mastodon();
		new Context().inject( mastodon );
		mastodon.run();

		final MamutProject project = new MamutProjectIO().load( "/Users/pietzsch/Desktop/Mastodon/testdata/MaMut_Parhyale_demo" );
		mastodon.openProject( project );
	}
}
