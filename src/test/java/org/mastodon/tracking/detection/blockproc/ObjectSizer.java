package org.mastodon.tracking.detection.blockproc;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.util.Grids;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;

public final class ObjectSizer
{

	public static List< Interval > expand( final List< Interval > intervals, final Interval source, final double[] calibration, final double border )
	{
		final long[] borders = new long[ calibration.length ]; // pixels
		for ( int d = 0; d < calibration.length; d++ )
			borders[ d ] = ( long ) ( border / calibration[ d ] );

		final List< Interval > expanded = new ArrayList< Interval >( intervals.size() );
		for ( final Interval interval : intervals )
		{
			final FinalInterval expand = Intervals.expand( interval, borders );
			final FinalInterval intersect = Intervals.intersect( source, expand );
			expanded.add( intersect );
		}
		return expanded;
	}

	public static List< Interval > chunkInterval( final Interval interval, final double[] calibration, final int nb )
	{
		if ( nb <= 1 )
			return Collections.singletonList( interval );

		int[] blockSize = blockSize( interval );
		for ( int i = 2; i < nb; i++ )
			blockSize = smallerBlockSize( blockSize );

		final long[] min = new long[ interval.numDimensions() ];
		final long[] max = new long[ interval.numDimensions() ];
		interval.min( min );
		interval.max( max );
		return Grids.collectAllContainedIntervals( min, max, blockSize );
	}

	public static final int[] smallerBlockSize( final int[] blockSize )
	{
		int largestDim = -1;
		int largestLength = -1;
		for ( int d = 0; d < blockSize.length; d++ )
		{
			if ( blockSize[ d ] > largestLength )
			{
				largestLength = blockSize[ d ];
				largestDim = d;
			}
		}

		final int[] sbl = new int[ blockSize.length ];
		for ( int d = 0; d < sbl.length; d++ )
		{
			if ( d == largestDim)
				sbl[ d ] = ( int ) Math.ceil( blockSize[ d ] / 2. );
			else
				sbl[ d ] = blockSize[d];
		}
		return sbl;
	}

	public static final int[] blockSize( final Interval interval )
	{
		final int[] blockSize = new int[ interval.numDimensions() ];
		for ( int d = 0; d < blockSize.length; d++ )
			blockSize[ d ] = ( int ) interval.dimension( d );

		return blockSize;
	}

	public static final int[] squareBlockSize( final Interval interval, final double[] calibration )
	{
		// Determine smallest physical size.
		double minLength = Double.POSITIVE_INFINITY;
		for ( int d = 0; d < interval.numDimensions(); d++ )
		{
			final double length = interval.dimension( d ) * calibration[ d ];
			if ( length < minLength )
				minLength = length;
		}
		final int[] blockSize = new int[ interval.numDimensions() ];
		for ( int d = 0; d < blockSize.length; d++ )
			blockSize[ d ] = Math.min(
					( int ) interval.dimension(
							d ),
					( int ) ( minLength / calibration[ d ] + 1 ) );
		
		return blockSize;
	}

	public static String hr( long bytes )
	{
		if ( -1000 < bytes && bytes < 1000 )
		{ return bytes + " B"; }
		final CharacterIterator ci = new StringCharacterIterator( "kMGTPE" );
		while ( bytes <= -999_950 || bytes >= 999_950 )
		{
			bytes /= 1000;
			ci.next();
		}
		return String.format( "%.1f %cB", bytes / 1000.0, ci.current() );
	}

	public static < T extends RealType< T > > long estimateDoGDetectionRAM( final RandomAccessibleInterval< T > rai )
	{
		final T t = Util.getTypeFromInterval( rai );
		return estimateDoGDetectionRAM( rai, t );
	}

	@SuppressWarnings( "unchecked" )
	public static < T extends RealType< T > > long estimateDoGDetectionRAM( final Interval interval, final T t )
	{
		T type;
		if ( t instanceof DoubleType )
			type = ( T ) new DoubleType();
		else
			type = ( T ) new FloatType();

		/*
		 * There are three duplicate images required of the type above:
		 *
		 * 1. One to store the DoG results (float if we don't have doubles).
		 *
		 * 2. A tmp image to compute the DoG.
		 *
		 * 3. In Gauss, a tmp holder to store convolution results.
		 */
		return 3 * type.getBitsPerPixel() / 8 * nPixels( interval );
	}

	public static < T extends RealType< T > > long imgSize( final RandomAccessibleInterval< T > img )
	{
		final T t = Util.getTypeFromInterval( img );
		final int nbits = t.getBitsPerPixel();
		final int nbytes = nbits / 8;
		return nPixels( img ) * nbytes;
	}

	private static long nPixels( final Interval interval )
	{
		long size = 1;
		for ( int d = 0; d < interval.numDimensions(); d++ )
			size *= interval.dimension( d );
		return size;
	}

	public static String status()
	{
		final StringBuilder str = new StringBuilder();
		str.append( String.format( "%10s: %s\n", "max RAM", hr( maxRAM() ) ) );
		str.append( String.format( "%10s: %s\n", "used RAM", hr( usedRAM() ) ) );
		str.append( String.format( "%10s: %s", "free RAM", hr( freeRAM() ) ) );
		return str.toString();
	}

	public static final long maxRAM()
	{
		return Runtime.getRuntime().maxMemory();
	}

	public static final long freeRAM()
	{
		return maxRAM() - usedRAM();
//		final long allocatedMemory = ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() );
//		final long presumableFreeMemory = Runtime.getRuntime().maxMemory() - allocatedMemory;
//		return presumableFreeMemory;
	}

	public static final long usedRAM()
	{
		return Runtime.getRuntime().totalMemory();
	}

	public static Interval getLargestInterval( final List< Interval > intervals )
	{
		return intervals
				.stream()
				.reduce( intervals.get( 0 ), ( i1, i2 ) -> ( nPixels( i1 ) > nPixels( i2 ) ? i1 : i2 ) );
	}

}
