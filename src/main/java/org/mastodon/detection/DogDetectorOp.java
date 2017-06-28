package org.mastodon.detection;

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
		final long start = System.currentTimeMillis();

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

			/*
			 * Load and extends image data.
			 */

			final RandomAccessibleInterval< ? > img = DetectionUtil.getImage( spimData, tp, setup, level );

			@SuppressWarnings( { "unchecked", "rawtypes" } )
			final RandomAccessible< FloatType > source = DetectionUtil.asExtendedFloat( ( RandomAccessibleInterval ) img );
			final Interval interval = new FinalInterval( img );

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
		this.qualityFeature = DetectionUtil.getQualityFeature( quality );

		final long end = System.currentTimeMillis();
		this.processingTime = end - start;
		statusService.clearStatus();
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}
}
