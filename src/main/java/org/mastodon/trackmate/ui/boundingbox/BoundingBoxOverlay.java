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
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;

import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.OverlayRenderer;
import net.imglib2.ui.TransformListener;

public class BoundingBoxOverlay implements OverlayRenderer, TransformListener< AffineTransform3D >
{
	public static enum DisplayMode
	{
		FULL, SECTION;
	}

	public static interface BoundingBoxOverlaySource
	{
		public Interval getInterval();

		public void getIntervalTransform( final AffineTransform3D transform );
	}

	final BoundingBoxOverlaySource model;

	private final Color backColor = new Color( 0x00994499 );// Color.MAGENTA;

	private final Color frontColor = Color.GREEN;

	private final Stroke normalStroke = new BasicStroke();

	private final Stroke intersectionStroke = new BasicStroke( 1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] { 10f, 10f }, 0f );

	private final Paint intersectionColor = Color.WHITE.darker();

	private Color cornerColor = frontColor;

	private final AffineTransform3D viewerTransform;

	private final AffineTransform3D transform;

	final RenderBoxHelper renderBoxHelper;

	private double perspective = 3;

	private int canvasWidth;

	private int canvasHeight;

	private DisplayMode displayMode = DisplayMode.FULL;

	private boolean editMode;

	private Ellipse2D cornerHandle;

	int cornerId;

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
		this.model = bbSource;
		this.viewerTransform = new AffineTransform3D();
		this.transform = new AffineTransform3D();
		this.renderBoxHelper = new RenderBoxHelper();
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

	public void setEditMode( final boolean editMode )
	{
		this.editMode = editMode;
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		final Graphics2D graphics = ( Graphics2D ) g;

		final GeneralPath front = new GeneralPath();
		final GeneralPath back = new GeneralPath();
		final GeneralPath intersection = new GeneralPath();

		final Interval interval = model.getInterval();
		final double sourceSize = Math.max( Math.max( interval.dimension( 0 ), interval.dimension( 1 ) ), interval.dimension( 2 ) );
		final double ox = canvasWidth / 2;
		final double oy = canvasHeight / 2;
		synchronized ( viewerTransform )
		{
			model.getIntervalTransform( transform );
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
		if ( displayMode == DisplayMode.FULL )
		{
			graphics.setStroke( normalStroke );
			graphics.setPaint( backColor );
			graphics.draw( back );
			graphics.setPaint( frontColor );
			graphics.draw( front );
		}

		if ( cornerHandle != null )
		{
			graphics.setColor( cornerColor );
			graphics.fill( cornerHandle );
			graphics.setColor( cornerColor.darker().darker() );
			graphics.draw( cornerHandle );
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

	public void setDisplayMode( final DisplayMode mode )
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

	public class CornerHighlighter extends MouseMotionAdapter
	{
		private static final double DISTANCE_TOLERANCE = 20.;

		private static final double HANDLE_RADIUS = DISTANCE_TOLERANCE / 2.;

		private static final double SQU_DISTANCE_TOLERANCE = DISTANCE_TOLERANCE * DISTANCE_TOLERANCE;

		@Override
		public void mouseMoved( final MouseEvent e )
		{
			final int x = e.getX();
			final int y = e.getY();
			cornerHandle = null;
			cornerId = -1;
			for ( int i = 0; i < RenderBoxHelper.numCorners; i++ )
			{
				final double[] corner = renderBoxHelper.projectedCorners[ i ];
				final double dx = x - corner[ 0 ];
				final double dy = y - corner[ 1 ];
				final double dr2 = dx * dx + dy * dy;
				if ( dr2 < SQU_DISTANCE_TOLERANCE )
				{
					cornerId = i;
					regen();
					return;
				}
			}
		}

		@Override
		public void mouseDragged( final MouseEvent e )
		{
			if ( cornerId < 0 )
				return;

			regen();
		}

		private void regen()
		{
			final double[] p = renderBoxHelper.projectedCorners[ cornerId ];
			cornerHandle = new Ellipse2D.Double(
					p[ 0 ] - HANDLE_RADIUS,
					p[ 1 ] - HANDLE_RADIUS,
					2 * HANDLE_RADIUS, 2 * HANDLE_RADIUS );
			final double z = renderBoxHelper.corners[ cornerId ][ 2 ];
			cornerColor = ( z > 0 ) ? backColor : frontColor;
		}
	}
}
