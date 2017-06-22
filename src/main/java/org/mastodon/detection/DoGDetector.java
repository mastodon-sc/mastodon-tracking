package org.mastodon.detection;

import java.util.ArrayList;

import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;

import bdv.ViewerImgLoader;
import bdv.spimdata.SpimDataMinimal;
import bdv.util.Affine3DHelpers;
import mpicbg.spim.data.generic.sequence.BasicViewDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.generic.sequence.ImgLoaderHints;
import mpicbg.spim.data.sequence.ViewId;
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
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class DoGDetector implements Algorithm, MultiThreaded, Benchmark
{
	/** The minimal diameter size, in pixel, under which we stop down-sampling. */
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

	public DoGDetector( final SpimDataMinimal spimData , final ModelGraph graph, final double radius, final double threshold, final int setup, final int minTimepoint, final int maxTimepoint)
	{
		this.spimData = spimData;
		this.graph = graph;
		this.radius = radius;
		this.threshold = threshold;
		this.setup = setup;
		this.minTimepoint = minTimepoint;
		this.maxTimepoint = maxTimepoint;

	}

	private < T extends RealType< T > > RandomAccessible< FloatType > asExtendedFloat(
			final RandomAccessibleInterval< T > img )
	{
		final RealFloatConverter< T > converter = new RealFloatConverter<>();
		return Views.extendMirrorSingle( Converters.convert( img, converter, new FloatType() ) );
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
		for ( int tp = minTimepoint; tp <= maxTimepoint; tp++ )
		{
			// Check if there is some data at this timepoint.
			final BasicViewDescription< BasicViewSetup > vd = spimData.getSequenceDescription().getViewDescriptions().get( new ViewId( tp, setup ) );
			if (  null == vd || !vd.isPresent() )
			{
				System.out.println( "No data found for tp = " + tp + ", setup = " + setup ); // DEBUG
				continue;
			}

			/*
			 * Determine optimal level for detection.
			 */

			final int numMipmapLevels = ( ( ViewerImgLoader ) spimData.getSequenceDescription().getImgLoader() ).getSetupImgLoader( setup ).numMipmapLevels();
			int level = 0;
			final AffineTransform3D transform = new AffineTransform3D();
			while ( level < numMipmapLevels - 1 )
			{

				/*
				 * Scan all axes. The "worst" one is the one with the largest scale.
				 * If at this scale the spot is too small, then we stop.
				 */

				transform.set( spimData.getViewRegistrations().getViewRegistration( tp, setup ).getModel() );
				double scale = Affine3DHelpers.extractScale( transform, 0 );
				for ( int axis = 1; axis < transform.numDimensions(); axis++ )
				{
					final double sc = Affine3DHelpers.extractScale( transform, axis );
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
			System.out.println( "Picked up level " + level + " out of " + numMipmapLevels + " levels."); // DEBUG
			System.out.println( "Transform at this level is " + transform ); // DEBUG
//			transform.set( 5., 2, 2 );

			/*
			 * Load and extends image data.
			 */

			final RandomAccessibleInterval< ? > img = spimData.getSequenceDescription().getImgLoader().getSetupImgLoader( setup )
					.getImage( tp, ImgLoaderHints.LOAD_COMPLETELY );
			@SuppressWarnings( { "unchecked", "rawtypes" } )
			final RandomAccessible< FloatType > source = asExtendedFloat( ( RandomAccessibleInterval ) img );
			final Interval interval = new FinalInterval( img );

			/*
			 * Process image.
			 */

			// get detections
			final int stepsPerOctave = 4;
			final double k = Math.pow( 2.0, 1.0 / stepsPerOctave );
			final double sigma = radius / Math.sqrt( 3 );
			final double sigmaSmaller = sigma ;
			final double sigmaLarger = k * sigmaSmaller;

			final double xs = Affine3DHelpers.extractScale( transform, 0 );
			final double ys = Affine3DHelpers.extractScale( transform, 1 );
			final double zs = Affine3DHelpers.extractScale( transform, 2 );
			final double[] pixelSize = new double[] { 1, ys / xs, zs / xs };

			final DogDetection< FloatType > DOG = new DogDetection<>(
					source,
					interval, pixelSize, sigmaSmaller, sigmaLarger, ExtremaType.MINIMA, threshold, true );
			final ArrayList< RefinedPeak< Point > > refinedPeaks = DOG.getSubpixelPeaks();
			System.out.println("At tp = " +tp + " found  " + refinedPeaks.size() + " initial spots");

			final Spot ref = graph.vertexRef();
			final double normalization = 1.0 / ( sigmaLarger / sigmaSmaller - 1.0 );
			final double[] pos = new double[3];
			final RealPoint sp = new RealPoint( 3 );
			for ( final RefinedPeak< Point > p : refinedPeaks )
			{
				final double value = p.getValue();
				final double normalizedValue = -value * normalization;
				if (normalizedValue < threshold)
					continue;

				transform.apply( p, sp );
				sp.localize( pos );
				final Spot spot = graph.addVertex( ref ).init(tp, pos , radius );
				System.out.println( "  kept spot " + spot + " with Quality = " + normalizedValue ); // DEBUG
			}
			graph.releaseRef( ref );
		}

		final long end = System.currentTimeMillis();
		this.processingTime = end - start;
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
