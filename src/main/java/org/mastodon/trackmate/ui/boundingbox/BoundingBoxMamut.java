package org.mastodon.trackmate.ui.boundingbox;

import java.awt.event.WindowAdapter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mastodon.revised.bdv.SharedBigDataViewerData;
import org.mastodon.revised.bdv.ViewerFrameMamut;
import org.mastodon.revised.mamut.MamutProject;
import org.mastodon.revised.mamut.MamutProjectIO;
import org.mastodon.revised.mamut.MamutViewBdv;
import org.mastodon.revised.mamut.ProjectManager;
import org.mastodon.revised.mamut.WindowManager;
import org.scijava.Context;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.viewer.Source;
import mpicbg.spim.data.SpimDataException;
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
public class BoundingBoxMamut
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

	private final BoundingBoxDialog dialog;

	private final BoundingBoxModel model;

	private final ViewerFrameMamut viewerFrame;

	private final BoundingBoxVisualizationMode bbVisualization;

	private final BoundingBoxEditMode bbEdit;

	private boolean editMode = false;

	public BoundingBoxMamut(
			final InputTriggerConfig keyconf,
			final ViewerFrameMamut viewerFrame,
			final SharedBigDataViewerData data,
			final int setupID,
			final String title )
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

		this.model = new BoundingBoxModel( interval, interval, sourceTransform );
		model.install( viewerFrame.getViewerPanel(), setupID );
		this.dialog = new BoundingBoxDialog(
				viewerFrame,
				title,
				model,
				viewerFrame.getViewerPanel(),
				data.getSetupAssignments(),
				interval );
		dialog.addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final java.awt.event.WindowEvent e )
			{
				toggleEditModeOff();
			};
		} );

		/*
		 * The actions in charge of toggling its visibility and its edit mode.
		 */

		this.bbEdit = new BoundingBoxEditMode( keyconf );
		this.bbVisualization = new BoundingBoxVisualizationMode( keyconf );
		toggleEditModeOff();
	}

	public void uninstall()
	{
		final TriggerBehaviourBindings triggerBehaviourBindings = viewerFrame.getTriggerbindings();
		triggerBehaviourBindings.removeInputTriggerMap( EDIT_MODE );
		triggerBehaviourBindings.removeBehaviourMap( EDIT_MODE );
		triggerBehaviourBindings.removeBehaviourMap( VISUALIZATION_MODE );
		triggerBehaviourBindings.removeInputTriggerMap( VISUALIZATION_MODE );
		dialog.setVisible( false );
	}

	public Interval getInterval()
	{
		return model.getInterval();
	}

	private void toggleEditModeOn()
	{
		if ( !dialog.isVisible() || editMode )
			return;

		editMode = true;

		dialog.boxOverlay.setEditMode( true );
		dialog.boxModePanel.modeLabel.setText( "Edit mode" );
		if ( !dialog.boxModePanel.full.isSelected() )
			dialog.boxModePanel.full.doClick();
		dialog.boxModePanel.setEnabled( false );

		final TriggerBehaviourBindings triggerBehaviourBindings = viewerFrame.getTriggerbindings();

		triggerBehaviourBindings.removeBehaviourMap( VISUALIZATION_MODE );
		triggerBehaviourBindings.removeInputTriggerMap( VISUALIZATION_MODE );

		triggerBehaviourBindings.addInputTriggerMap( EDIT_MODE, bbEdit.getInputTriggerMap(), "all", "navigation" );
		triggerBehaviourBindings.addBehaviourMap( EDIT_MODE, bbEdit.getBehaviourMap() );
	}

	private void toggleEditModeOff()
	{
		editMode = false;
		dialog.boxModePanel.modeLabel.setText( "Navigation mode" );
		dialog.boxOverlay.setEditMode( false );
		dialog.boxModePanel.setEnabled( true );

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
				dialog.setVisible( !dialog.isVisible() );
			}
		}
	}

	private class BoundingBoxEditMode extends Behaviours
	{

		private BoundingBoxEditMode( final InputTriggerConfig keyConfig )
		{
			super( keyConfig, "bdv" );
			behaviour( new ToggleEditModeBehaviour(),
					BOUNDING_BOX_TOGGLE_EDIT_MODE_OFF, BOUNDING_BOX_TOGGLE_EDIT_MODE_KEYS );
			behaviour( new BoundingBoxEditor( dialog.boxOverlay, viewerFrame.getViewerPanel(), dialog.boxSelectionPanel, model.getInterval() ),
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

	public static void main( final String[] args )
			throws IOException, SpimDataException, InvocationTargetException, InterruptedException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		Locale.setDefault( Locale.US );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		final Context context = new Context();
		final WindowManager windowManager = new WindowManager( context );
		final ProjectManager projectManager = windowManager.getProjectManager();
		final MamutProject project = new MamutProjectIO().load( "/Users/pietzsch/workspace/Mastodon/TrackMate3/samples/mamutproject" );
		projectManager.open( project );
		final MamutViewBdv[] bdv = new MamutViewBdv[ 1 ];
		SwingUtilities.invokeAndWait( () -> {
			bdv[ 0 ] = windowManager.createBigDataViewer();
		} );
		final ViewerFrameMamut viewerFrame = ( ViewerFrameMamut ) bdv[ 0 ].getFrame();
		final InputTriggerConfig keyconf = windowManager.getAppModel().getKeymap().getConfig();
		new BoundingBoxMamut(
				keyconf,
				viewerFrame,
				windowManager.getAppModel().getSharedBdvData(),
				0,
				"Test Bounding-box" );
	}


}
