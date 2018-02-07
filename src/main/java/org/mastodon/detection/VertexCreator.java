package org.mastodon.detection;

public interface VertexCreator< V >
{

	public V createVertex( double[] pos, double radius, int timepoint, double quality );

	public void preAddition();

	public void postAddition();

}
