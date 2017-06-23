package org.mastodon.detection;

import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.feature.Feature;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;

import bdv.spimdata.SpimDataMinimal;
import bdv.util.Affine3DHelpers;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;

/**
 * Laplacian of Gaussian detector.
 *
 * @author Jean-Yves Tinevez
 */
public class LoGDetector implements Algorithm, MultiThreaded, Benchmark
{
	/**
	 * The minimal diameter size, in pixel, under which we stop down-sampling.
	 */
	private static final double MIN_SPOT_PIXEL_SIZE = 10d;

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
	public LoGDetector( final SpimDataMinimal spimData, final ModelGraph graph, final double radius, final double threshold, final int setup, final int minTimepoint, final int maxTimepoint )
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

	@SuppressWarnings( "unchecked" )
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
			 * Load and extends image data.
			 */

			@SuppressWarnings( "rawtypes" )
			final RandomAccessibleInterval img = DetectionUtil.getImage( spimData, tp, setup, level );
			final Interval interval = new FinalInterval( img );

			/*
			 * Filter image.
			 */

			final double xs = Affine3DHelpers.extractScale( transform, 0 );
			final double ys = Affine3DHelpers.extractScale( transform, 1 );
			final double zs = Affine3DHelpers.extractScale( transform, 2 );
			final double[] calibration = new double[] { xs, ys , zs  };

			final Img< FloatType > output = Util.getArrayOrCellImgFactory( interval, new FloatType() )
					.create( interval, new FloatType() );
			final Img< FloatType > kernel = createLoGKernel( radius, img.numDimensions(), calibration );
			final FFTConvolver convolver = new FFTConvolver( kernel, interval );
			convolver.setNumThreads( numThreads );
			convolver.convolve( img, output );

			/*
			 * Detect local maxima.
			 */

			DetectionUtil.findLocalMaxima( graph, quality, output, transform, threshold, radius, true, tp, numThreads );
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
	public static final Img< FloatType > createLoGKernel( final double radius, final int nDims, final double[] calibration )
	{
		// Optimal sigma for LoG approach and dimensionality.
		final double sigma = radius / Math.sqrt( nDims );
		final double[] sigmaPixels = new double[ nDims ];
		for ( int i = 0; i < sigmaPixels.length; i++ )
			sigmaPixels[ i ] = sigma / calibration[ i ];

		final int n = sigmaPixels.length;
		final long[] sizes = new long[ n ];
		final long[] middle = new long[ n ];
		for ( int d = 0; d < n; ++d )
		{
			// From Tobias Gauss3
			final int hksizes = Math.max( 2, ( int ) ( 3 * sigmaPixels[ d ] + 0.5 ) + 1 );
			sizes[ d ] = 3 + 2 * hksizes;
			middle[ d ] = 1 + hksizes;

		}
		final ArrayImg< FloatType, FloatArray > kernel = ArrayImgs.floats( sizes );

		final ArrayCursor< FloatType > c = kernel.cursor();
		final long[] coords = new long[ nDims ];


		// Work in image coordinates
		while ( c.hasNext() )
		{
			c.fwd();
			c.localize( coords );

			double sumx2 = 0.;
			double mantissa = 0.;
			for ( int d = 0; d < coords.length; d++ )
			{
				final double x = calibration[ d ] * ( coords[ d ] - middle[ d ] );
				sumx2 += ( x * x );
				mantissa += 1. / sigmaPixels[ d ] / sigmaPixels[ d ] * ( x * x / sigma / sigma - 1 );
			}
			final double exponent = -sumx2 / 2. / sigma / sigma;

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
			final double C = 1. / Math.PI / sigmaPixels[ 0 ] / sigmaPixels[ 0 ];

			c.get().setReal( -C * mantissa * Math.exp( exponent ) );
		}

		return kernel;
	}

}
