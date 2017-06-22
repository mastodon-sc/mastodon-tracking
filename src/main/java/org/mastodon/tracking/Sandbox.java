package org.mastodon.tracking;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.revised.model.feature.FeatureProjection;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.tracking.kalman.KalmanTracker;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimDataException;

public class Sandbox
{

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

	public static void main( final String[] args ) throws SpimDataException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
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
		}

		final SpimDataMinimal spimData = sd;

		final int maxTimepoint = spimData.getSequenceDescription().getTimePoints().size();
		final int minTimepoint = 0;

		/*
		 * Load Model
		 */
		final String modelFile = "samples/model_revised.raw";
		final Model model = new Model();
		try
		{
			model.loadRaw( new File( modelFile ) );
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}

		/*
		 * Clear the edges, just keep the vertices.
		 */

		for ( final Link link : model.getGraph().edges() )
			model.getGraph().remove( link );

		final SpatioTemporalIndex< Spot > spots = model.getSpatioTemporalIndex();
		for ( int tp = 0; tp < maxTimepoint; tp++ )
		{
			final SpatialIndex< Spot > spatialIndex = spots.getSpatialIndex( tp );
			System.out.println( String.format( "Timepoint %2d, N spots = %d.", tp, spatialIndex.size() ) );
		}

		/*
		 * Display stuff.
		 */

		SwingUtilities.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				final WindowManager wm = new WindowManager( null, bdvFile, spimData, model, getInputTriggerConfig() );
				wm.getCreateTrackSchemeAction().actionPerformed( null );
				wm.getCreateBdvAction().actionPerformed( null );
			}
		} );

		/*
		 * Let's track them.
		 */

		final EdgeCreator< Spot, Link > edgeCreator = edgeCreator( model.getGraph() );

		//		final Map< String, Object > settings = LAPUtils.getDefaultLAPSettingsMap();
//		final SparseLAPTracker< Spot, Link > tracker = new SparseLAPTracker<>(
//				model.getSpatioTemporalIndex(),
//				model.getGraphFeatureModel(),
//				model.getGraph(),
//				edgeCreator,
//				minTimepoint, maxTimepoint, settings );
//		tracker.setProgressListener( ProgressListeners.defaultLogger() );
//		tracker.setNumThreads( 1 );

		final FeatureProjection< Spot > radiuses = new FeatureProjection< Spot >()
		{

			@Override
			public double value( final Spot obj )
			{
				return Math.sqrt( obj.getBoundingSphereRadiusSquared() );
			}

			@Override
			public boolean isSet( final Spot obj )
			{
				return true;
			}
		};
		final KalmanTracker< Spot, Link > tracker = new KalmanTracker<>(
				model.getSpatioTemporalIndex(),
				model.getGraph(),
				edgeCreator,
				radiuses,
				minTimepoint,
				maxTimepoint,
				TrackerKeys.DEFAULT_LINKING_MAX_DISTANCE,
				TrackerKeys.DEFAULT_GAP_CLOSING_MAX_FRAME_GAP,
				TrackerKeys.DEFAULT_LINKING_MAX_DISTANCE );
		tracker.setProgressListener( ProgressListeners.defaultLogger() );

		System.out.println( "\n\nTracking with " + tracker );
		if ( !tracker.checkInput() || !tracker.process() )
		{
			System.out.println( "Tracking failed: " + tracker.getErrorMessage() );
			return;
		}
		System.out.println( "Tracking completed in " + tracker.getProcessingTime() + " ms." );
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
