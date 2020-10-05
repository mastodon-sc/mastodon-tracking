package org.mastodon.tracking.detection.blockproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.dog.DogDetection.ExtremaType;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class BlockProcessingExample
{

	private static final int MAX_N_BLOCKS = 500;

	public static void main( final String[] args )
	{
		System.out.println( ObjectSizer.status() );
		System.out.println( "Creating image" );

		final double radius = 1.; // um
		final long width = 5000;
		final long height = 5000;
		final long depth = 50;
		final double[] calibration = new double[] { 0.2, 0.2, 1. }; // um

		final double spacing = 6.; // radius units
		final DetectionsOnGridCreator creator = new DetectionsOnGridCreator(
				new long[] { width, height, depth },
				calibration,
				radius,
				spacing );
		creator.create();
		final Img< UnsignedShortType > img = creator.getImg();
		final List< RealPoint > expected = creator.getPeaks();

		final BdvOptions options1 = new BdvOptions().sourceTransform( calibration );
		final BdvStackSource< UnsignedShortType > bdv = BdvFunctions.show( img, "detections", options1 );
		bdv.setDisplayRange( 0, 1000 );

		System.out.println( ObjectSizer.status() );
		System.out.println( "Memory used by the image (estimate) : " + ObjectSizer.hr( ObjectSizer.imgSize( img ) ) );

		System.out.println( "Determining the lowest number of blocks compatible with available RAM:" );
		int nb = 1;
		List< Interval > intervals = Collections.singletonList( img );
		for ( ; nb < MAX_N_BLOCKS; nb++ )
		{
			System.out.println( "\nProcessing with "
					+ ( ( nb == 1 )
							? "no subidivision: "
							: nb + " divisions: " ) );

			intervals = ObjectSizer.chunkInterval( img, calibration, nb );
			final List< Interval > expanded = ObjectSizer.expand( intervals, img, calibration, radius );

			for ( int i = 0; i < intervals.size(); i++ )
				System.out.println( " - " + Util.printInterval( intervals.get( i ) ) +
						" -> " + Util.printInterval( expanded.get( i ) ) );

			final Interval largest = ObjectSizer.getLargestInterval( expanded );
			final long dogRequirements = ObjectSizer.estimateDoGDetectionRAM( largest, img.firstElement() );
			System.out.println( "  Memory required by the DoG detection (estimate) : " + ObjectSizer.hr( dogRequirements ) );

			if ( dogRequirements > ObjectSizer.freeRAM() / 2 )
			{

				System.out.println( "  RAM requirements too big. Increasing ndivisions." );
				continue;
			}
			else
			{
				System.out.println( "  Matches RAM requirements. Will use ndivisions = " + nb );
				break;
			}
		}

//		intervals = ObjectSizer.chunkInterval( img, calibration, 7 ); // DEBUG
		System.out.println( "Detection" );
		final List< RealLocalizable > peaks = detect( img, calibration, radius, intervals );
		System.out.println( "Finished" );
		System.out.println( "Found " + peaks.size() + " spots. Expected " + expected.size() );
		System.out.println( ObjectSizer.status() );

		final BdvOptions options2 = new BdvOptions().addTo( bdv );
		BdvFunctions.showPoints( peaks, "spots", options2 );
	}

	private static List< RealLocalizable > detect( final Img< UnsignedShortType > img, final double[] calibration, final double radius, final List< Interval > intervals )
	{
		// DoG detection constants.
		final int stepsPerOctave = 4;
		final double k = Math.pow( 2.0, 1.0 / stepsPerOctave );
		final double sigma = radius / Math.sqrt( img.numDimensions() );
		final double sigmaSmaller = sigma;
		final double sigmaLarger = k * sigmaSmaller;
		final double minPeakValue = 0.1;

		final List< RealLocalizable > allPeaks = new ArrayList<>();
		final List< Interval > expanded = ObjectSizer.expand( intervals, img, calibration, radius );
		for ( int i = 0; i < intervals.size(); i++ )
		{
			final Interval interval = intervals.get( i );

			/*
			 * Dilate interval. In theory we could just expand by one pixel but
			 * we might have spots that are completely flat in intensity. We
			 * want to at least ensure we can detect any spots of the specified
			 * size.
			 */
			final Interval expand = expanded.get( i );

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
					allPeaks.add( transform( peak, calibration ) );
				}
			}
			System.out.println( "  Kept " + kept + " spots." );
		}
		return allPeaks;
	}

	private static RealLocalizable transform( final RefinedPeak< Point > peak, final double[] calibration )
	{
		final double[] position = new double[ peak.numDimensions() ];
		for ( int d = 0; d < position.length; d++ )
			position[ d ] = peak.getDoublePosition( d ) * calibration[ d ];

		return RealPoint.wrap( position );
	}
}
