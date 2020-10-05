package org.mastodon.tracking.mamut.linking;

import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.tracking.linking.graph.lap.SparseLAPLinker;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

@Plugin( type = SpotLinkerOp.class,
		name = "Simple LAP linker",
		priority  = Priority.HIGH,
		description = "<html>"
				+ "This tracker is a simplified version of the LAP tracker, based on the "
				+ "following paper: "
				+ "<p>"
				+ "<i>Robust single-particle tracking in live-cell time-lapse sequences</i> - "
				+ "Jaqaman <i> et al.</i>, 2008, Nature Methods. "
				+ "<p>"
				+ "It simply offers fewer configuration options. Namely, only gap closing is "
				+ "allowed, based solely on a distance and time condition. Track splitting "
				+ "and merging are not allowed, resulting in having non-branching tracks.  "
				+ "</html>" )
public class SimpleSparseLAPLinkerMamut extends AbstractSpotLinkerOp
{

	@Override
	public void mutate1( final ModelGraph graph, final SpatioTemporalIndex< Spot > spots )
	{
		exec( graph, spots, SparseLAPLinker.class );
	}

}
