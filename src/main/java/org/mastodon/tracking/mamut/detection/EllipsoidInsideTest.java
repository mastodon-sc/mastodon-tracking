package org.mastodon.tracking.mamut.detection;

import org.mastodon.mamut.model.Spot;

import net.imglib2.util.LinAlgHelpers;

/**
 * Offers facilities to determine whether a point is inside an ellipsoid, as
 * specified by a <code>double[3][3]</code> covariance matrix like in Mastodon.
 * <p>
 * Adapted from ScreenVertexMath.
 */
public class EllipsoidInsideTest
{

	/** Holder for the first spot position. */
	private final double[] pos1 = new double[ 3 ];

	/** Holder for the second spot position. */
	private final double[] pos2 = new double[ 3 ];

	/** Holder for the covariance position. */
	private final double[][] cov = new double[ 3 ][ 3 ];

	/** Used to determines whether a spot contains a position. */
	private final double[] diff = new double[ 3 ];

	/** Used to determines whether a spot contains a position. */
	private final double[] vn = new double[ 3 ];

	/** Precision. */
	private final double[][] P = new double[ 3 ][ 3 ];

	/**
	 * Returns <code>true</code> if the first spot contains the center of the
	 * second one, or if the second spot contains the center of the first one.
	 *
	 * @param s1
	 *            the first spot.
	 * @param s2
	 *            the second spot.
	 * @return <code>true</code> if a center of spot is included in the other
	 *         spot.
	 */
	public boolean areCentersInside( final Spot s1, final Spot s2 )
	{
		s2.localize( pos2 );
		if ( isPointInside( pos2, s1 ) )
			return true;

		s1.localize( pos2 );
		return isPointInside( pos2, s2 );
	}

	/**
	 * Returns <code>true</code> is the specified position lies inside the spot
	 * ellipsoid.
	 *
	 * @param pos
	 *            the position to test.
	 * @param spot
	 *            the spot.
	 * @return <code>true</code> if the position is inside the spot.
	 */
	public boolean isPointInside( final double[] pos, final Spot spot )
	{
		spot.localize( pos1 );
		LinAlgHelpers.subtract( pos1, pos, diff );
		spot.getCovariance( cov );
		LinAlgHelpers.invertSymmetric3x3( cov, P );
		LinAlgHelpers.mult( P, diff, vn );
		final double d2 = LinAlgHelpers.dot( diff, vn );
		return d2 < 1.;
	}

	/**
	 * Returns <code>true</code> if the spot center is within the specified
	 * radius around the specified position.
	 *
	 * @param spot
	 *            the spot to test.
	 * @param pos
	 *            the position.
	 * @param radius
	 *            the radius.
	 * @return <code>true</code> if the spot center is within
	 *         <code>radius</code> of <code>pos</code>.
	 */
	public boolean isCenterWithin( final Spot spot, final double[] pos, final double radius )
	{
		spot.localize( pos1 );
		return LinAlgHelpers.squareDistance( pos1, pos ) < radius * radius;
	}
}
