package org.mastodon.trackmate.ui.boundingbox;

import javax.swing.JPanel;

import org.mastodon.revised.bdv.SharedBigDataViewerData;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.viewer.Source;
import bdv.viewer.ViewerFrame;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

/**
 * Installs an interactive bounding-box tool on a BDV.
 * <p>
 * The feature consists of a bounding-box model, a dialog and an overlay added
 * to the BDV. The user can edit the bounding-box using a GUI panel, or directly
 * interacting with the overlay using an 'edit' mode. The user switches between
 * normal ('visualization') and 'edit' modes by pressing the ESC key. In 'edit'
 * mode, the mouse button is used to drag the corners of the bounding-box.
 * <p>
 * Adapted from code by Tobias Pietzsch and others taken in the BDV core
 * packages.
 *
 */
public class BoundingBoxMamut2
{

	private static final String TOGGLE_BOUNDING_BOX = "toggle bounding-box";

	private static final String BOUNDING_BOX_TOGGLE_EDIT_MODE_ON = "togggle bounding-box edit-mode on";

	private static final String EDIT_MODE = "edit bounding-box";

	private static final String BOUNDING_BOX_TOGGLE_EDIT_MODE_OFF = "togggle bounding-box edit-mode off";

	static final String TOGGLE_BOUNDING_BOX_KEYS = "V";

	private static final String[] BOUNDING_BOX_TOGGLE_EDIT_MODE_KEYS = new String[] { "ESCAPE" };

	private static final String VISUALIZATION_MODE = "bounding-box";

	private static final String BOUNDING_BOX_TOGGLE_EDITOR = "edit bounding-box";

	private static final String BOUNDING_BOX_TOGGLE_EDITOR_KEYS = "button1";

	private final BoundingBoxControlPanel controlPanel;

	private final BoundingBoxModel model;

	private final ViewerFrame viewerFrame;

	private final BoundingBoxVisualizationMode bbVisualization;

	private final BoundingBoxEditMode bbEdit;

	private boolean editMode = false;

	public BoundingBoxMamut2(
			final InputTriggerConfig keyconf,
			final ViewerFrame viewerFrame,
			final SharedBigDataViewerData data,
			final int setupID )
	{
		this.viewerFrame = viewerFrame;

		/*
		 * Compute an initial interval from the specified setup id.
		 */
		final Source< ? > source = data.getSources().get( setupID ).getSpimSource();
		final int numTimepoints = data.getNumTimepoints();
		int tp = 0;
		Interval interval = null;
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		while ( tp++ < numTimepoints )
		{
			if ( source.isPresent( tp ) )
			{
				final RandomAccessibleInterval< ? > intervalPix = source.getSource( tp, 0 );
				source.getSourceTransform( tp, 0, sourceTransform );
				interval = intervalPix;
				break;
			}
		}
		if ( null == interval )
			interval = Intervals.createMinMax( 0, 0, 0, 1, 1, 1 );

		/*
		 * Create bounding box dialog.
		 */

		this.model = new BoundingBoxModel( interval, sourceTransform  );
		model.install( viewerFrame.getViewerPanel(), setupID);
		this.controlPanel = new BoundingBoxControlPanel(
				model,
				viewerFrame.getViewerPanel(),
				data.getSetupAssignments(),
				interval, true, true );

		/*
		 * The actions in charge of toggling its visibility and its edit mode.
		 */

		this.bbEdit = new BoundingBoxEditMode( keyconf );
		this.bbVisualization = new BoundingBoxVisualizationMode( keyconf );
		toggleEditModeOff();
	}

	public JPanel getControlPanel()
	{
		return controlPanel;
	}

	public void uninstall()
	{
		final TriggerBehaviourBindings triggerBehaviourBindings = viewerFrame.getTriggerbindings();
		triggerBehaviourBindings.removeInputTriggerMap( EDIT_MODE );
		triggerBehaviourBindings.removeBehaviourMap( EDIT_MODE );
		triggerBehaviourBindings.removeBehaviourMap( VISUALIZATION_MODE );
		triggerBehaviourBindings.removeInputTriggerMap( VISUALIZATION_MODE );
		controlPanel.setVisible( false );
	}

	public Interval getInterval()
	{
		return model.getInterval();
	}

	private void toggleEditModeOn()
	{
		if ( editMode )
			return;

		editMode = true;

		controlPanel.boxOverlay.editMode = true;
		controlPanel.boxModePanel.modeLabel.setText( "Edit mode" );
		if ( !controlPanel.boxModePanel.full.isSelected() )
			controlPanel.boxModePanel.full.doClick();
		controlPanel.boxModePanel.setEnabled( false );

		final TriggerBehaviourBindings triggerBehaviourBindings = viewerFrame.getTriggerbindings();

		triggerBehaviourBindings.removeBehaviourMap( VISUALIZATION_MODE );
		triggerBehaviourBindings.removeInputTriggerMap( VISUALIZATION_MODE );

		triggerBehaviourBindings.addInputTriggerMap( EDIT_MODE, bbEdit.getInputTriggerMap(), "all", "navigation" );
		triggerBehaviourBindings.addBehaviourMap( EDIT_MODE, bbEdit.getBehaviourMap() );
	}

	private void toggleEditModeOff()
	{
		editMode = false;
		controlPanel.boxModePanel.modeLabel.setText( "Navigation mode" );
		controlPanel.boxOverlay.editMode = false;
		controlPanel.boxModePanel.setEnabled( true );

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

			behaviour( new ToggleDialogBehaviour(), TOGGLE_BOUNDING_BOX, TOGGLE_BOUNDING_BOX_KEYS );
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

		private class ToggleDialogBehaviour implements ClickBehaviour
		{
			@Override
			public void click( final int x, final int y )
			{
				controlPanel.setVisible( !controlPanel.isVisible() );
			}
		}
	}

	private class BoundingBoxEditMode extends Behaviours
	{

		private BoundingBoxEditMode( final InputTriggerConfig keyConfig )
		{
			super( keyConfig, "bdv" );
			behaviour( new ToggleEditModeBehaviour(), BOUNDING_BOX_TOGGLE_EDIT_MODE_OFF, BoundingBoxMamut2.BOUNDING_BOX_TOGGLE_EDIT_MODE_KEYS );
			behaviour( new BoundingBoxEditor( controlPanel.boxOverlay, model, viewerFrame.getViewerPanel(), controlPanel.boxSelectionPanel ), BOUNDING_BOX_TOGGLE_EDITOR, BOUNDING_BOX_TOGGLE_EDITOR_KEYS );
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
}
