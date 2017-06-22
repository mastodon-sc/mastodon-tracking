package org.mastodon.tracking.kalman;

import java.util.Collection;
import java.util.HashSet;

import org.mastodon.collection.ObjectRefMap;
import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.collection.RefMaps;
import org.mastodon.collection.RefRefMap;
import org.mastodon.collection.ref.RefArrayList;
import org.mastodon.collection.ref.RefObjectHashMap;
import org.mastodon.graph.Edge;
import org.mastodon.graph.Graph;
import org.mastodon.graph.Vertex;
import org.mastodon.revised.mamut.ProgressListener;
import org.mastodon.revised.model.feature.FeatureProjection;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.tracking.EdgeCreator;
import org.mastodon.tracking.ProgressListeners;
import org.mastodon.tracking.lap.costfunction.CostFunction;
import org.mastodon.tracking.lap.costfunction.SquareDistCostFunction;
import org.mastodon.tracking.lap.costmatrix.JaqamanLinkingCostMatrixCreator;
import org.mastodon.tracking.lap.linker.JaqamanLinker;

import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;

/**
 * @author Jean-Yves Tinevez
 *
 * @param <V> the type of vertices in the graph.
 * @param <E> the type of edges in the graph.
 */
public class KalmanTracker< V extends Vertex< E > & RealLocalizable & Comparable< V >, E extends Edge< V > > implements Algorithm, Benchmark
{

	private static final double ALTERNATIVE_COST_FACTOR = 1.05d;

	private static final double PERCENTILE = 1d;

	private static final String BASE_ERROR_MSG = "[KalmanTracker] ";

	private String errorMessage;

	private final double maxSearchRadius;

	private final int maxFrameGap;

	private final double initialSearchRadius;

	private long processingTime;

	private final SpatioTemporalIndex< V > spots;

	private final int minTimepoint;

	private final int maxTimepoint;

	private final Graph< V, E > graph;

	private final FeatureProjection< V > radiuses;

	private final EdgeCreator< V, E > edgeCreator;

	private ProgressListener logger = ProgressListeners.voidLogger();

	/*
	 * CONSTRUCTOR
	 */

