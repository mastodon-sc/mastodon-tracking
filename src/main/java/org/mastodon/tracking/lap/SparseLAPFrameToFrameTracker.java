package org.mastodon.tracking.lap;


import static org.mastodon.tracking.TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static org.mastodon.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static org.mastodon.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static org.mastodon.tracking.lap.LAPUtils.checkFeatureMap;
import static org.mastodon.tracking.lap.LAPUtils.checkMapKeys;
import static org.mastodon.tracking.lap.LAPUtils.checkParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.mastodon.collection.RefRefMap;
import org.mastodon.graph.Edge;
import org.mastodon.graph.Graph;
import org.mastodon.graph.Vertex;
import org.mastodon.revised.mamut.ProgressListener;
import org.mastodon.revised.model.feature.FeatureModel;
import org.mastodon.spatial.HasTimepoint;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.tracking.EdgeCreator;
import org.mastodon.tracking.ProgressListeners;
import org.mastodon.tracking.lap.costfunction.CostFunction;
import org.mastodon.tracking.lap.costfunction.FeaturePenaltyCostFunction;
import org.mastodon.tracking.lap.costfunction.SquareDistCostFunction;
import org.mastodon.tracking.lap.costmatrix.JaqamanLinkingCostMatrixCreator;
import org.mastodon.tracking.lap.linker.JaqamanLinker;

import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;
import net.imglib2.multithreading.SimpleMultiThreading;

@SuppressWarnings( "deprecation" )
public class SparseLAPFrameToFrameTracker< V extends Vertex< E > & HasTimepoint & RealLocalizable & Comparable< V >, E extends Edge< V > > extends MultiThreadedBenchmarkAlgorithm
{
	private final static String BASE_ERROR_MESSAGE = "[SparseLAPFrameToFrameTracker] ";

	private final SpatioTemporalIndex< V > spots;

	private final Map< String, Object > settings;

	private final Graph< V, E > graph;

	private final int minTimepoint;

	private final int maxTimepoint;

	private final FeatureModel< V, E > featureModel;

	private final EdgeCreator< V, E > edgeCreator;

	private ProgressListener logger = ProgressListeners.voidLogger();

	/*
	 * CONSTRUCTOR
	 */

	public SparseLAPFrameToFrameTracker(
			final SpatioTemporalIndex< V > spots,
			final FeatureModel< V, E > featureModel,
			final Graph< V, E > graph,
			final EdgeCreator< V, E > edgeCreator,
			final int minTimepoint,
			final int maxTimepoint,
			final Map< String, Object > settings )
	{
		this.spots = spots;
		this.featureModel = featureModel;
		this.graph = graph;
		this.edgeCreator = edgeCreator;
		this.minTimepoint = minTimepoint;
		this.maxTimepoint = maxTimepoint;
		this.settings = settings;
	}

	/*
	 * METHODS
	 */

	@Override
	public boolean checkInput()
	{
		return true;
	}

	@Override
	public boolean process()
	{
		/*
		 * Check input now.
		 */

		if ( maxTimepoint <= minTimepoint )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Max timepoint <= min timepoint.";
			return false;
		}

