package org.mastodon.linking.lap;

import static org.mastodon.linking.LinkingUtils.checkFeatureMap;
import static org.mastodon.linking.LinkingUtils.checkMapKeys;
import static org.mastodon.linking.LinkingUtils.checkParameter;
import static org.mastodon.linking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static org.mastodon.linking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static org.mastodon.linking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static org.mastodon.linking.TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static org.mastodon.linking.TrackerKeys.KEY_BLOCKING_VALUE;
import static org.mastodon.linking.TrackerKeys.KEY_CUTOFF_PERCENTILE;
import static org.mastodon.linking.TrackerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static org.mastodon.linking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static org.mastodon.linking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static org.mastodon.linking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static org.mastodon.linking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static org.mastodon.linking.TrackerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static org.mastodon.linking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;
import static org.mastodon.linking.TrackerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static org.mastodon.linking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mastodon.graph.Edge;
import org.mastodon.graph.Graph;
import org.mastodon.graph.Vertex;
import org.mastodon.linking.AbstractParticleLinkerOp;
import org.mastodon.linking.LinkingUtils;
import org.mastodon.linking.ParticleLinkerOp;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.spatial.HasTimepoint;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.special.inplace.Inplaces;
import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.Benchmark;

@Plugin( type = ParticleLinkerOp.class )
public class SparseLAPLinker< V extends Vertex< E > & HasTimepoint & RealLocalizable, E extends Edge< V > >
	extends AbstractParticleLinkerOp< V, E >
	implements Benchmark
{
	private final static String BASE_ERROR_MESSAGE = "[SparseLAPTracker] ";

	@Parameter( type = ItemIO.OUTPUT )
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

		if ( maxTimepoint <= minTimepoint )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Max timepoint <= min timepoint.";
			return;
		}

		// Check that the objects list itself isn't null
		if ( null == spots )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The spot index is null.";
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
		// Check parameters
		final StringBuilder errorHolder = new StringBuilder();
		if ( !checkSettingsValidity( settings, errorHolder ) )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Incorrect settings map:\n" + errorHolder.toString();
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
		ftfSettings.put( KEY_LINKING_MAX_DISTANCE, settings.get( KEY_LINKING_MAX_DISTANCE ) );
		ftfSettings.put( KEY_ALTERNATIVE_LINKING_COST_FACTOR, settings.get( KEY_ALTERNATIVE_LINKING_COST_FACTOR ) );
		ftfSettings.put( KEY_LINKING_FEATURE_PENALTIES, settings.get( KEY_LINKING_FEATURE_PENALTIES ) );

		@SuppressWarnings( "unchecked" )
		final SparseLAPFrameToFrameLinker<V,E> frameToFrameLinker = ( SparseLAPFrameToFrameLinker< V, E > ) Inplaces.binary1( ops(), SparseLAPFrameToFrameLinker.class,
				graph, spots,
				ftfSettings, featureModel, minTimepoint, maxTimepoint, spotComparator, edgeCreator );
		frameToFrameLinker.mutate1( graph, spots );
		if ( !frameToFrameLinker.wasSuccessful() )
		{
			errorMessage = frameToFrameLinker.getErrorMessage();
			return;
		}
		// Copy link costs.
		final DoublePropertyMap< E > ftfCosts = frameToFrameLinker.getLinkCostFeature().getPropertyMap();
		for ( final E e : ftfCosts.getMap().keySet() )
			linkcost.set( e, ftfCosts.get( e ) );

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
		@SuppressWarnings( { "unchecked", "rawtypes" } )
		final SparseLAPSegmentLinker< V, E > segmentLinker = ( SparseLAPSegmentLinker ) Inplaces.binary1( ops(), SparseLAPSegmentLinker.class,
				graph, spots,
				slSettings, featureModel, minTimepoint, maxTimepoint, spotComparator, edgeCreator );
		segmentLinker.mutate1( graph, spots );
		if ( !segmentLinker.wasSuccessful() )
		{
			errorMessage = segmentLinker.getErrorMessage();
			return ;
		}
		// Copy link costs.
		final DoublePropertyMap< E > slCosts = segmentLinker.getLinkCostFeature().getPropertyMap();
		for ( final E e : slCosts.getMap().keySet() )
			linkcost.set( e, slCosts.get( e ) );

		final long end = System.currentTimeMillis();
		processingTime = end - start;
		this.linkCostFeature = LinkingUtils.getLinkCostFeature( linkcost );
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
		ok = ok & checkMapKeys( settings, mandatoryKeys, optionalKeys, str );

		return ok;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public boolean wasSuccessful()
	{
		return ok;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

}
