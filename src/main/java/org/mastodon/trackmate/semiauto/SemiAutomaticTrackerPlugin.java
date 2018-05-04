package org.mastodon.trackmate.semiauto;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.WindowConstants;

import org.mastodon.app.ui.ViewMenuBuilder.MenuItem;
import org.mastodon.app.ui.settings.SettingsPanel;
import org.mastodon.collection.RefCollections;
import org.mastodon.grouping.GroupHandle;
import org.mastodon.model.NavigationHandler;
import org.mastodon.model.SelectionModel;
import org.mastodon.plugin.MastodonPlugin;
import org.mastodon.plugin.MastodonPluginAppModel;
import org.mastodon.revised.bdv.SharedBigDataViewerData;
import org.mastodon.revised.mamut.KeyConfigContexts;
import org.mastodon.revised.mamut.MamutMenuBuilder;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.revised.ui.keymap.CommandDescriptionProvider;
import org.mastodon.revised.ui.keymap.CommandDescriptions;
import org.mastodon.trackmate.semiauto.ui.SemiAutomaticTrackerConfigPage;
import org.mastodon.trackmate.semiauto.ui.SemiAutomaticTrackerSettings;
import org.mastodon.trackmate.semiauto.ui.SemiAutomaticTrackerSettingsManager;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.thread.ThreadService;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.swing.console.LoggingPanel;

import net.imagej.ops.OpService;
import net.imagej.ops.special.computer.Computers;

@Plugin( type = SemiAutomaticTrackerPlugin.class )
public class SemiAutomaticTrackerPlugin implements MastodonPlugin
{

	public static final String[] MENU_PATH = new String[] { "Plugins", "TrackMate" };
	private static final String ACTION_1 = "semi-automatic tracking";
	private static final String ACTION_2 = "config semi-automatic tracking";
	private static final String ACTION_3 = "log semi-automatic tracking";

	private static final String[] ACTION_1_KEYS = new String[] { "ctrl T" };
	private static final String[] ACTION_2_KEYS = new String[] { "not mapped" };

	@Parameter
	private OpService ops;

	@Parameter( required = false )
	private Logger log;

	@Parameter
	private LogService logService;

	private MastodonPluginAppModel appModel;

	private static Map< String, String > menuTexts = new HashMap<>();

	private final Map< String, Object > currentSettings;

	private JDialog dialog;

	private NavigationHandler< Spot, Link > navigationHandler;
	private LoggingPanel loggingPanel;
	private JDialog loggingDialog;

	public SemiAutomaticTrackerPlugin()
	{
		this.currentSettings = new HashMap<>();
		currentSettings.putAll( SemiAutomaticTrackerKeys.getDefaultDetectorSettingsMap() );
	}

	static
	{
		menuTexts.put( ACTION_1, "Semi-automatic tracking" );
		menuTexts.put( ACTION_2, "Configure semi-automatic tracker" );
	}

