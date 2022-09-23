/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2022 Tobias Pietzsch, Jean-Yves Tinevez
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
package org.mastodon.tracking.mamut.trackmate.wizard.descriptors;

import static org.mastodon.tracking.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_DO_LINK_SELECTION;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_KALMAN_SEARCH_RADIUS;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_LINKING_MAX_DISTANCE;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mastodon.mamut.WindowManager;
import org.mastodon.tracking.detection.DetectionUtil;
import org.mastodon.tracking.detection.DetectorKeys;
import org.mastodon.tracking.mamut.linking.KalmanLinkerMamut;
import org.mastodon.tracking.mamut.linking.SpotLinkerOp;
import org.mastodon.tracking.mamut.trackmate.Settings;
import org.mastodon.tracking.mamut.trackmate.TrackMate;
import org.mastodon.tracking.mamut.trackmate.wizard.util.SelectOnFocusListener;
import org.scijava.log.LogLevel;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.plugin.SciJavaPlugin;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;

@Plugin( type = SpotLinkerDescriptor.class, name = "Kalman linker configuration descriptor" )
public class KalmanLinkerDescriptor extends SpotLinkerDescriptor
{

	public static final String IDENTIFIER = "Canfigure Kalman linker";

	private static final Format FORMAT = new DecimalFormat( "0.0" );

	private static final Format INTEGER_FORMAT = new DecimalFormat( "0" );

	@Parameter
	private PluginService pluginService;

	private Settings settings;

	public KalmanLinkerDescriptor()
	{
		this.panelIdentifier = IDENTIFIER;
	}

	@Override
	public void aboutToDisplayPanel()
	{
		final Map< String, Object > ls = settings.values.getLinkerSettings();
		final KalmanLinkerPanel panel = ( KalmanLinkerPanel ) targetPanel;
		panel.searchRadius.setValue( ls.get( KEY_KALMAN_SEARCH_RADIUS ) );
		panel.initialSearchRadius.setValue( ls.get( KEY_LINKING_MAX_DISTANCE ) );
		panel.maxFrameGap.setValue( ls.get( KEY_GAP_CLOSING_MAX_FRAME_GAP ) );
	}

	@Override
	public void aboutToHidePanel()
	{
		// Panel settings.
		final Map< String, Object > ls = new HashMap<>( settings.values.getLinkerSettings() );
		final KalmanLinkerPanel panel = ( KalmanLinkerPanel ) targetPanel;
		ls.put( KEY_KALMAN_SEARCH_RADIUS, ( ( Number ) panel.searchRadius.getValue() ).doubleValue() );
		ls.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, ( ( Number ) panel.maxFrameGap.getValue() ).intValue() );
		ls.put( KEY_LINKING_MAX_DISTANCE, ( ( Number ) panel.initialSearchRadius.getValue() ).doubleValue() );
		settings.linkerSettings( ls );

		final Integer setupID = ( Integer ) settings.values.getDetectorSettings().get( DetectorKeys.KEY_SETUP_ID );
		final List< SourceAndConverter< ? > > sources = settings.values.getSources();
		final Source< ? > source = sources.get( setupID.intValue() ).getSpimSource();
		final String units = source.getVoxelDimensions().unit();
		logger.log( LogLevel.INFO, "Configured Kalman linker with the following parameters:\n" );
		logger.log( LogLevel.INFO, String.format( "  - initial search radius: %.1f %s\n", ( double ) ls.get( KEY_LINKING_MAX_DISTANCE ), units ) );
		logger.log( LogLevel.INFO, String.format( "  - search radius: %.1f %s\n", ( double ) ls.get( KEY_KALMAN_SEARCH_RADIUS ), units ) );
		logger.log( LogLevel.INFO, String.format( "  - max frame gap: %d\n", ( int ) ls.get( KEY_GAP_CLOSING_MAX_FRAME_GAP ) ) );
		logger.log( LogLevel.INFO, String.format( "  - target: %s\n", ( boolean ) ls.get( KEY_DO_LINK_SELECTION ) ? "selection only." : "all detections." ) );
		logger.log( LogLevel.INFO, String.format( "  - min time-point: %d\n", ( int ) ls.get( KEY_MIN_TIMEPOINT ) ) );
		logger.log( LogLevel.INFO, String.format( "  - max time-point: %d\n", ( int ) ls.get( KEY_MAX_TIMEPOINT ) ) );
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	@Override
	public Collection< Class< ? extends SpotLinkerOp > > getTargetClasses()
	{
		final Collection b = Collections.unmodifiableCollection( Arrays.asList( new Class[] {
				KalmanLinkerMamut.class
		} ) );
		final Collection< Class< ? extends SpotLinkerOp > > a = b;
		return a;
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		return KalmanLinkerMamut.getDefaultSettingsMap();
	}

