/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2022 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.tracking.linking.sequential.kalman;

import static org.mastodon.tracking.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.tracking.linking.LinkerKeys.DEFAULT_GAP_CLOSING_MAX_FRAME_GAP;
import static org.mastodon.tracking.linking.LinkerKeys.DEFAULT_LINKING_MAX_DISTANCE;
import static org.mastodon.tracking.linking.LinkerKeys.DEFAULT_MAX_SEARCH_RADIUS;
import static org.mastodon.tracking.linking.LinkerKeys.DEFAULT_POSITION_SIGMA;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_KALMAN_SEARCH_RADIUS;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_LINKING_MAX_DISTANCE;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_POSITION_SIGMA;
import static org.mastodon.tracking.linking.LinkingUtils.checkParameter;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.mastodon.collection.ObjectRefMap;
import org.mastodon.collection.RefCollection;
import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefDoubleMap;
import org.mastodon.collection.RefList;
import org.mastodon.collection.RefMaps;
import org.mastodon.collection.RefRefMap;
import org.mastodon.collection.ref.RefArrayList;
import org.mastodon.collection.ref.RefObjectHashMap;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.tracking.linking.EdgeCreator;
import org.mastodon.tracking.linking.sequential.AbstractSequentialParticleLinkerOp;
import org.mastodon.tracking.linking.sequential.SequentialParticleLinkerOp;
import org.mastodon.tracking.linking.sequential.lap.costfunction.CostFunction;
import org.mastodon.tracking.linking.sequential.lap.costfunction.SquareDistCostFunction;
import org.mastodon.tracking.linking.sequential.lap.costmatrix.JaqamanLinkingCostMatrixCreator;
import org.mastodon.tracking.linking.sequential.lap.linker.JaqamanLinker;
import org.mastodon.tracking.linking.sequential.lap.linker.SparseCostMatrix;
import org.scijava.plugin.Plugin;

import net.imagej.ops.special.function.Functions;
import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.Benchmark;

/**
 * @author Jean-Yves Tinevez
 *
 * @param <V>
 *            the type of vertices in the graph.
 */
