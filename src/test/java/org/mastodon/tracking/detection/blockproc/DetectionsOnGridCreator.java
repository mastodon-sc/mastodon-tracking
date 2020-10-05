package org.mastodon.tracking.detection.blockproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.neighborhood.CenteredRectangleShape;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;

public class DetectionsOnGridCreator
{

	private static final double RANDOM_FACTOR = 0.3;

	private static final double AMPLITUDE = 1000.;

	private final long[] dims;

	private final double radius;

	private final double spacingFactor;

	private final Random ran;

	private final Img< UnsignedShortType > img;

	private final List< RealPoint > peaks;

	private final double[] calibration;

	public DetectionsOnGridCreator( final long[] dims, final double[] calibration, final double radius, final double spacingFactor )
	{
		this.dims = dims;
		this.calibration = calibration;
		this.radius = radius;
		this.spacingFactor = spacingFactor;
		this.ran = new Random();
		this.img = ArrayImgs.unsignedShorts( dims );
		this.peaks = new ArrayList<>();
	}

	public void create()
	{
		peaks.clear();
		final long[] pos = new long[ dims.length ];
		for ( int d = 0; d < pos.length; d++ )
			pos[ d ] = ( long ) ( radius / calibration[ d ] * spacingFactor / 2. );

		final int[] span = new int[ dims.length ];
		for ( int d = 0; d < span.length; d++ )
			span[ d ] = ( int ) ( 1 + 3. * radius / calibration[ d ] / Math.sqrt( pos.length ) );

		final Shape shape = new CenteredRectangleShape( span, false );
		final RandomAccess< Neighborhood< UnsignedShortType > > ra = shape.neighborhoodsRandomAccessible(
				Views.extendZero( img ) ).randomAccess( img );

		final double[] center = new double[ pos.length ];
		final long[] centerRound = new long[ pos.length ];
		do
		{
			jitter( center, pos );
			peaks.add( RealPoint.wrap( center ) );
			roundTo( center, centerRound );
			ra.setPosition( centerRound );
			final Neighborhood< UnsignedShortType > neighborhood = ra.get();
			writeGaussian(
					neighborhood,
					center,
					radius * ( 1. + RANDOM_FACTOR * ( ran.nextDouble() - 0.5 ) ),
					AMPLITUDE * ( 1. + RANDOM_FACTOR * ( ran.nextDouble() - 0.5 ) ),
					calibration );

		}
		while ( increment( pos ) );
	}

	public Img< UnsignedShortType > getImg()
	{
		return img;
	}

	public List< RealPoint > getPeaks()
	{
		return peaks;
	}

	private static final void roundTo( final double[] center, final long[] centerRound )
	{
		for ( int d = 0; d < centerRound.length; d++ )
			centerRound[ d ] = Math.round( center[ d ] );
	}

	private static final void writeGaussian(
			final Neighborhood< UnsignedShortType > neighborhood,
			final double[] center,
			final double rad,
			final double amplitude,
			final double[] calibration )
	{
		final long[] pos = new long[ neighborhood.numDimensions() ];
		final Cursor< UnsignedShortType > cursor = neighborhood.localizingCursor();
		final UnsignedShortType t = new UnsignedShortType( 0 );
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			cursor.localize( pos );
			double dr2 = 0.;
			for ( int d = 0; d < pos.length; d++ )
			{
				final double sigma = rad / calibration[ d ] / Math.sqrt( neighborhood.numDimensions() );
				final double dx = center[ d ] - pos[ d ];
				dr2 += dx * dx / sigma / sigma;
			}
			final double val = amplitude * Math.exp( -dr2  );
			t.setReal( val );
			cursor.get().add( t );
		}
	}

	private final void jitter( final double[] center, final long[] pos )
	{
		for ( int d = 0; d < pos.length; d++ )
			center[ d ] = pos[ d ] + radius / calibration[ d ] * spacingFactor * RANDOM_FACTOR * ( ran.nextDouble() - 0.5 );
	}

	private boolean increment( final long[] pos )
	{
		for ( int d = 0; d < pos.length; d++ )
		{
			pos[ d ] += radius / calibration[ d ] * spacingFactor;
			if ( pos[ d ] >= dims[ d ] - radius / calibration[ d ] * spacingFactor / 4. )
			{
				pos[ d ] = ( long ) ( radius / calibration[ d ] * spacingFactor / 2. );
				if ( d == pos.length - 1 )
				{
					// Finished.
					return false;
				}
				else
				{
					// We can still increment the next dim.
					continue;
				}
			}
			else
			{
				return true;
			}
		}
		return true;
	}
}
