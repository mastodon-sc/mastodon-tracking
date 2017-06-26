package org.mastodon.tracking.lap;



import static org.mastodon.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static org.mastodon.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static org.mastodon.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static org.mastodon.tracking.TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static org.mastodon.tracking.TrackerKeys.KEY_BLOCKING_VALUE;
import static org.mastodon.tracking.TrackerKeys.KEY_CUTOFF_PERCENTILE;
import static org.mastodon.tracking.TrackerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static org.mastodon.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static org.mastodon.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static org.mastodon.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static org.mastodon.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static org.mastodon.tracking.TrackerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static org.mastodon.tracking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;
import static org.mastodon.tracking.TrackerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static org.mastodon.tracking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;
import static org.mastodon.tracking.lap.LAPUtils.checkFeatureMap;
import static org.mastodon.tracking.lap.LAPUtils.checkMapKeys;
import static org.mastodon.tracking.lap.LAPUtils.checkParameter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mastodon.graph.Edge;
import org.mastodon.graph.Graph;
import org.mastodon.graph.Vertex;
import org.mastodon.revised.mamut.ProgressListener;
import org.mastodon.revised.model.feature.FeatureModel;
import org.mastodon.spatial.HasTimepoint;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.tracking.EdgeCreator;
import org.mastodon.tracking.ProgressListeners;

import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;

public class SparseLAPTracker< V extends Vertex< E > & HasTimepoint & RealLocalizable, E extends Edge< V > > extends MultiThreadedBenchmarkAlgorithm
{
	private final static String BASE_ERROR_MESSAGE = "[SparseLAPTracker] ";


	private final SpatioTemporalIndex< V > spots;

	private FeatureModel< V, E > featureModel;

	private final Graph< V, E > graph;

	private final Map< String, Object > settings;

	private final int minTimepoint;

	private final int maxTimepoint;

	private final Comparator< V>  spotComparator;

	private final EdgeCreator< V, E > edgeCreator;

	private ProgressListener logger = ProgressListeners.voidLogger();

	/*
	 * CONSTRUCTOR
	 */

	public SparseLAPTracker(
			final SpatioTemporalIndex< V > spots,
			final FeatureModel< V, E > featureModel,
			final Graph< V, E > graph,
			final EdgeCreator< V, E > edgeCreator,
			final int minTimepoint,
			final int maxTimepoint,
			final Map< String, Object > settings,
			final Comparator< V > spotComparator )
	{
		this.spots = spots;
		this.graph = graph;
		this.edgeCreator = edgeCreator;
		this.minTimepoint = minTimepoint;
		this.maxTimepoint = maxTimepoint;
		this.settings = settings;
		this.spotComparator = spotComparator;
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
			errorMessage = BASE_ERROR_MESSAGE + "Incorrect settings map:\n" + errorHolder.toString();
			return false;
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

		final SparseLAPFrameToFrameTracker< V, E > frameToFrameLinker = new SparseLAPFrameToFrameTracker<>(
				spots, featureModel, graph, edgeCreator, minTimepoint, maxTimepoint, ftfSettings, spotComparator );
		frameToFrameLinker.setNumThreads( numThreads );
		frameToFrameLinker.setProgressListener( logger );

		if ( !frameToFrameLinker.checkInput() || !frameToFrameLinker.process() )
		{
			errorMessage = frameToFrameLinker.getErrorMessage();
			return false;
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
		final SparseLAPSegmentTracker< V, E > segmentLinker = new SparseLAPSegmentTracker<>( graph, edgeCreator, featureModel, slSettings, spotComparator );
		segmentLinker.setNumThreads( numThreads );
		segmentLinker.setProgressListener( logger );

		if ( !segmentLinker.checkInput() || !segmentLinker.process() )
		{
			errorMessage = segmentLinker.getErrorMessage();
			return false;
		}

		logger.showStatus( "LAP tracking done." );
		logger.showProgress( 1, 1 );
		final long end = System.currentTimeMillis();
		processingTime = end - start;

		return true;
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

	public void setProgressListener( final ProgressListener logger )
	{
		this.logger = logger;
	}
}
