package org.mastodon.linking.mamut;

import org.mastodon.linking.lap.SparseLAPLinker;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.scijava.plugin.Plugin;

@Plugin( type = SpotLinkerOp.class,
		name = "Simple LAP linker",
		description = "<html>"
				+ "This tracker is a simplified version of the LAP tracker, based on the "
				+ "following paper: "
				+ "<p>"
				+ "<i>Robust single-particle tracking in live-cell time-lapse sequences</i> - "
				+ "Jaqaman <i> et al.</i>, 2008, Nature Methods. "
				+ "<p>"
				+ "It simply offers fewer configuration options. Namely, it limits itself  "
				+ "</html>" )
public class SimpleSparseLAPLinkerMamut extends AbstractSpotLinkerOp
{

	@Override
	public void mutate1( final ModelGraph graph, final SpatioTemporalIndex< Spot > spots )
	{
		exec( graph, spots, SparseLAPLinker.class );
	}

}
