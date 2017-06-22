package org.mastodon.detection;

import bdv.util.Affine3DHelpers;
import bdv.viewer.Source;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.realtransform.AffineTransform3D;

public class LoGDetector<T> implements Algorithm, MultiThreaded, Benchmark
{
	/** The minimal diameter size, in pixel, under which we stop down-sampling. */
	private static final double MIN_SPOT_PIXEL_SIZE = 5d;

	private final Source< T > source;

	private final double radius;

	private final double threshold;

	private final int minTimepoint;

	private final int maxTimepoint;

	private long processingTime;

	private int numThreads;

	private String errorMessage;

	public LoGDetector(final Source< T > source, final double radius, final double threshold, final int minTimepoint, final int maxTimepoint)
	{
		this.source = source;
		this.radius = radius;
		this.threshold = threshold;
		this.minTimepoint = minTimepoint;
		this.maxTimepoint = maxTimepoint;
	}

	@Override
	public boolean checkInput()
	{
		return true;
	}


	@Override
	public boolean process()
	{
		for ( int tp = minTimepoint; tp <= maxTimepoint; tp++ )
		{
			if (!source.isPresent( tp ))
				continue;

			int level = 0;
			while ( level < source.getNumMipmapLevels() - 1 )
			{

				/*
				 * Scan all axes. The "worst" one is the one with the largest scale.
				 * If at this scale the spot is too small, then we stop.
				 */

				final AffineTransform3D sourceToGlobal = new AffineTransform3D();
				source.getSourceTransform( tp, level, sourceToGlobal );
				double scale = Affine3DHelpers.extractScale( sourceToGlobal, 0 );
				for ( int axis = 1; axis < sourceToGlobal.numDimensions(); axis++ )
				{
					final double sc = Affine3DHelpers.extractScale( sourceToGlobal, axis );
					if ( sc > scale )
					{
						scale = sc;
					}
				}

				final double diameterInPix = 2 * radius / scale;
				if ( diameterInPix < MIN_SPOT_PIXEL_SIZE )
				{
					break;
				}
				level++;
			}


			final RandomAccessibleInterval< T > rai = source.getSource( tp, level );




		}
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}


	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}

	@Override
	public void setNumThreads()
	{
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;
	}
}
