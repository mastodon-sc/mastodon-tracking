/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2024 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.tracking.mamut.trackmate.semiauto;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.WindowConstants;

import org.mastodon.app.MastodonIcons;
import org.mastodon.app.ui.ViewMenuBuilder.MenuItem;
import org.mastodon.collection.RefCollections;
import org.mastodon.grouping.GroupHandle;
import org.mastodon.mamut.KeyConfigScopes;
import org.mastodon.mamut.MamutMenuBuilder;
import org.mastodon.mamut.PreferencesDialog;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.plugin.MamutPlugin;
import org.mastodon.model.FocusModel;
import org.mastodon.model.NavigationHandler;
import org.mastodon.model.SelectionModel;
import org.mastodon.tracking.mamut.trackmate.semiauto.ui.SemiAutomaticTrackerConfigPage;
import org.mastodon.tracking.mamut.trackmate.semiauto.ui.SemiAutomaticTrackerSettings;
import org.mastodon.tracking.mamut.trackmate.semiauto.ui.SemiAutomaticTrackerSettingsManager;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.scijava.Cancelable;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.swing.console.LoggingPanel;

import net.imagej.ops.OpService;
import net.imagej.ops.special.computer.Computers;

@Plugin( type = SemiAutomaticTrackerPlugin.class )
public class SemiAutomaticTrackerPlugin implements MamutPlugin
{

	public static final String[] MENU_PATH = new String[] { "Plugins", "Tracking" };

	private static final String PERFORM_SEMI_AUTO_TRACKING_ACTION = "semi-automatic tracking";
	private static final String CANCEL_SEMI_AUTO_TRACKING_ACTION = "cancel semi-automatic tracking";
	private static final String CONFIGURE_SEMI_AUTO_TRACKING_ACTION = "config semi-automatic tracking";
	private static final String SHOW_LOGGING_PANEL_ACTION = "log semi-automatic tracking";

	private static final String[] PERFORM_SEMI_AUTO_TRACKING_KEYS = new String[] { "ctrl T" };
	private static final String[] CANCEL_SEMI_AUTO_TRACKING_KEYS = new String[] { "ctrl shift T" };
	private static final String[] CONFIGURE_SEMI_AUTO_TRACKING_KEYS = new String[] { "not mapped" };

	@Parameter
	private OpService ops;

	@Parameter( required = false )
	private Logger log;

	@Parameter
	private LogService logService;

	private ProjectModel appModel;

	private static Map< String, String > menuTexts = new HashMap<>();

	private final Map< String, Object > currentSettings;

	private NavigationHandler< Spot, Link > navigationHandler;

	private LoggingPanel loggingPanel;

	private JDialog loggingDialog;

	/**
	 * Reference to a {@link Cancelable} created by the semi-auto tracker
	 * action, so that we can call {@link Cancelable#cancel(String)} from
	 * another action.
	 */
	private Cancelable cancelable;

	private SemiAutomaticTrackerConfigPage page;

	public SemiAutomaticTrackerPlugin()
	{
		this.currentSettings = new HashMap<>();
		currentSettings.putAll( SemiAutomaticTrackerKeys.getDefaultDetectorSettingsMap() );
	}

	static
	{
		menuTexts.put( PERFORM_SEMI_AUTO_TRACKING_ACTION, "Semi-automatic tracking" );
		menuTexts.put( CANCEL_SEMI_AUTO_TRACKING_ACTION, "Cancel semi-automatic tracking" );
		menuTexts.put( CONFIGURE_SEMI_AUTO_TRACKING_ACTION, "Configure semi-automatic tracker" );
	}

