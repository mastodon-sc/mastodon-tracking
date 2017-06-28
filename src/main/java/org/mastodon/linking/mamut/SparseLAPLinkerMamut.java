package org.mastodon.linking.mamut;

import org.mastodon.linking.lap.SparseLAPLinker;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.scijava.plugin.Plugin;

@Plugin( type = SpotLinkerOp.class )
public class SparseLAPLinkerMamut extends AbstractSpotLinkerOp
{

	@Override
	public void mutate1( final ModelGraph graph, final SpatioTemporalIndex< Spot > spots )
	{
		exec( graph, spots, SparseLAPLinker.class );
	}

}