	public KalmanTracker(
			final SpatioTemporalIndex< V > spots,
			final Graph< V, E > graph,
			final EdgeCreator< V, E > edgeCreator,
			final FeatureProjection< V > radiuses,
			final int minTimepoint,
			final int maxTimepoint,
			final double maxSearchRadius,
			final int maxFrameGap,
			final double initialSearchRadius )
	{
		this.spots = spots;
		this.graph = graph;
		this.edgeCreator = edgeCreator;
		this.radiuses = radiuses;
		this.minTimepoint = minTimepoint;
		this.maxTimepoint = maxTimepoint;
		this.maxSearchRadius = maxSearchRadius;
		this.maxFrameGap = maxFrameGap;
		this.initialSearchRadius = initialSearchRadius;
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public boolean checkInput()
	{
		// Check that the objects list itself isn't null
		if ( null == graph )
		{
			errorMessage = BASE_ERROR_MSG + "The input graph is null.";
			return false;
		}

		if ( maxTimepoint <= minTimepoint )
		{
			errorMessage = BASE_ERROR_MSG + "Max timepoint <= min timepoint.";
			return false;
		}

		// Check that the objects list itself isn't null
		if ( null == spots )
		{
			errorMessage = BASE_ERROR_MSG + "The spot collection is null.";
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
			errorMessage = BASE_ERROR_MSG + "The spot collection is empty.";
			return false;
		}

		return true;
	}


	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		/*
		 * Constants.
		 */

		// Max KF search cost.
		final double maxCost = maxSearchRadius * maxSearchRadius;
		// Cost function to nucleate KFs.
		final CostFunction< V, V > nucleatingCostFunction = new SquareDistCostFunction<>();
		// Max cost to nucleate KFs.
		final double maxInitialCost = initialSearchRadius * initialSearchRadius;

		/*
		 * Initialize. Find first links just based on square distance. We do
		 * this via the orphan spots lists.
		 */

		/* Spots in the PREVIOUS frame that were not part of a link. */
		RefList< V > previousOrphanSpots = null;
		int firstFrame = -1;
		for ( int tp = minTimepoint; tp < maxTimepoint; tp++ )
		{
			if ( !spots.getSpatialIndex( tp ).isEmpty() )
			{
				previousOrphanSpots = generateSpotList( tp );
				firstFrame = tp;
				break;
			}
		}
		if ( null == previousOrphanSpots )
			return true; // Nothing to do.

		/*
		 * Spots in the current frame that are not part of a new link (no
		 * parent).
		 */
		RefList< V > orphanSpots = null;
		int secondFrame = firstFrame + 1;
		for ( int tp = secondFrame; tp < maxTimepoint; tp++ )
		{
			if ( !spots.getSpatialIndex( tp ).isEmpty() )
			{
				orphanSpots = generateSpotList( secondFrame );
				secondFrame = tp;
				break;
			}
		}
		if ( null == orphanSpots )
			return true; // Nothing to do.

		/*
		 * Prediction pool.
		 */

		final PredictionPool predictionPool = new PredictionPool( orphanSpots.size() );

		/*
		 * Estimate Kalman filter variances.
		 *
		 * The search radius is used to derive an estimate of the noise that
		 * affects position and velocity. The two are linked: if we need a large
		 * search radius, then the fluoctuations over predicted states are
		 * large.
		 */
		final double positionProcessStd = maxSearchRadius / 3d;
		final double velocityProcessStd = maxSearchRadius / 3d;
		/*
		 * We assume the detector did a good job and that positions measured are
		 * accurate up to a fraction of the spot radius
		 */

		double meanSpotRadius = 0d;
		int count = 0;
		for ( final V spot : orphanSpots )
		{
			if ( radiuses.isSet( spot ) )
			{
				count++;
				meanSpotRadius += radiuses.value( spot );
			}
		}
		meanSpotRadius /= count;
		final double positionMeasurementStd = meanSpotRadius / 10d;

		// The master map that contains the currently active KFs.

		final ObjectRefMap< CVMKalmanFilter, V > kalmanFiltersMap =
				RefMaps.createObjectRefMap( graph.vertices(), orphanSpots.size() );

		/*
		 * Then loop over time, starting from second frame.
		 */

		final V vref1 = graph.vertexRef();
		final V vref2 = graph.vertexRef();

		int p = 0;
		for ( int tp = secondFrame; tp <= maxTimepoint - 1; tp++ )
		{
			p++;

			/*
			 * Predict for all Kalman filters, and use it to generate linking
			 * candidates.
			 */
			final RefObjectHashMap< Prediction, CVMKalmanFilter > predictionMap =
					new RefObjectHashMap<>( predictionPool, kalmanFiltersMap.size() );
			final Prediction pref = predictionPool.createRef();
			for ( final CVMKalmanFilter kf : kalmanFiltersMap.keySet() )
			{
				final double[] X = kf.predict();
				final Prediction point = predictionPool.create( pref ).init( X );
				predictionMap.put( point, kf );
			}
			predictionPool.releaseRef( pref );

			final RefArrayList< Prediction > predictions = new RefArrayList<>( predictionPool, predictionMap.size() );
			predictions.addAll( predictionMap.keySet() );

			/*
			 * The KF for which we could not find a measurement in the target
			 * frame. Is updated later.
			 */
			final Collection< CVMKalmanFilter > childlessKFs = new HashSet< CVMKalmanFilter >( kalmanFiltersMap.keySet() );

			/*
			 * Find the global (in space) optimum for associating a prediction
			 * to a measurement.
			 */

			// Use the spot in the next frame has measurements.
			final RefList< V > measurements = generateSpotList( tp );
			if ( !predictions.isEmpty() && !measurements.isEmpty() )
			{
				// Only link measurements to predictions if we have predictions.
				final JaqamanLinkingCostMatrixCreator< Prediction, V > crm = new JaqamanLinkingCostMatrixCreator<>(
						predictions, measurements, CF, maxCost, ALTERNATIVE_COST_FACTOR, PERCENTILE );
				final JaqamanLinker< Prediction, V > linker = new JaqamanLinker<>( crm, predictions, measurements );
				if ( !linker.checkInput() || !linker.process() )
				{
					errorMessage = BASE_ERROR_MSG + "Error linking candidates in frame " + tp + ": " + linker.getErrorMessage();
					return false;
				}
				final RefRefMap< Prediction, V > agnts = linker.getResult();

				// Deal with found links.
				orphanSpots = RefCollections.createRefList( orphanSpots, measurements.size() );
				orphanSpots.addAll( measurements );

				for ( final Prediction cm : agnts.keySet() )
				{
					final CVMKalmanFilter kf = predictionMap.get( cm );

					// Create links for found match.
					final V source = kalmanFiltersMap.get( kf, vref1 );
					final V target = agnts.get( cm, vref2 );
					edgeCreator.createEdge( source, target );

					// Update Kalman filter
					kf.update( toMeasurement( target ) );

					// Update Kalman track spot
					kalmanFiltersMap.put( kf, target, vref1 );

					// Remove from orphan set
					orphanSpots.remove( target );

					// Remove from childless KF set
					childlessKFs.remove( kf );
				}
			}

			/*
			 * Deal with orphans from the previous frame. (We deal with orphans
			 * from previous frame only now because we want to link in priority
			 * target spots to predictions. Nucleating new KF from nearest
			 * neighbor only comes second.
			 */
			if ( !previousOrphanSpots.isEmpty() && !orphanSpots.isEmpty() )
			{

				/*
				 * We now deal with orphans of the previous frame. We try to
				 * find them a target from the list of spots that are not
				 * already part of a link created via KF. That is: the orphan
				 * spots of this frame.
				 */

				final JaqamanLinkingCostMatrixCreator< V, V > ic =
						new JaqamanLinkingCostMatrixCreator<>( previousOrphanSpots, orphanSpots, nucleatingCostFunction, maxInitialCost, ALTERNATIVE_COST_FACTOR, PERCENTILE );
				final JaqamanLinker< V, V > newLinker = new JaqamanLinker<>( ic, previousOrphanSpots, orphanSpots );
				if ( !newLinker.checkInput() || !newLinker.process() )
				{
					errorMessage = BASE_ERROR_MSG + "Error linking spots from frame " + ( tp - 1 ) + " to frame " + tp + ": " + newLinker.getErrorMessage();
					return false;
				}
				final RefRefMap< V, V > newAssignments = newLinker.getResult();

				// Build links and new KFs from these links.
				for ( final V source : newAssignments.keySet() )
				{
					final V target = newAssignments.get( source, vref1 );

					// Remove from orphan collection.
					orphanSpots.remove( target );

					// Derive initial state and create Kalman filter.
					final double[] XP = estimateInitialState( source, target );
					final CVMKalmanFilter kt = new CVMKalmanFilter( XP, Double.MIN_NORMAL, positionProcessStd, velocityProcessStd, positionMeasurementStd );
					// We trust the initial state a lot.

					// Store filter and source
					kalmanFiltersMap.put( kt, target, vref2 );

					// Add edge to the graph.
					edgeCreator.createEdge( source, target );
				}
			}
			previousOrphanSpots = orphanSpots;

			// Deal with childless KFs.
			for ( final CVMKalmanFilter kf : childlessKFs )
			{
				// Echo we missed a measurement
				kf.update( null );

				// We can bridge a limited number of gaps. If too much, we die.
				// If not, we will use predicted state next time.
				if ( kf.getNOcclusion() > maxFrameGap )
					kalmanFiltersMap.remove( kf );
			}

			logger.showProgress( p, maxTimepoint - minTimepoint - 1 );
		}

		final long end = System.currentTimeMillis();
		processingTime = end - start;

		graph.releaseRef( vref1 );
		graph.releaseRef( vref2 );

		return true;
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

	private final double[] toMeasurement( final V spot )
	{
		final double[] d = new double[] {
				spot.getDoublePosition( 0 ), spot.getDoublePosition( 1 ), spot.getDoublePosition( 2 )
		};
		return d;
	}

	private final double[] estimateInitialState( final V first, final V second )
	{
		final double dx = second.getDoublePosition( 0 ) - first.getDoublePosition( 0 );
		final double dy = second.getDoublePosition( 1 ) - first.getDoublePosition( 1 );
		final double dz = second.getDoublePosition( 2 ) - first.getDoublePosition( 2 );
		final double[] xp = new double[] { second.getDoublePosition( 0 ), second.getDoublePosition( 1 ), second.getDoublePosition( 2 ),
				dx, dy, dz };
		return xp;
	}

	/**
	 * Creates a new list containing all the spots at the specified timepoint.
	 *
	 * @param timepoint
	 *            the timepoint to grab.
	 * @return a new list.
	 */
	private final RefList< V > generateSpotList( final int timepoint )
	{
		final SpatialIndex< V > si = spots.getSpatialIndex( timepoint );
		final RefList< V > list = RefCollections.createRefList( graph.vertices(), si.size() );
		for ( final V v : si )
			list.add( v );
		return list;
	}

	/**
	 * Cost function that returns the square distance between a KF state and a
	 * spots.
	 */
	private final CostFunction< Prediction, V > CF = new CostFunction< Prediction, V >()
	{

		@Override
		public double linkingCost( final Prediction state, final V spot )
		{
			final double dx = state.getDoublePosition( 0 ) - spot.getDoublePosition( 0 );
			final double dy = state.getDoublePosition( 1 ) - spot.getDoublePosition( 1 );
			final double dz = state.getDoublePosition( 2 ) - spot.getDoublePosition( 2 );
			return dx * dx + dy * dy + dz * dz + Double.MIN_NORMAL;
			// So that it's never 0
		}
	};

	public void setProgressListener( final ProgressListener logger )
	{
		this.logger = logger;
	}
}
