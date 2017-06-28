package org.mastodon.linking;

import org.mastodon.graph.Edge;
import org.mastodon.graph.Graph;
import org.mastodon.graph.Vertex;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.feature.Feature;
import org.mastodon.spatial.SpatioTemporalIndex;

import net.imagej.ops.special.inplace.BinaryInplace1OnlyOp;
import net.imglib2.RealLocalizable;

public interface ParticleLinkerOp< V extends Vertex< E > & RealLocalizable, E extends Edge< V > >
		extends BinaryInplace1OnlyOp< Graph< V, E >, SpatioTemporalIndex< V > >
{
	/**
	 * Returns <code>true</code> if the particle-linking process completed
	 * successfully. If not, a meaningful error message can be obtained with
	 * {@link #getErrorMessage()}.
	 *
	 * @return <code>true</code> if the particle-linking process completed
	 *         successfully.
	 * @see #getErrorMessage()
	 */
	public boolean wasSuccessful();

	/**
	 * Returns a meaningful error message after the particle-linking process
	 * failed to complete.
	 *
	 * @return an error message.
	 */
	public String getErrorMessage();

	/**
	 * Returns the edge linking cost feature calculated by this particle-linker.
	 * <p>
	 * The edge linking cost feature is defined for all edges created by the
	 * last call to the linker and only them. By convention, cost values are
	 * real positive <code>double</code>s, with large values indicating the high
	 * linking costs.
	 *
	 * @return the edge linking cost feature.
	 */
	Feature< E, Double, DoublePropertyMap< E > > getLinkCostFeature();

}
