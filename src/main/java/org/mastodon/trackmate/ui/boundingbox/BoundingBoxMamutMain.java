package org.mastodon.trackmate.ui.boundingbox;

import java.awt.Frame;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Locale;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.mastodon.revised.bdv.ViewerFrameMamut;
import org.mastodon.revised.bdv.ViewerPanelMamut;
import org.mastodon.revised.mamut.MamutProject;
import org.mastodon.revised.mamut.MamutProjectIO;
import org.mastodon.revised.mamut.MamutViewBdv;
import org.mastodon.revised.mamut.ProjectManager;
import org.mastodon.revised.mamut.WindowManager;
import org.scijava.Context;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;

import bdv.util.ModifiableInterval;
import bdv.viewer.Source;
import bdv.viewer.state.ViewerState;
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
public class BoundingBoxMamutMain
{
	static final String TOGGLE_BOUNDING_BOX = "toggle bounding-box";

	static final String[] TOGGLE_BOUNDING_BOX_KEYS = new String[] { "V" };

	public static void main( final String[] args ) throws Exception
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
		final ViewerPanelMamut viewer = viewerFrame.getViewerPanel();
		final InputTriggerConfig keyconf = windowManager.getAppModel().getKeymap().getConfig();


		/*
		 * Compute an initial interval from the specified setup id.
		 */
		final int setupID = 0;
		final ViewerState state = viewer.getState();
		final Source< ? > source = state.getSources().get( setupID ).getSpimSource();
		final int numTimepoints = state.getNumTimepoints();
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
		final DefaultBoundingBoxModel model = new DefaultBoundingBoxModel( new ModifiableInterval( interval ), sourceTransform );



		final Frame owner = viewerFrame; // for BoundingBoxDialog...
		final BoundingBoxMamut boundingBoxMamut = new BoundingBoxMamut(
				keyconf,
				viewerFrame.getViewerPanel(),
				windowManager.getAppModel().getSharedBdvData().getSetupAssignments(),
				viewerFrame.getTriggerbindings(),
				model,
				true );







		/*
		 * Create bounding box dialog.
		 */
		final BoundingBoxDialog dialog = new BoundingBoxDialog(
				owner,
				"Test Bounding-box",
				model,
				interval );
		dialog.addComponentListener( new ComponentAdapter()
		{
			@Override
			public void componentShown( final ComponentEvent e )
			{
				boundingBoxMamut.install();
			}

			@Override
			public void componentHidden( final ComponentEvent e )
			{
				boundingBoxMamut.uninstall();
			}
		} );
		dialog.boxModePanel.addListener( () -> boundingBoxMamut.setBoxDisplayMode( dialog.boxModePanel.getBoxDisplayMode() ) );
		model.intervalChangedListeners().add( () -> {
			dialog.boxSelectionPanel.updateSliders( model.getInterval() );
			viewer.getDisplay().repaint();
		});

		final Actions actions = new Actions( keyconf, "bbtest" );
		actions.install( viewerFrame.getKeybindings(), "bbtest" );
		actions.runnableAction( () -> dialog.setVisible( !dialog.isVisible() ), TOGGLE_BOUNDING_BOX, TOGGLE_BOUNDING_BOX_KEYS );
	}
}
