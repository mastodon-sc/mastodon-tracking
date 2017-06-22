package org.mastodon.detection;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimDataException;

public class DetectionSandbox
{

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		Locale.setDefault( Locale.US );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		/*
		 * Load SpimData
		 */
		final String bdvFile = "samples/datasethdf5.xml";
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

		final int maxTimepoint = spimData.getSequenceDescription().getTimePoints().size() -1;
		final int minTimepoint = 0;

		final Model model = new Model();
		final ModelGraph graph = model.getGraph();

		final double radius = 6.;
		final double threshold = 100.;
		final int setup = 0;
		final DoGDetector detector = new DoGDetector(spimData, graph, radius, threshold , setup, minTimepoint, maxTimepoint);
		if (!detector.checkInput() || !detector.process())
		{
			System.out.println( "Problem encountered during detection: " + detector.getErrorMessage() );
			return;
		}

		System.out.println( "Detection completed in " + detector.getProcessingTime() + " ms." );
		System.out.println( graph );


		final WindowManager wm = new WindowManager( null, bdvFile, spimData, model, getInputTriggerConfig() );
		wm.getCreateTrackSchemeAction().actionPerformed( null );
		wm.getCreateBdvAction().actionPerformed( null );
	}


	static final InputTriggerConfig getInputTriggerConfig()
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
