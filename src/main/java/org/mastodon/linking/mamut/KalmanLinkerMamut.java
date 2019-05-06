package org.mastodon.linking.mamut;

import static org.mastodon.detection.DetectorKeys.DEFAULT_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.DEFAULT_MIN_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.linking.LinkerKeys.DEFAULT_DO_LINK_SELECTION;
import static org.mastodon.linking.LinkerKeys.DEFAULT_GAP_CLOSING_MAX_FRAME_GAP;
import static org.mastodon.linking.LinkerKeys.DEFAULT_LINKING_MAX_DISTANCE;
import static org.mastodon.linking.LinkerKeys.DEFAULT_MAX_SEARCH_RADIUS;
import static org.mastodon.linking.LinkerKeys.KEY_DO_LINK_SELECTION;
import static org.mastodon.linking.LinkerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static org.mastodon.linking.LinkerKeys.KEY_KALMAN_SEARCH_RADIUS;
import static org.mastodon.linking.LinkerKeys.KEY_LINKING_MAX_DISTANCE;
import static org.mastodon.linking.LinkerKeys.KEY_POSITION_SIGMA;
import static org.mastodon.linking.LinkingUtils.checkParameter;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.mastodon.linking.EdgeCreator;
import org.mastodon.linking.sequential.SequentialParticleLinkerOp;
import org.mastodon.linking.sequential.kalman.KalmanLinker;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.scijava.plugin.Plugin;

import net.imagej.ops.special.inplace.Inplaces;

@Plugin( type = SpotLinkerOp.class,
		name = "Linear motion Kalman linker",
		description = "<html>"
				+ "This tracker is best suited for objects that "
				+ "move with a roughly constant velocity vector."
				+ "<p>"
				+ "It relies on the Kalman filter to predict the next most likely position of a spot. "
				+ "The predictions for all current tracks are linked to the spots actually "
				+ "found in the next frame, thanks to the LAP framework already present in the LAP tracker. "
				+ "Predictions are continuously refined and the tracker can accomodate moderate "
				+ "velocity direction and magnitude changes. "
				+ "<p>"
				+ "This tracker can bridge gaps: If a spot is not found close enough to a prediction, "
				+ "then the Kalman filter will make another prediction in the next frame and re-iterate "
				+ "the search. "
				+ "<p>"
				+ "The first frames of a track are critical for this tracker to work properly: Tracks "
				+ "are initiated by looking for close neighbors (again via the LAP tracker). "
				+ "Spurious spots in the beginning of each track can confuse the tracker. "
				+ "<p>"
				+ "This tracker needs two parameters (on top of the maximal frame gap tolerated): "
				+ "<br/>"
				+ "\t - the max search radius defines how far from a predicted position it should look "
				+ "for candidate spots;<br/>"
				+ "\t - the initial search radius defines how far two spots can be apart when initiating "
				+ "a new track."
				+ "<br/></html>")
public class KalmanLinkerMamut extends AbstractSpotLinkerOp
{

	@Override
	public void mutate1( final ModelGraph graph, final SpatioTemporalIndex< Spot > spots )
	{
		final long start = System.currentTimeMillis();
		ok = false;
		final StringBuilder str = new StringBuilder();
		if ( !checkSettingsValidity( settings, str ) )
		{
			errorMessage = str.toString();
			return;
		}

		final int minTimepoint = ( int ) settings.get( KEY_MIN_TIMEPOINT );
		final int maxTimepoint = ( int ) settings.get( KEY_MAX_TIMEPOINT );

		/*
		 * Before we run the generic linker, we need to provide it with a
		 * settings map augmented with a position std calculated from the spot
		 * radii in the first non-empty frame.
		 */
		final HashMap< String, Object > kalmanSettings = new HashMap<>( settings );
		spots.readLock().lock();
		try
		{
			int t = minTimepoint;
			while ( t <= maxTimepoint && spots.getSpatialIndex( t++ ).isEmpty() );
			final double meanR2 = StreamSupport.stream( spots.getSpatialIndex( t ).spliterator(), false )
					.mapToDouble( e -> e.getBoundingSphereRadiusSquared() )
					.average()
					.getAsDouble();
			final double meanR = Math.sqrt( meanR2 );
			kalmanSettings.put( KEY_POSITION_SIGMA, meanR / 10. );
		}
		finally
		{
			spots.readLock().unlock();
		}

		if ( null == linkCostFeature )
			linkCostFeature = new LinkCostFeature( graph.edges().getRefPool() );

		final EdgeCreator< Spot > edgeCreator = edgeCreator( graph );

		@SuppressWarnings( { "rawtypes", "unchecked" } )
		SequentialParticleLinkerOp< Spot > linker = ( SequentialParticleLinkerOp ) Inplaces.binary1( ops(), KalmanLinker.class,
				edgeCreator, spots,
				kalmanSettings, featureModel,
				spotComparator(), graph.vertices() );
		this.cancelable = linker;
		linker.mutate1( edgeCreator, spots );
		final long end = System.currentTimeMillis();

		processingTime = end - start;
		ok = linker.isSuccessful();
		linker = null;
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
		ok = ok & checkParameter( settings, KEY_MIN_TIMEPOINT, Integer.class, str );
		ok = ok & checkParameter( settings, KEY_MAX_TIMEPOINT, Integer.class, str );
		ok = ok & checkParameter( settings, KEY_DO_LINK_SELECTION, Boolean.class, str );
		return ok;
	}

	public static Map< String, Object > getDefaultSettingsMap()
	{
		final Map< String, Object > sm = new HashMap<>( 6 );
		sm.put( KEY_KALMAN_SEARCH_RADIUS, DEFAULT_MAX_SEARCH_RADIUS );
		sm.put( KEY_LINKING_MAX_DISTANCE, DEFAULT_LINKING_MAX_DISTANCE );
		sm.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, DEFAULT_GAP_CLOSING_MAX_FRAME_GAP );
		sm.put( KEY_DO_LINK_SELECTION, DEFAULT_DO_LINK_SELECTION );
		sm.put( KEY_MIN_TIMEPOINT, DEFAULT_MIN_TIMEPOINT );
		sm.put( KEY_MAX_TIMEPOINT, DEFAULT_MAX_TIMEPOINT );
		return sm;
	}

}
