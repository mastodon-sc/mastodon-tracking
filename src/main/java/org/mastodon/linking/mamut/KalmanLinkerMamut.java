package org.mastodon.linking.mamut;

import static org.mastodon.linking.LinkingUtils.checkParameter;
import static org.mastodon.linking.TrackerKeys.DEFAULT_GAP_CLOSING_MAX_FRAME_GAP;
import static org.mastodon.linking.TrackerKeys.DEFAULT_LINKING_MAX_DISTANCE;
import static org.mastodon.linking.TrackerKeys.DEFAULT_MAX_SEARCH_RADIUS;
import static org.mastodon.linking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static org.mastodon.linking.TrackerKeys.KEY_KALMAN_SEARCH_RADIUS;
import static org.mastodon.linking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static org.mastodon.linking.TrackerKeys.KEY_POSITION_SIGMA;

import java.util.HashMap;
import java.util.Map;

import org.mastodon.linking.ParticleLinkerOp;
import org.mastodon.linking.kalman.KalmanLinker;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.scijava.plugin.Plugin;

import com.google.common.collect.Streams;

import net.imagej.ops.special.inplace.Inplaces;

@Plugin( type = SpotLinkerOp.class )
public class KalmanLinkerMamut extends AbstractSpotLinkerOp
{

	@Override
	public void mutate1( final ModelGraph graph, final SpatioTemporalIndex< Spot > spots )
	{
		final long start = System.currentTimeMillis();
		ok = false;

		/*
		 * Before we run the generic linker, we need to provide it with a
		 * settings map augmented with a position std calculated from the spot
		 * radii in the first non-empty frame.
		 */
		final HashMap< String, Object > kalmanSettings = new HashMap<>( settings );
		int t = minTimepoint;
		while ( t <= maxTimepoint && spots.getSpatialIndex( t++ ).isEmpty() );
		final double meanR2 = Streams.stream( spots.getSpatialIndex( t ) )
				.mapToDouble( e -> e.getBoundingSphereRadiusSquared() )
				.average()
				.getAsDouble();
		final double meanR = Math.sqrt( meanR2 );
		kalmanSettings.put( KEY_POSITION_SIGMA, meanR / 10. );

		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final ParticleLinkerOp< Spot, Link > linker = ( ParticleLinkerOp ) Inplaces.binary1( ops(), KalmanLinker.class,
				graph, spots,
				kalmanSettings, featureModel, minTimepoint, maxTimepoint,
				spotComparator(), edgeCreator() );
		linker.mutate1( graph, spots );
		final long end = System.currentTimeMillis();

		processingTime = end - start;
		linkCostFeature = linker.getLinkCostFeature();
		ok = linker.wasSuccessful();
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
		return ok;
	}

	public static Map< String, Object > getDefaultSettingsMap()
	{
		final Map< String, Object > sm = new HashMap< String, Object >( 3 );
		sm.put( KEY_KALMAN_SEARCH_RADIUS, DEFAULT_MAX_SEARCH_RADIUS );
		sm.put( KEY_LINKING_MAX_DISTANCE, DEFAULT_LINKING_MAX_DISTANCE );
		sm.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, DEFAULT_GAP_CLOSING_MAX_FRAME_GAP );
		return sm;
	}

}
