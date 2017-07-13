package org.mastodon.trackmate.ui.wizard.descriptors;

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
import org.mastodon.trackmate.ui.wizard.WizardPanelDescriptor;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.tools.boundingbox.BoxSelectionPanel;
import bdv.tools.brightness.SetupAssignments;
import bdv.viewer.Source;
import bdv.viewer.ViewerFrame;
import bdv.viewer.ViewerPanel;
import bdv.viewer.VisibilityAndGrouping;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;

public class BoundingBoxDescriptor extends WizardPanelDescriptor
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
		this.roi = new BoundingBoxModel( new FinalInterval( a, a ), new AffineTransform3D() );
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
			roi = getBoundingBoxModelFor();
			boxOverlay = new BoundingBoxOverlay( roi );
			boxOverlay.setPerspective( 0 );
			cornerHighlighter = boxOverlay.new CornerHighlighter();

			/*
			 * We also have to recreate the selection panel linked to the new
			 * ROI.
			 */
			panel.remove( panel.boxSelectionPanel );
			panel.boxSelectionPanel = new BoxSelectionPanel( roi.getInterval(), roi.getInterval() );
			panel.boxSelectionPanel.addSelectionUpdateListener( new BoxSelectionPanel.SelectionUpdateListener()
			{
				@Override
				public void selectionUpdated()
				{
					if ( null != viewerFrame )
						viewerFrame.getViewerPanel().requestRepaint();
				}
			} );

			final GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 2;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weighty = 1.;
			gbc.insets = new Insets( 5, 5, 5, 5 );
			setPanelEnabled( panel.boxSelectionPanel, panel.useRoi.isSelected() );
			setPanelEnabled( panel.boxModePanel, panel.useRoi.isSelected() );
			panel.add( panel.boxSelectionPanel, gbc );

			panel.boxSelectionPanel.setBoundsInterval( roi.getInterval() );
			panel.boxSelectionPanel.updateSliders( roi.getInterval() );
			previousSetupID = setupID;
		}

		toggleBoundingBox( panel.useRoi.isSelected() );
	}

	@Override
	public void aboutToHidePanel()
	{
		toggleBoundingBox( false );
		settings.values.getDetectorSettings().put( KEY_ROI, roi.getInterval() );
	}

	@Override
	public String getBackPanelDescriptorIdentifier()
	{
		return SetupIdDecriptor.IDENTIFIER;
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
	private BoundingBoxModel getBoundingBoxModelFor()
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
		}

		return new BoundingBoxModel( interval, sourceTransform );
	}

	private void toggleBoundingBox( final boolean useRoi )
	{
		final BoundingBoxPanel panel = ( BoundingBoxPanel ) targetPanel;
		setPanelEnabled( panel.boxSelectionPanel, useRoi );
		setPanelEnabled( panel.boxModePanel, useRoi );

		if ( useRoi )
		{
			showViewer();
			final int setupId = ( int ) settings.values.getDetectorSettings().get( KEY_SETUP_ID );
			roi.install( viewerFrame.getViewerPanel(), setupId );
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
			if ( viewer.isShowing() )
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
		final ViewerPanel viewer = viewerFrame.getViewerPanel();
		final SetupAssignments setupAssignments = wm.getSharedBigDataViewerData().getSetupAssignments();

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

		final TriggerBehaviourBindings triggerBehaviourBindings = viewerFrame.getTriggerbindings();
		triggerBehaviourBindings.removeInputTriggerMap( EDIT_MODE );
		triggerBehaviourBindings.removeBehaviourMap( EDIT_MODE );
		bbVisualization.install( viewerFrame.getTriggerbindings(), VISUALIZATION_MODE );
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

		private final JCheckBox useRoi;

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
			this.boxSelectionPanel = new BoxSelectionPanel( roi.getInterval(), roi.getInterval() );
			boxSelectionPanel.addSelectionUpdateListener( new BoxSelectionPanel.SelectionUpdateListener()
			{
				@Override
				public void selectionUpdated()
				{
					if ( null != viewerFrame )
						viewerFrame.getViewerPanel().requestRepaint();
				}
			} );

			add( boxSelectionPanel, gbc );

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

}