@Plugin( type = SequentialParticleLinkerOp.class )
public class KalmanLinker< V extends RealLocalizable >
		extends AbstractSequentialParticleLinkerOp< V >
		implements Benchmark
{

	private static final double ALTERNATIVE_COST_FACTOR = 1.05d;

	private static final double PERCENTILE = 1d;

	private static final String BASE_ERROR_MSG = "[KalmanTracker] ";

	private long processingTime;

	@Override
	public void mutate1( final EdgeCreator< V > edgeCreator, final SpatioTemporalIndex< V > spots )
	{
		ok = false;
		final long start = System.currentTimeMillis();

		// Check parameters
		final StringBuilder errorHolder = new StringBuilder();
		if ( !checkSettingsValidity( settings, errorHolder ) )
		{
			errorMessage = BASE_ERROR_MSG + "Incorrect settings map:\n" + errorHolder.toString();
			return;
		}

		final int minTimepoint = ( int ) settings.get( KEY_MIN_TIMEPOINT );
		final int maxTimepoint = ( int ) settings.get( KEY_MAX_TIMEPOINT );

		if ( maxTimepoint <= minTimepoint )
		{
			errorMessage = BASE_ERROR_MSG + "Max timepoint <= min timepoint.";
			return;
		}

		// Check that the objects list itself isn't null
		if ( null == spots )
		{
			errorMessage = BASE_ERROR_MSG + "The spot collection is null.";
			return;
		}

		// Check that at least one inner collection contains an object.
		boolean empty = true;
		spots.readLock().lock();
		try
		{
			for ( int tp = minTimepoint; tp <= maxTimepoint; tp++ )
			{
				if ( !spots.getSpatialIndex( tp ).isEmpty() )
				{
					empty = false;
					break;
				}
			}
		}
		finally
		{
			spots.readLock().unlock();
		}
		if ( empty )
		{
			errorMessage = BASE_ERROR_MSG + "The spot collection is empty.";
			return;
		}

		/*
		 * Constants.
		 */

		final double maxSearchRadius = ( Double ) settings.get( KEY_KALMAN_SEARCH_RADIUS );
		final int maxFrameGap = ( Integer ) settings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP );
		final double initialSearchRadius = ( Double ) settings.get( KEY_LINKING_MAX_DISTANCE );
		final double positionMeasurementStd = ( Double ) settings.get( KEY_POSITION_SIGMA );

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
				previousOrphanSpots = generateSpotList( tp, spots, refcol );
				firstFrame = tp;
				break;
			}
		}
		if ( null == previousOrphanSpots )
		{
			ok = true;
			return; // Nothing to do.
		}

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
				orphanSpots = generateSpotList( secondFrame, spots, refcol );
				secondFrame = tp;
				break;
			}
		}
		if ( null == orphanSpots )
		{
			ok = true;
			return; // Nothing to do.
		}

		/*
		 * Prediction pool.
		 */

		final PredictionPool predictionPool = new PredictionPool( orphanSpots.size() );
		final Comparator< Prediction > predictionComparator = new Comparator< Prediction >()
		{

			@Override
			public int compare( final Prediction o1, final Prediction o2 )
			{
				return o1.getInternalPoolIndex() - o2.getInternalPoolIndex();
			}
		};

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

		// The master map that contains the currently active KFs.
		final ObjectRefMap< CVMKalmanFilter, V > kalmanFiltersMap =
				RefMaps.createObjectRefMap( refcol, orphanSpots.size() );

		/*
		 * Then loop over time, starting from second frame.
		 */

		final V vref1 = refcol.createRef();
		final V vref2 = refcol.createRef();

		for ( int tp = secondFrame; tp <= maxTimepoint; tp++ )
		{
			statusService.showProgress( tp - minTimepoint + 1, maxTimepoint - minTimepoint + 1 );

			if ( isCanceled() )
				break;

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
			final Collection< CVMKalmanFilter > childlessKFs = new HashSet< >( kalmanFiltersMap.keySet() );

			/*
			 * Find the global (in space) optimum for associating a prediction
			 * to a measurement.
			 */

			// Use the spot in the next frame has measurements.
			final RefList< V > measurements = generateSpotList( tp, spots, refcol );
			orphanSpots = RefCollections.createRefList( orphanSpots, measurements.size() );
			orphanSpots.addAll( measurements );
			if ( !predictions.isEmpty() && !measurements.isEmpty() )
			{
				// Only link measurements to predictions if we have predictions.
				@SuppressWarnings( "unchecked" )
				final JaqamanLinkingCostMatrixCreator< Prediction, V > crm =
						( JaqamanLinkingCostMatrixCreator< Prediction, V > ) Functions.nullary( ops(), JaqamanLinkingCostMatrixCreator.class, SparseCostMatrix.class,
								predictions,
								measurements,
								CF,
								maxCost,
								ALTERNATIVE_COST_FACTOR,
								PERCENTILE,
								predictionPool.asRefCollection(),
								refcol,
								predictionComparator,
								spotComparator );
				final JaqamanLinker< Prediction, V > linker = new JaqamanLinker<>( crm, predictions, measurements );
				if ( !linker.checkInput() || !linker.process() )
				{
					errorMessage = BASE_ERROR_MSG + "Error linking candidates in frame " + tp + ": " + linker.getErrorMessage();
					return;
				}
				final RefRefMap< Prediction, V > agnts = linker.getResult();
				final RefDoubleMap< Prediction > assignmentCosts = linker.getAssignmentCosts();

				// Deal with found links.

				edgeCreator.preAddition();
				try
				{
					for ( final Prediction cm : agnts.keySet() )
					{
						final CVMKalmanFilter kf = predictionMap.get( cm );

						// Create links for found match.
						final V source = kalmanFiltersMap.get( kf, vref1 );
						final V target = agnts.get( cm, vref2 );
						final double cost = assignmentCosts.get( cm );
						edgeCreator.createEdge( source, target, cost );

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
				catch ( final Exception e )
				{
					e.printStackTrace();
				}
				finally
				{
					edgeCreator.postAddition();
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
				@SuppressWarnings( "unchecked" )
				final JaqamanLinkingCostMatrixCreator< V, V > ic =
						( JaqamanLinkingCostMatrixCreator< V, V > ) Functions.nullary( ops(), JaqamanLinkingCostMatrixCreator.class, SparseCostMatrix.class,
								previousOrphanSpots,
								orphanSpots,
								nucleatingCostFunction,
								maxInitialCost,
								ALTERNATIVE_COST_FACTOR,
								PERCENTILE,
								refcol,
								refcol,
								spotComparator,
								spotComparator );
				final JaqamanLinker< V, V > newLinker = new JaqamanLinker<>( ic, previousOrphanSpots, orphanSpots );
				if ( !newLinker.checkInput() || !newLinker.process() )
				{
					errorMessage = BASE_ERROR_MSG + "Error linking vertices from frame " + ( tp - 1 ) + " to frame " + tp + ": " + newLinker.getErrorMessage();
					return;
				}
				final RefRefMap< V, V > newAssignments = newLinker.getResult();
				final RefDoubleMap< V > assignmentCosts = newLinker.getAssignmentCosts();

				// Build links and new KFs from these links.
				edgeCreator.preAddition();
				try
				{
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
						final double cost = assignmentCosts.get( source );
						edgeCreator.createEdge( source, target, cost );
					}
				}
				catch ( final Exception e )
				{
					e.printStackTrace();
				}
				finally
				{
					edgeCreator.postAddition();
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
		}

		final long end = System.currentTimeMillis();
		processingTime = end - start;
		statusService.clearStatus();
		ok = true;
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
	 * @param refcol
	 * @return a new list.
	 */
	private static final < V  > RefList< V > generateSpotList( final int timepoint, final SpatioTemporalIndex< V > spots, final RefCollection< V > refcol )
	{
		final RefList< V > list;
		spots.readLock().lock();
		try
		{
			final SpatialIndex< V > si = spots.getSpatialIndex( timepoint );
			list = RefCollections.createRefList( refcol, si.size() );
			for ( final V v : si )
				list.add( v );
		}
		finally
		{
			spots.readLock().unlock();
		}
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

	@Override
	public boolean isSuccessful()
	{
		return ok;
	}

	public static boolean checkSettingsValidity( final Map< String, Object > settings, final StringBuilder str )
	{
		if ( null == settings )
		{
			str.append( "Settings map is null.\n" );
			return false;
		}

		boolean ok = true;
		ok = ok & checkParameter( settings, KEY_LINKING_MAX_DISTANCE, Double.class, str );
		ok = ok & checkParameter( settings, KEY_KALMAN_SEARCH_RADIUS, Double.class, str );
		ok = ok & checkParameter( settings, KEY_GAP_CLOSING_MAX_FRAME_GAP, Integer.class, str );
		ok = ok & checkParameter( settings, KEY_POSITION_SIGMA, Double.class, str );

		// Check min & max time-point
		final int minTimepoint = ( int ) settings.get( KEY_MIN_TIMEPOINT );
		final int maxTimepoint = ( int ) settings.get( KEY_MAX_TIMEPOINT );
		if ( maxTimepoint < minTimepoint )
		{
			ok = false;
			str.append( "Min time-point should smaller than or equal to max time-point, be was min = "
					+ minTimepoint + " and max = " + maxTimepoint + "\n" );
		}

		return ok;
	}

	public static Map< String, Object > getDefaultSettingsMap()
	{
		final Map< String, Object > sm = new HashMap< >( 3 );
		sm.put( KEY_KALMAN_SEARCH_RADIUS, DEFAULT_MAX_SEARCH_RADIUS );
		sm.put( KEY_LINKING_MAX_DISTANCE, DEFAULT_LINKING_MAX_DISTANCE );
		sm.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, DEFAULT_GAP_CLOSING_MAX_FRAME_GAP );
		sm.put( KEY_POSITION_SIGMA, DEFAULT_POSITION_SIGMA );
		return sm;
	}

}
