package org.mastodon.trackmate.ui.boundingbox;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;

import bdv.util.Affine3DHelpers;
import net.imglib2.Interval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.OverlayRenderer;
import net.imglib2.ui.TransformListener;

import static org.mastodon.trackmate.ui.boundingbox.BoundingBoxOverlay.BoxDisplayMode.FULL;

public class BoundingBoxOverlay implements OverlayRenderer, TransformListener< AffineTransform3D >
{
	private static final double DISTANCE_TOLERANCE = 20.;

	private static final double HANDLE_RADIUS = DISTANCE_TOLERANCE / 2.;

	public enum BoxDisplayMode
	{
		FULL, SECTION;
	}

	public interface BoundingBoxOverlaySource
	{
		public RealInterval getInterval();

		public void getIntervalTransform( final AffineTransform3D transform );
	}

	public interface HighlightedCornerListener
	{
		public void highlightedCornerChanged();
	}

	private final BoundingBoxOverlaySource bbSource;

	private final Color backColor = new Color( 0x00994499 );

	private final Color frontColor = Color.GREEN;

	private final Stroke normalStroke = new BasicStroke();

	private final Stroke intersectionStroke = new BasicStroke( 1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] { 10f, 10f }, 0f );

	private final Paint intersectionColor = Color.WHITE.darker();

	private final AffineTransform3D viewerTransform;

	private final AffineTransform3D transform;

	final RenderBoxHelper renderBoxHelper;

	private final CornerHighlighter cornerHighlighter;

	private double perspective = 0.5;

	private int canvasWidth;

	private int canvasHeight;

	private BoxDisplayMode displayMode = FULL;

	private boolean showCornerHandles = true;

	private int cornerId;

	private HighlightedCornerListener highlightedCornerListener;

	public BoundingBoxOverlay( final Interval interval )
	{
		this( new BoundingBoxOverlaySource()
		{
			@Override
			public Interval getInterval()
			{
				return interval;
			}

			@Override
			public void getIntervalTransform( final AffineTransform3D transform )
			{
				transform.identity();
			}
		} );
	}

	public BoundingBoxOverlay( final BoundingBoxOverlaySource bbSource )
	{
		this.bbSource = bbSource;

		viewerTransform = new AffineTransform3D();
		transform = new AffineTransform3D();
		renderBoxHelper = new RenderBoxHelper();
		cornerHighlighter = new CornerHighlighter( DISTANCE_TOLERANCE );
	}

	/**
	 * Sets the perspective value. {@code perspective < 0} means parallel
	 * projection.
	 *
	 * @param perspective
	 *            the perspective value.
	 */
	public void setPerspective( final double perspective )
	{
		this.perspective = perspective;
	}

	public void showCornerHandles( final boolean showCornerHandles )
	{
		this.showCornerHandles = showCornerHandles;
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		final Graphics2D graphics = ( Graphics2D ) g;

		final GeneralPath front = new GeneralPath();
		final GeneralPath back = new GeneralPath();
		final GeneralPath intersection = new GeneralPath();

		final RealInterval interval = bbSource.getInterval();
		final double sourceSize; // Math.max( Math.max( interval.dimension( 0 ), interval.dimension( 1 ) ), interval.dimension( 2 ) );
		final double ox = canvasWidth / 2;
		final double oy = canvasHeight / 2;
		synchronized ( viewerTransform )
		{
			sourceSize = Affine3DHelpers.extractScale( viewerTransform, 0 ) * canvasWidth;
			bbSource.getIntervalTransform( transform );
			transform.preConcatenate( viewerTransform );
		}
		renderBoxHelper.setPerspectiveProjection( perspective > 0 );
		renderBoxHelper.setDepth( perspective * sourceSize );
		renderBoxHelper.setOrigin( ox, oy );
		renderBoxHelper.setScale( 1 );
		renderBoxHelper.renderBox( interval, transform, front, back, intersection );

		graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

		graphics.setPaint( intersectionColor );
		graphics.setStroke( intersectionStroke );
		graphics.draw( intersection );
		if ( displayMode == FULL )
		{
			graphics.setStroke( normalStroke );
			graphics.setPaint( backColor );
			graphics.draw( back );
			graphics.setPaint( frontColor );
			graphics.draw( front );
		}

		if ( showCornerHandles )
		{
			final int id = getHighlightedCornerIndex();
			if ( id >= 0 )
			{
				final double[] p = renderBoxHelper.projectedCorners[ id ];
				final Ellipse2D cornerHandle = new Ellipse2D.Double(
						p[ 0 ] - HANDLE_RADIUS,
						p[ 1 ] - HANDLE_RADIUS,
						2 * HANDLE_RADIUS, 2 * HANDLE_RADIUS );
				final double z = renderBoxHelper.corners[ cornerId ][ 2 ];
				final Color cornerColor = ( z > 0 ) ? backColor : frontColor;

				graphics.setColor( cornerColor );
				graphics.fill( cornerHandle );
				graphics.setColor( cornerColor.darker().darker() );
				graphics.draw( cornerHandle );
			}
		}
	}

	@Override
	public void setCanvasSize( final int width, final int height )
	{
		this.canvasWidth = width;
		this.canvasHeight = height;
	}

	@Override
	public void transformChanged( final AffineTransform3D t )
	{
		synchronized ( viewerTransform )
		{
			viewerTransform.set( t );
		}
	}

	public void setDisplayMode( final BoxDisplayMode mode )
	{
		this.displayMode = mode;

	}

	/**
	 * Get the transformation from the local coordinate frame of the
	 * {@link BoundingBoxOverlaySource} to viewer coordinates.
	 *
	 * @param t is set to the box-to-viewer transform.
	 */
	public void getBoxToViewerTransform( final AffineTransform3D t )
	{
		synchronized ( viewerTransform ) // not a typo, all transform modifications synchronize on viewerTransform
		{
			t.set( transform );
		}
	}

	/**
	 * Get the index of the highlighted corner (if any).
	 *
	 * @return corner index or {@code -1} if no corner is highlighted
	 */
	public int getHighlightedCornerIndex()
	{
		return cornerId;
	}

	public MouseMotionListener getCornerHighlighter()
	{
		return cornerHighlighter;
	}

	public void setHighlightedCornerListener( final HighlightedCornerListener highlightedCornerListener )
	{
		this.highlightedCornerListener = highlightedCornerListener;
	}

	/**
	 * Set the index of the highlighted corner.
	 *
	 * @param id
	 *            corner index, {@code -1} means that no corner is highlighted.
	 */
	private void setHighlightedCorner( final int id )
	{
		final int oldId = cornerId;
		cornerId = ( id >= 0 && id < RenderBoxHelper.numCorners ) ? id : -1;
		if ( cornerId != oldId && highlightedCornerListener != null )
			highlightedCornerListener.highlightedCornerChanged();
	}

	private class CornerHighlighter extends MouseMotionAdapter
	{
		private final double squTolerance;

		CornerHighlighter( final double tolerance )
		{
			squTolerance = tolerance * tolerance;
		}

		@Override
		public void mouseMoved( final MouseEvent e )
		{
			final int x = e.getX();
			final int y = e.getY();
			for ( int i = 0; i < RenderBoxHelper.numCorners; i++ )
			{
				final double[] corner = renderBoxHelper.projectedCorners[ i ];
				final double dx = x - corner[ 0 ];
				final double dy = y - corner[ 1 ];
				final double dr2 = dx * dx + dy * dy;
				if ( dr2 < squTolerance )
				{
					setHighlightedCorner( i );
					return;
				}
			}
			setHighlightedCorner( -1 );
		}
	}
}
