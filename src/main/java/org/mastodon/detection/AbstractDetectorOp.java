package org.mastodon.detection;

import org.mastodon.graph.Graph;
import org.mastodon.graph.Vertex;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.feature.Feature;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;

import bdv.spimdata.SpimDataMinimal;
import net.imagej.ops.special.inplace.AbstractBinaryInplace1Op;

public abstract class AbstractDetectorOp< V extends Vertex< ? > >
		extends AbstractBinaryInplace1Op< Graph< V, ? >, SpimDataMinimal >
		implements DetectorOp< V >
{

	/**
	 * The id of the setup in the provided SpimData object to process.
	 */
	@Parameter( required = true )
	protected int setup = 0;

	/**
	 * the expected radius (in units of the global coordinate system) of blobs
	 * to detect.
	 */
	@Parameter( required = true )
	protected double radius = 5.;

	/**
	 * The quality threshold below which spots will be rejected.
	 */
	@Parameter
	protected double threshold = 0.;

	/**
	 * The min time-point to process, inclusive.
	 */
	@Parameter
	protected int minTimepoint = 0;

	/**
	 * The max time-point to process, inclusive.
	 */
	@Parameter
	protected int maxTimepoint = 0;

	/**
	 * The {@link VertexCreator} that will be used to create and add vertices to
	 * the graph.
	 */
	@Parameter
	protected VertexCreator< V > vertexCreator;

	/**
	 * The quality feature provided by this detector.
	 */
	@Parameter( type = ItemIO.OUTPUT )
	protected Feature< V, Double, DoublePropertyMap< V > > qualityFeature;

	@Override
	public Feature< V, Double, DoublePropertyMap< V > > getQualityFeature()
	{
		return qualityFeature;
	}
}
