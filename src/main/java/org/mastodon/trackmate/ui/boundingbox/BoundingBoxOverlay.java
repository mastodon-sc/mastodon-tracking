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
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

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

	private final BoundingBoxModel model;

	private final Color backColor = new Color( 0x00994499 );// Color.MAGENTA;

	private final Color frontColor = Color.GREEN;

	private Stroke normalStroke = new BasicStroke();

	private Stroke intersectionStroke = new BasicStroke( 1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] { 10f, 10f }, 0f );

	private Paint intersectionColor = Color.WHITE.darker();

	private Color cornerColor = frontColor;

	private final AffineTransform3D viewerTransform;

	final AffineTransform3D transform;

	final RenderBoxHelper renderBoxHelper;

	private double perspective = 100.;

	private int canvasWidth;

	private int canvasHeight;

	private DisplayMode displayMode = DisplayMode.FULL;

	private boolean editMode;

	private GeneralPath front;

	private GeneralPath back;

	private GeneralPath intersection;

	private Ellipse2D cornerHandle;

	int cornerId;

	public BoundingBoxOverlay( final BoundingBoxModel model )
	{
		this.model = model;
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
		final AffineTransform t = graphics.getTransform();

		front = new GeneralPath();
		back = new GeneralPath();
		intersection = new GeneralPath();

		final Interval interval = model.getInterval();
		final double sourceSize = Math.max( Math.max( interval.dimension( 0 ), interval.dimension( 1 ) ), interval.dimension( 2 ) );
		final double ox = canvasWidth / 2;
		final double oy = canvasHeight / 2;
		model.getIntervalTransform( transform );
		transform.preConcatenate( viewerTransform );
		renderBoxHelper.setPerspectiveProjection( perspective > 0 );
		renderBoxHelper.setDepth( perspective * sourceSize );
		renderBoxHelper.setOrigin( ox, oy );
		renderBoxHelper.setScale( 1 );
		renderBoxHelper.renderBox( interval, transform, front, back, intersection );

		final AffineTransform translate = new AffineTransform( 1, 0, 0, 1, ox, oy );
		translate.preConcatenate( t );
		graphics.setTransform( translate );
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

		graphics.setTransform( t );

		if ( editMode && null != cornerHandle )
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
		viewerTransform.set( t );
	}

	/**
	 * Helper for rendering overlay boxes.
	 *
	 * @author Stephan Saalfeld
	 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
	 */
	static final class RenderBoxHelper
	{
		/**
		 * distance from the eye to the projection plane z=0.
		 */
		private double depth = 10.0;

		/**
		 * scale the 2D projection of the overlay box by this factor.
		 */
		private double scale = 0.1;

		private final double[] origin = new double[ 3 ];

		private boolean perspective = false;

		private final List< Point2D > intersectionPoints = new ArrayList<>();

		private final double[] q000 = new double[ 3 ];
		private final double[] q100 = new double[ 3 ];
		private final double[] q010 = new double[ 3 ];
		private final double[] q110 = new double[ 3 ];
		private final double[] q001 = new double[ 3 ];
		private final double[] q101 = new double[ 3 ];
		private final double[] q011 = new double[ 3 ];
		private final double[] q111 = new double[ 3 ];

		// Order matters. We will use the order to know what we edit.
		final double[][] corners = new double[][] { q000, q100, q010, q110, q001, q101, q011, q111 };

		public void setPerspectiveProjection( final boolean b )
		{
			perspective = b;
		}

		public void setScale( final double scale )
		{
			this.scale = scale;
		}

		public void setDepth( final double depth )
		{
			this.depth = depth;
			origin[ 2 ] = -depth;
		}

		public void setOrigin( final double x, final double y )
		{
			origin[ 0 ] = x;
			origin[ 1 ] = y;
		}

		/**
		 *
		 * @param p
		 *            point to project
		 * @return X coordinate of projected point
		 */
		private double perspectiveX( final double[] p )
		{
			return scale * ( p[ 0 ] - origin[ 0 ] ) / ( p[ 2 ] - origin[ 2 ] ) * depth;
		}

		/**
		 *
		 * @param p
		 *            point to project
		 * @return Y coordinate of projected point
		 */
		private double perspectiveY( final double[] p )
		{
			return scale * ( p[ 1 ] - origin[ 1 ] ) / ( p[ 2 ] - origin[ 2 ] ) * depth;
		}

		/**
		 *
		 * @param p
		 *            point to project
		 * @return X coordinate of projected point
		 */
		private double parallelX( final double[] p )
		{
			return scale * ( p[ 0 ] - origin[ 0 ] );
		}

		/**
		 *
		 * @param p
		 *            point to project
		 * @return Y coordinate of projected point
		 */
		private double parallelY( final double[] p )
		{
			return scale * ( p[ 1 ] - origin[ 1 ] );
		}

		private void splitEdge( final double[] a, final double[] b, final GeneralPath before, final GeneralPath behind )
		{
			if ( perspective )
				splitEdgePerspective( a, b, before, behind );
			else
				splitEdgeParallel( a, b, before, behind );
		}

		private void splitEdgePerspective( final double[] a, final double[] b, final GeneralPath before, final GeneralPath behind )
		{
			final double[] t = new double[ 3 ];
			if ( a[ 2 ] <= 0 )
			{
				before.moveTo( perspectiveX( a ), perspectiveY( a ) );
				if ( b[ 2 ] <= 0 )
					before.lineTo( perspectiveX( b ), perspectiveY( b ) );
				else
				{
					final double d = a[ 2 ] / ( a[ 2 ] - b[ 2 ] );
					t[ 0 ] = ( b[ 0 ] - a[ 0 ] ) * d + a[ 0 ];
					t[ 1 ] = ( b[ 1 ] - a[ 1 ] ) * d + a[ 1 ];
					before.lineTo( perspectiveX( t ), perspectiveY( t ) );
					behind.moveTo( perspectiveX( t ), perspectiveY( t ) );
					behind.lineTo( perspectiveX( b ), perspectiveY( b ) );
					intersectionPoints.add( new Point2D.Double( parallelX( t ), parallelY( t ) ) );
				}
			}
			else
			{
				behind.moveTo( perspectiveX( a ), perspectiveY( a ) );
				if ( b[ 2 ] > 0 )
					behind.lineTo( perspectiveX( b ), perspectiveY( b ) );
				else
				{
					final double d = a[ 2 ] / ( a[ 2 ] - b[ 2 ] );
					t[ 0 ] = ( b[ 0 ] - a[ 0 ] ) * d + a[ 0 ];
					t[ 1 ] = ( b[ 1 ] - a[ 1 ] ) * d + a[ 1 ];
					behind.lineTo( perspectiveX( t ), perspectiveY( t ) );
					before.moveTo( perspectiveX( t ), perspectiveY( t ) );
					before.lineTo( perspectiveX( b ), perspectiveY( b ) );
					intersectionPoints.add( new Point2D.Double( parallelX( t ), parallelY( t ) ) );
				}
			}
		}

		private void splitEdgeParallel( final double[] a, final double[] b, final GeneralPath before, final GeneralPath behind )
		{
			final double[] t = new double[ 3 ];
			if ( a[ 2 ] <= 0 )
			{
				before.moveTo( parallelX( a ), parallelY( a ) );
				if ( b[ 2 ] <= 0 )
					before.lineTo( parallelX( b ), parallelY( b ) );
				else
				{
					final double d = a[ 2 ] / ( a[ 2 ] - b[ 2 ] );
					t[ 0 ] = ( b[ 0 ] - a[ 0 ] ) * d + a[ 0 ];
					t[ 1 ] = ( b[ 1 ] - a[ 1 ] ) * d + a[ 1 ];
					before.lineTo( parallelX( t ), parallelY( t ) );
					behind.moveTo( parallelX( t ), parallelY( t ) );
					behind.lineTo( parallelX( b ), parallelY( b ) );
					intersectionPoints.add( new Point2D.Double( parallelX( t ), parallelY( t ) ) );
				}
			}
			else
			{
				behind.moveTo( parallelX( a ), parallelY( a ) );
				if ( b[ 2 ] > 0 )
					behind.lineTo( parallelX( b ), parallelY( b ) );
				else
				{
					final double d = a[ 2 ] / ( a[ 2 ] - b[ 2 ] );
					t[ 0 ] = ( b[ 0 ] - a[ 0 ] ) * d + a[ 0 ];
					t[ 1 ] = ( b[ 1 ] - a[ 1 ] ) * d + a[ 1 ];
					behind.lineTo( parallelX( t ), parallelY( t ) );
					before.moveTo( parallelX( t ), parallelY( t ) );
					before.lineTo( parallelX( b ), parallelY( b ) );
					intersectionPoints.add( new Point2D.Double( parallelX( t ), parallelY( t ) ) );
				}
			}
		}

		public void renderBox( final Interval sourceInterval, final AffineTransform3D transform, final GeneralPath front, final GeneralPath back, final GeneralPath intersection )
		{
			final double sX0 = sourceInterval.min( 0 );
			final double sX1 = sourceInterval.max( 0 );
			final double sY0 = sourceInterval.min( 1 );
			final double sY1 = sourceInterval.max( 1 );
			final double sZ0 = sourceInterval.min( 2 );
			final double sZ1 = sourceInterval.max( 2 );

			final double[] p000 = new double[] { sX0, sY0, sZ0 };
			final double[] p100 = new double[] { sX1, sY0, sZ0 };
			final double[] p010 = new double[] { sX0, sY1, sZ0 };
			final double[] p110 = new double[] { sX1, sY1, sZ0 };
			final double[] p001 = new double[] { sX0, sY0, sZ1 };
			final double[] p101 = new double[] { sX1, sY0, sZ1 };
			final double[] p011 = new double[] { sX0, sY1, sZ1 };
			final double[] p111 = new double[] { sX1, sY1, sZ1 };

			transform.apply( p000, q000 );
			transform.apply( p100, q100 );
			transform.apply( p010, q010 );
			transform.apply( p110, q110 );
			transform.apply( p001, q001 );
			transform.apply( p101, q101 );
			transform.apply( p011, q011 );
			transform.apply( p111, q111 );

			intersectionPoints.clear();
			splitEdge( q000, q100, front, back );
			splitEdge( q100, q110, front, back );
			splitEdge( q110, q010, front, back );
			splitEdge( q010, q000, front, back );

			splitEdge( q001, q101, front, back );
			splitEdge( q101, q111, front, back );
			splitEdge( q111, q011, front, back );
			splitEdge( q011, q001, front, back );

			splitEdge( q000, q001, front, back );
			splitEdge( q100, q101, front, back );
			splitEdge( q110, q111, front, back );
			splitEdge( q010, q011, front, back );

			if ( intersectionPoints.size() > 2 )
			{
				final double x0 = intersectionPoints.stream()
						.mapToDouble( e -> e.getX() )
						.average()
						.getAsDouble();
				final double y0 = intersectionPoints.stream()
						.mapToDouble( e -> e.getY() )
						.average()
						.getAsDouble();
				final Point2D P = new Point2D.Double( x0, y0 );
				intersectionPoints.sort( new PolarOrder( P ) );
				final Iterator< Point2D > hull = intersectionPoints.iterator();
				if ( hull.hasNext() )
				{
					final Point2D first = hull.next();
					intersection.moveTo( first.getX(), first.getY() );
					while ( hull.hasNext() )
					{
						final Point2D next = hull.next();
						intersection.lineTo( next.getX(), next.getY() );
					}
					intersection.closePath();
				}
			}
		}

		private static final class PolarOrder implements Comparator< Point2D >
		{

			private Point2D p;

			public PolarOrder( final Point2D p )
			{
				this.p = p;
			}

			@Override
			public int compare( final Point2D q1, final Point2D q2 )
			{
				final double dx1 = q1.getX() - p.getX();
				final double dy1 = q1.getY() - p.getY();
				final double dx2 = q2.getX() - p.getX();
				final double dy2 = q2.getY() - p.getY();

				if ( dy1 >= 0 && dy2 < 0 )
					return -1; // q1 above; q2 below
				else if ( dy2 >= 0 && dy1 < 0 )
					return +1; // q1 below; q2 above
				else if ( dy1 == 0 && dy2 == 0 )
				{ // 3-collinear and horizontal
					if ( dx1 >= 0 && dx2 < 0 )
						return -1;
					else if ( dx2 >= 0 && dx1 < 0 )
						return +1;
					else
						return 0;
				}
				else
					return -ccw( p, q1, q2 );
			}

			/**
			 * Returns true if a→b→c is a counterclockwise turn.
			 *
			 * @param a
			 *            first point
			 * @param b
			 *            second point
			 * @param c
			 *            third point
			 * @return { -1, 0, +1 } if a→b→c is a { clockwise, collinear;
			 *         counterclocwise } turn.
			 */
			private static int ccw( final Point2D a, final Point2D b, final Point2D c )
			{
				final double area2 = ( b.getX() - a.getX() ) * ( c.getY() - a.getY() ) - ( b.getY() - a.getY() ) * ( c.getX() - a.getX() );
				if ( area2 < 0 )
					return -1;
				else if ( area2 > 0 )
					return +1;
				else
					return 0;
			}
		}
	}

	public void setDisplayMode( final DisplayMode mode )
	{
		this.displayMode = mode;

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
			for ( int i = 0; i < renderBoxHelper.corners.length; i++ )
			{
				final double[] corner = renderBoxHelper.corners[ i ];
				final double dx = x - corner[ 0 ];
				final double dy = y - corner[ 1 ];
				final double dr2 = dx * dx + dy * dy;
				if ( dr2 < SQU_DISTANCE_TOLERANCE )
				{
					cornerId = i;
					cornerColor = ( corner[ 2 ] > 0 ) ? backColor : frontColor;
					regenFrom( corner );
					return;
				}
			}
		}

		@Override
		public void mouseDragged( final MouseEvent e )
		{
			if ( cornerId < 0 )
				return;

			regenFrom( renderBoxHelper.corners[ cornerId ] );
		}

		private void regenFrom( final double[] corner )
		{
			cornerHandle = new Ellipse2D.Double(
					corner[ 0 ] - HANDLE_RADIUS,
					corner[ 1 ] - HANDLE_RADIUS,
					2 * HANDLE_RADIUS, 2 * HANDLE_RADIUS );
			cornerColor = ( corner[ 2 ] > 0 ) ? backColor : frontColor;
		}
	}
}
