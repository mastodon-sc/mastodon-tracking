/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2025 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.tracking.detection;

import static org.mastodon.tracking.detection.DetectorKeys.KEY_DETECTION_TYPE;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_RADIUS;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_ROI;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_SETUP_ID;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_THRESHOLD;

import java.util.ArrayList;
import java.util.List;

import org.mastodon.tracking.detection.DetectionCreatorFactory.DetectionCreator;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import bdv.viewer.SourceAndConverter;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.dog.DogDetection.ExtremaType;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 * Difference of Gaussian detector.
 *
 * @author Tobias Pietzsch
 * @author Jean-Yves Tinevez
 */
@Plugin( type = DetectorOp.class )
public class DoGDetectorOp
		extends AbstractDetectorOp
		implements DetectorOp, Benchmark
{

	@Parameter
	private ThreadService threadService;

	/**
	 * The minimal diameter size, in pixel, under which we stop down-sampling.
	 */
	public static final double MIN_SPOT_PIXEL_SIZE = 5d;

	private long processingTime;

	@Override
	public void mutate1( final DetectionCreatorFactory detectionCreatorFactory, final List< SourceAndConverter< ? > > sources )
	{
		ok = false;
		final long start = System.currentTimeMillis();

		final StringBuilder str = new StringBuilder();
		if ( !DetectionUtil.checkSettingsValidity( settings, str ) )
		{
			processingTime = System.currentTimeMillis() - start;
			statusService.clearStatus();
			errorMessage = str.toString();
			return;
		}

		final int minTimepoint = ( int ) settings.get( KEY_MIN_TIMEPOINT );
		final int maxTimepoint = ( int ) settings.get( KEY_MAX_TIMEPOINT );
		final int setup = ( int ) settings.get( KEY_SETUP_ID );
		final double radius = ( double ) settings.get( KEY_RADIUS );
		final double threshold = ( double ) settings.get( KEY_THRESHOLD );
		final Interval roi = ( Interval ) settings.get( KEY_ROI );
		final DetectionType detectionType = DetectionType.getOrDefault( ( String ) settings.get( KEY_DETECTION_TYPE ), DetectionType.MINIMA );

		statusService.showStatus( "DoG detection." );
		for ( int tp = minTimepoint; tp <= maxTimepoint; tp++ )
		{
			statusService.showProgress( tp - minTimepoint + 1, maxTimepoint - minTimepoint + 1 );

			// Did we get canceled?
			if ( isCanceled() )
				break;

			// Check if there is some data at this timepoint.
			if ( !DetectionUtil.isPresent( sources, setup, tp ) )
				continue;

			/*
			 * Determine optimal level for detection.
			 */

			final int level = DetectionUtil.determineOptimalResolutionLevel( sources, radius, MIN_SPOT_PIXEL_SIZE / 2., tp, setup );

			/*
			 * Load and extends image data.
			 */

			final RandomAccessibleInterval< ? > img = DetectionUtil.getImage( sources, tp, setup, level );
			if ( !DetectionUtil.isReallyPresent( img ) )
				continue;

			// If 2D, the 3rd dimension will be dropped here.
			final RandomAccessibleInterval< ? > zeroMin = Views.dropSingletonDimensions( Views.zeroMin( img ) );

			@SuppressWarnings( { "unchecked", "rawtypes" } )
			final RandomAccessible< FloatType > source = DetectionUtil.asExtendedFloat( ( RandomAccessibleInterval ) zeroMin );

			/*
			 * Transform ROI in higher level.
			 */

			final Interval interval;
			if ( null == roi )
			{
				interval = zeroMin;
			}
			else
			{
				final double[] minSource = new double[ 3 ];
				final double[] maxSource = new double[ 3 ];
				roi.realMin( minSource );
				roi.realMax( maxSource );
				final double[] minTarget = new double[ 3 ];
				final double[] maxTarget = new double[ 3 ];

				final AffineTransform3D mipmapTransform = DetectionUtil.getMipmapTransform( sources, tp, setup, level );
				mipmapTransform.applyInverse( minTarget, minSource );
				mipmapTransform.applyInverse( maxTarget, maxSource );

				// Only take 2D or 3D version of the transformed interval.
				final long[] tmin = new long[ zeroMin.numDimensions() ];
				final long[] tmax = new long[ zeroMin.numDimensions() ];
				for ( int d = 0; d < zeroMin.numDimensions(); d++ )
				{
					tmin[ d ] = ( long ) Math.ceil( minTarget[ d ] );
					tmax[ d ] = ( long ) Math.floor( maxTarget[ d ] );
				}
				final FinalInterval transformedRoi = new FinalInterval( tmin, tmax );
				interval = Intervals.intersect( transformedRoi, zeroMin );
			}

			// Ensure that the interval size is at least 3 in all dimensions.
			final long[] min = new long[interval.numDimensions()];
			interval.min( min );
			final long[] max = new long[interval.numDimensions()];
			interval.max( max );
			for ( int d = 0; d < interval.numDimensions(); d++ )
				if ( interval.dimension( d ) < 3 )
				{
					min[ d ]--;
					max[ d ]++;
				}
			final FinalInterval minInterval = new FinalInterval( min, max );

			/*
			 * Process image.
			 */


			final int stepsPerOctave = 4;
			final double k = Math.pow( 2.0, 1.0 / stepsPerOctave );
			final double sigma = radius / Math.sqrt( zeroMin.numDimensions() );
			final double sigmaSmaller = sigma;
			final double sigmaLarger = k * sigmaSmaller;
			final double normalization = ( ( detectionType == DetectionType.MAXIMA ) ? 1.0 : -1.0 )
					/ ( sigmaLarger / sigmaSmaller - 1.0 );

			final double[] pixelSize = DetectionUtil.getPixelSize( sources, tp, setup, level );
			final DogDetection< FloatType > dog = new DogDetection<>(
					source,
					minInterval,
					pixelSize,
					sigmaSmaller,
					sigmaLarger,
					( detectionType == DetectionType.MAXIMA ) ? ExtremaType.MAXIMA : ExtremaType.MINIMA,
					threshold,
					true );
			dog.setExecutorService( threadService.getExecutorService() );
			final ArrayList< RefinedPeak< Point > > refinedPeaks = dog.getSubpixelPeaks();

			if( refinedPeaks.size() == 0 ) {
				final ArrayList< Point > peaks = dog.getPeaks();
				peaks.stream().forEach( p -> refinedPeaks.add( new RefinedPeak< Point >( p, p, 0, false ) ));
			}

			final double[] pos = new double[ 3 ];
			final RealPoint sp = RealPoint.wrap( pos );
			final RealPoint p3d = new RealPoint( 3 );

			final AffineTransform3D transform = DetectionUtil.getTransform( sources, tp, setup, level );
			final DetectionCreator detectionCreator = detectionCreatorFactory.create( tp );
			detectionCreator.preAddition();
			try
			{
				for ( final RefinedPeak< Point > p : refinedPeaks )
				{
					final double value = p.getValue();
					final double normalizedValue = value * normalization;

					/*
					 * In case p is 2D we pass it to a 3D RealPoint to work
					 * nicely with the 3D transform.
					 */
					for ( int d = 0; d < p.numDimensions(); d++ )
						p3d.setPosition( p.getDoublePosition( d ), d );
					transform.apply( p3d, sp );
					detectionCreator.createDetection( pos, radius, normalizedValue );
				}
			}
			finally
			{
				detectionCreator.postAddition();
			}
		}

		final long end = System.currentTimeMillis();
		processingTime = end - start;
		statusService.clearStatus();
		ok = true;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

}