		// Check that the objects list itself isn't null
		if ( null == spots )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The spot collection is null.";
			return false;
		}

		// Check that at least one inner collection contains an object.
		boolean empty = true;
		for ( int tp = minTimepoint; tp <= maxTimepoint; tp++ )
		{
			if ( !spots.getSpatialIndex( tp ).isEmpty() )
			{
				empty = false;
				break;
			}
		}
		if ( empty )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The spot collection is empty.";
			return false;
		}

		// Check parameters
		final StringBuilder errorHolder = new StringBuilder();
		if ( !checkSettingsValidity( settings, errorHolder ) )
		{
			errorMessage = BASE_ERROR_MESSAGE + errorHolder.toString();
			return false;
		}

		/*
		 * Process.
		 */

		final long start = System.currentTimeMillis();

		// Prepare frame pairs in order. For now they are separated by 1.
		final ArrayList< int[] > framePairs = new ArrayList< int[] >( maxTimepoint - minTimepoint );
		for ( int tp = minTimepoint; tp <= maxTimepoint - 1; tp++ )
		{ // ascending order
			framePairs.add( new int[] { tp, tp + 1 } );
		}

		// Prepare cost function
		@SuppressWarnings( "unchecked" )
		final Map< String, Double > featurePenalties = ( Map< String, Double > ) settings.get( KEY_LINKING_FEATURE_PENALTIES );
		final CostFunction< V, V > costFunction;
		if ( null == featurePenalties || featurePenalties.isEmpty() )
		{
			costFunction = new SquareDistCostFunction<>();
		}
		else
		{
			costFunction = new FeaturePenaltyCostFunction<>( featurePenalties, featureModel );
		}
		final Double maxDist = ( Double ) settings.get( KEY_LINKING_MAX_DISTANCE );
		final double costThreshold = maxDist * maxDist;
		final double alternativeCostFactor = ( Double ) settings.get( KEY_ALTERNATIVE_LINKING_COST_FACTOR );

		// Prepare threads
		final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
		final AtomicInteger progress = new AtomicInteger();
		// Prepare the thread array
		final AtomicInteger ai = new AtomicInteger( 0 );
		final AtomicBoolean ok = new AtomicBoolean( true );
		for ( int ithread = 0; ithread < threads.length; ithread++ )
		{
			threads[ ithread ] = new Thread( BASE_ERROR_MESSAGE + " thread " + ( 1 + ithread ) + "/" + threads.length )
			{
				@Override
				public void run()
				{
					for ( int i = ai.getAndIncrement(); i < framePairs.size(); i = ai.getAndIncrement() )
					{
						if ( !ok.get() )
						{
							break;
						}

						// Get frame pairs
						final int frame0 = framePairs.get( i )[ 0 ];
						final int frame1 = framePairs.get( i )[ 1 ];

						final SpatialIndex< V > sources = spots.getSpatialIndex( frame0 );
						final SpatialIndex< V > targets = spots.getSpatialIndex( frame1 );

						if ( sources.isEmpty() || targets.isEmpty() )
							continue;

						/*
						 * Run the linker.
						 */

						final JaqamanLinkingCostMatrixCreator< V, V > creator = new JaqamanLinkingCostMatrixCreator<>(
								sources, targets, costFunction, costThreshold, alternativeCostFactor, 1d, graph.vertices(), graph.vertices() );
						final JaqamanLinker< V, V > linker = new JaqamanLinker< V, V >( creator, graph.vertices(), graph.vertices() );
						if ( !linker.checkInput() || !linker.process() )
						{
							errorMessage = "Linking frame " + frame0 + " to " + frame1 + ": " + linker.getErrorMessage();
							ok.set( false );
							return;
						}

						/*
						 * Update graph.
						 */

						synchronized ( graph )
						{
							final RefRefMap< V, V > assignment = linker.getResult();
							final V vref = graph.vertexRef();
							for ( final V source : assignment.keySet() )
							{
								final V target = assignment.get( source, vref );
								edgeCreator.createEdge( source, target );
							}
							graph.releaseRef( vref );
						}

						logger.showProgress( progress.incrementAndGet(), framePairs.size() );

					}
				}
			};
		}

		logger.showStatus( "Frame to frame linking..." );
		SimpleMultiThreading.startAndJoin( threads );
		logger.showProgress( 1, 1 );
		logger.showStatus( "" );

		final long end = System.currentTimeMillis();
		processingTime = end - start;

		return ok.get();
	}

	private static final boolean checkSettingsValidity( final Map< String, Object > settings, final StringBuilder str )
	{
		if ( null == settings )
		{
			str.append( "Settings map is null.\n" );
			return false;
		}

		boolean ok = true;
		// Linking
		ok = ok & checkParameter( settings, KEY_LINKING_MAX_DISTANCE, Double.class, str );
		ok = ok & checkFeatureMap( settings, KEY_LINKING_FEATURE_PENALTIES, str );
		// Others
		ok = ok & checkParameter( settings, KEY_ALTERNATIVE_LINKING_COST_FACTOR, Double.class, str );

		// Check keys
		final List< String > mandatoryKeys = new ArrayList< String >();
		mandatoryKeys.add( KEY_LINKING_MAX_DISTANCE );
		mandatoryKeys.add( KEY_ALTERNATIVE_LINKING_COST_FACTOR );
		final List< String > optionalKeys = new ArrayList< String >();
		optionalKeys.add( KEY_LINKING_FEATURE_PENALTIES );
		ok = ok & checkMapKeys( settings, mandatoryKeys, optionalKeys, str );

		return ok;
	}

	public void setProgressListener( final ProgressListener logger )
	{
		this.logger = logger;
	}
}
