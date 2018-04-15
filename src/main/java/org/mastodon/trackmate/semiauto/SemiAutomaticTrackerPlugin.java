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
import org.mastodon.revised.mamut.MamutMenuBuilder;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.trackmate.semiauto.ui.SemiAutomaticTrackerConfigPage;
import org.mastodon.trackmate.semiauto.ui.SemiAutomaticTrackerSettings;
import org.mastodon.trackmate.semiauto.ui.SemiAutomaticTrackerSettingsManager;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;

import bdv.spimdata.SpimDataMinimal;
import net.imagej.ops.OpService;
import net.imagej.ops.special.computer.Computers;

@Plugin( type = SemiAutomaticTrackerPlugin.class )
public class SemiAutomaticTrackerPlugin implements MastodonPlugin
{

	private static final String ACTION_1 = "[semiautotrack] track";
	private static final String ACTION_2 = "[semiautotrack] show config";

	private static final String[] ACTION_1_KEYS = new String[] { "ctrl T" };
	private static final String[] ACTION_2_KEYS = new String[] { "not mapped" };

	@Parameter
	private OpService ops;

	private MastodonPluginAppModel appModel;

	private static Map< String, String > menuTexts = new HashMap<>();

	private final Map< String, Object > currentSettings;

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

	private final AbstractNamedAction performSemiAutoTrackAction = new AbstractNamedAction( ACTION_1 )
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			if ( null == appModel )
				return;

			final SelectionModel< Spot, Link > selectionModel = appModel.getAppModel().getSelectionModel();
			final Collection< Spot > selectedSpots = selectionModel.getSelectedVertices();
			final Collection< Spot > spots = RefCollections.createRefList(
					appModel.getAppModel().getModel().getGraph().vertices(), selectedSpots.size() );
			spots.addAll( selectedSpots );

			final Map< String, Object > settings = ( currentSettings == null )
					? SemiAutomaticTrackerKeys.getDefaultDetectorSettingsMap()
							: currentSettings;
			final Model model = appModel.getAppModel().getModel();

			final SpimDataMinimal spimData = ( SpimDataMinimal ) appModel.getAppModel().getSharedBdvData().getSpimData();
			final SemiAutomaticTracker tracker = ( SemiAutomaticTracker ) Computers.binary(
					ops, SemiAutomaticTracker.class, model, spots, settings,
					spimData,
					navigationHandler,
					selectionModel );
			tracker.compute( spots, settings, model );
		}
	};

	private final AbstractNamedAction toggleConfigDialogVisibility = new AbstractNamedAction( ACTION_2)
	{

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			if (null == dialog)
				return;
			dialog.setVisible( !dialog.isVisible() );
		}
	};

	private JDialog dialog;

	private NavigationHandler< Spot, Link > navigationHandler;

	@Override
	public Map< String, String > getMenuTexts()
	{
		return menuTexts;
	}

	@Override
	public List< MenuItem > getMenuItems()
	{
		return Arrays.asList(
				MamutMenuBuilder.menu( "Plugins",
						MamutMenuBuilder.menu( "TrackMate",
								MamutMenuBuilder.item( ACTION_1 ) ) ),
				MamutMenuBuilder.menu( "Plugins",
						MamutMenuBuilder.menu( "TrackMate",
								MamutMenuBuilder.item( ACTION_2 ) ) ));
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

		final GroupHandle groupHandle = appModel.getAppModel().getGroupManager().createGroupHandle();
		this.navigationHandler = groupHandle.getModel( appModel.getAppModel().NAVIGATION );

		final SpimDataMinimal spimData = ( SpimDataMinimal ) appModel.getAppModel().getSharedBdvData().getSpimData();
		final SemiAutomaticTrackerSettingsManager styleManager = new SemiAutomaticTrackerSettingsManager();
		final SemiAutomaticTrackerConfigPage page = new SemiAutomaticTrackerConfigPage( "Settings", styleManager, spimData, groupHandle )
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
}
