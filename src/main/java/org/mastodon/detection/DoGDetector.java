package org.mastodon.detection;

import java.util.ArrayList;

import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.feature.Feature;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;

import bdv.spimdata.SpimDataMinimal;
import bdv.util.Affine3DHelpers;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;
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
 */
public class DoGDetector implements Algorithm, MultiThreaded, Benchmark
{
	/**
	 * The minimal diameter size, in pixel, under which we stop down-sampling.
	 */
	private static final double MIN_SPOT_PIXEL_SIZE = 5d;

	private long processingTime;

	private int numThreads;

	private String errorMessage;

	private final SpimDataMinimal spimData;

	private final double radius;

	private final double threshold;

	private final int setup;

	private final int minTimepoint;

	private final int maxTimepoint;

	private final ModelGraph graph;

	private Feature< Spot, Double, DoublePropertyMap< Spot > > qualityFeature;

	/**
	 * Instantiates a new DoG-based detector.
	 *
	 * @param spimData
	 *            the {@link SpimDataMinimal} linking to the image data.
	 * @param graph
	 *            the model graph. Spots found by the detector will be added to
	 *            it. It is not modified otherwise.
	 * @param radius
	 *            the expected radius (in units of the global coordinate system)
	 *            of blobs to detect.
	 * @param threshold
	 *            the quality threshold below which spots will be rejected.
	 * @param setup
	 *            the setup id in the data.
	 * @param minTimepoint
	 *            the min time-point to process, inclusive.
	 * @param maxTimepoint
	 *            the max time-point to process, inclusive.
	 */
	public DoGDetector( final SpimDataMinimal spimData, final ModelGraph graph, final double radius, final double threshold, final int setup, final int minTimepoint, final int maxTimepoint )
	{
		this.spimData = spimData;
		this.graph = graph;
		this.radius = radius;
		this.threshold = threshold;
		this.setup = setup;
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
		final long start = System.currentTimeMillis();

		final DoublePropertyMap< Spot > quality = new DoublePropertyMap<>( graph.vertices(), Double.NaN );

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
			 * Find scale to express radius in pixel units.
			 */

			double scale = Affine3DHelpers.extractScale( transform, 0 );
			for ( int axis = 1; axis < transform.numDimensions(); axis++ )
			{
				final double sc = Affine3DHelpers.extractScale( transform, axis );
				if ( sc > scale )
					scale = sc;
			}

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

			final int stepsPerOctave = 4;
			final double k = Math.pow( 2.0, 1.0 / stepsPerOctave );
			final double sigma = radius / Math.sqrt( 3 ) / scale;
			final double sigmaSmaller = sigma;
			final double sigmaLarger = k * sigmaSmaller;
			final double normalization = 1.0 / ( sigmaLarger / sigmaSmaller - 1.0 );

			final double xs = Affine3DHelpers.extractScale( transform, 0 );
			final double ys = Affine3DHelpers.extractScale( transform, 1 );
			final double zs = Affine3DHelpers.extractScale( transform, 2 );
			final double[] pixelSize = new double[] { 1, ys / xs, zs / xs };

			final DogDetection< FloatType > dog = new DogDetection<>(
					source,
					interval, pixelSize, sigmaSmaller, sigmaLarger, ExtremaType.MINIMA, threshold / normalization, true );
			dog.setNumThreads( numThreads );
			final ArrayList< RefinedPeak< Point > > refinedPeaks = dog.getSubpixelPeaks();

			final Spot ref = graph.vertexRef();
			final double[] pos = new double[ 3 ];
			final RealPoint sp = RealPoint.wrap( pos );
			for ( final RefinedPeak< Point > p : refinedPeaks )
			{
				final double value = p.getValue();
				final double normalizedValue = -value * normalization;
				transform.apply( p, sp );
				final Spot spot = graph.addVertex( ref ).init( tp, pos, radius );
				quality.set( spot, normalizedValue );
			}
			graph.releaseRef( ref );
		}

		this.qualityFeature = DetectionUtil.getQualityFeature( quality );

		final long end = System.currentTimeMillis();
		this.processingTime = end - start;
		return true;
	}

	public Feature< Spot, Double, DoublePropertyMap< Spot > > getQualityFeature()
	{
		return qualityFeature;
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
