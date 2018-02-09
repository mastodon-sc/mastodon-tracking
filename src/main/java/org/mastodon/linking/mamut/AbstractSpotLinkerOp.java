package org.mastodon.linking.mamut;

import java.util.Comparator;
import java.util.Map;

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
	protected FeatureModel featureModel;

	protected long processingTime;

	protected boolean ok;

	protected String errorMessage;

	protected ParticleLinkerOp< Spot, Link > linker;

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	protected void exec( final ModelGraph graph, final SpatioTemporalIndex< Spot > spots, final Class< ? extends ParticleLinkerOp > cl )
	{
		ok = false;
		final long start = System.currentTimeMillis();
		this.linker = ( ParticleLinkerOp ) Inplaces.binary1( ops(), cl,
				graph, spots,
				settings, featureModel,
				spotComparator(), edgeCreator( graph ) );
		linker.mutate1( graph, spots );
		final long end = System.currentTimeMillis();

		processingTime = end - start;
		linkCostFeature = linker.getLinkCostFeature();
		ok = linker.isSuccessful();
		errorMessage = linker.getErrorMessage();
		linker = null;
	}

	protected EdgeCreator< Spot, Link > edgeCreator(final ModelGraph graph)
	{
		return new MyEdgeCreator( graph );
	}

	protected Comparator< Spot > spotComparator()
	{
		return SPOT_COMPARATOR;
	}

	/**
	 * The edge linking cost feature provided by this particle-linker.
	 */
	@Parameter( type = ItemIO.OUTPUT )
	protected Feature< Link, DoublePropertyMap< Link > > linkCostFeature;

	@Override
	public Feature< Link, DoublePropertyMap< Link > > getLinkCostFeature()
	{
		return linkCostFeature;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public boolean isSuccessful()
	{
		return ok;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	// -- Cancelable methods --

	/** Reason for cancelation, or null if not canceled. */
	protected String cancelReason;

	@Override
	public boolean isCanceled()
	{
		return cancelReason != null;
	}

	/** Cancels the command execution, with the given reason for doing so. */
	@Override
	public void cancel( final String reason )
	{
		if ( reason != null && linker != null )
		{
			cancelReason = reason;
			linker.cancel( reason );
		}
		else
		{
			cancelReason = "";
		}
	}

	@Override
	public String getCancelReason()
	{
		return cancelReason;
	}

	private static final Comparator< Spot > SPOT_COMPARATOR = new Comparator< Spot >()
	{

		@Override
		public int compare( final Spot o1, final Spot o2 )
		{
			return o1.getInternalPoolIndex() - o2.getInternalPoolIndex();
		}
	};

	private static class MyEdgeCreator implements EdgeCreator< Spot, Link >
	{

		private final ModelGraph graph;

		private final Link ref;

		public MyEdgeCreator(final ModelGraph graph)
		{
			this.graph = graph;
			this.ref = graph.edgeRef();
		}

		@Override
		public Link createEdge( final Spot source, final Spot target, final double edgeCost )
		{
			return graph.addEdge( source, target, ref ).init();
		}

		@Override
		public void preAddition()
		{
			graph.getLock().writeLock().lock();
		}

		@Override
		public void postAddition()
		{
			graph.getLock().writeLock().unlock();
		}
	};
}
