package org.mastodon.linking.mamut;

import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.spatial.SpatioTemporalIndex;

import net.imagej.ops.special.inplace.BinaryInplace1OnlyOp;

public interface SpotLinkerOp extends BinaryInplace1OnlyOp< ModelGraph, SpatioTemporalIndex< Spot > >
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
