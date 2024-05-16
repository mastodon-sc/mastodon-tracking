/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2024 Tobias Pietzsch, Jean-Yves Tinevez
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
package org.mastodon.tracking.linking.graph.lap;

import static org.mastodon.tracking.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_ALLOW_GAP_CLOSING;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_ALLOW_TRACK_MERGING;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_BLOCKING_VALUE;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_CUTOFF_PERCENTILE;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_DO_LINK_SELECTION;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_LINKING_MAX_DISTANCE;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_MERGING_MAX_DISTANCE;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_SPLITTING_MAX_DISTANCE;
import static org.mastodon.tracking.linking.LinkingUtils.checkFeatureMap;
import static org.mastodon.tracking.linking.LinkingUtils.checkMapKeys;
import static org.mastodon.tracking.linking.LinkingUtils.checkParameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mastodon.graph.Edge;
import org.mastodon.graph.ReadOnlyGraph;
import org.mastodon.graph.Vertex;
import org.mastodon.spatial.HasTimepoint;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.tracking.linking.graph.AbstractGraphParticleLinkerOp;
import org.mastodon.tracking.linking.graph.GraphParticleLinkerOp;
import org.mastodon.tracking.linking.sequential.lap.SparseLAPFrameToFrameLinker;
import org.scijava.Cancelable;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.special.inplace.Inplaces;
import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.Benchmark;

