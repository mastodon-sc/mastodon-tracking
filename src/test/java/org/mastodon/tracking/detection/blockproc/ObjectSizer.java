package org.mastodon.tracking.detection.blockproc;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.List;

import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;

public final class ObjectSizer
{

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
		final long allocatedMemory = ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() );
		final long presumableFreeMemory = Runtime.getRuntime().maxMemory() - allocatedMemory;
		return presumableFreeMemory;
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
