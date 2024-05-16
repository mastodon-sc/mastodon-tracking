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
package org.mastodon.tracking.mamut.trackmate.wizard.descriptors;

import static org.mastodon.tracking.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_ROI;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_SETUP_ID;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.util.stream.IntStream;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.WindowManager;
import org.mastodon.mamut.views.bdv.MamutViewBdv;
import org.mastodon.tracking.mamut.trackmate.Settings;
import org.mastodon.tracking.mamut.trackmate.wizard.WizardLogService;
import org.mastodon.tracking.mamut.trackmate.wizard.WizardPanelDescriptor;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.mastodon.views.bdv.ViewerFrameMamut;

import bdv.tools.boundingbox.BoxDisplayModePanel;
import bdv.tools.boundingbox.BoxSelectionPanel;
import bdv.tools.boundingbox.TransformedBoxEditor;
import bdv.tools.boundingbox.TransformedBoxEditor.BoxSourceType;
import bdv.tools.boundingbox.TransformedBoxModel;
import bdv.tools.brightness.SliderPanel;
import bdv.util.BoundedInterval;
import bdv.util.ModifiableInterval;
import bdv.viewer.Source;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;

public class BoundingBoxDescriptor extends WizardPanelDescriptor
{

	public static final String IDENTIFIER = "Setup bounding-box";

	private final Settings settings;

	private final WizardLogService log;

	private ViewerFrameMamut viewFrame;

	private TransformedBoxEditor boundingBoxEditor;

	private MyBoundingBoxModel roi;

	private final ProjectModel appModel;

	public BoundingBoxDescriptor( final Settings settings, final ProjectModel appModel, final WizardLogService log )
	{
		this.appModel = appModel;
		this.panelIdentifier = IDENTIFIER;
		this.settings = settings;
		this.log = log;
		this.targetPanel = new BoundingBoxPanel();
	}

	private void toggleBoundingBox( final boolean visible )
	{
		if ( visible )
			install();
		else
			uninstall();
	}

	private void uninstall()
	{
		if ( boundingBoxEditor != null )
			boundingBoxEditor.uninstall();
		// Clear panel.
		( ( BoundingBoxPanel ) targetPanel ).regen( null );
	}

	private void install()
	{
		// Target frame.
		getViewerFrame();

		// Roi.
		roi = getBoundingBoxModel();
		roi.intervalChangedListeners().add( () -> viewFrame.getViewerPanel().getDisplay().repaint() );

		// Editor.
		final int boxSetupId = appModel.getSharedBdvData().getSources().size();
		boundingBoxEditor = new TransformedBoxEditor(
				appModel.getKeymap().getConfig(),
				viewFrame.getViewerPanel(),
				appModel.getSharedBdvData().getConverterSetups(),
				boxSetupId,
				viewFrame.getTriggerbindings(),
				roi,
				"ROI",
				BoxSourceType.PLACEHOLDER );

		// Perspective.
		final double sourceSize = IntStream
				.range( 0, roi.maxInterval.numDimensions() )
				.mapToDouble( i -> roi.maxInterval.dimension( i ) )
				.max()
				.getAsDouble();
		boundingBoxEditor.setPerspective( 1., Math.max( sourceSize, 1000. ) );

		boundingBoxEditor.install();
		viewFrame.toFront();

		// Update panel.
		( ( BoundingBoxPanel ) targetPanel ).regen( roi );

	}

