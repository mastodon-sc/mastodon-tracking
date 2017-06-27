package org.mastodon.detection;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mastodon.revised.mamut.MainWindow;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.tracking.EdgeCreator;
import org.mastodon.tracking.ParticleLinkerOp;
import org.mastodon.tracking.kalman.KalmanTracker;
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

		final Class< DoGDetector > cl = DoGDetector.class;
//		final Class< LoGDetector > cl = LoGDetector.class;
		final SpotDetectorOp detector = ( SpotDetectorOp ) Hybrids.unaryCF( ops, cl, graph, spimData,
				setup, radius, threshold, minTimepoint, maxTimepoint );
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

		final EdgeCreator< Spot, Link > edgeCreator = edgeCreator( model.getGraph() );
		final Comparator< Spot > spotComparator = new Comparator< Spot >()
		{

			@Override
			public int compare( final Spot o1, final Spot o2 )
			{
				return o1.getInternalPoolIndex() - o2.getInternalPoolIndex();
			}
		};

//		final Map< String, Object > settings = LAPUtils.getDefaultLAPSettingsMap();
//		@SuppressWarnings( "rawtypes" )
//		final Class< SparseLAPLinker > plcl = SparseLAPLinker.class;

		final Map< String, Object > settings = KalmanTracker.getDefaultSettingsMap();
		@SuppressWarnings( "rawtypes" )
		final Class< KalmanTracker > plcl = KalmanTracker.class;

		@SuppressWarnings( { "unchecked", "rawtypes" } )
		final ParticleLinkerOp< Spot, Link > tracker =
				( ParticleLinkerOp ) Inplaces.binary1( ops, plcl, model.getGraph(), model.getSpatioTemporalIndex(),
						settings, model.getGraphFeatureModel(), minTimepoint, maxTimepoint, spotComparator, edgeCreator );

		System.out.println( "\n\nTracking with " + tracker );
		tracker.mutate1( model.getGraph(), model.getSpatioTemporalIndex() );
		if ( !tracker.wasSuccessful() )
		{
			System.out.println( "Tracking failed: " + tracker.getErrorMessage() );
			return;
		}
		if ( tracker instanceof Benchmark )
			System.out.println( "Tracking completed in " + ( ( Benchmark ) tracker ).getProcessingTime() + " ms." );
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

	private static EdgeCreator< Spot, Link > edgeCreator( final ModelGraph graph )
	{
		final Link ref = graph.edgeRef();
		return new EdgeCreator< Spot, Link >()
		{

			@Override
			public Link createEdge( final Spot source, final Spot target )
			{
				return graph.addEdge( source, target, ref ).init();
			}
		};
	};

}
