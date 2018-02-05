package org.mastodon.detection;

import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_RADIUS;
import static org.mastodon.detection.DetectorKeys.KEY_ROI;
import static org.mastodon.detection.DetectorKeys.KEY_SETUP_ID;
import static org.mastodon.detection.DetectorKeys.KEY_THRESHOLD;
import static org.mastodon.detection.DogDetectorOp.MIN_SPOT_PIXEL_SIZE;

import java.util.List;

import org.mastodon.graph.Graph;
import org.mastodon.graph.Vertex;
import org.mastodon.properties.DoublePropertyMap;
import org.scijava.app.StatusService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import bdv.spimdata.SpimDataMinimal;
import bdv.util.Affine3DHelpers;
import net.imagej.ops.special.function.Functions;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * Laplacian of Gaussian detector.
 *
 * @author Jean-Yves Tinevez
 * @param <V>
 *            the type of the vertices in the graph.
 */
@Plugin( type = DetectorOp.class )
public class LoGDetectorOp< V extends Vertex< ? > & RealLocalizable >
		extends AbstractDetectorOp< V >
		implements DetectorOp< V >, Benchmark
{

	@Parameter
	private ThreadService threadService;

	@Parameter
	private StatusService statusService;

	private long processingTime;

	private final boolean doSubpixelLocalization = true;

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
		statusService.showStatus( "LoG detection" );
		for ( int tp = minTimepoint; tp <= maxTimepoint; tp++ )
		{
			statusService.showProgress( tp - minTimepoint + 1, maxTimepoint - minTimepoint + 1 );

			// Did we get canceled?
			if ( isCanceled() )
				break;

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

			@SuppressWarnings( "rawtypes" )
			final RandomAccessibleInterval img = DetectionUtil.getImage( spimData, tp, setup, level );
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

			final double xs = Affine3DHelpers.extractScale( transform, 0 );
			final double ys = Affine3DHelpers.extractScale( transform, 1 );
			final double zs = Affine3DHelpers.extractScale( transform, 2 );
			final double[] calibration = new double[] { xs, ys, zs };

			final RandomAccessibleInterval< FloatType > kernel = createLoGKernel( radius, zeroMin.numDimensions(), calibration );
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
			final double sigmaPixels = sigma / calibration[ 0 ];
			final FloatType C = new FloatType( ( float ) ( 1. / Math.PI / sigmaPixels / sigmaPixels ) );
			Views.iterable( output ).forEach( ( e ) -> e.div( C ) );

			/*
			 * Detect local maxima.
			 */

			final List< Point > peaks = DetectionUtil.findLocalMaxima( output, threshold, threadService.getExecutorService() );
			if ( doSubpixelLocalization )
			{
				final int maxNumMoves = 10;
				final boolean allowMaximaTolerance = true;
				final boolean returnInvalidPeaks = true;
				@SuppressWarnings( { "rawtypes", "unchecked" } )
				final SubpixelLocalization< Point, FloatType > subpixel =
						( SubpixelLocalization ) Functions.binary( ops(), SubpixelLocalization.class,
								List.class, output, peaks,
								output, maxNumMoves, allowMaximaTolerance, returnInvalidPeaks );
				final List< RefinedPeak< Point > > refined = subpixel.calculate( output, peaks );

				final RandomAccess< FloatType > ra = output.randomAccess();
				final V ref = graph.vertexRef();
				final double[] pos = new double[ 3 ];
				final RealPoint point = RealPoint.wrap( pos );
				final RealPoint p3d = new RealPoint( 3 );
				for ( final RefinedPeak< Point > refinedPeak : refined )
				{
					ra.setPosition( refinedPeak.getOriginalPeak() );
					final double q = ra.get().getRealDouble();

					p3d.setPosition( refinedPeak );
					transform.apply( p3d, point );
					final V spot = vertexCreator.createVertex( graph, ref, pos, radius, tp, q );
					quality.set( spot, q );
				}
				graph.releaseRef( ref );
			}
			else
			{
				final RandomAccess< FloatType > ra = output.randomAccess();
				final double[] pos = new double[ 3 ];
				final RealPoint point = RealPoint.wrap( pos );
				final V ref = graph.vertexRef();
				for ( final Point peak : peaks )
				{
					ra.setPosition( peak );
					final double q = ra.get().getRealDouble();

					transform.apply( peak, point );
					final V spot = vertexCreator.createVertex( graph, ref, pos, radius, tp, q );
					quality.set( spot, q );
				}
				graph.releaseRef( ref );
			}
		}

		final long end = System.currentTimeMillis();
		processingTime = end - start;
		@SuppressWarnings( "unchecked" )
		final Class< V > clazz = ( Class< V > ) graph.vertexRef().getClass();
		qualityFeature = DetectionUtil.getQualityFeature( quality, clazz );
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
