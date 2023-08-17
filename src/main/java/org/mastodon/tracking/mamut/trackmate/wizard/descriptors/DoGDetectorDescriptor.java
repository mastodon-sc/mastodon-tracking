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

import static org.mastodon.tracking.detection.DetectorKeys.KEY_ADD_BEHAVIOR;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_DETECTION_TYPE;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_RADIUS;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_THRESHOLD;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartPanel;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.model.Model;
import org.mastodon.tracking.detection.DetectionType;
import org.mastodon.tracking.detection.DetectionUtil;
import org.mastodon.tracking.detection.DetectorKeys;
import org.mastodon.tracking.detection.DoGDetectorOp;
import org.mastodon.tracking.mamut.detection.DoGDetectorMamut;
import org.mastodon.tracking.mamut.detection.LoGDetectorMamut;
import org.mastodon.tracking.mamut.detection.MamutDetectionCreatorFactories;
import org.mastodon.tracking.mamut.detection.SpotDetectorOp;
import org.mastodon.tracking.mamut.trackmate.Settings;
import org.mastodon.tracking.mamut.trackmate.TrackMate;
import org.mastodon.tracking.mamut.trackmate.wizard.Wizard;
import org.mastodon.tracking.mamut.trackmate.wizard.util.WizardUtils;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.mastodon.views.bdv.ViewerFrameMamut;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import bdv.viewer.SourceAndConverter;
import net.imagej.ops.OpService;

@Plugin( type = SpotDetectorDescriptor.class, name = "DoG detector configuration descriptor" )
public class DoGDetectorDescriptor extends SpotDetectorDescriptor
{

	public static final String IDENTIFIER = "Configure DoG detector";

	private static final Icon PREVIEW_ICON = new ImageIcon( Wizard.class.getResource( "led-icon-eye-green.png" ) );

	private static final NumberFormat FORMAT = new DecimalFormat( "0.0" );

	@Parameter
	private OpService ops;

	private Settings settings;

	private ProjectModel appModel;

	private ChartPanel chartPanel;

	private ViewerFrameMamut viewFrame;

	private final Model localModel;

	public DoGDetectorDescriptor()
	{
		this.panelIdentifier = IDENTIFIER;
		this.targetPanel = new DoGDetectorPanel();
		/*
		 * Use a separate model for the preview. We do not want to touch the
		 * existing model.
		 */
		this.localModel = new Model();
	}

	@Override
	public void aboutToHidePanel()
	{
		if ( null != viewFrame )
			viewFrame.dispose();
		viewFrame = null;

		grabSettings();
		final Integer setupID = ( Integer ) settings.values.getDetectorSettings().get( DetectorKeys.KEY_SETUP_ID );
		final double radius = ( double ) settings.values.getDetectorSettings().get( KEY_RADIUS );
		final double minSizePixel = DoGDetectorOp.MIN_SPOT_PIXEL_SIZE / 2.;
		final int timepoint = ( int ) settings.values.getDetectorSettings().get( KEY_MIN_TIMEPOINT );
		final double threshold = ( double ) settings.values.getDetectorSettings().get( KEY_THRESHOLD );
		final List< SourceAndConverter< ? > > sources = settings.values.getSources();
		logger.info( WizardUtils.echoDetectorConfigInfo( sources, minSizePixel, timepoint, setupID, radius, threshold ) );
	}

	private void preview()
	{
		if ( null == appModel )
			return;

		final SharedBigDataViewerData shared = appModel.getSharedBdvData();
		viewFrame = WizardUtils.previewFrame( viewFrame, shared, localModel );
		final int currentTimepoint = viewFrame.getViewerPanel().state().getCurrentTimepoint();

		final DoGDetectorPanel panel = ( DoGDetectorPanel ) targetPanel;
		panel.preview.setEnabled( false );
		final JLabelLogger previewLogger = new JLabelLogger( panel.lblInfo );
		new Thread( "DogDetectorPanel preview thread" )
		{
			@Override
			public void run()
			{
				try
				{
					grabSettings();
					final boolean ok = WizardUtils.executeDetectionPreview( localModel, settings, ops, currentTimepoint, previewLogger, statusService );
					if ( !ok )
						return;

					final int nSpots = WizardUtils.countSpotsIn( localModel, currentTimepoint );
					panel.lblInfo.setText( "Found " + nSpots + " spots in time-point " + currentTimepoint );
					plotQualityHistogram();
				}
				finally
				{
					panel.preview.setEnabled( true );
				}
			};
		}.start();

	}

	private void plotQualityHistogram()
	{
		final DoGDetectorPanel panel = ( DoGDetectorPanel ) targetPanel;
		if ( null != chartPanel )
		{
			panel.remove( chartPanel );
			panel.repaint();
		}
		this.chartPanel = WizardUtils.createQualityHistogram( localModel );
		if ( null == chartPanel )
			return;

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridy = 5;
		gbc.gridx = 0;
		gbc.gridwidth = 3;
		gbc.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets( 5, 5, 5, 5 );
		panel.add( chartPanel, gbc );
		panel.revalidate();
	}

