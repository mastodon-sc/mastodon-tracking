package org.mastodon.trackmate.ui.boundingbox;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
import org.mastodon.revised.util.ToggleDialogAction;
import org.scijava.Context;
import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.BehaviourMap;
import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.util.ModifiableInterval;
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

	static final String[] TOGGLE_BOUNDING_BOX_KEYS = new String[] { "V" };

	private static final String BOUNDING_BOX_TOGGLE_EDITOR = "edit bounding-box";

	private static final String[] BOUNDING_BOX_TOGGLE_EDITOR_KEYS = new String[] { "button1" };

	private static final String BOUNDING_BOX_MAP = "bounding-box";

	private static final String BLOCKING_MAP = "bounding-box-blocking";

	private final BoundingBoxDialog dialog;

	private final BoundingBoxModel model;

	private final ViewerFrameMamut viewerFrame;

	private boolean editMode = false;

	private final ModifiableInterval mInterval;

	private final TriggerBehaviourBindings triggerbindings;

	private final InputActionBindings keybindings;

	private final Behaviours behaviours;

	private final BehaviourMap blockMap;

	private final InputTriggerConfig keyconf;

	public BoundingBoxMamut(
			final InputTriggerConfig keyconf,
			final ViewerFrameMamut viewerFrame,
			final SharedBigDataViewerData data,
			final int setupID,
			final String title )
	{
		this.keyconf = keyconf;
		this.viewerFrame = viewerFrame;
		triggerbindings = viewerFrame.getTriggerbindings();
		keybindings = viewerFrame.getKeybindings();

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

		mInterval = new ModifiableInterval( interval );
		this.model = new BoundingBoxModel( mInterval, sourceTransform );
		model.install( viewerFrame.getViewerPanel(), setupID );
		this.dialog = new BoundingBoxDialog(
				viewerFrame,
				title,
				mInterval,
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
		dialog.addComponentListener( new ComponentAdapter()
		{
			@Override
			public void componentShown( final ComponentEvent e )
			{
				behaviours.install( triggerbindings, BOUNDING_BOX_MAP );
			}

			@Override
			public void componentHidden( final ComponentEvent e )
			{
				triggerbindings.removeInputTriggerMap( BOUNDING_BOX_MAP );
				triggerbindings.removeBehaviourMap( BOUNDING_BOX_MAP );
			}
		} );

		/*
		 * The actions in charge of toggling its visibility and its edit mode.
		 */

		final Actions actions = new Actions( keyconf, "bdv " );
		actions.install( keybindings, BOUNDING_BOX_MAP );
		actions.namedAction( new ToggleDialogAction( TOGGLE_BOUNDING_BOX, dialog ), TOGGLE_BOUNDING_BOX_KEYS );

		behaviours = new Behaviours( keyconf, "bdv" );
		behaviours.behaviour( new BoundingBoxEditor( dialog.boxOverlay, viewerFrame.getViewerPanel(), dialog.boxSelectionPanel, mInterval ),
				BOUNDING_BOX_TOGGLE_EDITOR, BOUNDING_BOX_TOGGLE_EDITOR_KEYS );

		blockMap = new BehaviourMap();
		refreshBlockMap();

		dialog.boxOverlay.setHighlightedCornerListener( this::highlightedCornerChanged );
		toggleEditModeOff();
	}

	private void highlightedCornerChanged()
	{
		final int index = dialog.boxOverlay.getHighlightedCornerIndex();
		if ( index < 0 )
			unblock();
		else
			block();
	}

	private void block()
	{
		triggerbindings.addBehaviourMap( BLOCKING_MAP, blockMap );
	}

	private void unblock()
	{
		triggerbindings.removeBehaviourMap( BLOCKING_MAP );
	}

	private void refreshBlockMap()
	{
		triggerbindings.removeBehaviourMap( BLOCKING_MAP );

		final Set< InputTrigger > moveCornerTriggers = new HashSet<>();
		for ( final String s : BOUNDING_BOX_TOGGLE_EDITOR_KEYS )
			moveCornerTriggers.add( InputTrigger.getFromString( s ) );

		final Map< InputTrigger, Set< String > > bindings = triggerbindings.getConcatenatedInputTriggerMap().getAllBindings();
		final Set< String > behavioursToBlock = new HashSet<>();
		for ( final InputTrigger t : moveCornerTriggers )
			behavioursToBlock.addAll( bindings.get( t ) );

		blockMap.clear();
		final Behaviour block = new Behaviour() {};
		for ( final String key : behavioursToBlock )
			blockMap.put( key, block );
	}

	public void uninstall()
	{
		triggerbindings.removeInputTriggerMap( BOUNDING_BOX_MAP );
		triggerbindings.removeBehaviourMap( BOUNDING_BOX_MAP );

		keybindings.removeInputMap( BOUNDING_BOX_MAP );
		keybindings.removeActionMap( BOUNDING_BOX_MAP );

		dialog.setVisible( false );
	}

	public Interval getInterval()
	{
		return mInterval;
	}

	private void toggleEditModeOn()
	{
		if ( !dialog.isVisible() || editMode )
			return;

		editMode = true;

		// TODO: this should influence whether editing is possible
		if ( !dialog.boxModePanel.full.isSelected() )
			dialog.boxModePanel.full.doClick();
		dialog.boxModePanel.setEnabled( false );

		viewerFrame.getViewerPanel().requestRepaint();
	}

	private void toggleEditModeOff()
	{
		editMode = false;
		dialog.boxModePanel.setEnabled( true );
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
