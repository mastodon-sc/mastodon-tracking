package org.mastodon.tracking.mamut.linking;

import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.tracking.linking.ParticleLinker;

import net.imagej.ops.special.inplace.BinaryInplace1OnlyOp;

public interface SpotLinkerOp extends BinaryInplace1OnlyOp< ModelGraph, SpatioTemporalIndex< Spot > >, ParticleLinker
{

	/**
	 * Returns the linking cost feature calculated by this particle-linker.
	 * <p>
	 * The linking cost feature is defined for all links created by the
	 * last call to the linker and only them. By convention, cost values are
	 * real positive <code>double</code>s, with large values indicating the high
	 * linking costs.
	 *
	 * @return the linking cost feature.
	 */
	public LinkCostFeature getLinkCostFeature();

}