@Plugin( type = GraphParticleLinkerOp.class )
public class SparseLAPLinker< V extends Vertex< E > & HasTimepoint & RealLocalizable, E extends Edge< V > >
		extends AbstractGraphParticleLinkerOp< V, E >
		implements Benchmark
{
	private final static String BASE_ERROR_MESSAGE = "[SparseLAPLinker] ";

	@Parameter( type = ItemIO.OUTPUT )
	private long processingTime;

	private Cancelable currentCancelable;

	/*
	 * METHODS
	 */

	@Override
	public void mutate1( final ReadOnlyGraph< V, E > graph, final SpatioTemporalIndex< V > spots )
	{
		ok = false;

		// Check parameters
		final StringBuilder errorHolder = new StringBuilder();
		if ( !checkSettingsValidity( settings, errorHolder ) )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Incorrect settings map:\n" + errorHolder.toString();
			return;
		}

		final int minTimepoint = ( int ) settings.get( KEY_MIN_TIMEPOINT );
		final int maxTimepoint = ( int ) settings.get( KEY_MAX_TIMEPOINT );

		// Check that the objects list itself isn't null
		if ( null == spots )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The spot index is null.";
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

		/*
		 * 1. Frame to frame linking.
		 */

		// Prepare settings object
		final Map< String, Object > ftfSettings = new HashMap< String, Object >();
		ftfSettings.put( KEY_MIN_TIMEPOINT, settings.get( KEY_MIN_TIMEPOINT ) );
		ftfSettings.put( KEY_MAX_TIMEPOINT, settings.get( KEY_MAX_TIMEPOINT ) );
		ftfSettings.put( KEY_LINKING_MAX_DISTANCE, settings.get( KEY_LINKING_MAX_DISTANCE ) );
		ftfSettings.put( KEY_ALTERNATIVE_LINKING_COST_FACTOR, settings.get( KEY_ALTERNATIVE_LINKING_COST_FACTOR ) );
		ftfSettings.put( KEY_LINKING_FEATURE_PENALTIES, settings.get( KEY_LINKING_FEATURE_PENALTIES ) );

		@SuppressWarnings( "unchecked" )
		final SparseLAPFrameToFrameLinker< V > frameToFrameLinker = ( SparseLAPFrameToFrameLinker< V > ) Inplaces.binary1( ops(),
				SparseLAPFrameToFrameLinker.class,
				edgeCreator, spots,
				ftfSettings, featureModel, spotComparator, graph.vertices() );
		this.currentCancelable = frameToFrameLinker;

		frameToFrameLinker.mutate1( edgeCreator, spots );
		if ( !frameToFrameLinker.isSuccessful() )
		{
			errorMessage = frameToFrameLinker.getErrorMessage();
			return;
		}

		/*
		 * 2. Gap-closing, merging and splitting.
		 */

		// Prepare settings object
		final Map< String, Object > slSettings = new HashMap< String, Object >();

		slSettings.put( KEY_ALLOW_GAP_CLOSING, settings.get( KEY_ALLOW_GAP_CLOSING ) );
		slSettings.put( KEY_GAP_CLOSING_FEATURE_PENALTIES, settings.get( KEY_GAP_CLOSING_FEATURE_PENALTIES ) );
		slSettings.put( KEY_GAP_CLOSING_MAX_DISTANCE, settings.get( KEY_GAP_CLOSING_MAX_DISTANCE ) );
		slSettings.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, settings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP ) );

		slSettings.put( KEY_ALLOW_TRACK_SPLITTING, settings.get( KEY_ALLOW_TRACK_SPLITTING ) );
		slSettings.put( KEY_SPLITTING_FEATURE_PENALTIES, settings.get( KEY_SPLITTING_FEATURE_PENALTIES ) );
		slSettings.put( KEY_SPLITTING_MAX_DISTANCE, settings.get( KEY_SPLITTING_MAX_DISTANCE ) );

		slSettings.put( KEY_ALLOW_TRACK_MERGING, settings.get( KEY_ALLOW_TRACK_MERGING ) );
		slSettings.put( KEY_MERGING_FEATURE_PENALTIES, settings.get( KEY_MERGING_FEATURE_PENALTIES ) );
		slSettings.put( KEY_MERGING_MAX_DISTANCE, settings.get( KEY_MERGING_MAX_DISTANCE ) );

		slSettings.put( KEY_ALTERNATIVE_LINKING_COST_FACTOR, settings.get( KEY_ALTERNATIVE_LINKING_COST_FACTOR ) );
		slSettings.put( KEY_CUTOFF_PERCENTILE, settings.get( KEY_CUTOFF_PERCENTILE ) );

		// Solve.

		if ( !isCanceled() )
		{
			@SuppressWarnings( { "unchecked", "rawtypes" } )
			final SparseLAPSegmentLinker< V, E > segmentLinker = ( SparseLAPSegmentLinker ) Inplaces.binary1( ops(), SparseLAPSegmentLinker.class,
					graph, spots,
					slSettings, featureModel, spotComparator, edgeCreator );
			this.currentCancelable = segmentLinker;
			segmentLinker.mutate1( graph, spots );
			if ( !segmentLinker.isSuccessful() )
			{
				errorMessage = segmentLinker.getErrorMessage();
				return;
			}

		}

		currentCancelable = null;
		final long end = System.currentTimeMillis();
		processingTime = end - start;
		statusService.clearStatus();
		ok = true;
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
		// Gap-closing
		ok = ok & checkParameter( settings, KEY_ALLOW_GAP_CLOSING, Boolean.class, str );
		ok = ok & checkParameter( settings, KEY_GAP_CLOSING_MAX_DISTANCE, Double.class, str );
		ok = ok & checkParameter( settings, KEY_GAP_CLOSING_MAX_FRAME_GAP, Integer.class, str );
		ok = ok & checkFeatureMap( settings, KEY_GAP_CLOSING_FEATURE_PENALTIES, str );
		// Splitting
		ok = ok & checkParameter( settings, KEY_ALLOW_TRACK_SPLITTING, Boolean.class, str );
		ok = ok & checkParameter( settings, KEY_SPLITTING_MAX_DISTANCE, Double.class, str );
		ok = ok & checkFeatureMap( settings, KEY_SPLITTING_FEATURE_PENALTIES, str );
		// Merging
		ok = ok & checkParameter( settings, KEY_ALLOW_TRACK_MERGING, Boolean.class, str );
		ok = ok & checkParameter( settings, KEY_MERGING_MAX_DISTANCE, Double.class, str );
		ok = ok & checkFeatureMap( settings, KEY_MERGING_FEATURE_PENALTIES, str );
		// Others
		ok = ok & checkParameter( settings, KEY_CUTOFF_PERCENTILE, Double.class, str );
		ok = ok & checkParameter( settings, KEY_ALTERNATIVE_LINKING_COST_FACTOR, Double.class, str );

		// Check keys
		final List< String > mandatoryKeys = new ArrayList< String >();
		mandatoryKeys.add( KEY_MIN_TIMEPOINT );
		mandatoryKeys.add( KEY_MAX_TIMEPOINT );
		mandatoryKeys.add( KEY_LINKING_MAX_DISTANCE );
		mandatoryKeys.add( KEY_ALLOW_GAP_CLOSING );
		mandatoryKeys.add( KEY_GAP_CLOSING_MAX_DISTANCE );
		mandatoryKeys.add( KEY_GAP_CLOSING_MAX_FRAME_GAP );
		mandatoryKeys.add( KEY_ALLOW_TRACK_SPLITTING );
		mandatoryKeys.add( KEY_SPLITTING_MAX_DISTANCE );
		mandatoryKeys.add( KEY_ALLOW_TRACK_MERGING );
		mandatoryKeys.add( KEY_MERGING_MAX_DISTANCE );
		mandatoryKeys.add( KEY_ALTERNATIVE_LINKING_COST_FACTOR );
		mandatoryKeys.add( KEY_CUTOFF_PERCENTILE );
		final List< String > optionalKeys = new ArrayList< String >();
		optionalKeys.add( KEY_LINKING_FEATURE_PENALTIES );
		optionalKeys.add( KEY_GAP_CLOSING_FEATURE_PENALTIES );
		optionalKeys.add( KEY_SPLITTING_FEATURE_PENALTIES );
		optionalKeys.add( KEY_MERGING_FEATURE_PENALTIES );
		optionalKeys.add( KEY_BLOCKING_VALUE );
		optionalKeys.add( KEY_DO_LINK_SELECTION );
		ok = ok & checkMapKeys( settings, mandatoryKeys, optionalKeys, str );

		// Check min & max time-point
		if ( ok )
		{
			final int minTimepoint = ( int ) settings.get( KEY_MIN_TIMEPOINT );
			final int maxTimepoint = ( int ) settings.get( KEY_MAX_TIMEPOINT );
			if ( maxTimepoint < minTimepoint )
			{
				ok = false;
				str.append( "Min time-point should smaller than or equal to max time-point, be was min = "
						+ minTimepoint + " and max = " + maxTimepoint + "\n" );
			}
		}

		return ok;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
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
	public void cancel( final String reason )
	{
		super.cancel( reason );
		if ( null != currentCancelable && ( currentCancelable instanceof Cancelable ) )
		{
			final Cancelable cancelable = currentCancelable;
			cancelable.cancel( reason );
		}
	}
}