	/*
	 * Command descriptions for all provided commands
	 */
	@Plugin( type = Descriptions.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigContexts.MASTODON );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( ACTION_1, ACTION_1_KEYS, "Execute semi-automatic tracking, using the spots currently in the selection." );
			descriptions.add( ACTION_2, ACTION_2_KEYS, "Toggle the smi-automatic tracking configuration panel." );
		}
	}

	@Override
	public Map< String, String > getMenuTexts()
	{
		return menuTexts;
	}

	@Override
	public List< MenuItem > getMenuItems()
	{
		return Arrays.asList(
				makeFullMenuItem( MamutMenuBuilder.item( ACTION_1 ) ),
				makeFullMenuItem( MamutMenuBuilder.item( ACTION_2 ) ) );
	}

	private static final MenuItem makeFullMenuItem( final MenuItem item )
	{
		MenuItem menuPath = item;
		for ( int i = MENU_PATH.length - 1; i >= 0; i-- )
			menuPath = MamutMenuBuilder.menu( MENU_PATH[ i ], menuPath );
		return menuPath;
	}

	@Override
	public void installGlobalActions( final Actions actions )
	{
		actions.namedAction( performSemiAutoTrackAction, ACTION_1_KEYS );
		actions.namedAction( toggleConfigDialogVisibility, ACTION_2_KEYS );
	}

	@Override
	public void setAppModel( final MastodonPluginAppModel appModel )
	{
		this.appModel = appModel;

		final Context context = appModel.getWindowManager().getContext();
		final ThreadService threadService = context.getService(ThreadService.class);
		final PrefService prefService = context.getService(PrefService.class);
		final String prefKey = "Mastodon semi-automatic tracker";
		this.loggingPanel= new LoggingPanel( threadService, prefService, prefKey );
		this.loggingDialog = new JDialog( ( Frame ) null, "Semi-automatic tracker log" );
		loggingDialog.getContentPane().add( loggingPanel, BorderLayout.CENTER );
		loggingDialog.setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
		loggingDialog.pack();

		final GroupHandle groupHandle = appModel.getAppModel().getGroupManager().createGroupHandle();
		this.navigationHandler = groupHandle.getModel( appModel.getAppModel().NAVIGATION );

		final SharedBigDataViewerData data = appModel.getAppModel().getSharedBdvData();
		final SemiAutomaticTrackerSettingsManager styleManager = new SemiAutomaticTrackerSettingsManager();
		final SemiAutomaticTrackerConfigPage page = new SemiAutomaticTrackerConfigPage( "Settings", styleManager, data, groupHandle, performSemiAutoTrackAction )
		{
			@Override
			public void apply()
			{
				super.apply();
				final SemiAutomaticTrackerSettings forwardDefaultStyle = styleManager.getForwardDefaultStyle();
				currentSettings.clear();
				currentSettings.putAll( forwardDefaultStyle.getAsSettingsMap() );
			}
		};
		page.apply();
		final SettingsPanel settings = new SettingsPanel();
		settings.addPage( page );

		dialog = new JDialog( ( Frame ) null, "Semi-automatic tracker settings" );
		dialog.getContentPane().add( settings, BorderLayout.CENTER );
		settings.onOk( () -> dialog.setVisible( false )  );
		settings.onCancel( () -> dialog.setVisible( false ) );

		dialog.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
		dialog.addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				settings.cancel();
			}
		} );
		dialog.pack();
	}

	private final AbstractNamedAction performSemiAutoTrackAction = new AbstractNamedAction( ACTION_1 )
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			if ( null == appModel )
				return;

			showLogDialog.actionPerformed( e );
			if (null == log)
				log = logService;

			final Logger subLogger = log.subLogger( "Semi-auto tracker" );
			subLogger.addLogListener( loggingPanel );

			final SelectionModel< Spot, Link > selectionModel = appModel.getAppModel().getSelectionModel();
			final Collection< Spot > selectedSpots = selectionModel.getSelectedVertices();
			final Collection< Spot > spots = RefCollections.createRefList(
					appModel.getAppModel().getModel().getGraph().vertices(), selectedSpots.size() );
			spots.addAll( selectedSpots );

			final Map< String, Object > settings = ( currentSettings == null )
					? SemiAutomaticTrackerKeys.getDefaultDetectorSettingsMap()
					: currentSettings;
			final Model model = appModel.getAppModel().getModel();

			final SharedBigDataViewerData data = appModel.getAppModel().getSharedBdvData();
			final SemiAutomaticTracker tracker = ( SemiAutomaticTracker ) Computers.binary(
					ops, SemiAutomaticTracker.class, model, spots, settings,
					data,
					navigationHandler,
					selectionModel,
					subLogger );
			tracker.compute( spots, settings, model );
			subLogger.removeLogListener( loggingPanel );
		}
	};

	private final AbstractNamedAction toggleConfigDialogVisibility = new AbstractNamedAction( ACTION_2 )
	{

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			if ( null == dialog )
				return;
			dialog.setVisible( !dialog.isVisible() );
		}
	};

	private final AbstractNamedAction showLogDialog = new AbstractNamedAction( ACTION_3 )
	{

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			if ( null == loggingDialog )
				return;
			loggingDialog.setVisible( true );
		}
	};
}
