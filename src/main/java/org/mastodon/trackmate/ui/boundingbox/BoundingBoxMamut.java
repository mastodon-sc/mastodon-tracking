package org.mastodon.trackmate.ui.boundingbox;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mastodon.revised.bdv.SharedBigDataViewerData;
import org.mastodon.revised.bdv.ViewerFrameMamut;
import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.BehaviourMap;
import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.util.ModifiableInterval;
import bdv.viewer.Source;
import bdv.viewer.ViewerPanel;
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
	public void toggle()
	{
		dialog.setVisible( !dialog.isVisible() );
	}

	private final BoundingBoxOverlay boxOverlay;

	private static final String BOUNDING_BOX_TOGGLE_EDITOR = "edit bounding-box";

	private static final String[] BOUNDING_BOX_TOGGLE_EDITOR_KEYS = new String[] { "button1" };

	private static final String BOUNDING_BOX_MAP = "bounding-box";

	private static final String BLOCKING_MAP = "bounding-box-blocking";

	private final BoundingBoxDialog dialog;

	private final BoundingBoxModel model;

	private final ViewerFrameMamut viewerFrame;

	private final ModifiableInterval mInterval;

	private final TriggerBehaviourBindings triggerbindings;

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

		mInterval = new ModifiableInterval( interval );
		this.model = new BoundingBoxModel( mInterval, sourceTransform );


		/*
		 * Create an Overlay to show 3D wireframe box
		 */
		boxOverlay = new BoundingBoxOverlay( model );
//		boxOverlay.setPerspective( 0 );
		boxOverlay.setHighlightedCornerListener( this::highlightedCornerChanged );

		/*
		 * Create bounding box dialog.
		 */

		model.install( viewerFrame.getViewerPanel(), setupID );
		this.dialog = new BoundingBoxDialog(
				viewerFrame,
				title,
				mInterval,
				model,
				viewerFrame.getViewerPanel(),
				data.getSetupAssignments(),
				interval );
		dialog.addComponentListener( new ComponentAdapter()
		{
			@Override
			public void componentShown( final ComponentEvent e )
			{
				install();
			}

			@Override
			public void componentHidden( final ComponentEvent e )
			{
				uninstall();
			}
		} );

		/*
		 * Create DragBoxCornerBehaviour
		 */

		behaviours = new Behaviours( keyconf, "bdv" );
		behaviours.behaviour( new DragBoxCornerBehaviour( boxOverlay, viewerFrame.getViewerPanel(), dialog.boxSelectionPanel, mInterval ),
				BOUNDING_BOX_TOGGLE_EDITOR, BOUNDING_BOX_TOGGLE_EDITOR_KEYS );

		/*
		 * Create BehaviourMap to block behaviours interfering with
		 * DragBoxCornerBehaviour. The block map is only active while a corner
		 * is highlighted.
		 */
		blockMap = new BehaviourMap();
		refreshBlockMap();
	}

	public void install()
	{
		final ViewerPanel viewer = viewerFrame.getViewerPanel();

		viewer.getDisplay().addOverlayRenderer( boxOverlay );
		viewer.addRenderTransformListener( boxOverlay );
		viewer.getDisplay().addHandler( boxOverlay.getCornerHighlighter() );

		behaviours.install( triggerbindings, BOUNDING_BOX_MAP );
	}

	public void uninstall()
	{
		final ViewerPanel viewer = viewerFrame.getViewerPanel();

		viewer.getDisplay().removeOverlayRenderer( boxOverlay );
		viewer.removeTransformListener( boxOverlay );
		viewer.getDisplay().removeHandler( boxOverlay.getCornerHighlighter() );

		triggerbindings.removeInputTriggerMap( BOUNDING_BOX_MAP );
		triggerbindings.removeBehaviourMap( BOUNDING_BOX_MAP );
	}

	private void highlightedCornerChanged()
	{
		final int index = boxOverlay.getHighlightedCornerIndex();
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

	public Interval getInterval()
	{
		return mInterval;
	}

	private void toggleEditModeOn()
	{
		// TODO: this should influence whether editing is possible
		if ( !dialog.boxModePanel.full.isSelected() )
			dialog.boxModePanel.full.doClick();
		dialog.boxModePanel.setEnabled( false );
	}
}
