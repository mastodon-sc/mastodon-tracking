package org.mastodon.trackmate.ui.boundingbox;

import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * Helper for rendering overlay boxes.
 *
 * @author Stephan Saalfeld
 * @author Tobias Pietzsch
 * @author Jean-Yves Tinevez
 */
public final class RenderBoxHelper
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

	private final List< double[] > intersectionPoints = new ArrayList<>();

	// TODO: remove
	private final double[] q000 = new double[ 3 ];
	private final double[] q100 = new double[ 3 ];
	private final double[] q010 = new double[ 3 ];
	private final double[] q110 = new double[ 3 ];
	private final double[] q001 = new double[ 3 ];
	private final double[] q101 = new double[ 3 ];
	private final double[] q011 = new double[ 3 ];
	private final double[] q111 = new double[ 3 ];

	// TODO: remove
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
	 * Project a point.
	 *
	 * @param point
	 *            point to project
	 * @param projection
	 *            projected point is stored here
	 */
	public void project( final double[] point, final double[] projection )
	{
		final double f = perspective
				? scale * depth / ( point[ 2 ] - origin[ 2 ] )
				: scale;
		projection[ 0 ] = ( point[ 0 ] - origin[ 0 ] ) * f;
		projection[ 1 ] = ( point[ 1 ] - origin[ 1 ] ) * f;
	}

	/**
	 * Project a point.
	 *
	 * @param point
	 *            point to project
	 * @return projected point
	 */
	public double[] project( final double[] point )
	{
		final double[] projection = new double[ 2 ];
		project( point, projection );
		return projection;
	}

	private void splitEdge( final double[] a, final double[] b, final GeneralPath before, final GeneralPath behind )
	{
		final double[] pa = project( a );
		final double[] pb = project( b );
		if ( a[ 2 ] <= 0 )
		{
			before.moveTo( pa[ 0 ], pa[ 1 ] );
			if ( b[ 2 ] <= 0 )
				before.lineTo( pb[ 0 ], pb[ 1 ] );
			else
			{
				final double[] t = new double[ 3 ];
				final double d = a[ 2 ] / ( a[ 2 ] - b[ 2 ] );
				t[ 0 ] = ( b[ 0 ] - a[ 0 ] ) * d + a[ 0 ];
				t[ 1 ] = ( b[ 1 ] - a[ 1 ] ) * d + a[ 1 ];
				final double[] pt = project( t );
				before.lineTo( pt[ 0 ], pt[ 1 ] );
				behind.moveTo( pt[ 0 ], pt[ 1 ] );
				behind.lineTo( pb[ 0 ], pb[ 1 ] );
				intersectionPoints.add( new double[] { pt[ 0 ], pt[ 1 ] } );
			}
		}
		else
		{
			behind.moveTo( pa[ 0 ], pa[ 1 ] );
			if ( b[ 2 ] > 0 )
				behind.lineTo( pb[ 0 ], pb[ 1 ] );
			else
			{
				final double[] t = new double[ 3 ];
				final double d = a[ 2 ] / ( a[ 2 ] - b[ 2 ] );
				t[ 0 ] = ( b[ 0 ] - a[ 0 ] ) * d + a[ 0 ];
				t[ 1 ] = ( b[ 1 ] - a[ 1 ] ) * d + a[ 1 ];
				final double[] pt = project( t );
				behind.lineTo( pt[ 0 ], pt[ 1 ] );
				before.moveTo( pt[ 0 ], pt[ 1 ] );
				before.lineTo( pb[ 0 ], pb[ 1 ] );
				intersectionPoints.add( new double[] { pt[ 0 ], pt[ 1 ] } );
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
					.mapToDouble( e -> e[ 0 ] )
					.average()
					.getAsDouble();
			final double y0 = intersectionPoints.stream()
					.mapToDouble( e -> e[ 1 ] )
					.average()
					.getAsDouble();
			intersectionPoints.sort( new PolarOrder( new double[] { x0, y0 } ) );
			final Iterator< double[] > hull = intersectionPoints.iterator();
			if ( hull.hasNext() )
			{
				final double[] first = hull.next();
				intersection.moveTo( first[ 0 ], first[ 1 ] );
				while ( hull.hasNext() )
				{
					final double[] next = hull.next();
					intersection.lineTo( next[ 0 ], next[ 1 ] );
				}
				intersection.closePath();
			}
		}
	}

	private static final class PolarOrder implements Comparator< double[] >
	{

		private final double[] p;

		public PolarOrder( final double[] p )
		{
			this.p = p;
		}

		@Override
		public int compare( final double[] q1, final double[] q2 )
		{
			final double dx1 = q1[ 0 ] - p[ 0 ];
			final double dy1 = q1[ 1 ] - p[ 1 ];
			final double dx2 = q2[ 0 ] - p[ 0 ];
			final double dy2 = q2[ 1 ] - p[ 1 ];

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
		private static int ccw( final double[] a, final double[] b, final double[] c )
		{
			final double area2 = ( b[ 0 ] - a[ 0 ] ) * ( c[ 1 ] - a[ 1 ] ) - ( b[ 1 ] - a[ 1 ] ) * ( c[ 0 ] - a[ 0 ] );
			if ( area2 < 0 )
				return -1;
			else if ( area2 > 0 )
				return +1;
			else
				return 0;
		}
	}
}
