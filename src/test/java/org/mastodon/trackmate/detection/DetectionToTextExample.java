package org.mastodon.trackmate.detection;

import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_RADIUS;
import static org.mastodon.detection.DetectorKeys.KEY_THRESHOLD;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.mastodon.detection.DetectionCreator;
import org.mastodon.detection.DetectionUtil;
import org.mastodon.detection.DetectorOp;
import org.mastodon.detection.DogDetectorOp;
import org.scijava.Context;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.sequence.TimePoint;
import net.imagej.ops.OpService;
import net.imagej.ops.special.inplace.Inplaces;

public class DetectionToTextExample
{
	private static class MyTextDetectionOutputter implements DetectionCreator
	{

		private int tp;

		private long id;

		private PrintWriter out;

		private final String outputFolder;

		public MyTextDetectionOutputter( final String outputFolder )
		{
			this.outputFolder = outputFolder;
			final File directory = new File( outputFolder );
			if ( !directory.exists() )
				directory.mkdirs();

			this.tp = 0;
			this.id = 0;
		}

		@Override
		public void createDetection( final double[] pos, final double radius, final int timepoint, final double quality )
		{
			out.println( String.format( "id = %15d, t = %3d, pos = ( %8.1f, %8.1f, %8.1f), R = %5.1f, Q = %7.1f",
					id++, timepoint, pos[ 0 ], pos[ 1 ], pos[ 2 ], radius, quality ) );
		}

		@Override
		public void preAddition()
		{
			final String fileName = String.format( "detections_%03d.txt", tp++ );
			final File targetFile = new File( outputFolder, fileName );
			System.out.println( "Adding to file " + targetFile );
			FileWriter fw = null;
			try
			{
				fw = new FileWriter( targetFile, true );
			}
			catch ( final IOException e )
			{
				e.printStackTrace();
			}
		    final BufferedWriter bw = new BufferedWriter(fw);
			out = new PrintWriter( bw );
		}

		@Override
		public void postAddition()
		{
			out.close();
		}

	}

	public static void main( final String[] args )
	{
		Locale.setDefault( Locale.ROOT );
		final Context context = new Context();
		final OpService ops = context.getService( OpService.class );

		/*
		 * Load SpimData
		 */
//		final String bdvFile = "samples/datasethdf5.xml";
//		final String bdvFile = "/Users/Jean-Yves/Desktop/MaMuT_demo_dataset/MaMuT_Parhyale_demo.xml";
		final String bdvFile = "/Users/tinevez/Projects/JYTinevez/MaMuT/MaMuT_demo_dataset/MaMuT_Parhyale_demo.xml";

		SpimDataMinimal sd = null;
		try
		{
			sd = new XmlIoSpimDataMinimal().load( bdvFile );
		}
		catch ( final SpimDataException e )
		{
			e.printStackTrace();
			return;
		}
		final SpimDataMinimal spimData = sd;

		final long start = System.currentTimeMillis();

		final DetectionCreator detectionCreator = new MyTextDetectionOutputter( "samples/detections" );

		final Map< String, Object > detectorSettings = DetectionUtil.getDefaultDetectorSettingsMap();
		detectorSettings.put( KEY_RADIUS, Double.valueOf( 20. ) );
		detectorSettings.put( KEY_THRESHOLD, Double.valueOf( 100. ) );
		final List< TimePoint > tps = sd.getSequenceDescription().getTimePoints().getTimePointsOrdered();
		detectorSettings.put( KEY_MIN_TIMEPOINT, tps.get( 0 ).getId() );
		detectorSettings.put( KEY_MAX_TIMEPOINT, tps.get( tps.size() - 1 ).getId() );

		final DetectorOp detector = ( DetectorOp ) Inplaces.binary1( ops, DogDetectorOp.class,
				detectionCreator, spimData, detectorSettings );
		detector.mutate1( detectionCreator, spimData );

		final long end = System.currentTimeMillis();
		final long processingTime = end - start;
		if ( !detector.isSuccessful() )
		{
			System.out.println( "Could not perform detection:\n" + detector.getErrorMessage() );
			return;
		}
		System.out.println( String.format( "Detection performed in %.1f s.", processingTime / 1000. ) );

	}

}
