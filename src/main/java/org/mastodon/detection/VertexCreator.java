package org.mastodon.detection;

import org.mastodon.graph.Graph;
import org.mastodon.graph.Vertex;

public interface VertexCreator< V extends Vertex< ? > >
{

	public V createVertex( Graph< V, ? > graph, V ref, double[] pos, double radius, int timepoint, double quality );

}
