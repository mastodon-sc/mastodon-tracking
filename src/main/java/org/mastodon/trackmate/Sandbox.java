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
import org.mastodon.linking.LinkingUtils;
import org.mastodon.linking.mamut.SparseLAPLinkerMamut;
import org.mastodon.revised.mamut.MainWindow;
import org.mastodon.revised.model.mamut.Model;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimDataException;
import net.imagej.ImageJ;

public class Sandbox
{

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		Locale.setDefault( Locale.US );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		final ImageJ ij = new ImageJ();
		ij.launch( args );

		/*
		 * Load SpimData
		 */
		final String bdvFile = "samples/datasethdf5.xml";
//		final String bdvFile = "/Users/tinevez/Projects/JYTinevez/MaMuT/MaMuT_demo_dataset/MaMuT_Parhyale_demo.xml";
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
		final double radius = 6.;
		final double threshold = 1000.;
		final int setup = 0;

		final Class< DoGDetectorMamut > detectorClass = DoGDetectorMamut.class;
//		final Class< LoGDetectorMamut > detectorClass = LoGDetectorMamut.class;

		final Map< String, Object > detectorSettings = DetectionUtil.getDefaultDetectorSettingsMap();
		detectorSettings.put( KEY_SETUP_ID, setup );
		detectorSettings.put( KEY_MIN_TIMEPOINT, minTimepoint );
		detectorSettings.put( KEY_MAX_TIMEPOINT, maxTimepoint );
		detectorSettings.put( KEY_RADIUS, radius );
		detectorSettings.put( KEY_THRESHOLD, threshold );

		final Class< SparseLAPLinkerMamut > linkerClass = SparseLAPLinkerMamut.class;
		final Map< String, Object > linkerSettings = LinkingUtils.getDefaultLAPSettingsMap();
		linkerSettings.put( KEY_MIN_TIMEPOINT, minTimepoint );
		linkerSettings.put( KEY_MAX_TIMEPOINT, maxTimepoint );

//		final Map< String, Object > linkerSettings = KalmanLinkerMamut.getDefaultSettingsMap();
//		final Class< KalmanLinkerMamut > linkerClass = KalmanLinkerMamut.class;

		final Settings settings = new Settings()
				.spimData( spimData )
				.detector( detectorClass )
				.detectorSettings( detectorSettings )
				.linker( linkerClass )
				.linkerSettings( linkerSettings );
		final Model model = new Model();
		final TrackMate trackmate = new TrackMate( settings, model );
		trackmate.setContext( ij.context() );
		trackmate.run();

		new MainWindow( model, spimData, bdvFile, getInputTriggerConfig() ).setVisible( true );
	}

	public static final InputTriggerConfig getInputTriggerConfig()
	{
		InputTriggerConfig conf = null;

		// try "keyconfig.yaml" in current directory
		if ( new File( "keyconfig.yaml" ).isFile() )
		{
			try
			{
				conf = new InputTriggerConfig( YamlConfigIO.read( "keyconfig.yaml" ) );
			}
			catch ( final IOException e )
			{}
		}

		// try "~/.mastodon/keyconfig.yaml"
		if ( conf == null )
		{
			final String fn = System.getProperty( "user.home" ) + "/.mastodon/keyconfig.yaml";
			if ( new File( fn ).isFile() )
			{
				try
				{
					conf = new InputTriggerConfig( YamlConfigIO.read( fn ) );
				}
				catch ( final IOException e )
				{}
			}
		}

		if ( conf == null )
		{
			conf = new InputTriggerConfig();
		}

		return conf;
	}
}
