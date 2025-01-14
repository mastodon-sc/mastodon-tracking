/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2024 Tobias Pietzsch, Jean-Yves Tinevez
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

import java.util.Map;

import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.tracking.linking.LinkingUtils;
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

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		return LinkingUtils.getDefaultLAPSettingsMap();
	}

}
