package org.mastodon.linking;

import org.mastodon.graph.Edge;
import org.mastodon.graph.Graph;
import org.mastodon.graph.Vertex;

public interface EdgeCreator< V, E >
{

	/**
	 * Method called before a batch of edges is added to the output via the
	 * {@link #createEdge(Graph, Edge, Vertex, Vertex, double) } method.
	 */
	public void preAddition();

	/**
	 * Method called after a batch of edges is added to the output via the
	 * {@link #createEdge(Graph, Edge, Vertex, Vertex, double) } method.
	 */
	public void postAddition();

	public E createEdge( V source, V target, double edgeCost );
}