	/**
	 * Build a model for the bounding-box from the settings passed to this
	 * descriptor or sensible defaults if there are no settings yet.
	 */
	private MyBoundingBoxModel getBoundingBoxModel()
	{
		// Max interval.
		final int setupID = ( int ) settings.values.getDetectorSettings().get( KEY_SETUP_ID );
		final SharedBigDataViewerData data = appModel.getSharedBdvData();
		final Source< ? > source = data.getSources().get( setupID ).getSpimSource();
		final int numTimepoints = data.getNumTimepoints();
		Interval maxInterval = Intervals.createMinMax( 0, 0, 0, 1, 1, 1 );
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		int tp = 0;
		while ( tp++ < numTimepoints )
		{
			if ( source.isPresent( tp ) )
			{
				maxInterval = source.getSource( tp, 0 );
				source.getSourceTransform( tp, 0, sourceTransform );
				break;
			}
		}

		// ROI interval.
		Interval interval = ( Interval ) settings.values.getDetectorSettings().get( KEY_ROI );
		if ( null == interval )
			interval = maxInterval;
		else
			// Intersection.
			interval = Intervals.intersect( interval, maxInterval );

		return new MyBoundingBoxModel( interval, maxInterval, sourceTransform );
	}

	/**
	 * Sets the {@link #viewFrame} field. If this variable points to a showing
	 * ViewerFrame, it is left as is. Otherwise, we check if a viewer frame is
	 * open and showing, and use it. If there is none open and showing, we
	 * create one and use it.
	 */
	private void getViewerFrame()
	{
		if ( viewFrame != null && viewFrame.isShowing() )
			return;

		// Is there a BDV open?
		final WindowManager wm = appModel.getWindowManager();
		wm.forEachView( MamutViewBdv.class, view -> {
			final ViewerFrameMamut vf = ( ViewerFrameMamut ) view.getFrame();
			if ( vf != null && vf.isShowing() )
			{
				viewFrame = vf;
				return;
			}
		} );

		// Create one
		if ( viewFrame == null )
			viewFrame = ( ViewerFrameMamut ) wm.createView( MamutViewBdv.class ).getFrame();

		viewFrame.addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final java.awt.event.WindowEvent e )
			{
				uninstall();
				viewFrame = null;
			};
		} );
	}

	private static int previousSetupID = -1;

	/*
	 * DESCRIPTOR METHODS.
	 */

	@Override
	public void aboutToDisplayPanel()
	{
		final BoundingBoxPanel panel = ( BoundingBoxPanel ) targetPanel;
		if ( null == settings.values.getDetectorSettings().get( KEY_SETUP_ID ) )
		{
			panel.removeAll();
			panel.add( new JLabel( "Error: setup ID is null." ) );
			return;
		}

		final int setupID = ( int ) settings.values.getDetectorSettings().get( KEY_SETUP_ID );
		if ( setupID != previousSetupID )
		{
			settings.values.getDetectorSettings().put( KEY_ROI, null );
			panel.useRoi.setSelected( false );
		}
		else
		{
			panel.useRoi.setSelected( null != settings.values.getDetectorSettings().get( KEY_ROI ) );
		}
		previousSetupID = setupID;
		toggleBoundingBox( panel.useRoi.isSelected() );
	}

	@Override
	public void aboutToHidePanel()
	{
		final BoundingBoxPanel panel = ( BoundingBoxPanel ) targetPanel;
		final String info;
		final int t0;
		final int t1;
		final Interval interval;
		if ( panel.useRoi.isSelected() )
		{
			interval = roi.box().getInterval();
			info = "Processing within ROI with bounds: " + Util.printInterval( interval ) + '\n';
			t0 = panel.intervalT.getMinBoundedValue().getCurrentValue();
			t1 = panel.intervalT.getMaxBoundedValue().getCurrentValue();
		}
		else
		{
			interval = null;
			t0 = appModel.getMinTimepoint();
			t1 = appModel.getMaxTimepoint();
			info = "Processing whole image.\n";
		}
		settings.values.getDetectorSettings().put( KEY_ROI, interval );
		settings.values.getDetectorSettings().put( KEY_MIN_TIMEPOINT, t0 );
		settings.values.getDetectorSettings().put( KEY_MAX_TIMEPOINT, t1 );

		toggleBoundingBox( false );
		log.log( info );
		log.log( String.format( "  - min time-point: %d\n", ( int ) settings.values.getDetectorSettings().get( KEY_MIN_TIMEPOINT ) ) );
		log.log( String.format( "  - max time-point: %d\n", ( int ) settings.values.getDetectorSettings().get( KEY_MAX_TIMEPOINT ) ) );
	}

	/*
	 * INNER CLASSES.
	 */

	private class BoundingBoxPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		private final JCheckBox useRoi;

		private final JPanel roiPanel;

		private final BoundedInterval intervalT;

		public BoundingBoxPanel()
		{
			final int t0 = appModel.getMinTimepoint();
			final int t1 = appModel.getMaxTimepoint();
			this.intervalT = new BoundedInterval( t0, t1, t0, t1, 0 );

			final GridBagLayout layout = new GridBagLayout();
			layout.columnWidths = new int[] { 80 };
			layout.columnWeights = new double[] { 1. };
			setLayout( layout );

			final GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weighty = 0.;
			gbc.insets = new Insets( 5, 5, 5, 5 );

			final JLabel lblTitle = new JLabel( "Region of interest." );
			lblTitle.setFont( getFont().deriveFont( Font.BOLD ) );
			add( lblTitle, gbc );

			gbc.gridy++;
			gbc.anchor = GridBagConstraints.CENTER;
			this.useRoi = new JCheckBox( "Process only a ROI.", false );
			useRoi.addActionListener( ( e ) -> toggleBoundingBox( useRoi.isSelected() ) );
			add( useRoi, gbc );

			gbc.gridy++;
			gbc.weighty = 1.;
			gbc.fill = GridBagConstraints.BOTH;
			this.roiPanel = new JPanel();
			add( roiPanel, gbc );
			roiPanel.setLayout( new GridBagLayout() );
		}

		private void regen( final MyBoundingBoxModel roi )
		{
			roiPanel.removeAll();
			useRoi.setSelected( roi != null );

			if ( null != roi )
			{
				final GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridy = 0;
				gbc.gridx = 0;
				gbc.anchor = GridBagConstraints.NORTHWEST;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.weightx = 1.;

				final BoxSelectionPanel boxSelectionPanel = new BoxSelectionPanel( roi.box(), roi.getMaxInterval() );
				roiPanel.add( boxSelectionPanel, gbc );

				gbc.gridy++;
				gbc.insets = new Insets( 15, 5, 15, 5 );
				roiPanel.add( new JSeparator(), gbc );

				// Time panel
				gbc.gridy++;
				gbc.insets = new Insets( 0, 0, 0, 0 );
				final SliderPanel tMinPanel = new SliderPanel( "t min", intervalT.getMinBoundedValue(), 1 );
				tMinPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
				roiPanel.add( tMinPanel, gbc );

				gbc.gridy++;
				final SliderPanel tMaxPanel = new SliderPanel( "t max", intervalT.getMaxBoundedValue(), 1 );
				tMaxPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
				roiPanel.add( tMaxPanel, gbc );

				gbc.gridy++;
				gbc.insets = new Insets( 15, 5, 15, 5 );
				roiPanel.add( new JSeparator(), gbc );

				gbc.gridy++;
				gbc.fill = GridBagConstraints.NONE;
				gbc.insets = new Insets( 5, 5, 5, 5 );
				final BoxDisplayModePanel boxModePanel = new BoxDisplayModePanel( boundingBoxEditor.boxDisplayMode() );
				roiPanel.add( boxModePanel, gbc );

				gbc.gridy++;
				gbc.weighty = 1.;
				roiPanel.add( new JLabel(), gbc );

				roi.intervalChangedListeners().add( () -> boxSelectionPanel.updateSliders( roi.box().getInterval() ) );
			}

			roiPanel.revalidate();
			roiPanel.repaint();
		}
	}

	private static class MyBoundingBoxModel extends TransformedBoxModel
	{
		private final Interval maxInterval;

		public MyBoundingBoxModel(
				final Interval interval,
				final Interval maxInterval,
				final AffineTransform3D transform )
		{
			super( new ModifiableInterval( interval ), transform );
			this.maxInterval = new FinalInterval( maxInterval );
		}

		public Interval getMaxInterval()
		{
			return maxInterval;
		}
	}
}
