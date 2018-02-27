package org.mastodon.trackmate.ui.boundingbox;

import net.imglib2.RealInterval;

/**
 * Utilities for computing the corners of a RealInterval.
 *
 * The index of a corner is interpreted as a binary number where bit 0
 * corresponds to X, bit 1 corresponds to Y, etc. A zero bit means min in the
 * corresponding dimension, a one bit means max in the corresponding dimension.
 *
 * @author Tobias Pietzsch
 */
public class IntervalCorners
{
	public static int numCorners( final RealInterval interval )
	{
		return 1 << interval.numDimensions();
	}

	public static double[] corner( final RealInterval interval, final int index )
	{
		final int n = interval.numDimensions();
		final double[] corner = new double[ n ];
		for ( int  d = 0, mask = 1; d < n; ++d, mask = mask << 1 )
			corner[ d ] = ( index & mask ) == 0 ? interval.realMin( d ) : interval.realMax( d );
		return corner;
	}

	public static void corner( final RealInterval interval, final int index, final double[] corner )
	{
		assert corner.length == interval.numDimensions();
		for ( int  d = 0, mask = 1; d < corner.length; ++d, mask = mask << 1 )
			corner[ d ] = ( index & mask ) == 0 ? interval.realMin( d ) : interval.realMax( d );
	}

	public static double[][] corners( final RealInterval interval )
	{
		final int n = interval.numDimensions();
		final int numCorners = 1 << n;
		final double[][] corners = new double[ numCorners ][ n ];
		for ( int index = 0; index < numCorners; index++ )
			for ( int d = 0, mask = 1; d < n; ++d, mask <<= 1 )
				corners[ index ][ d ] = ( index & mask ) == 0 ? interval.realMin( d ) : interval.realMax( d );
		return corners;
	}

	private IntervalCorners()
	{}
}
