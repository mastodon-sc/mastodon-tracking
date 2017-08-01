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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;

import org.mastodon.revised.bdv.SharedBigDataViewerData;
import org.mastodon.revised.mamut.BdvManager.BdvWindow;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.ui.boundingbox.BoundingBoxEditor;
import org.mastodon.trackmate.ui.boundingbox.BoundingBoxModel;
import org.mastodon.trackmate.ui.boundingbox.BoundingBoxOverlay;
import org.mastodon.trackmate.ui.boundingbox.BoundingBoxOverlay.CornerHighlighter;
import org.mastodon.trackmate.ui.boundingbox.BoundingBoxOverlay.DisplayMode;
import org.mastodon.trackmate.ui.wizard.WizardLogService;
import org.mastodon.trackmate.ui.wizard.WizardPanelDescriptor;
import org.scijava.Context;
import org.scijava.Contextual;
import org.scijava.NullContextException;
import org.scijava.plugin.Parameter;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.tools.boundingbox.BoxSelectionPanel;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.brightness.SliderPanel;
import bdv.util.BoundedValue;
import bdv.viewer.Source;
import bdv.viewer.ViewerFrame;
import bdv.viewer.ViewerPanel;
import bdv.viewer.VisibilityAndGrouping;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;

public class BoundingBoxDescriptor extends WizardPanelDescriptor implements Contextual
{

	public static final String IDENTIFIER = "Setup bounding-box";

	private static final String BOUNDING_BOX_TOGGLE_EDIT_MODE_ON = "togggle bounding-box edit-mode on";

	private static final String EDIT_MODE = "edit bounding-box";

	private static final String BOUNDING_BOX_TOGGLE_EDIT_MODE_OFF = "togggle bounding-box edit-mode off";

	private static final String[] BOUNDING_BOX_TOGGLE_EDIT_MODE_KEYS = new String[] { "ESCAPE" };

	private static final String VISUALIZATION_MODE = "bounding-box";

	private static final String BOUNDING_BOX_TOGGLE_EDITOR = "edit bounding-box";

	private static final String BOUNDING_BOX_TOGGLE_EDITOR_KEYS = "button1";

	private static final boolean showBoxSource = true;

	private static final boolean showBoxOverlay = true;

	@Parameter
	private WizardLogService log;

	private final Settings settings;

	private final WindowManager wm;

	private BoundingBoxOverlay boxOverlay;

	private CornerHighlighter cornerHighlighter;

	private ViewerFrame viewerFrame;

	private BoundingBoxModel roi;

	private BoundingBoxEditMode bbEdit;

	private BoundingBoxVisualizationMode bbVisualization;

	public BoundingBoxDescriptor( final Settings settings, final WindowManager wm )
	{
		this.settings = settings;
		this.wm = wm;
		this.panelIdentifier = IDENTIFIER;
		final long[] a = Util.getArrayFromValue( 1000l, 3 );
		this.roi = new BoundingBoxModel( new FinalInterval( a, a ), new FinalInterval( a, a ), new AffineTransform3D() );
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
			hideOverlay();

			/*
			 * Change ROI source and overlay.
			 */
			roi = getBoundingBoxModel();
			boxOverlay = new BoundingBoxOverlay( roi );
			boxOverlay.setPerspective( 0 );
			cornerHighlighter = boxOverlay.new CornerHighlighter();

			/*
			 * We also have to recreate the selection panel linked to the new
			 * ROI.
			 */
			panel.remove( panel.boundsPanel );
			panel.boundsPanel = new JPanel();
			panel.boundsPanel.setLayout( new BoxLayout( panel.boundsPanel, BoxLayout.PAGE_AXIS ) );

			panel.boxSelectionPanel = new BoxSelectionPanel( roi.getInterval(), roi.getMaxInterval() );
			panel.boxSelectionPanel.addSelectionUpdateListener( new BoxSelectionPanel.SelectionUpdateListener()
			{
				@Override
				public void selectionUpdated()
				{
					if ( null != viewerFrame )
						viewerFrame.getViewerPanel().requestRepaint();
				}
			} );

			/*
			 * We reset time bounds.
			 */

			final int nTimepoints = wm.getSharedBigDataViewerData().getNumTimepoints();

			panel.minT = new BoundedValue( 0, nTimepoints, 0 );
			final SliderPanel tMinPanel = new SliderPanel( "t min", panel.minT, 1 );
			tMinPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );

