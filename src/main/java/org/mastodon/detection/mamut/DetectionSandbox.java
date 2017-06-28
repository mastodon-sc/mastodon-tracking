package org.mastodon.detection.mamut;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mastodon.linking.LinkingUtils;
import org.mastodon.linking.mamut.SparseLAPLinkerMamut;
import org.mastodon.linking.mamut.SpotLinkerOp;
import org.mastodon.revised.mamut.MainWindow;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimDataException;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imagej.ops.special.hybrid.Hybrids;
import net.imagej.ops.special.inplace.Inplaces;
import net.imglib2.algorithm.Benchmark;

public class DetectionSandbox
{

	/**
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws UnsupportedLookAndFeelException
	 */
	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		Locale.setDefault( Locale.US );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		final ImageJ ij = new ImageJ();
		ij.launch( args );
		final OpService ops = ij.op();

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

		final Model model = new Model();
		final ModelGraph graph = model.getGraph();

		final double radius = 6.;
		final double threshold = 1000.;
		final int setup = 0;

		final Class< DoGDetectorMamut > cl = DoGDetectorMamut.class;
//		final Class< LoGDetectorMamut > cl = LoGDetectorMamut.class;
		final SpotDetectorOp detector = ( SpotDetectorOp ) Hybrids.unaryCF( ops, cl, graph, spimData,
				setup, radius, threshold, minTimepoint, maxTimepoint );

		System.out.println( "\n\nSpot detection with " + detector );
		detector.compute( spimData, graph );

		model.getGraphFeatureModel().declareFeature( detector.getQualityFeature() );
		if ( detector instanceof Benchmark )
		{
			final Benchmark bm = ( Benchmark ) detector;
			System.out.println( "Detection completed in " + bm.getProcessingTime() + " ms." );
		}
		System.out.println( "Found " + graph.vertices().size() + " spots." );

		/*
		 * Let's track them.
		 */

		final Map< String, Object > settings = LinkingUtils.getDefaultLAPSettingsMap();
		final Class< SparseLAPLinkerMamut > plcl = SparseLAPLinkerMamut.class;

//		final Map< String, Object > settings = KalmanLinkerMamut.getDefaultSettingsMap();
//		final Class< KalmanLinkerMamut > plcl = KalmanLinkerMamut.class;

		final SpotLinkerOp linker =
				( SpotLinkerOp ) Inplaces.binary1( ops, plcl, model.getGraph(), model.getSpatioTemporalIndex(),
						settings, model.getGraphFeatureModel(), minTimepoint, maxTimepoint );

		System.out.println( "\n\nParticle-linking with " + linker );
		linker.mutate1( model.getGraph(), model.getSpatioTemporalIndex() );
		if ( !linker.wasSuccessful() )
		{
			System.out.println( "Tracking failed: " + linker.getErrorMessage() );
			return;
		}
		model.getGraphFeatureModel().declareFeature( linker.getLinkCostFeature() );
		if ( linker instanceof Benchmark )
			System.out.println( "Tracking completed in " + ( ( Benchmark ) linker ).getProcessingTime() + " ms." );
		else
			System.out.println( "Tracking completed." );

		new MainWindow( model, spimData, bdvFile, getInputTriggerConfig() ).setVisible( true );
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
