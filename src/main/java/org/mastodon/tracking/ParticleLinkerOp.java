package org.mastodon.tracking;

import org.mastodon.graph.Edge;
import org.mastodon.graph.Graph;
import org.mastodon.graph.Vertex;
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
}