			panel.maxT = new BoundedValue( 0, nTimepoints, nTimepoints );
			final SliderPanel tMaxPanel = new SliderPanel( "t max", panel.maxT, 1 );
			tMaxPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );

			panel.boundsPanel.add( panel.boxSelectionPanel );
			panel.boundsPanel.add( tMinPanel );
			panel.boundsPanel.add( tMaxPanel );

			final GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 2;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weighty = 1.;
			gbc.insets = new Insets( 5, 5, 5, 5 );
			setPanelEnabled( panel.boundsPanel, panel.useRoi.isSelected() );
			setPanelEnabled( panel.boxModePanel, panel.useRoi.isSelected() );
			panel.add( panel.boundsPanel, gbc );

			panel.boxSelectionPanel.setBoundsInterval( roi.getMaxInterval() );
			panel.boxSelectionPanel.updateSliders( roi.getInterval() );
			previousSetupID = setupID;
		}
		else
		{
			panel.minT.setCurrentValue( ( int ) settings.values.getDetectorSettings().get( KEY_MIN_TIMEPOINT ) );
			panel.maxT.setCurrentValue( ( int ) settings.values.getDetectorSettings().get( KEY_MAX_TIMEPOINT ) );
		}

		toggleBoundingBox( panel.useRoi.isSelected() );
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

		toggleEditModeOff();
		toggleBoundingBox( false );
		log.info( info );
		log.info( String.format( "  - min time-point: %d\n", ( int ) settings.values.getDetectorSettings().get( KEY_MIN_TIMEPOINT ) ) );
		log.info( String.format( "  - max time-point: %d\n", ( int ) settings.values.getDetectorSettings().get( KEY_MAX_TIMEPOINT ) ) );
	}

	@Override
	public String getNextPanelDescriptorIdentifier()
	{
		return ChooseDetectorDescriptor.IDENTIFIER;
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
	 *
	 * @return a new {@link BoundingBoxModel}.
	 */
	private BoundingBoxModel getBoundingBoxModel()
	{
		Interval interval = ( Interval ) settings.values.getDetectorSettings().get( KEY_ROI );
		final int setupID = ( int ) settings.values.getDetectorSettings().get( KEY_SETUP_ID );

		final SharedBigDataViewerData data = wm.getSharedBigDataViewerData();
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
		return new BoundingBoxModel( interval, maxInterval, sourceTransform );
	}

	private void toggleBoundingBox( final boolean useRoi )
	{
		final BoundingBoxPanel panel = ( BoundingBoxPanel ) targetPanel;
		setPanelEnabled( panel.boundsPanel, useRoi );
		setPanelEnabled( panel.boxModePanel, useRoi );

		if ( useRoi )
		{
			showViewer();
			showOverlay();
		}
		else if ( viewerFrame != null )
		{
			hideOverlay();
		}
	}

	private void hideOverlay()
	{
		if ( null == viewerFrame )
			return;

		final ViewerPanel viewer = viewerFrame.getViewerPanel();
		final SetupAssignments setupAssignments = wm.getSharedBigDataViewerData().getSetupAssignments();

		if ( showBoxSource )
		{
			if ( viewer.isShowing() && null != roi.getBoxSourceAndConverter() )
				viewer.removeSource( roi.getBoxSourceAndConverter().getSpimSource() );
			setupAssignments.removeSetup( roi.getBoxConverterSetup() );
		}
		if ( showBoxOverlay )
		{
			viewer.getDisplay().removeOverlayRenderer( boxOverlay );
			viewer.removeTransformListener( boxOverlay );
		}
		viewer.getDisplay().removeHandler( cornerHighlighter );
	}

	private void showOverlay()
	{
		final SetupAssignments setupAssignments = wm.getSharedBigDataViewerData().getSetupAssignments();
		final ViewerPanel viewer = viewerFrame.getViewerPanel();
		final int setupId = ( int ) settings.values.getDetectorSettings().get( KEY_SETUP_ID );
		roi.install( viewerFrame.getViewerPanel(), setupId );

		if ( showBoxSource )
		{
			viewer.addSource( roi.getBoxSourceAndConverter() );
			setupAssignments.addSetup( roi.getBoxConverterSetup() );
			roi.getBoxConverterSetup().setViewer( viewer );

			final int bbSourceIndex = viewer.getState().numSources() - 1;
			final VisibilityAndGrouping vg = viewer.getVisibilityAndGrouping();
			if ( vg.getDisplayMode() != bdv.viewer.DisplayMode.FUSED )
			{
				for ( int i = 0; i < bbSourceIndex; ++i )
					vg.setSourceActive( i, vg.isSourceVisible( i ) );
				vg.setDisplayMode( bdv.viewer.DisplayMode.FUSED );
			}
			vg.setSourceActive( bbSourceIndex, true );
			vg.setCurrentSource( bbSourceIndex );
		}
		if ( showBoxOverlay )
		{
			viewer.getDisplay().addOverlayRenderer( boxOverlay );
			viewer.addRenderTransformListener( boxOverlay );
		}
		viewer.getDisplay().addHandler( cornerHighlighter );
	}

	private void showViewer()
	{
		final List< BdvWindow > bdvWindows = wm.getMamutWindowModel().getBdvWindows();
		if ( bdvWindows == null || bdvWindows.isEmpty() )
			viewerFrame = wm.createBigDataViewer();
		else
			viewerFrame = bdvWindows.get( 0 ).getViewerFrame();

		final InputTriggerConfig keyconf = wm.getMamutWindowModel().getInputTriggerConfig();
		this.bbEdit = new BoundingBoxEditMode( keyconf );
		this.bbVisualization = new BoundingBoxVisualizationMode( keyconf );
		toggleEditModeOff();
		viewerFrame.toFront();
	}

	private boolean editMode = false;

	private void toggleEditModeOn()
	{
		final BoundingBoxPanel panel = ( BoundingBoxPanel ) targetPanel;

		if ( editMode || !panel.useRoi.isSelected() )
			return;

		editMode = true;
		boxOverlay.setEditMode( true );

		panel.boxModePanel.modeToggle.setText( "Edit mode" );
		if ( !panel.boxModePanel.full.isSelected() )
			panel.boxModePanel.full.doClick();
		setPanelEnabled( panel.boxModePanel, false );
		panel.boxModePanel.modeToggle.setEnabled( true );
		panel.boxModePanel.modeToggle.setSelected( true );

		final TriggerBehaviourBindings triggerBehaviourBindings = viewerFrame.getTriggerbindings();
		triggerBehaviourBindings.removeBehaviourMap( VISUALIZATION_MODE );
		triggerBehaviourBindings.removeInputTriggerMap( VISUALIZATION_MODE );
		triggerBehaviourBindings.addInputTriggerMap( EDIT_MODE, bbEdit.getInputTriggerMap(), "all", "navigation" );
		triggerBehaviourBindings.addBehaviourMap( EDIT_MODE, bbEdit.getBehaviourMap() );
	}

	private void toggleEditModeOff()
	{
		editMode = false;
		boxOverlay.setEditMode( false );

		final BoundingBoxPanel panel = ( BoundingBoxPanel ) targetPanel;
		panel.boxModePanel.modeToggle.setText( "Navigation mode" );
		panel.boxModePanel.modeToggle.setSelected( false );
		setPanelEnabled( panel.boxModePanel, true );

		if ( null != viewerFrame )
		{
			final TriggerBehaviourBindings triggerBehaviourBindings = viewerFrame.getTriggerbindings();
			triggerBehaviourBindings.removeInputTriggerMap( EDIT_MODE );
			triggerBehaviourBindings.removeBehaviourMap( EDIT_MODE );
			bbVisualization.install( viewerFrame.getTriggerbindings(), VISUALIZATION_MODE );
		}
	}

	private class BoundingBoxVisualizationMode extends Behaviours
	{

		public BoundingBoxVisualizationMode( final InputTriggerConfig keyConfig )
		{
			super( keyConfig, "bdv" );
			behaviour( new ToggleEditMode(), BOUNDING_BOX_TOGGLE_EDIT_MODE_ON, BOUNDING_BOX_TOGGLE_EDIT_MODE_KEYS );
		}

		private class ToggleEditMode implements ClickBehaviour
		{

			@Override
			public void click( final int x, final int y )
			{
				toggleEditModeOn();
			}
		}
	}

	private class BoundingBoxEditMode extends Behaviours
	{

		private BoundingBoxEditMode( final InputTriggerConfig keyConfig )
		{
			super( keyConfig, "bdv" );
			final BoundingBoxPanel panel = ( BoundingBoxPanel ) targetPanel;
			behaviour( new ToggleEditModeBehaviour(),
					BOUNDING_BOX_TOGGLE_EDIT_MODE_OFF, BOUNDING_BOX_TOGGLE_EDIT_MODE_KEYS );
			behaviour( new BoundingBoxEditor( boxOverlay, viewerFrame.getViewerPanel(), panel.boxSelectionPanel, roi.getInterval() ),
					BOUNDING_BOX_TOGGLE_EDITOR, BOUNDING_BOX_TOGGLE_EDITOR_KEYS );
		}

		private class ToggleEditModeBehaviour implements ClickBehaviour
		{

			@Override
			public void click( final int x, final int y )
			{
				toggleEditModeOff();
			}
		}
	}

	private class BoundingBoxPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		private BoxSelectionPanel boxSelectionPanel;

		private final BoxModePanel boxModePanel;

		private JPanel boundsPanel;

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
			useRoi.addActionListener( ( e ) -> toggleBoundingBox( useRoi.isSelected() ) );
			add( useRoi, gbc );

			gbc.gridy++;
			this.boxSelectionPanel = new BoxSelectionPanel( roi.getInterval(), roi.getMaxInterval() );
			boxSelectionPanel.addSelectionUpdateListener( new BoxSelectionPanel.SelectionUpdateListener()
			{
				@Override
				public void selectionUpdated()
				{
					if ( null != viewerFrame )
						viewerFrame.getViewerPanel().requestRepaint();
				}
			} );

			// Time panel
			final int nTimepoints = wm.getSharedBigDataViewerData().getNumTimepoints();
			final int minTimepoint = ( int ) settings.values.getDetectorSettings().get( KEY_MIN_TIMEPOINT );
			final int maxTimepoint = ( int ) settings.values.getDetectorSettings().get( KEY_MAX_TIMEPOINT );

			this.minT = new BoundedValue( 0, nTimepoints, minTimepoint );
			final SliderPanel tMinPanel = new SliderPanel( "t min", minT, 1 );
			tMinPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );

			this.maxT = new BoundedValue( 0, nTimepoints, maxTimepoint );
			final SliderPanel tMaxPanel = new SliderPanel( "t max", maxT, 1 );
			tMaxPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );

			this.boundsPanel = new JPanel();
			boundsPanel.setLayout( new BoxLayout( boundsPanel, BoxLayout.PAGE_AXIS ) );
			boundsPanel.add( boxSelectionPanel );
			boundsPanel.add( tMinPanel );
			boundsPanel.add( tMaxPanel );
			add( boundsPanel, gbc );

			gbc.gridy++;
			this.boxModePanel = new BoxModePanel();
			add( boxModePanel, gbc );
		}
	}

	private class BoxModePanel extends JPanel
	{
		private static final long serialVersionUID = 1L;

		final JRadioButton full;

		final JToggleButton modeToggle;

		public BoxModePanel()
		{
			final GridBagLayout layout = new GridBagLayout();
			layout.columnWidths = new int[] { 80, 80 };
			layout.columnWeights = new double[] { 0.5, 0.5 };
			setLayout( layout );
			final GridBagConstraints gbc = new GridBagConstraints();

			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.gridwidth = 2;
			gbc.anchor = GridBagConstraints.BASELINE_LEADING;
			gbc.insets = new Insets( 5, 5, 5, 5 );

			final JLabel overlayLabel = new JLabel( "Overlay:", JLabel.LEFT );
			overlayLabel.setFont( getFont().deriveFont( Font.BOLD ) );
			add( overlayLabel, gbc );

			gbc.gridy++;
			gbc.gridwidth = 1;
			this.full = new JRadioButton( "Full" );
			final JRadioButton section = new JRadioButton( "Section" );
			final ActionListener l = new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					if ( null != boxOverlay && null == viewerFrame )
						return;

					boxOverlay.setDisplayMode( full.isSelected() ? DisplayMode.FULL : DisplayMode.SECTION );
					viewerFrame.getViewerPanel().requestRepaint();
				}
			};
			full.addActionListener( l );
			section.addActionListener( l );
			final ButtonGroup group = new ButtonGroup();
			group.add( full );
			group.add( section );
			full.setSelected( true );
			add( full, gbc );
			gbc.gridx++;
			add( section, gbc );

			gbc.gridy++;
			gbc.gridx = 0;
			this.modeToggle = new JToggleButton( "Navigation mode", false );
			modeToggle.addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					if ( modeToggle.isSelected() )
						toggleEditModeOn();
					else
						toggleEditModeOff();
				}
			} );
			add( modeToggle, gbc );
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
