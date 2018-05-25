package org.mastodon.trackmate.ui.boundingbox;

import static org.mastodon.trackmate.ui.boundingbox.BoundingBoxOverlay.BoxDisplayMode.FULL;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mastodon.trackmate.ui.boundingbox.BoundingBoxOverlay.BoxDisplayMode;
import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.BehaviourMap;
import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.tools.brightness.SetupAssignments;
import bdv.viewer.ViewerPanel;

/**
 * Installs an interactive bounding-box tool on a BDV.
 * <p>
 * The feature consists of an overlay added to the BDV and editing behaviours
 * where the user can edit the bounding-box directly interacting with the
 * overlay. The mouse button is used to drag the corners of the bounding-box.
 *
 * @author Tobias Pietzsch
 * @author Jean-Yves Tinevez
 */
public class BoundingBoxEditor
{
	private static final String BOUNDING_BOX_TOGGLE_EDITOR = "edit bounding-box";

	private static final String[] BOUNDING_BOX_TOGGLE_EDITOR_KEYS = new String[] { "button1" };

	private static final String BOUNDING_BOX_MAP = "bounding-box";

	private static final String BLOCKING_MAP = "bounding-box-blocking";

	private final BoundingBoxOverlay boxOverlay;

	private final BoundingBoxSource boxSource;

	private final ViewerPanel viewer;

	private final TriggerBehaviourBindings triggerbindings;

	private final Behaviours behaviours;

	private final BehaviourMap blockMap;

	private boolean editable = true;

	public enum BoxSourceType
	{
		NONE,
		VIRTUAL,
		PLACEHOLDER
	}

	public BoundingBoxEditor(
			final InputTriggerConfig keyconf,
			final ViewerPanel viewer,
			final SetupAssignments setupAssignments,
			final TriggerBehaviourBindings triggerbindings,
			final BoundingBoxModel model )
	{
		this( keyconf, viewer, setupAssignments, triggerbindings, model, "selection", BoxSourceType.PLACEHOLDER );
	}

	public BoundingBoxEditor(
			final InputTriggerConfig keyconf,
			final ViewerPanel viewer,
			final SetupAssignments setupAssignments,
			final TriggerBehaviourBindings triggerbindings,
			final BoundingBoxModel model,
			final String boxSourceName,
			final BoxSourceType boxSourceType )
	{
		this.viewer = viewer;
		this.triggerbindings = triggerbindings;

		/*
		 * Create an Overlay to show 3D wireframe box
		 */
		boxOverlay = new BoundingBoxOverlay( model );
		boxOverlay.setPerspective( 0 );

		/*
		 * Create a BDV source to show bounding box slice
		 */
		switch ( boxSourceType )
		{
		case PLACEHOLDER:
			boxSource = new BoundingBoxPlaceholderSource( boxSourceName, boxOverlay, model, viewer, setupAssignments );
			break;
		case VIRTUAL:
			boxSource = new BoundingBoxVirtualSource( boxSourceName, model, viewer, setupAssignments );
			boxOverlay.fillIntersection( false );
			break;

		case NONE:
		default:
			boxSource = null;
			break;
		}

		/*
		 * Create DragBoxCornerBehaviour
		 */

		behaviours = new Behaviours( keyconf, "bdv" );
		behaviours.behaviour( new DragBoxCornerBehaviour( boxOverlay, model ), BOUNDING_BOX_TOGGLE_EDITOR, BOUNDING_BOX_TOGGLE_EDITOR_KEYS );

		/*
		 * Create BehaviourMap to block behaviours interfering with
		 * DragBoxCornerBehaviour. The block map is only active while a corner
		 * is highlighted.
		 */
		blockMap = new BehaviourMap();
	}

	public void install()
	{
		viewer.getDisplay().addOverlayRenderer( boxOverlay );
		viewer.addRenderTransformListener( boxOverlay );
		viewer.getDisplay().addHandler( boxOverlay.getCornerHighlighter() );

		refreshBlockMap();
		updateEditability();

		if ( boxSource != null )
			boxSource.addToViewer();
	}

	public void uninstall()
	{
		viewer.getDisplay().removeOverlayRenderer( boxOverlay );
		viewer.removeTransformListener( boxOverlay );
		viewer.getDisplay().removeHandler( boxOverlay.getCornerHighlighter() );

		triggerbindings.removeInputTriggerMap( BOUNDING_BOX_MAP );
		triggerbindings.removeBehaviourMap( BOUNDING_BOX_MAP );

		unblock();

		if ( boxSource != null )
			boxSource.removeFromViewer();
	}

	public BoxDisplayMode getBoxDisplayMode()
	{
		return boxOverlay.getDisplayMode();
	}

	public void setBoxDisplayMode( final BoxDisplayMode mode )
	{
		boxOverlay.setDisplayMode( mode );
		viewer.requestRepaint();
		updateEditability();

	}

	public boolean isEditable()
	{
		return editable;
	}

	public void setEditable( final boolean editable )
	{
		if ( this.editable == editable )
			return;
		this.editable = editable;
		boxOverlay.showCornerHandles( editable );
		updateEditability();
	}

	/**
	 * Sets up perspective projection for the overlay. Basically, the projection
	 * center is placed at distance {@code perspective * sourceSize} from the
	 * projection plane (screen). Specify {@code perspective = 0} to set
	 * parallel projection.
	 * 
	 * @param perspective
	 *            the perspective value.
	 * @param sourceSize
	 *            the the size of the source.
	 */
	public void setPerspective( final double perspective, final double sourceSize )
	{
		boxOverlay.setPerspective( perspective );
		boxOverlay.setSourceSize( sourceSize );
	}


	private void updateEditability()
	{
		if ( editable && boxOverlay.getDisplayMode() == FULL )
		{
			boxOverlay.setHighlightedCornerListener( this::highlightedCornerChanged );
			behaviours.install( triggerbindings, BOUNDING_BOX_MAP );
			highlightedCornerChanged();
		}
		else
		{
			boxOverlay.setHighlightedCornerListener( null );
			triggerbindings.removeInputTriggerMap( BOUNDING_BOX_MAP );
			triggerbindings.removeBehaviourMap( BOUNDING_BOX_MAP );
			unblock();
		}
	}

	private void block()
	{
		triggerbindings.addBehaviourMap( BLOCKING_MAP, blockMap );
	}

	private void unblock()
	{
		triggerbindings.removeBehaviourMap( BLOCKING_MAP );
	}

	private void highlightedCornerChanged()
	{
		final int index = boxOverlay.getHighlightedCornerIndex();
		if ( index < 0 )
			unblock();
		else
			block();
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
}
