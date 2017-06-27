package org.mastodon.linking;

import org.mastodon.graph.Edge;
import org.mastodon.graph.Vertex;

public interface EdgeCreator< V extends Vertex< E >, E extends Edge< V > >
{
	public E createEdge( V source, V target );
}
