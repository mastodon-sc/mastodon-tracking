package org.mastodon.trackmate.ui.boundingbox;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mastodon.revised.bdv.ViewerFrameMamut;
import org.mastodon.revised.mamut.MamutProject;
import org.mastodon.revised.mamut.MamutProjectIO;
import org.mastodon.revised.mamut.MamutViewBdv;
import org.mastodon.revised.mamut.ProjectManager;
import org.mastodon.revised.mamut.WindowManager;
import org.scijava.Context;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;

import mpicbg.spim.data.SpimDataException;

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

		final BoundingBoxMamut boundingBoxMamut = new BoundingBoxMamut(
				keyconf,
				viewerFrame,
				windowManager.getAppModel().getSharedBdvData(),
				0,
				"Test Bounding-box" );


		final Actions actions = new Actions( keyconf, "bbtest" );
		actions.install( viewerFrame.getKeybindings(), "bbtest" );
		actions.runnableAction( boundingBoxMamut::toggle, TOGGLE_BOUNDING_BOX, TOGGLE_BOUNDING_BOX_KEYS );
	}
}
