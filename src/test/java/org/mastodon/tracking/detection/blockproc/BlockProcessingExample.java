package org.mastodon.tracking.detection.blockproc;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvPointsSource;
import bdv.util.BdvStackSource;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.dog.DogDetection.ExtremaType;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.img.Img;
import net.imglib2.loops.IntervalChunks;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class BlockProcessingExample
{

	private static final int MAX_N_BLOCKS = 500;

	private static Random rand = new Random();

	public static void main( final String[] args )
	{
		System.out.println( ObjectSizer.status() );
		System.out.println( "Creating image" );

		final double radius = 5.;
		final long width = 1000;
		final long height = 1000;
		final long depth = 500;
		final double spacing = 6.; // radius units
		final DetectionsOnGridCreator creator = new DetectionsOnGridCreator(
				new long[] { width, height, depth },
				radius,
				spacing );
		creator.create();
		final Img< UnsignedShortType > img = creator.getImg();
		final List< RealPoint > expected = creator.getPeaks();

		final BdvStackSource< UnsignedShortType > bdv = BdvFunctions.show( img, "detections" );
		bdv.setDisplayRange( 0, 1000 );

		System.out.println( ObjectSizer.status() );
		System.out.println( "Memory used by the image (estimate) : " + ObjectSizer.hr( ObjectSizer.imgSize( img ) ) );

		System.out.println( "Determining the lowest number of blocks compatible with available RAM:" );
		int nb = 1;
		for ( ; nb < MAX_N_BLOCKS; nb++ )
		{
			System.out.println( "  Processing with "
					+ ( ( nb == 1 )
							? "1 block: "
							: nb + " blocks: " ) );

			final List< Interval > intervals = IntervalChunks.chunkInterval( img, nb );
			final Interval largest = Intervals.expand( ObjectSizer.getLargestInterval( intervals ), ( long ) Math.ceil( radius ) );

			final long dogRequirements = ObjectSizer.estimateDoGDetectionRAM( largest, img.firstElement() );
			System.out.println( "  Memory required by the DoG detection (estimate) : " + ObjectSizer.hr( dogRequirements ) );

			if ( dogRequirements > ObjectSizer.freeRAM() / 2 )
			{

				System.out.println( "  RAM requirements too big. Increasing nBlocks." );
				continue;
			}
			else
			{
				System.out.println( "  Matches RAM requirements. Will use nBlocks = " + nb );
				break;
			}
		}
		final int nBlocks = nb;

		System.out.println( "Detection" );
		final List< RefinedPeak< Point > > peaks = detect( img, radius, nBlocks );
		System.out.println( "Finished" );
		System.out.println( "Found " + peaks.size() + " spots. Expected " + expected.size() );
		System.out.println( ObjectSizer.status() );

		final BdvOptions options = new BdvOptions().addTo( bdv );
		final BdvPointsSource pointsSource = BdvFunctions.showPoints( peaks, "spots", options );
		pointsSource.setColor( randomColor() );

	}

	private static List< RefinedPeak< Point > > detect( final Img< UnsignedShortType > img, final double radius, final int nBlocks )
	{
		final List< Interval > intervals = IntervalChunks.chunkInterval( img, nBlocks );

		// DoG detection constants.
		final int stepsPerOctave = 4;
		final double k = Math.pow( 2.0, 1.0 / stepsPerOctave );
		final double sigma = radius / Math.sqrt( img.numDimensions() );
		final double sigmaSmaller = sigma;
		final double sigmaLarger = k * sigmaSmaller;
		final double[] calibration = new double[ img.numDimensions() ];
		Arrays.fill( calibration, 1. );
		final double minPeakValue = 5.;

		final List< RefinedPeak< Point > > allPeaks = new ArrayList<>();
		for ( final Interval interval : intervals )
		{
			/*
			 * Dilate interval. In theory we could just expand by one pixel but
			 * we might have spots that are completely flat in intensity. We
			 * want to at least ensure we can detect any spots of the specified
			 * size. Normally we should increase taking into account the
			 * calibration.
			 */
			final FinalInterval expand = Intervals.expand( interval, ( long ) Math.ceil( radius ) );
			System.out.println( "Detection with " + Util.printInterval( expand ) );

			final DogDetection< UnsignedShortType > dog = new DogDetection< UnsignedShortType >(
					Views.extendMirrorDouble( img ),
					expand,
					calibration,
					sigmaSmaller,
					sigmaLarger,
					ExtremaType.MINIMA,
					minPeakValue,
					false );
			final List< RefinedPeak< Point > > peaks = dog.getSubpixelPeaks();

			System.out.println( "  Found " + peaks.size() + " raw spots." );
			System.out.println( "  Keeping spots in " + Util.printInterval( interval ) );

			int kept = 0;
			for ( final RefinedPeak< Point > peak : peaks )
			{
				if ( Intervals.contains( interval, peak.getOriginalPeak() ) )
				{
					kept++;
					allPeaks.add( peak );
				}
			}
			System.out.println( "  Kept " + kept + " spots." );
		}

		return allPeaks;
	}

	private static ARGBType randomColor()
	{
		final float r = rand.nextFloat();
		final float g = rand.nextFloat();
		final float b = rand.nextFloat();
		return new ARGBType( new Color( r, g, b ).getRGB() );
	}
}
