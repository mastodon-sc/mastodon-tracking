package org.mastodon.linking;

import java.util.Comparator;
import java.util.Map;

import org.mastodon.graph.Edge;
import org.mastodon.graph.Graph;
import org.mastodon.graph.Vertex;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.feature.Feature;
import org.mastodon.revised.model.feature.FeatureModel;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.plugin.Parameter;

import net.imagej.ops.special.inplace.AbstractBinaryInplace1Op;
import net.imglib2.RealLocalizable;

public abstract class AbstractParticleLinkerOp< V extends Vertex< E > & RealLocalizable, E extends Edge< V > >
	extends AbstractBinaryInplace1Op< Graph< V, E >, SpatioTemporalIndex< V > >
	implements ParticleLinkerOp< V, E >
{

	@Parameter
	protected StatusService statusService;

	@Parameter(type = ItemIO.INPUT )
	protected Map< String, Object > settings;

	@Parameter(type = ItemIO.INPUT )
	protected FeatureModel< V, E > featureModel;

	@Parameter(type = ItemIO.INPUT )
	protected Comparator< V > spotComparator;

	@Parameter(type = ItemIO.INPUT )
	protected EdgeCreator< V, E > edgeCreator;

	@Parameter( type = ItemIO.OUTPUT )
	protected String errorMessage;

	@Parameter( type = ItemIO.OUTPUT )
	protected boolean ok;

	/**
	 * The edge linking cost feature provided by this particle-linker.
	 */
	@Parameter( type = ItemIO.OUTPUT )
	protected Feature< E, Double, DoublePropertyMap< E > > linkCostFeature;

	@Override
	public Feature< E, Double, DoublePropertyMap< E > > getLinkCostFeature()
	{
		return linkCostFeature;
	}
}
