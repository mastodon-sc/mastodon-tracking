package org.mastodon.trackmate.ui.wizard.descriptors;

import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_ROI;
import static org.mastodon.detection.DetectorKeys.KEY_SETUP_ID;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mastodon.revised.bdv.SharedBigDataViewerData;
import org.mastodon.revised.bdv.ViewerFrameMamut;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.ui.boundingbox.BoundingBoxEditor;
import org.mastodon.trackmate.ui.boundingbox.BoundingBoxEditor.BoxSourceType;
import org.mastodon.trackmate.ui.boundingbox.BoxModePanel;
import org.mastodon.trackmate.ui.boundingbox.DefaultBoundingBoxModel;
import org.mastodon.trackmate.ui.wizard.WizardLogService;
import org.mastodon.trackmate.ui.wizard.WizardPanelDescriptor;
import org.scijava.Context;
import org.scijava.Contextual;
import org.scijava.NullContextException;
import org.scijava.plugin.Parameter;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.tools.boundingbox.BoundingBoxUtil;
import bdv.tools.boundingbox.BoxSelectionPanel;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.brightness.SliderPanel;
import bdv.util.BoundedValue;
import bdv.util.ModifiableInterval;
import bdv.viewer.Source;
import bdv.viewer.ViewerPanel;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;

public class BoundingBoxDescriptor extends WizardPanelDescriptor implements Contextual
{
	public static final String IDENTIFIER = "Setup bounding-box";

	private final Settings settings;

	private final WindowManager wm;

	private BoundingBoxEditor boundingBoxEditor;

	private ViewerFrameMamut viewFrame;

	private static class MyBoundingBoxModel extends DefaultBoundingBoxModel
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

	private MyBoundingBoxModel roi;

	private final WizardLogService log;

	public BoundingBoxDescriptor( final Settings settings, final WindowManager wm, final WizardLogService log )
	{
		this.settings = settings;
		this.wm = wm;
		this.log = log;
		this.panelIdentifier = IDENTIFIER;
		final long[] a = Util.getArrayFromValue( 1000l, 3 );
		this.roi = new MyBoundingBoxModel( new FinalInterval( a, a ), new FinalInterval( a, a ), new AffineTransform3D() );
		this.targetPanel = new BoundingBoxPanel();
	}

	private int previousSetupID = -1;

	@Override
	public void aboutToDisplayPanel()
	{
		final BoundingBoxPanel panel = ( BoundingBoxPanel ) targetPanel;
		if ( null == settings.values.getDetectorSettings().get( KEY_SETUP_ID ) )
		{
			panel.removeAll();
			panel.add( new JLabel( "Error: setup ID is null." ) );
			setPanelEnabled( panel, false );
			return;
		}

		final int setupID = ( int ) settings.values.getDetectorSettings().get( KEY_SETUP_ID );
		if ( setupID != previousSetupID )
		{
			// Remove old overlay.
			if ( boundingBoxEditor != null )
				boundingBoxEditor.uninstall();
			viewFrame = null;

			/*
			 * Change ROI source and overlay.
			 */
			roi = getBoundingBoxModel();
			roi.intervalChangedListeners().add( () -> {
				panel.boxSelectionPanel.updateSliders( roi.getInterval() );
				if ( viewFrame != null )
					viewFrame.getViewerPanel().getDisplay().repaint();
			} );

			/*
			 * We also have to recreate the selection panel linked to the new
			 * ROI.
			 */
			panel.boxSelectionPanel = new BoxSelectionPanel( roi, roi.getMaxInterval() );

			/*
			 * We reset time bounds.
			 */

			final int nTimepoints = wm.getAppModel().getMaxTimepoint() - wm.getAppModel().getMinTimepoint();

			panel.minT = new BoundedValue( 0, nTimepoints, 0 );
			final SliderPanel tMinPanel = new SliderPanel( "t min", panel.minT, 1 );
			tMinPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );

			panel.maxT = new BoundedValue( 0, nTimepoints, nTimepoints );
			final SliderPanel tMaxPanel = new SliderPanel( "t max", panel.maxT, 1 );
			tMaxPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );

