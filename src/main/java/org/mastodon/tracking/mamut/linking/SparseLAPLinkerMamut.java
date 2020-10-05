package org.mastodon.tracking.mamut.linking;

import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.tracking.linking.graph.lap.SparseLAPLinker;
import org.scijava.plugin.Plugin;

@Plugin( type = SpotLinkerOp.class,
		name = "LAP linker",
		description = "<html>"
				+ "This tracker is based on the Linear Assignment Problem mathematical framework. "
				+ "Its implementation is adapted from the following paper: "
				+ "<p>"
				+ "<i>Robust single-particle tracking in live-cell time-lapse sequences</i> - "
				+ "Jaqaman <i> et al.</i>, 2008, Nature Methods. "
				+ "<p>"
				+ "Particle-linking is done in two steps: First spots are linked from frame to frame to "
				+ "build track segments. These track segments are investigated in a second step "
				+ "for gap-closing (missing detection), splitting and merging events. "
				+ "<p>"
				+ "Linking costs are proportional to the square distance between source and  "
				+ "target spots, which makes this tracker suitable for Brownian motion. "
				+ "Penalties can be set to favor linking between spots that have similar "
				+ "features. "
				+ "<p>"
				+ "Solving the LAP relies on the Jonker-Volgenant solver, and a sparse cost matrix formulation, "
				+ "allowing it to handle very large problems. "
				+ "</html>" )
public class SparseLAPLinkerMamut extends AbstractSpotLinkerOp
{

	@Override
	public void mutate1( final ModelGraph graph, final SpatioTemporalIndex< Spot > spots )
	{
		exec( graph, spots, SparseLAPLinker.class );
	}

}
