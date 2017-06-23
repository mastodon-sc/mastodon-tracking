package org.mastodon.detection;

import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.fft2.FFT;
import net.imglib2.algorithm.fft2.FFTMethods;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class FFTConvolver implements MultiThreaded
{

	private int nThreads = 1;

	private final Img< ComplexFloatType > kernelfft;

	private final Interval paddingIntervalCentered;

	public FFTConvolver( final RandomAccessibleInterval< FloatType > kernel, final Interval interval )
	{
		setNumThreads();

		final long[] paddedDimensions = new long[ interval.numDimensions() ];
		final long[] fftDimensions = new long[ interval.numDimensions() ];
		FFTMethods.dimensionsRealToComplexFast( interval, paddedDimensions, fftDimensions );
		final Dimensions paddingDimensions = FinalDimensions.wrap( paddedDimensions );
		this.paddingIntervalCentered = FFTMethods.paddingIntervalCentered( interval, paddingDimensions );

		final Interval kernelPaddingIntervalCentered = FFTMethods.paddingIntervalCentered( kernel, paddingDimensions );

		final long[] min = new long[ interval.numDimensions() ];
		final long[] max = new long[ interval.numDimensions() ];

		for ( int d = 0; d < interval.numDimensions(); ++d )
		{
			min[ d ] = kernel.min( d ) + kernel.dimension( d ) / 2;
			max[ d ] = min[ d ] + kernelPaddingIntervalCentered.dimension( d ) - 1;
		}

		// Forward FFT
		final RandomAccessibleInterval< FloatType > kernelRAI = Views.interval(
				Views.extendPeriodic(
						Views.interval(
								Views.extendZero( kernel ),
								kernelPaddingIntervalCentered ) ),
				new FinalInterval( min, max ) );

		// Factory
		final ImgFactory< ComplexFloatType > factory = Util.getArrayOrCellImgFactory( kernelRAI, new ComplexFloatType() );

		// Forward FFT of kernel.
		this.kernelfft = FFT.realToComplex( kernelRAI, factory, nThreads );
	}

	public < T extends RealType< T > > void convolve( final RandomAccessibleInterval< T > source, final RandomAccessibleInterval< FloatType > output )
	{
		// Forward FFT
		final RandomAccessibleInterval< T > imgRAI = Views.interval(
				Views.extendPeriodic( source ),
				paddingIntervalCentered );

		// Factory
		final ImgFactory< ComplexFloatType > factory = Util.getArrayOrCellImgFactory( imgRAI, new ComplexFloatType() );

		// Forward FFT of source.
		final Img< ComplexFloatType > fft = FFT.realToComplex( imgRAI, factory, nThreads );

		// Complex multiplication.
		final Cursor< ComplexFloatType > cursor = fft.localizingCursor();
		final RandomAccess< ComplexFloatType > ra = Views.extendZero( kernelfft ).randomAccess( fft );
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			ra.setPosition( cursor );
			cursor.get().mul( ra.get() );
		}

		// Output
		FFT.complexToRealUnpad( fft, output, nThreads );
	}

	/*
	 * MULTITHREADED.
	 */

	@Override
	public int getNumThreads()
	{
		return nThreads;
	}

	@Override
	public void setNumThreads()
	{
		setNumThreads( Runtime.getRuntime().availableProcessors() );
	}

	@Override
	public void setNumThreads( final int nThreads )
	{
		this.nThreads = nThreads;
	}
}