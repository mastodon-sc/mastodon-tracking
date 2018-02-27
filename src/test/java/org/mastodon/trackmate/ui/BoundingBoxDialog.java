package org.mastodon.trackmate.ui;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Locale;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.mastodon.revised.bdv.ViewerFrameMamut;
import org.mastodon.revised.bdv.ViewerPanelMamut;
import org.mastodon.revised.mamut.MamutProject;
import org.mastodon.revised.mamut.MamutProjectIO;
import org.mastodon.revised.mamut.MamutViewBdv;
import org.mastodon.revised.mamut.ProjectManager;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.revised.util.ToggleDialogAction;
import org.mastodon.trackmate.ui.boundingbox.BoundingBoxMamut;
import org.mastodon.trackmate.ui.boundingbox.BoundingBoxOverlay;
import org.mastodon.trackmate.ui.boundingbox.BoxModePanel;
import org.mastodon.trackmate.ui.boundingbox.DefaultBoundingBoxModel;
import org.mastodon.trackmate.ui.boundingbox.tobdv.BoxSelectionPanel;
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
 * Example of how to wire up a bounding box dialog using
 * {@link BoxSelectionPanel}, {@link BoxModePanel}, and
 * {@link BoundingBoxOverlay}.
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

		/*
		 * Initialize bounding box model from the computed interval.
		 */
		final DefaultBoundingBoxModel model = new DefaultBoundingBoxModel( new ModifiableInterval( interval ), sourceTransform );

		/*
		 * Create bounding box overlay and editor.
		 */
		final BoundingBoxMamut boundingBoxMamut = new BoundingBoxMamut(
				keyconf,
				viewerFrame.getViewerPanel(),
				windowManager.getAppModel().getSharedBdvData().getSetupAssignments(),
				viewerFrame.getTriggerbindings(),
				model,
				true );
		boundingBoxMamut.setPerspective( 1, 1000 );

		/*
		 * Create bounding box dialog.
		 */
		final JDialog dialog = new JDialog( viewerFrame, "Test Bounding-box" );
		final BoxSelectionPanel boxSelectionPanel = new BoxSelectionPanel( model, interval );
		final BoxModePanel boxModePanel = new BoxModePanel();
		dialog.getContentPane().add( boxSelectionPanel, BorderLayout.NORTH );
		dialog.getContentPane().add( boxModePanel, BorderLayout.SOUTH );
		dialog.pack();
		dialog.setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
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
		boxModePanel.modeChangeListeners().add( () -> boundingBoxMamut.setBoxDisplayMode( boxModePanel.getBoxDisplayMode() ) );
		model.intervalChangedListeners().add( () -> {
			boxSelectionPanel.updateSliders( model.getInterval() );
			viewer.getDisplay().repaint();
		});

		/*
		 * Install a action to toggle the dialog
		 */
		final Actions actions = new Actions( keyconf, "bbtest" );
		actions.install( viewerFrame.getKeybindings(), "bbtest" );
		actions.namedAction( new ToggleDialogAction( TOGGLE_BOUNDING_BOX, dialog ), TOGGLE_BOUNDING_BOX_KEYS );
	}
}
