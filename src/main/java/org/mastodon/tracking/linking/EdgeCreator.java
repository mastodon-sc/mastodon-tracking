package org.mastodon.tracking.linking;

public interface EdgeCreator< V >
{

	/**
	 * Method called before a batch of edges is added to the output via the
	 * {@link #createEdge(Object, Object, double) } method.
	 */
	public void preAddition();

	/**
	 * Method called after a batch of edges is added to the output via the
	 * {@link #createEdge( Object, Object, double) } method.
	 */
	public void postAddition();

	public void createEdge( V source, V target, double edgeCost );
}
