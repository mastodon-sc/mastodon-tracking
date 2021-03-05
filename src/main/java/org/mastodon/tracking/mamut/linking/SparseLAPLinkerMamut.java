/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2021 Tobias Pietzsch, Jean-Yves Tinevez
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