			panel.boundsPanel.removeAll();
			panel.boundsPanel.add( panel.boxSelectionPanel );
			panel.boundsPanel.add( tMinPanel );
			panel.boundsPanel.add( tMaxPanel );

			setPanelEnabled( panel.boundsPanel, panel.useRoi.isSelected() );
			setPanelEnabled( panel.boxModePanel, panel.useRoi.isSelected() );

			panel.boxSelectionPanel.setBoundsInterval( roi.getMaxInterval() );
			panel.boxSelectionPanel.updateSliders( roi.getInterval() );

			previousSetupID = setupID;
		}
		else
		{
			panel.minT.setCurrentValue( ( int ) settings.values.getDetectorSettings().get( KEY_MIN_TIMEPOINT ) );
			panel.maxT.setCurrentValue( ( int ) settings.values.getDetectorSettings().get( KEY_MAX_TIMEPOINT ) );
		}

		toggleBoundingBoxVisibility( panel.useRoi.isSelected() );
	}

	@Override
	public void aboutToHidePanel()
	{
		final BoundingBoxPanel panel = ( BoundingBoxPanel ) targetPanel;
		final String info;
		if ( panel.useRoi.isSelected() )
		{
			settings.values.getDetectorSettings().put( KEY_ROI, roi.getInterval() );
			info = "Processing within ROI with bounds: " + Util.printInterval( roi.getInterval() ) + '\n';
		}
		else
		{
			settings.values.getDetectorSettings().put( KEY_ROI, null );
			info = "Processing whole image.\n";
		}

		settings.values.getDetectorSettings().put( KEY_MIN_TIMEPOINT, panel.minT.getCurrentValue() );
		settings.values.getDetectorSettings().put( KEY_MAX_TIMEPOINT, panel.maxT.getCurrentValue() );

		toggleBoundingBoxVisibility( false );
		log.log( info );
		log.log( String.format( "  - min time-point: %d\n", ( int ) settings.values.getDetectorSettings().get( KEY_MIN_TIMEPOINT ) ) );
		log.log( String.format( "  - max time-point: %d\n", ( int ) settings.values.getDetectorSettings().get( KEY_MAX_TIMEPOINT ) ) );
	}

	private void setPanelEnabled( final JPanel panel, final boolean isEnabled )
	{
		panel.setEnabled( isEnabled );
		final Component[] components = panel.getComponents();
		for ( int i = 0; i < components.length; i++ )
		{
			if ( components[ i ] instanceof JPanel )
				setPanelEnabled( ( JPanel ) components[ i ], isEnabled );

			components[ i ].setEnabled( isEnabled );
		}
	}

	/**
	 * Build a model for the bounding-box from the settings passed to this
	 * descriptor or sensible defaults if there are no settings yet.
	 */
	private MyBoundingBoxModel getBoundingBoxModel()
	{
		Interval interval = ( Interval ) settings.values.getDetectorSettings().get( KEY_ROI );
		final int setupID = ( int ) settings.values.getDetectorSettings().get( KEY_SETUP_ID );

		final SharedBigDataViewerData data = wm.getAppModel().getSharedBdvData();
		final Source< ? > source = data.getSources().get( setupID ).getSpimSource();
		final int numTimepoints = data.getNumTimepoints();
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		int tp = 0;
		Interval maxInterval = null;
		while ( tp++ < numTimepoints )
		{
			if ( source.isPresent( tp ) )
			{
				maxInterval = source.getSource( tp, 0 );
				source.getSourceTransform( tp, 0, sourceTransform );
				break;
			}
		}
		if ( maxInterval != null )
		{
			if ( null == interval )
				interval = maxInterval;
			else
				// Intersection.
				interval = Intervals.intersect( interval, maxInterval );
		}
		else
		{
			if ( null == interval )
				interval = Intervals.createMinMax( 0, 0, 0, 1, 1, 1 );
			maxInterval = interval;
		}

		return new MyBoundingBoxModel( interval, maxInterval, sourceTransform );
	}

	private void toggleBoundingBoxVisibility( final boolean useRoi )
	{
		final BoundingBoxPanel panel = ( BoundingBoxPanel ) targetPanel;
		setPanelEnabled( panel.boundsPanel, useRoi );
		setPanelEnabled( panel.boxModePanel, useRoi );

		if ( useRoi )
		{
			showViewer();
		}
		else
		{
			if ( boundingBoxEditor != null )
				boundingBoxEditor.uninstall();
		}
	}

	private void showViewer()
	{
		final ViewerFrameMamut oldViewFrame = viewFrame;

		// Is there a BDV open?
		if ( viewFrame == null || !viewFrame.isShowing() )
			wm.forEachBdvView( view -> viewFrame = ( ViewerFrameMamut ) view.getFrame() );

		// Create one
		if ( viewFrame == null )
			viewFrame = ( ViewerFrameMamut ) wm.createBigDataViewer().getFrame();

		if ( oldViewFrame != viewFrame )
		{
			if ( boundingBoxEditor != null )
			{
				boundingBoxEditor.uninstall();
				boundingBoxEditor = null;
			}

			final InputTriggerConfig keyconf = wm.getAppModel().getKeymap().getConfig();
			final ViewerPanel viewer = viewFrame.getViewerPanel();
			final SetupAssignments setupAssignments = wm.getAppModel().getSharedBdvData().getSetupAssignments();
			final TriggerBehaviourBindings triggerBindings = viewFrame.getTriggerbindings();
			boundingBoxEditor = new BoundingBoxEditor( keyconf, viewer, setupAssignments, triggerBindings, roi, "ROI", BoxSourceType.PLACEHOLDER );

			final Interval bb = BoundingBoxUtil.getSourcesBoundingBox( viewFrame.getViewerPanel().getState() );
			final double sourceSize = Math.max( Math.max( bb.dimension( 0 ), bb.dimension( 1 ) ), bb.dimension( 2 ) );
			boundingBoxEditor.setPerspective( 1, Math.max( sourceSize, 1 ) );
		}
		boundingBoxEditor.install();
		viewFrame.toFront();
	}

	private class BoundingBoxPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		private BoxSelectionPanel boxSelectionPanel;

		private final BoxModePanel boxModePanel;

		private final JPanel boundsPanel;

		private final JCheckBox useRoi;

		private BoundedValue minT;

		private BoundedValue maxT;

		private BoundingBoxPanel()
		{
			final GridBagLayout layout = new GridBagLayout();
			layout.columnWidths = new int[] { 80 };
			layout.columnWeights = new double[] { 1. };
			setLayout( layout );

			final GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weighty = 1.;
			gbc.insets = new Insets( 5, 5, 5, 5 );

			final JLabel lblTitle = new JLabel( "Region of interest." );
			lblTitle.setFont( getFont().deriveFont( Font.BOLD ) );
			add( lblTitle, gbc );

			gbc.gridy++;
			gbc.anchor = GridBagConstraints.CENTER;
			this.useRoi = new JCheckBox( "Process only a ROI.", false );
			useRoi.addActionListener( ( e ) -> toggleBoundingBoxVisibility( useRoi.isSelected() ) );
			add( useRoi, gbc );

			gbc.gridy++;
			this.boxSelectionPanel = new BoxSelectionPanel( roi, roi.getMaxInterval() );

			// Time panel
			final int nTimepoints = wm.getAppModel().getMaxTimepoint() - wm.getAppModel().getMinTimepoint();
			minT = new BoundedValue( 0, nTimepoints, 0 );
			maxT = new BoundedValue( 0, nTimepoints, nTimepoints );

			boundsPanel = new JPanel();
			boundsPanel.setLayout( new BoxLayout( boundsPanel, BoxLayout.PAGE_AXIS ) );
			add( boundsPanel, gbc );

			gbc.gridy++;
			boxModePanel = new BoxModePanel();
			boxModePanel.modeChangeListeners().add( () -> {
				if ( boundingBoxEditor != null )
					boundingBoxEditor.setBoxDisplayMode( boxModePanel.getBoxDisplayMode() );
			} );
			add( boxModePanel, gbc );
		}
	}

	// -- Contextual methods --

	@Parameter
	private Context context;

	@Override
	public Context context()
	{
		if ( context == null )
			throw new NullContextException();
		return context;
	}

	@Override
	public Context getContext()
	{
		return context;
	}
}