	/*
	 * Command descriptions for all provided commands
	 */
	@Plugin( type = Descriptions.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigScopes.MAMUT, KeyConfigContexts.MASTODON );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add(
					PERFORM_SEMI_AUTO_TRACKING_ACTION,
					PERFORM_SEMI_AUTO_TRACKING_KEYS,
					"Execute semi-automatic tracking, using the spots currently in the selection." );
			descriptions.add(
					CANCEL_SEMI_AUTO_TRACKING_ACTION,
					CANCEL_SEMI_AUTO_TRACKING_KEYS,
					"Cancel the current semi-automatic tracking process." );
			descriptions.add(
					CONFIGURE_SEMI_AUTO_TRACKING_ACTION,
					CONFIGURE_SEMI_AUTO_TRACKING_KEYS,
					"Displays the semi-automatic tracking configuration panel." );
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
				makeFullMenuItem( MamutMenuBuilder.item( PERFORM_SEMI_AUTO_TRACKING_ACTION ) ),
				makeFullMenuItem( MamutMenuBuilder.item( CANCEL_SEMI_AUTO_TRACKING_ACTION ) ),
				makeFullMenuItem( MamutMenuBuilder.item( CONFIGURE_SEMI_AUTO_TRACKING_ACTION ) ),
				makeFullMenuItem( MamutMenuBuilder.separator() ) );
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
		actions.namedAction( performSemiAutoTrackAction, PERFORM_SEMI_AUTO_TRACKING_KEYS );
		actions.namedAction( cancelSemiAutoTrackAction, CANCEL_SEMI_AUTO_TRACKING_KEYS );
		actions.namedAction( toggleConfigPageVisibility, CONFIGURE_SEMI_AUTO_TRACKING_KEYS );
	}

	@Override
	public void setAppPluginModel( final ProjectModel appModel )
	{
		this.appModel = appModel;

		final Context context = appModel.getContext();
		final String prefKey = "Mastodon semi-automatic tracker";
		this.loggingPanel = new LoggingPanel( context, prefKey );
		this.loggingDialog = new JDialog( ( Frame ) null, "Semi-automatic tracker log" );
		loggingDialog.getContentPane().add( loggingPanel, BorderLayout.CENTER );
		loggingDialog.setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
		loggingDialog.setIconImage( MastodonIcons.MASTODON_ICON_MEDIUM.getImage() );
		loggingDialog.pack();
		loggingDialog.setLocationByPlatform( true );
		loggingDialog.setLocationRelativeTo( null );

		final GroupHandle groupHandle = appModel.getGroupManager().createGroupHandle();
		this.navigationHandler = groupHandle.getModel( appModel.NAVIGATION );

		final SharedBigDataViewerData data = appModel.getSharedBdvData();
		final SemiAutomaticTrackerSettingsManager styleManager = new SemiAutomaticTrackerSettingsManager();
		this.page = new SemiAutomaticTrackerConfigPage(
				"Plugins > Semi-automatic tracking",
				styleManager,
				data,
				groupHandle,
				performSemiAutoTrackAction,
				cancelSemiAutoTrackAction )
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
		final PreferencesDialog settings = appModel.getWindowManager().getPreferencesDialog();
		settings.addPage( page );
	}

	private final AbstractNamedAction performSemiAutoTrackAction = new AbstractNamedAction( PERFORM_SEMI_AUTO_TRACKING_ACTION )
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			if ( null == appModel )
				return;

			showLogDialog.actionPerformed( e );
			if ( null == log )
				log = logService;

			final Logger subLogger = log.subLogger( "Semi-auto tracker" );
			subLogger.addLogListener( loggingPanel );

			final SelectionModel< Spot, Link > selectionModel = appModel.getSelectionModel();
			final FocusModel< Spot > focusModel = appModel.getFocusModel();
			final Collection< Spot > selectedSpots = selectionModel.getSelectedVertices();
			final Collection< Spot > spots = RefCollections.createRefList(
					appModel.getModel().getGraph().vertices(), selectedSpots.size() );
			spots.addAll( selectedSpots );

			final Map< String, Object > settings = ( currentSettings == null )
					? SemiAutomaticTrackerKeys.getDefaultDetectorSettingsMap()
					: currentSettings;
			final Model model = appModel.getModel();

			final SharedBigDataViewerData data = appModel.getSharedBdvData();
			final SemiAutomaticTracker tracker = ( SemiAutomaticTracker ) Computers.binary(
					ops, SemiAutomaticTracker.class, model, spots, settings,
					data,
					navigationHandler,
					selectionModel,
					focusModel,
					subLogger );
			cancelable = tracker;
			new Thread( () -> {
				cancelSemiAutoTrackAction.setEnabled( true );
				setEnabled( false );
				tracker.compute( spots, settings, model );
				subLogger.removeLogListener( loggingPanel );
				cancelable = null;
				setEnabled( true );
				cancelSemiAutoTrackAction.setEnabled( false );
			}, "Mastodon semi-auto tracking" ).start();
		}
	};

	private final AbstractNamedAction cancelSemiAutoTrackAction = new AbstractNamedAction( CANCEL_SEMI_AUTO_TRACKING_ACTION )
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			if ( null != cancelable )
			{
				cancelable.cancel( "User canceled semi-automatic tracking." );
				cancelable = null;
			}
		}
	};

	private final AbstractNamedAction showLogDialog = new AbstractNamedAction( SHOW_LOGGING_PANEL_ACTION )
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

	private final AbstractNamedAction toggleConfigPageVisibility =
			new AbstractNamedAction( CONFIGURE_SEMI_AUTO_TRACKING_ACTION )
			{

				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( final ActionEvent e )
				{
					if ( appModel == null )
						return;

					final PreferencesDialog dialog = appModel.getWindowManager().getPreferencesDialog();
					if ( null == dialog )
						return;

					dialog.showPage( page.getTreePath() );
					dialog.setVisible( true );
				}
			};
}

