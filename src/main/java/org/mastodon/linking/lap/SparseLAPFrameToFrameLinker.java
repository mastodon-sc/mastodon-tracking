package org.mastodon.linking.lap;

import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.linking.LinkerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static org.mastodon.linking.LinkerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static org.mastodon.linking.LinkerKeys.KEY_LINKING_MAX_DISTANCE;
import static org.mastodon.linking.LinkingUtils.checkFeatureMap;
import static org.mastodon.linking.LinkingUtils.checkMapKeys;
import static org.mastodon.linking.LinkingUtils.checkParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.mastodon.collection.RefDoubleMap;
import org.mastodon.collection.RefRefMap;
import org.mastodon.graph.Edge;
import org.mastodon.graph.Graph;
import org.mastodon.graph.Vertex;
import org.mastodon.linking.AbstractParticleLinkerOp;
import org.mastodon.linking.LinkingUtils;
import org.mastodon.linking.lap.costfunction.CostFunction;
import org.mastodon.linking.lap.costfunction.FeaturePenaltyCostFunction;
import org.mastodon.linking.lap.costfunction.SquareDistCostFunction;
import org.mastodon.linking.lap.costmatrix.JaqamanLinkingCostMatrixCreator;
import org.mastodon.linking.lap.linker.JaqamanLinker;
import org.mastodon.linking.lap.linker.SparseCostMatrix;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.spatial.HasTimepoint;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import net.imagej.ops.special.function.Functions;
import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.Benchmark;

