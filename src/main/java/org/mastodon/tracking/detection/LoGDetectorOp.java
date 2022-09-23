/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2022 Tobias Pietzsch, Jean-Yves Tinevez
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

import static org.mastodon.tracking.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_RADIUS;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_ROI;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_SETUP_ID;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_THRESHOLD;
import static org.mastodon.tracking.detection.DoGDetectorOp.MIN_SPOT_PIXEL_SIZE;

import java.util.Arrays;
import java.util.List;

import org.mastodon.tracking.detection.DetectionCreatorFactory.DetectionCreator;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import bdv.viewer.SourceAndConverter;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.algorithm.localextrema.SubpixelLocalization;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * Laplacian of Gaussian detector.
 *
 * @author Jean-Yves Tinevez
 */
@Plugin( type = DetectorOp.class )
public class LoGDetectorOp
		extends AbstractDetectorOp
		implements DetectorOp, Benchmark
{

	@Parameter
	private ThreadService threadService;

	private long processingTime;

	private final boolean doSubpixelLocalization = true;

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
		final double radius = ( double ) settings.get( KEY_RADIUS ); // um
		final double threshold = ( double ) settings.get( KEY_THRESHOLD );
		final Interval roi = ( Interval ) settings.get( KEY_ROI );

		statusService.showStatus( "LoG detection" );
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

			@SuppressWarnings( "rawtypes" )
			final RandomAccessibleInterval img = DetectionUtil.getImage( sources, tp, setup, level );
			if ( !DetectionUtil.isReallyPresent( img ) )
				continue;

			@SuppressWarnings( "unchecked" )
			final RandomAccessibleInterval< ? > zeroMin = Views.dropSingletonDimensions( Views.zeroMin( img ) );

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

			/*
			 * Filter image.
			 */

			final double[] pixelSize = DetectionUtil.getPixelSize( sources, tp, setup, level );
			final RandomAccessibleInterval< FloatType > kernel = createLoGKernel( radius, zeroMin.numDimensions(), pixelSize );
			@SuppressWarnings( "rawtypes" )
			final IntervalView source = Views.interval( zeroMin, interval );

			@SuppressWarnings( "unchecked" )
			final RandomAccessibleInterval< FloatType > output = ops().filter().convolve( source, kernel );

			/*
			 * LoG normalization factor, so that the filtered peak have the
			 * maximal value for spots that have the size this kernel is tuned
			 * to. With this value, the peak value will be of the same order of
			 * magnitude than the raw spot (if it has the right size). This
			 * value also ensures that if the image has its calibration changed,
			 * one will retrieve the same peak value than before scaling.
			 * However, I (JYT) could not derive the exact formula if the image
			 * is scaled differently across X, Y and Z.
			 */
			final double sigma = radius / Math.sqrt( img.numDimensions() );
			final double sigmaPixels = sigma / pixelSize[ 0 ];
			final FloatType C = new FloatType( ( float ) ( 1. / Math.PI / sigmaPixels / sigmaPixels ) );
			Views.iterable( output ).forEach( ( e ) -> e.div( C ) );

			/*
			 * Detect local maxima.
			 */

			final AffineTransform3D transform = DetectionUtil.getTransform( sources, tp, setup, level );
			final DetectionCreator detectionCreator = detectionCreatorFactory.create( tp );
			final List< Point > peaks = DetectionUtil.findLocalMaxima( output, threshold, threadService.getExecutorService() );
			if ( doSubpixelLocalization )
			{
				final int maxNumMoves = 10;
				final boolean allowMaximaTolerance = true;
				final boolean returnInvalidPeaks = true;
				final boolean[] allowedToMoveInDim = new boolean[ img.numDimensions() ];
				Arrays.fill( allowedToMoveInDim, true );
				final float maximaTolerance = 0.01f;
				final List< RefinedPeak< Point > > refined = SubpixelLocalization.refinePeaks( peaks, output, output,
						returnInvalidPeaks, maxNumMoves, allowMaximaTolerance, maximaTolerance , allowedToMoveInDim );

				final RandomAccess< FloatType > ra = output.randomAccess();
				final double[] pos = new double[ 3 ];
				final RealPoint point = RealPoint.wrap( pos );
				final RealPoint p3d = new RealPoint( 3 );

				detectionCreator.preAddition();
				try
				{
					for ( final RefinedPeak< Point > refinedPeak : refined )
					{
						ra.setPosition( refinedPeak.getOriginalPeak() );
						final double q = ra.get().getRealDouble();

						for ( int d = 0; d < refinedPeak.numDimensions(); d++ )
							p3d.setPosition( refinedPeak.getDoublePosition( d ), d );
						transform.apply( p3d, point );
						detectionCreator.createDetection( pos, radius, q );
					}
				}
				finally
				{
					detectionCreator.postAddition();
				}
			}
			else
			{
				final RandomAccess< FloatType > ra = output.randomAccess();
				final double[] pos = new double[ 3 ];
				final RealPoint point = RealPoint.wrap( pos );

				detectionCreator.preAddition();
				try
				{
					for ( final Point peak : peaks )
					{
						ra.setPosition( peak );
						final double q = ra.get().getRealDouble();

						transform.apply( peak, point );
						detectionCreator.createDetection( pos, radius, q );
					}
				}
				finally
				{
					detectionCreator.postAddition();
				}
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

	/**
	 * Creates a laplacian of gaussian (LoG) kernel tuned for blobs with a
	 * radius specified <b>using calibrated units</b>. The specified calibration
	 * is used to determine the dimensionality of the kernel and to map it on a
	 * pixel grid.
	 *
	 * @param radius
	 *            the blob radius (in image unit).
	 * @param nDims
	 *            the dimensionality of the desired kernel. Must be 1, 2 or 3.
	 * @param calibration
	 *            the pixel sizes, specified as <code>double[]</code> array.
	 * @return a new image containing the LoG kernel.
	 */
	public final RandomAccessibleInterval< FloatType > createLoGKernel( final double radius, final int nDims, final double[] calibration )
	{
		// Optimal sigma for LoG approach and dimensionality.
		final double sigma = radius / Math.sqrt( nDims );
		final double[] sigmaPixels = new double[ nDims ];
		for ( int d = 0; d < nDims; d++ )
			sigmaPixels[ d ] = sigma / calibration[ d ];

		final RandomAccessibleInterval< FloatType > kernel = ops().create().kernelLog( sigmaPixels, new FloatType() );
		return kernel;
	}
}
