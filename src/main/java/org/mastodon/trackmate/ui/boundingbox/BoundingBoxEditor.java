package org.mastodon.trackmate.ui.boundingbox;

import org.scijava.ui.behaviour.DragBehaviour;

import bdv.tools.boundingbox.BoxSelectionPanel;
import bdv.util.ModifiableInterval;
import bdv.viewer.ViewerPanel;
import net.imglib2.FinalInterval;
import net.imglib2.realtransform.AffineTransform3D;

public class BoundingBoxEditor implements DragBehaviour
{
	private final BoundingBoxOverlay boxOverlay;

	private boolean moving = false;

	private final ViewerPanel viewerPanel;

	private final BoxSelectionPanel boxSelectionPanel;

	private final ModifiableInterval interval;

	private final long[] initMin = new long[ 3 ];

	private final long[] initMax = new long[ 3 ];

	private final double[] initCorner = new double[ 3 ];

	private int cornerId;

	public BoundingBoxEditor( final BoundingBoxOverlay boxOverlay, final ViewerPanel viewerPanel, final BoxSelectionPanel boxSelectionPanel, final ModifiableInterval interval )
	{
		this.boxOverlay = boxOverlay;
		this.viewerPanel = viewerPanel;
		this.boxSelectionPanel = boxSelectionPanel;
		this.interval = interval;
	}

	@Override
	public void init( final int x, final int y )
	{
		cornerId = boxOverlay.getHighlightedCornerIndex();
		if ( cornerId < 0 )
			return;

		IntervalCorners.corner( interval, cornerId, initCorner );
		interval.min( initMin );
		interval.max( initMax );

		moving = true;
	}

	private final AffineTransform3D transform = new AffineTransform3D();

	@Override
	public void drag( final int x, final int y )
	{
		if ( !moving )
			return;

		boxOverlay.getBoxToViewerTransform( transform );
		final double[] gPos = new double[ 3 ];
		transform.apply( initCorner, gPos );
		final double[] lPos = boxOverlay.renderBoxHelper.reproject( x, y, gPos[ 2 ] );
		transform.applyInverse( gPos, lPos );

		final long[] min = new long[ 3 ];
		final long[] max = new long[ 3 ];
		for ( int d = 0; d < 3; ++d )
		{
			final long p = Math.round( gPos[ d ] );
			if ( ( cornerId & ( 1 << d ) ) == 0 )
			{
				min[ d ] = p;
				if ( p > initMax[ d ] )
					initMax[ d ] = p;
				max[ d ] = initMax[ d ];
			}
			else
			{
				max[ d ] = p;
				if ( p < initMin[ d ] )
					initMin[ d ] = p;
				min[ d ] = initMin[ d ];
			}
		}

		interval.set( new FinalInterval( min, max ) );
		boxSelectionPanel.updateSliders( interval );
		viewerPanel.requestRepaint();
	}

	@Override
	public void end( final int x, final int y )
	{
		moving = false;
		viewerPanel.requestRepaint();
	}
}
