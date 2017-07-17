package org.mastodon.linking.mamut;

import static org.mastodon.detection.DetectorKeys.DEFAULT_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.DEFAULT_MIN_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.linking.LinkerKeys.DEFAULT_GAP_CLOSING_MAX_FRAME_GAP;
import static org.mastodon.linking.LinkerKeys.DEFAULT_LINKING_MAX_DISTANCE;
import static org.mastodon.linking.LinkerKeys.DEFAULT_MAX_SEARCH_RADIUS;
import static org.mastodon.linking.LinkerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static org.mastodon.linking.LinkerKeys.KEY_KALMAN_SEARCH_RADIUS;
import static org.mastodon.linking.LinkerKeys.KEY_LINKING_MAX_DISTANCE;
import static org.mastodon.linking.LinkerKeys.KEY_POSITION_SIGMA;
import static org.mastodon.linking.LinkingUtils.checkParameter;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.mastodon.linking.ParticleLinkerOp;
import org.mastodon.linking.kalman.KalmanLinker;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.scijava.plugin.Plugin;

import net.imagej.ops.special.inplace.Inplaces;

@Plugin( type = SpotLinkerOp.class )
public class KalmanLinkerMamut extends AbstractSpotLinkerOp
{

	@Override
	public void mutate1( final ModelGraph graph, final SpatioTemporalIndex< Spot > spots )
	{
		final long start = System.currentTimeMillis();
		ok = false;
		final StringBuilder str = new StringBuilder();
		if (!checkSettingsValidity( settings, str ))
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
		int t = minTimepoint;
		while ( t <= maxTimepoint && spots.getSpatialIndex( t++ ).isEmpty() );
		final double meanR2 = StreamSupport.stream( spots.getSpatialIndex( t ).spliterator(), false )
				.mapToDouble( e -> e.getBoundingSphereRadiusSquared() )
				.average()
				.getAsDouble();
		final double meanR = Math.sqrt( meanR2 );
		kalmanSettings.put( KEY_POSITION_SIGMA, meanR / 10. );

		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final ParticleLinkerOp< Spot, Link > linker = ( ParticleLinkerOp ) Inplaces.binary1( ops(), KalmanLinker.class,
				graph, spots,
				kalmanSettings, featureModel,
				spotComparator(), edgeCreator() );
		linker.mutate1( graph, spots );
		final long end = System.currentTimeMillis();

		processingTime = end - start;
		linkCostFeature = linker.getLinkCostFeature();
		ok = linker.isSuccessful();
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
		return ok;
	}

	public static Map< String, Object > getDefaultSettingsMap()
	{
		final Map< String, Object > sm = new HashMap< String, Object >( 3 );
		sm.put( KEY_KALMAN_SEARCH_RADIUS, DEFAULT_MAX_SEARCH_RADIUS );
		sm.put( KEY_LINKING_MAX_DISTANCE, DEFAULT_LINKING_MAX_DISTANCE );
		sm.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, DEFAULT_GAP_CLOSING_MAX_FRAME_GAP );
		sm.put( KEY_MIN_TIMEPOINT, DEFAULT_MIN_TIMEPOINT );
		sm.put( KEY_MAX_TIMEPOINT, DEFAULT_MAX_TIMEPOINT );
		return sm;
	}

}
