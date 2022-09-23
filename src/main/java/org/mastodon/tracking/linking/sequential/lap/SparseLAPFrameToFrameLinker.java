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
package org.mastodon.tracking.linking.sequential.lap;

import static org.mastodon.tracking.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_DO_LINK_SELECTION;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_LINKING_MAX_DISTANCE;
import static org.mastodon.tracking.linking.LinkingUtils.checkFeatureMap;
import static org.mastodon.tracking.linking.LinkingUtils.checkMapKeys;
import static org.mastodon.tracking.linking.LinkingUtils.checkParameter;

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
import org.mastodon.feature.FeatureProjectionKey;
import org.mastodon.spatial.HasTimepoint;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.tracking.linking.EdgeCreator;
import org.mastodon.tracking.linking.LinkingUtils;
import org.mastodon.tracking.linking.sequential.AbstractSequentialParticleLinkerOp;
import org.mastodon.tracking.linking.sequential.lap.costfunction.CostFunction;
import org.mastodon.tracking.linking.sequential.lap.costmatrix.JaqamanLinkingCostMatrixCreator;
import org.mastodon.tracking.linking.sequential.lap.linker.JaqamanLinker;
import org.mastodon.tracking.linking.sequential.lap.linker.SparseCostMatrix;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import net.imagej.ops.special.function.Functions;
import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.Benchmark;

@Plugin( type = SparseLAPFrameToFrameLinker.class )
public class SparseLAPFrameToFrameLinker< V extends HasTimepoint & RealLocalizable  >
		extends AbstractSequentialParticleLinkerOp< V >
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
	public void mutate1( final EdgeCreator< V > edgeCreator, final SpatioTemporalIndex< V > spots )
	{
		ok = false;

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
			errorMessage = BASE_ERROR_MESSAGE + "The spot collection is empty.";
			return;
		}

		/*
		 * Process.
		 */

		final long start = System.currentTimeMillis();

		// Prepare frame pairs in order. For now they are separated by 1.
		final ArrayList< int[] > framePairs = new ArrayList< >( maxTimepoint - minTimepoint );
		for ( int tp = minTimepoint; tp <= maxTimepoint - 1; tp++ )
		{ // ascending order
			framePairs.add( new int[] { tp, tp + 1 } );
		}

		// Prepare cost function
		@SuppressWarnings( "unchecked" )
		final Map< FeatureProjectionKey, Double > featurePenalties = ( Map< FeatureProjectionKey, Double > ) settings.get( KEY_LINKING_FEATURE_PENALTIES );
		@SuppressWarnings( "unchecked" )
		final Class< V > vertexClass = ( Class< V > ) refcol.createRef().getClass();
		final CostFunction< V, V > costFunction = LinkingUtils.getCostFunctionFor( featurePenalties, featureModel, vertexClass );

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
					if ( isCanceled() || !aok.get() )
						return null;

					// Get frame pairs
					final int frame0 = framePairs.get( i )[ 0 ];
					final int frame1 = framePairs.get( i )[ 1 ];

					spots.readLock().lock();
					JaqamanLinker< V, V > linker = null;
					try
					{
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
								refcol, refcol,
								spotComparator, spotComparator );
						linker = new JaqamanLinker< >( creator, refcol, refcol );
						if ( !linker.checkInput() || !linker.process() )
						{
							errorMessage = "Linking frame " + frame0 + " to " + frame1 + ": " + linker.getErrorMessage();
							aok.set( false );
							return null;
						}
					}
					catch (final Exception e)
					{
						e.printStackTrace();
					}
					finally
					{
						spots.readLock().unlock();
					}

					/*
					 * Update graph.
					 */

					edgeCreator.preAddition();
					try
					{
						final RefRefMap< V, V > assignment = linker.getResult();
						final RefDoubleMap< V > assignmentCosts = linker.getAssignmentCosts();
						final V vref = refcol.createRef();
						for ( final V source : assignment.keySet() )
						{
							final V target = assignment.get( source, vref );
							final double cost = assignmentCosts.get( source );
							edgeCreator.createEdge( source, target, cost );
						}
						refcol.releaseRef( vref );
					}
					catch ( final Exception e )
					{
						e.printStackTrace();
					}
					finally
					{
						edgeCreator.postAddition();
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
		final List< String > mandatoryKeys = new ArrayList< >();
		mandatoryKeys.add( KEY_MIN_TIMEPOINT );
		mandatoryKeys.add( KEY_MAX_TIMEPOINT );
		mandatoryKeys.add( KEY_LINKING_MAX_DISTANCE );
		mandatoryKeys.add( KEY_ALTERNATIVE_LINKING_COST_FACTOR );
		final List< String > optionalKeys = new ArrayList< >();
		optionalKeys.add( KEY_LINKING_FEATURE_PENALTIES );
		optionalKeys.add( KEY_DO_LINK_SELECTION );
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