@Plugin( type = SparseLAPFrameToFrameLinker.class )
public class SparseLAPFrameToFrameLinker< V extends Vertex< E > & HasTimepoint & RealLocalizable, E extends Edge< V > >
		extends AbstractParticleLinkerOp< V, E >
		implements Benchmark
{
	private final static String BASE_ERROR_MESSAGE = "[SparseLAPFrameToFrameLinker] ";

	@Parameter
	private ThreadService threadService;

	private long processingTime;

	/*
	 * METHODS
	 */

	@Override
	public void mutate1( final Graph< V, E > graph, final SpatioTemporalIndex< V > spots )
	{
		ok = false;
		final DoublePropertyMap< E > linkcost = new DoublePropertyMap<>( graph.edges(), Double.NaN );

		/*
		 * Check input now.
		 */
		// Check parameters
		final StringBuilder errorHolder = new StringBuilder();
		if ( !checkSettingsValidity( settings, errorHolder ) )
		{
			errorMessage = BASE_ERROR_MESSAGE + errorHolder.toString();
			return;
		}

		final int minTimepoint = ( int ) settings.get( KEY_MIN_TIMEPOINT );
		final int maxTimepoint = ( int ) settings.get( KEY_MAX_TIMEPOINT );

		if ( maxTimepoint <= minTimepoint )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Max timepoint <= min timepoint.";
			return;
		}

		// Check that the objects list itself isn't null
		if ( null == spots )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The spot collection is null.";
			return;
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
			return;
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
			costFunction = new SquareDistCostFunction<>();
		else
			costFunction = new FeaturePenaltyCostFunction<>( featurePenalties, featureModel );

		final Double maxDist = ( Double ) settings.get( KEY_LINKING_MAX_DISTANCE );
		final double costThreshold = maxDist * maxDist;
		final double alternativeCostFactor = ( Double ) settings.get( KEY_ALTERNATIVE_LINKING_COST_FACTOR );

		// Prepare threads
		final AtomicInteger progress = new AtomicInteger( 0 );
		final AtomicBoolean aok = new AtomicBoolean( true );
		statusService.showStatus( "Frame to frame linking..." );
		final ArrayList< Future< Void > > futures = new ArrayList<>( framePairs.size() );
		final ExecutorService service = threadService.getExecutorService();
		for ( int fp = 0; fp < framePairs.size(); fp++ )
		{
			final int i = fp;
			futures.add( service.submit( new Callable< Void >()
			{
				@Override
				public Void call()
				{
					if ( !aok.get() )
						return null;

					// Get frame pairs
					final int frame0 = framePairs.get( i )[ 0 ];
					final int frame1 = framePairs.get( i )[ 1 ];

					final SpatialIndex< V > sources = spots.getSpatialIndex( frame0 );
					final SpatialIndex< V > targets = spots.getSpatialIndex( frame1 );

					if ( sources.isEmpty() || targets.isEmpty() )
						return null;

					/*
					 * Run the linker.
					 */

					@SuppressWarnings( "unchecked" )
					final JaqamanLinkingCostMatrixCreator< V, V > creator = ( JaqamanLinkingCostMatrixCreator< V, V > ) Functions.nullary( ops(), JaqamanLinkingCostMatrixCreator.class, SparseCostMatrix.class,
							sources, targets, costFunction, costThreshold, alternativeCostFactor, 1d,
							graph.vertices(), graph.vertices(),
							spotComparator, spotComparator );
					final JaqamanLinker< V, V > linker = new JaqamanLinker< V, V >( creator, graph.vertices(), graph.vertices() );
					if ( !linker.checkInput() || !linker.process() )
					{
						errorMessage = "Linking frame " + frame0 + " to " + frame1 + ": " + linker.getErrorMessage();
						aok.set( false );
						return null;
					}

					/*
					 * Update graph. We have to do it in a single thread at a
					 * time.
					 */

					synchronized ( graph )
					{
						final RefRefMap< V, V > assignment = linker.getResult();
						final RefDoubleMap< V > assignmentCosts = linker.getAssignmentCosts();
						final V vref = graph.vertexRef();
						final E eref = graph.edgeRef();
						for ( final V source : assignment.keySet() )
						{
							final V target = assignment.get( source, vref );
							final double cost = assignmentCosts.get( source );
							final E edge = edgeCreator.createEdge( graph, eref, source, target, cost );
							linkcost.set( edge, cost );
						}
						graph.releaseRef( vref );
						graph.releaseRef( eref );
					}

					statusService.showProgress( progress.incrementAndGet(), framePairs.size() );
					return null;
				}
			} ) );

		}

		for ( final Future< Void > f : futures )
		{
			try
			{
				f.get();
			}
			catch ( final InterruptedException e )
			{
				e.printStackTrace();
			}
			catch ( final ExecutionException e )
			{
				e.printStackTrace();
			}
		}
		statusService.clearStatus();

		this.linkCostFeature = LinkingUtils.getLinkCostFeature( linkcost );
		final long end = System.currentTimeMillis();
		processingTime = end - start;

		this.ok = aok.get();
	}

	private static final boolean checkSettingsValidity( final Map< String, Object > settings, final StringBuilder str )
	{
		if ( null == settings )
		{
			str.append( "Settings map is null.\n" );
			return false;
		}

		boolean ok = true;
		ok = ok & checkParameter( settings, KEY_MIN_TIMEPOINT, Integer.class, str );
		ok = ok & checkParameter( settings, KEY_MAX_TIMEPOINT, Integer.class, str );
		// Linking
		ok = ok & checkParameter( settings, KEY_LINKING_MAX_DISTANCE, Double.class, str );
		ok = ok & checkFeatureMap( settings, KEY_LINKING_FEATURE_PENALTIES, str );
		// Others
		ok = ok & checkParameter( settings, KEY_ALTERNATIVE_LINKING_COST_FACTOR, Double.class, str );

		// Check keys
		final List< String > mandatoryKeys = new ArrayList< String >();
		mandatoryKeys.add( KEY_MIN_TIMEPOINT );
		mandatoryKeys.add( KEY_MAX_TIMEPOINT );
		mandatoryKeys.add( KEY_LINKING_MAX_DISTANCE );
		mandatoryKeys.add( KEY_ALTERNATIVE_LINKING_COST_FACTOR );
		final List< String > optionalKeys = new ArrayList< String >();
		optionalKeys.add( KEY_LINKING_FEATURE_PENALTIES );
		ok = ok & checkMapKeys( settings, mandatoryKeys, optionalKeys, str );

		return ok;
	}

	@Override
	public boolean isSuccessful()
	{
		return ok;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}
}