	/**
	 * Update the settings field of this descriptor with the values set on the
	 * GUI.
	 */
	private void grabSettings()
	{
		if ( null == settings )
			return;

		final DoGDetectorPanel panel = ( DoGDetectorPanel ) targetPanel;
		final Map< String, Object > detectorSettings = settings.values.getDetectorSettings();
		detectorSettings.put( KEY_RADIUS, ( ( Number ) panel.diameter.getValue() ).doubleValue() / 2. );
		detectorSettings.put( KEY_THRESHOLD, ( ( Number ) panel.threshold.getValue() ).doubleValue() );
		detectorSettings.put( KEY_DETECTION_TYPE, DetectionType.MINIMA.name() );
		detectorSettings.put( KEY_ADD_BEHAVIOR, MamutDetectionCreatorFactories.DetectionBehavior.REMOVEALL.name() );
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	@Override
	public Collection< Class< ? extends SpotDetectorOp > > getTargetClasses()
	{
		final Collection b = Collections.unmodifiableCollection( Arrays.asList( new Class[] {
				DoGDetectorMamut.class,
				LoGDetectorMamut.class
		} ) );
		final Collection< Class< ? extends SpotDetectorOp > > a = b;
		return a;
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		return DetectionUtil.getDefaultDetectorSettingsMap();
	}

	@Override
	public void setTrackMate( final TrackMate trackmate )
	{
		final DoGDetectorPanel panel = ( DoGDetectorPanel ) targetPanel;

		this.settings = trackmate.getSettings();
		if ( null == settings )
			return;

		final Map< String, Object > detectorSettings = settings.values.getDetectorSettings();

		final double diameter;
		final Object objRadius = detectorSettings.get( KEY_RADIUS );
		if ( null == objRadius )
			diameter = 2. * DetectorKeys.DEFAULT_RADIUS;
		else
			diameter = 2. * ( double ) objRadius;

		final double threshold;
		final Object objThreshold = detectorSettings.get( KEY_THRESHOLD );
		if ( null == objThreshold )
			threshold = DetectorKeys.DEFAULT_THRESHOLD;
		else
			threshold = ( double ) objThreshold;

		panel.diameter.setValue( diameter );
		panel.threshold.setValue( threshold );
		final String unit = DetectionUtil.getSpatialUnits( settings.values.getSources() );
		panel.lblDiameterUnit.setText( unit );
	}

	@Override
	public void setAppModel( final ProjectModel appModel )
	{
		this.appModel = appModel;
	}

	private class DoGDetectorPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		private final JFormattedTextField diameter;

		private final JFormattedTextField threshold;

		private final JLabel lblDiameterUnit;

		private final JButton preview;

		private final JLabel lblInfo;

		public DoGDetectorPanel()
		{
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

			final JLabel lblTitle = new JLabel( "Configure detector" );
			lblTitle.setFont( getFont().deriveFont( Font.BOLD ) );
			add( lblTitle, gbc );

			// Diameter.

			final JLabel lblDiameter = new JLabel( "Estimated diameter:", JLabel.RIGHT );
			gbc.gridy++;
			gbc.gridwidth = 1;
			gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
			add( lblDiameter, gbc );

			this.diameter = new JFormattedTextField( FORMAT );
			diameter.setHorizontalAlignment( JLabel.RIGHT );
			diameter.addFocusListener( new SelectAllOnFocus( diameter ) );
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.CENTER;
			add( diameter, gbc );

			lblDiameterUnit = new JLabel();
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.LINE_END;
			add( lblDiameterUnit, gbc );

			// Threshold.

			final JLabel lblThreshold = new JLabel( "Quality threshold:", JLabel.RIGHT );
			gbc.gridy++;
			gbc.gridx = 0;
			gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
			add( lblThreshold, gbc );

			this.threshold = new JFormattedTextField( FORMAT );
			threshold.setHorizontalAlignment( JLabel.RIGHT );
			threshold.addFocusListener( new SelectAllOnFocus( threshold ) );
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.CENTER;
			add( threshold, gbc );

			// Preview button.
			preview = new JButton( "Preview", PREVIEW_ICON );
			preview.addActionListener( ( e ) -> preview() );
			gbc.gridy++;
			gbc.gridx = 0;
			gbc.gridwidth = 3;
			gbc.anchor = GridBagConstraints.EAST;
			gbc.fill = GridBagConstraints.NONE;
			add( preview, gbc );

			// Info text.
			this.lblInfo = new JLabel( "", JLabel.RIGHT );
			lblInfo.setFont( getFont().deriveFont( getFont().getSize2D() - 2f ) );
			gbc.gridwidth = 3;
			gbc.gridy++;
			gbc.gridx = 0;
			add( lblInfo, gbc );

			// Quality histogram place holder.
		}
	}

	private static class SelectAllOnFocus extends FocusAdapter
	{
		private final JFormattedTextField textField;

		public SelectAllOnFocus( final JFormattedTextField textField )
		{
			this.textField = textField;
		}

		@Override
		public void focusGained( final FocusEvent e )
		{
			SwingUtilities.invokeLater( () -> textField.selectAll() );
		}
	}
}
