package org.mastodon.linking;

import org.mastodon.graph.Edge;
import org.mastodon.graph.Graph;
import org.mastodon.graph.Vertex;

public interface EdgeCreator< V extends Vertex< E >, E extends Edge< V > >
{
	public E createEdge( Graph< V, E >graph, E ref, V source, V target, double edgeCost );
}