	@Override
	public void setTrackMate( final TrackMate trackmate )
	{
		this.settings = trackmate.getSettings();
		final String units = DetectionUtil.getSpatialUnits( settings.values.getSources() );
		this.targetPanel = new KalmanLinkerPanel( units );
	}

	@Override
	public void setWindowManager( final WindowManager windowManager )
	{}

	private class KalmanLinkerPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		private final JFormattedTextField searchRadius;

		private final JFormattedTextField maxFrameGap;

		private final JFormattedTextField initialSearchRadius;

		public KalmanLinkerPanel( final String units )
		{
			final SelectOnFocusListener onFocusListener = new SelectOnFocusListener();
			final PluginInfo< SciJavaPlugin > pluginInfo = pluginService.getPlugin( KalmanLinkerMamut.class );

			final GridBagLayout layout = new GridBagLayout();
			layout.columnWidths = new int[] { 80, 80, 40 };
			layout.columnWeights = new double[] { 0.2, 0.7, 0.1 };
			layout.rowHeights = new int[] { 26, 0, 0, 0, 26, 26 };
			layout.rowWeights = new double[] { 1., 0., 0., 0., 0., 1. };
			setLayout( layout );

			final GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.gridwidth = 3;
			gbc.anchor = GridBagConstraints.BASELINE_LEADING;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets( 5, 5, 5, 5 );

			final JLabel title = new JLabel( "Configure " + pluginInfo.getName() + "." );
			title.setFont( getFont().deriveFont( Font.BOLD ) );
			add( title, gbc );

			// Search radius

			final JLabel lblSearchRadius = new JLabel( "Search radius:", JLabel.RIGHT );
			gbc.gridy++;
			gbc.gridwidth = 1;
			gbc.anchor = GridBagConstraints.LINE_END;
			add( lblSearchRadius, gbc );

			this.searchRadius = new JFormattedTextField( FORMAT );
			searchRadius.setHorizontalAlignment( JLabel.RIGHT );
			searchRadius.addFocusListener( onFocusListener );
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.CENTER;
			add( searchRadius, gbc );

			final JLabel lblSearchRadiusUnit = new JLabel( units );
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.LINE_END;
			add( lblSearchRadiusUnit, gbc );

			// Max frame gap.

			final JLabel lblMaxFrameGap = new JLabel( "Max frame gap:", JLabel.RIGHT );
			gbc.gridy++;
			gbc.gridx = 0;
			gbc.anchor = GridBagConstraints.LINE_END;
			add( lblMaxFrameGap, gbc );

			this.maxFrameGap = new JFormattedTextField( INTEGER_FORMAT );
			maxFrameGap.setHorizontalAlignment( JLabel.RIGHT );
			maxFrameGap.addFocusListener( onFocusListener );
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.CENTER;
			add( maxFrameGap, gbc );

			// Initial search radius

			final JLabel lblInitialSearchRadius = new JLabel( "Initial search radius:", JLabel.RIGHT );
			gbc.gridx = 0;
			gbc.gridy++;
			gbc.gridwidth = 1;
			gbc.anchor = GridBagConstraints.LINE_END;
			add( lblInitialSearchRadius, gbc );

			this.initialSearchRadius = new JFormattedTextField( FORMAT );
			initialSearchRadius.setHorizontalAlignment( JLabel.RIGHT );
			initialSearchRadius.addFocusListener( onFocusListener );
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.CENTER;
			add( initialSearchRadius, gbc );

			final JLabel lblInitialSearchRadiusUnit = new JLabel( units );
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.LINE_END;
			add( lblInitialSearchRadiusUnit, gbc );

			// Info text.
			final JLabel lblInfo = new JLabel( pluginInfo.getDescription(), JLabel.RIGHT );
			lblInfo.setFont( getFont().deriveFont( getFont().getSize2D() - 2f ) );
			gbc.gridwidth = 3;
			gbc.gridy++;
			gbc.gridx = 0;
			add( lblInfo, gbc );
		}
	}
}
