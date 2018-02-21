package org.mastodon.trackmate;

import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_RADIUS;
import static org.mastodon.detection.DetectorKeys.KEY_SETUP_ID;
import static org.mastodon.detection.DetectorKeys.KEY_THRESHOLD;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mastodon.detection.DetectionUtil;
import org.mastodon.detection.mamut.DoGDetectorMamut;
import org.mastodon.linking.mamut.KalmanLinkerMamut;
import org.mastodon.revised.mamut.MainWindow;
import org.mastodon.revised.mamut.MamutProject;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.revised.model.mamut.Model;
import org.scijava.Context;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimDataException;

public class Sandbox
{

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, InterruptedException, IOException, SpimDataException
	{
		Locale.setDefault( Locale.US );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		final Context context = new Context();

		/*
		 * Load SpimData
		 */
//		final String bdvFile = "samples/datasethdf5.xml";
//		final String bdvFile = "/Users/pietzsch/Desktop/data/MAMUT/MaMuT_demo_dataset/MaMuT_Parhyale_demo.xml";
		final String bdvFile = "/Users/tinevez/Projects/JYTinevez/MaMuT/MaMuT_demo_dataset/MaMuT_Parhyale_demo.xml";
//		final String bdvFile = "/Users/Jean-Yves/Desktop/MaMuT_demo_dataset/MaMuT_Parhyale_demo.xml";
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

		final int maxTimepoint = spimData.getSequenceDescription().getTimePoints().size() - 1;
		final int minTimepoint = 0;
		final double radius = 20.;
		final double threshold = 100.;
		final int setup = 0;

		final Class< DoGDetectorMamut > detectorClass = DoGDetectorMamut.class;
//		final Class< LoGDetectorMamut > detectorClass = LoGDetectorMamut.class;

		final Map< String, Object > detectorSettings = DetectionUtil.getDefaultDetectorSettingsMap();
		detectorSettings.put( KEY_SETUP_ID, setup );
		detectorSettings.put( KEY_MIN_TIMEPOINT, minTimepoint );
		detectorSettings.put( KEY_MAX_TIMEPOINT, maxTimepoint );
		detectorSettings.put( KEY_RADIUS, radius );
		detectorSettings.put( KEY_THRESHOLD, threshold );

//		final Class< SparseLAPLinkerMamut > linkerClass = SparseLAPLinkerMamut.class;
//		final Map< String, Object > linkerSettings = LinkingUtils.getDefaultLAPSettingsMap();

		final Map< String, Object > linkerSettings = KalmanLinkerMamut.getDefaultSettingsMap();
		final Class< KalmanLinkerMamut > linkerClass = KalmanLinkerMamut.class;

		linkerSettings.put( KEY_MIN_TIMEPOINT, minTimepoint );
		linkerSettings.put( KEY_MAX_TIMEPOINT, maxTimepoint );

		final Settings settings = new Settings()
				.spimData( spimData )
				.detector( detectorClass )
				.detectorSettings( detectorSettings )
				.linker( linkerClass )
				.linkerSettings( linkerSettings );

		final WindowManager wm = new WindowManager( context );
		final MamutProject project = new MamutProject( null, new File(bdvFile) );
		wm.getProjectManager().open( project );
		final Model model = wm.getAppModel().getModel();
		final TrackMate trackmate = new TrackMate( settings, model );
		trackmate.setContext( context );

//		new Thread( trackmate ).start();
//		System.out.println( "Started TrackMate thread." );
//		Thread.sleep( 5000 );
//		System.out.println( "Cancelling after 5s." );
//		trackmate.cancel( "I was bored." );

		trackmate.run();
		if ( trackmate.isCanceled() )
			System.out.println( "Calculation was canceled. Reason: " + trackmate.getCancelReason() );
		else if (!trackmate.isSuccessful())
			System.err.println( "Calculation failed with error message:\n" + trackmate.getErrorMessage() );
		else
			System.out.println( "Calculation complete." );


		new MainWindow( wm).setVisible( true );
	}
}
