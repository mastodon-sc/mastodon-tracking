package org.mastodon.detection;

import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_RADIUS;
import static org.mastodon.detection.DetectorKeys.KEY_ROI;
import static org.mastodon.detection.DetectorKeys.KEY_SETUP_ID;
import static org.mastodon.detection.DetectorKeys.KEY_THRESHOLD;

import java.util.ArrayList;

import org.mastodon.graph.Graph;
import org.mastodon.graph.Vertex;
import org.mastodon.properties.DoublePropertyMap;
import org.scijava.app.StatusService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import bdv.spimdata.SpimDataMinimal;
import bdv.util.Affine3DHelpers;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.dog.DogDetection.ExtremaType;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * Difference of Gaussian detector.
 *
 * @author Tobias Pietzsch
 * @author Jean-Yves Tinevez
 * @param <V>
 *            the type of the vertices in the graph.
 */
@Plugin( type = DetectorOp.class )
public class DogDetectorOp< V extends Vertex< ? > & RealLocalizable >
		extends AbstractDetectorOp< V >
		implements DetectorOp< V >, Benchmark
{

	@Parameter
	private ThreadService threadService;

	@Parameter
	private StatusService statusService;

	/**
	 * The minimal diameter size, in pixel, under which we stop down-sampling.
	 */
	private static final double MIN_SPOT_PIXEL_SIZE = 10d;

	private long processingTime;

	@Override
	public void mutate1( final Graph< V, ? > graph, final SpimDataMinimal spimData )
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

		final DoublePropertyMap< V > quality = new DoublePropertyMap<>( graph.vertices(), Double.NaN );

		statusService.showStatus( "DoG detection." );
		for ( int tp = minTimepoint; tp <= maxTimepoint; tp++ )
		{

			// Check if there is some data at this timepoint.
			if ( !DetectionUtil.isPresent( spimData, setup, tp ) )
				continue;

			/*
			 * Determine optimal level for detection.
			 */

			final int level = DetectionUtil.determineOptimalResolutionLevel( spimData, radius, MIN_SPOT_PIXEL_SIZE / 2., tp, setup );
			final AffineTransform3D transform = DetectionUtil.getTransform( spimData, tp, setup, level );
			final AffineTransform3D mipmapTransform = DetectionUtil.getMipmapTransform( spimData, tp, setup, level );

			/*
			 * Load and extends image data.
			 */

			final RandomAccessibleInterval< ? > img = DetectionUtil.getImage( spimData, tp, setup, level );
			final IntervalView< ? > zeroMin = Views.zeroMin( img );

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

				mipmapTransform.applyInverse( minTarget, minSource );
				mipmapTransform.applyInverse( maxTarget, maxSource );

				final long[] tmin = new long[ 3 ];
				final long[] tmax = new long[ 3 ];
				for ( int d = 0; d < tmax.length; d++ )
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

			final double xs = Affine3DHelpers.extractScale( transform, 0 );
			final double ys = Affine3DHelpers.extractScale( transform, 1 );
			final double zs = Affine3DHelpers.extractScale( transform, 2 );
			final double[] pixelSize = new double[] { 1, ys / xs, zs / xs };
			final double scale = xs;

			final int stepsPerOctave = 4;
			final double k = Math.pow( 2.0, 1.0 / stepsPerOctave );
			final double sigma = radius / Math.sqrt( img.numDimensions() ) / scale;
			final double sigmaSmaller = sigma;
			final double sigmaLarger = k * sigmaSmaller;
			final double normalization = 1.0 / ( sigmaLarger / sigmaSmaller - 1.0 );

			final DogDetection< FloatType > dog = new DogDetection<>(
					source,
					interval,
					pixelSize,
					sigmaSmaller,
					sigmaLarger,
					ExtremaType.MINIMA,
					threshold / normalization,
					true );
			dog.setExecutorService( threadService.getExecutorService() );
			final ArrayList< RefinedPeak< Point > > refinedPeaks = dog.getSubpixelPeaks();

			final V ref = graph.vertexRef();
			final double[] pos = new double[ 3 ];
			final RealPoint sp = RealPoint.wrap( pos );
			for ( final RefinedPeak< Point > p : refinedPeaks )
			{
				final double value = p.getValue();
				final double normalizedValue = -value * normalization;
				transform.apply( p, sp );
				final V spot = vertexCreator.createVertex( graph, ref, pos, radius, tp, normalizedValue );
				quality.set( spot, normalizedValue );
			}
			graph.releaseRef( ref );

			statusService.showProgress( tp, maxTimepoint - minTimepoint + 1 );
		}

		final long end = System.currentTimeMillis();
		qualityFeature = DetectionUtil.getQualityFeature( quality );
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
