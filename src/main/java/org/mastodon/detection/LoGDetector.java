package org.mastodon.detection;

import java.util.List;

import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.feature.Feature;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import bdv.spimdata.SpimDataMinimal;
import bdv.util.Affine3DHelpers;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.hybrid.AbstractUnaryHybridCF;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * Laplacian of Gaussian detector.
 *
 * @author Jean-Yves Tinevez
 */
@Plugin( type = SpotDetectorOp.class )
public class LoGDetector extends AbstractUnaryHybridCF< SpimDataMinimal, ModelGraph > implements SpotDetectorOp, Benchmark
{

	@Parameter
	private ThreadService threadService;

	@Parameter
	private StatusService statusService;

	/**
	 * The minimal diameter size, in pixel, under which we stop down-sampling.
	 */
	private static final double MIN_SPOT_PIXEL_SIZE = 10d;

	/**
	 * The id of the setup in the provided SpimData object to process.
	 */
	@Parameter( required = true )
	private int setup = 0;

	/**
	 * the expected radius (in units of the global coordinate system) of blobs
	 * to detect.
	 */
	@Parameter( required = true )
	private double radius = 5.;

	/**
	 * The quality threshold below which spots will be rejected.
	 */
	@Parameter
	private double threshold = 0.;

	/**
	 * The min time-point to process, inclusive.
	 */
	@Parameter
	private int minTimepoint = 0;

	/**
	 * The max time-point to process, inclusive.
	 */
	@Parameter
	private int maxTimepoint = 0;

	/**
	 * The quality feature provided by this detector.
	 */
	@Parameter( type = ItemIO.OUTPUT )
	private Feature< Spot, Double, DoublePropertyMap< Spot > > qualityFeature;

	private long processingTime;

	private final boolean doSubpixelLocalization = true;

	@Override
	public ModelGraph createOutput( final SpimDataMinimal spimData )
	{
		return new ModelGraph();
	}

	@SuppressWarnings( { "unchecked" } )
	@Override
	public void compute( final SpimDataMinimal spimData, final ModelGraph graph )
	{
		final long start = System.currentTimeMillis();

		final DoublePropertyMap< Spot > quality = new DoublePropertyMap<>( graph.vertices(), Double.NaN );
		statusService.showStatus( "LoG detection" );
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

			/*
			 * Filter image.
			 */

			final double xs = Affine3DHelpers.extractScale( transform, 0 );
			final double ys = Affine3DHelpers.extractScale( transform, 1 );
			final double zs = Affine3DHelpers.extractScale( transform, 2 );
			final double[] calibration = new double[] { xs, ys, zs };

			final RandomAccessibleInterval< FloatType > kernel = createLoGKernel( radius, img.numDimensions(), calibration );
			final RandomAccessibleInterval< FloatType > output = ops().filter().convolve( img, kernel );

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
				final boolean  allowMaximaTolerance = true;
				final boolean returnInvalidPeaks = true;
				@SuppressWarnings( "rawtypes" )
				final SubpixelLocalization< Point, FloatType > subpixel =
						( SubpixelLocalization ) Functions.binary( ops(), SubpixelLocalization.class,
								List.class, output, peaks,
								output, maxNumMoves , allowMaximaTolerance , returnInvalidPeaks  );
				final List< RefinedPeak< Point > > refined = subpixel.calculate( output, peaks );

				final RandomAccess< FloatType > ra = output.randomAccess();
				final Spot ref = graph.vertexRef();
				final double[] pos = new double[ 3 ];
				final RealPoint point = RealPoint.wrap( pos );
				for ( final RefinedPeak< Point > refinedPeak : refined )
				{
					point.setPosition( refinedPeak );
					transform.apply( refinedPeak, point );
					final Spot spot = graph.addVertex( ref ).init( tp, pos, radius );

					ra.setPosition( refinedPeak.getOriginalPeak() );
					final double q = ra.get().getRealDouble();
					quality.set( spot, q );
				}
				graph.releaseRef( ref );
			}
			else
			{
				final RandomAccess< FloatType > ra = output.randomAccess();
				final double[] pos = new double[ 3 ];
				final RealPoint point = RealPoint.wrap( pos );
				final Spot ref = graph.vertexRef();
				for ( final Point peak : peaks )
				{
					transform.apply( peak, point );
					final Spot spot = graph.addVertex( ref ).init( tp, pos, radius );

					ra.setPosition( peak );
					final double q = ra.get().getRealDouble();
					quality.set( spot, q );
				}
				graph.releaseRef( ref );
			}

			statusService.showProgress( tp, maxTimepoint - minTimepoint + 1 );
		}

		this.qualityFeature = DetectionUtil.getQualityFeature( quality );

		final long end = System.currentTimeMillis();
		this.processingTime = end - start;
		statusService.clearStatus();
	}

	@Override
	public Feature< Spot, Double, DoublePropertyMap< Spot > > getQualityFeature()
	{
		return qualityFeature;
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
		for ( int i = 0; i < sigmaPixels.length; i++ )
			sigmaPixels[ i ] = sigma / calibration[ i ];

		final RandomAccessibleInterval< FloatType > kernel = ops().create().kernelLog( sigmaPixels, new FloatType() );
		return kernel;
	}

}
