package org.mastodon.linking.mamut;

import java.util.Comparator;
import java.util.Map;

import org.mastodon.graph.Graph;
import org.mastodon.linking.EdgeCreator;
import org.mastodon.linking.ParticleLinkerOp;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.feature.Feature;
import org.mastodon.revised.model.feature.FeatureModel;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;

import net.imagej.ops.special.inplace.AbstractBinaryInplace1Op;
import net.imagej.ops.special.inplace.Inplaces;
import net.imglib2.algorithm.Benchmark;

public abstract class AbstractSpotLinkerOp
		extends AbstractBinaryInplace1Op< ModelGraph, SpatioTemporalIndex< Spot > >
		implements SpotLinkerOp, Benchmark
{

	@Parameter( type = ItemIO.INPUT )
	protected Map< String, Object > settings;

	@Parameter( type = ItemIO.INPUT )
	protected FeatureModel< Spot, Link > featureModel;

	protected long processingTime;

	protected boolean ok;

	protected String errorMessage;

	protected void exec( final ModelGraph graph, final SpatioTemporalIndex< Spot > spots, @SuppressWarnings( "rawtypes" ) final Class< ? extends ParticleLinkerOp > cl )
	{
		ok = false;
		final long start = System.currentTimeMillis();
		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final ParticleLinkerOp< Spot, Link > linker = ( ParticleLinkerOp ) Inplaces.binary1( ops(), cl,
				graph, spots,
				settings, featureModel,
				spotComparator(), edgeCreator() );
		linker.mutate1( graph, spots );
		final long end = System.currentTimeMillis();

		processingTime = end - start;
		linkCostFeature = linker.getLinkCostFeature();
		ok = linker.isSuccessful();
		errorMessage = linker.getErrorMessage();
	}

	protected EdgeCreator< Spot, Link > edgeCreator()
	{
		return EDGE_CREATOR;
	}

	protected Comparator< Spot > spotComparator()
	{
		return SPOT_COMPARATOR;
	}

	/**
	 * The edge linking cost feature provided by this particle-linker.
	 */
	@Parameter( type = ItemIO.OUTPUT )
	protected Feature< Link, Double, DoublePropertyMap< Link > > linkCostFeature;

	@Override
	public Feature< Link, Double, DoublePropertyMap< Link > > getLinkCostFeature()
	{
		return linkCostFeature;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public boolean wasSuccessful()
	{
		return ok;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	private static final Comparator< Spot > SPOT_COMPARATOR = new Comparator< Spot >()
	{

		@Override
		public int compare( final Spot o1, final Spot o2 )
		{
			return o1.getInternalPoolIndex() - o2.getInternalPoolIndex();
		}
	};

	private static final EdgeCreator< Spot, Link > EDGE_CREATOR = new EdgeCreator< Spot, Link >()
	{

		@Override
		public Link createEdge( final Graph< Spot, Link > graph, final Link ref, final Spot source, final Spot target, final double edgeCost )
		{
			return graph.addEdge( source, target, ref ).init();
		}
	};
}
