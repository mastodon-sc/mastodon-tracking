package org.mastodon.tracking.mamut.trackmate.wizard;

import java.util.Locale;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.mastodon.mamut.MamutViewBdv;
import org.mastodon.mamut.ProjectManager;
import org.mastodon.mamut.WindowManager;
import org.mastodon.mamut.project.MamutProject;
import org.mastodon.mamut.project.MamutProjectIO;
import org.mastodon.util.ToggleDialogAction;
import org.mastodon.views.bdv.ViewerFrameMamut;
import org.mastodon.views.bdv.ViewerPanelMamut;
import org.scijava.Context;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;

import bdv.tools.boundingbox.BoxSelectionOptions;
import bdv.tools.boundingbox.TransformedBoxSelectionDialog;
import bdv.tools.brightness.SetupAssignments;
import bdv.viewer.ConverterSetups;
import bdv.viewer.Source;
import bdv.viewer.ViewerState;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

/**
 * Example of how to make a bounding box dialog.
 *
 * @author Tobias Pietzsch
 * @author Jean-Yves Tinevez
 */
public class BoundingBoxDialog
{
	static final String TOGGLE_BOUNDING_BOX = "toggle bounding-box";

	static final String[] TOGGLE_BOUNDING_BOX_KEYS = new String[] { "V" };

	public static void main( final String[] args ) throws Exception
	{
		Locale.setDefault( Locale.US );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		/*
		 * Load a Mastodon project.
		 */
		final Context context = new Context();
		final WindowManager windowManager = new WindowManager( context );
		final ProjectManager projectManager = windowManager.getProjectManager();
		final MamutProject project = new MamutProjectIO().load( "../mastodon/samples/mamutproject" );
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
		final ViewerState state = viewer.state();
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

		final ConverterSetups converterSetups = windowManager.getAppModel().getSharedBdvData().getConverterSetups();
		final int boxSetupId = SetupAssignments.getUnusedSetupId( windowManager.getAppModel().getSharedBdvData().getSetupAssignments() );
		final JDialog dialog = new TransformedBoxSelectionDialog(
				viewer,
				converterSetups,
				boxSetupId,
				keyconf,
				viewerFrame.getTriggerbindings(),
				sourceTransform,
				interval,
				interval,
				BoxSelectionOptions.options().title( "Test Bounding-Box" ) );

		/*
		 * Install a action to toggle the dialog
		 */
		final Actions actions = new Actions( keyconf, "bbtest" );
		actions.install( viewerFrame.getKeybindings(), "bbtest" );
		actions.namedAction( new ToggleDialogAction( TOGGLE_BOUNDING_BOX, dialog ), TOGGLE_BOUNDING_BOX_KEYS );
	}
}
