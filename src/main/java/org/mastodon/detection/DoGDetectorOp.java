package org.mastodon.detection;

import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_RADIUS;
import static org.mastodon.detection.DetectorKeys.KEY_ROI;
import static org.mastodon.detection.DetectorKeys.KEY_SETUP_ID;
import static org.mastodon.detection.DetectorKeys.KEY_THRESHOLD;

import java.util.ArrayList;
import java.util.List;

import org.mastodon.detection.DetectionCreatorFactory.DetectionCreator;
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

			/*
			 * Process image.
			 */

			final double[] calibration = DetectionUtil.getPhysicalCalibration( sources, tp, setup, level );
			final int stepsPerOctave = 4;
			final double k = Math.pow( 2.0, 1.0 / stepsPerOctave );
			final double sigma = radius / Math.sqrt( zeroMin.numDimensions() );
			final double sigmaSmaller = sigma;
			final double sigmaLarger = k * sigmaSmaller;
			final double normalization = 1.0 / ( sigmaLarger / sigmaSmaller - 1.0 );

			final DogDetection< FloatType > dog = new DogDetection<>(
					source,
					interval,
					calibration,
					sigmaSmaller,
					sigmaLarger,
					ExtremaType.MINIMA,
					threshold,
					true );
			dog.setExecutorService( threadService.getExecutorService() );
			final ArrayList< RefinedPeak< Point > > refinedPeaks = dog.getSubpixelPeaks();

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
					final double normalizedValue = -value * normalization;

					/*
					 * In case p is 2D we pass it to a 3D RealPoint to work
					 * nicely with the 3D transform.
					 */
					p3d.setPosition( p );
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
