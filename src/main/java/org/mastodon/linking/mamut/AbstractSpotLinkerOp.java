package org.mastodon.linking.mamut;

import java.util.Comparator;
import java.util.Map;

import org.mastodon.linking.EdgeCreator;
import org.mastodon.linking.LinkingUtils;
import org.mastodon.linking.ParticleLinker;
import org.mastodon.linking.graph.GraphParticleLinkerOp;
import org.mastodon.linking.sequential.SequentialParticleLinkerOp;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.feature.FeatureModel;
import org.mastodon.revised.model.feature.WritableDoubleScalarFeature;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.scijava.Cancelable;
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

	protected Cancelable cancelable;

	protected void exec( final ModelGraph graph, final SpatioTemporalIndex< Spot > spots, final Class< ? extends ParticleLinker > cl )
	{
		ok = false;
		final long start = System.currentTimeMillis();

		final DoublePropertyMap< Link > pm = new DoublePropertyMap<>( graph.edges(), Double.NaN );
		linkCostFeature = LinkingUtils.getLinkCostFeature( pm, Link.class );
		final EdgeCreator< Spot > edgeCreator = edgeCreator( graph, pm );

		if ( GraphParticleLinkerOp.class.isAssignableFrom( cl ) )
		{
			@SuppressWarnings( "rawtypes" )
			final Class< ? extends GraphParticleLinkerOp > gploCl = cl.asSubclass( GraphParticleLinkerOp.class );
			@SuppressWarnings( { "rawtypes", "unchecked" } )
			final GraphParticleLinkerOp< Spot, Link > linker = ( GraphParticleLinkerOp ) Inplaces.binary1( ops(), gploCl,
					graph, spots,
					settings, featureModel,
					spotComparator(), edgeCreator );
			this.cancelable = linker;
			linker.mutate1( graph, spots );

			ok = linker.isSuccessful();
			errorMessage = linker.getErrorMessage();
		}
		else if ( SequentialParticleLinkerOp.class.isAssignableFrom( cl ) )
		{
			@SuppressWarnings( "rawtypes" )
			final Class< ? extends SequentialParticleLinkerOp > sploCl = cl.asSubclass( SequentialParticleLinkerOp.class );
			@SuppressWarnings( { "rawtypes", "unchecked" } )
			final SequentialParticleLinkerOp< Spot > linker = ( SequentialParticleLinkerOp ) Inplaces.binary1( ops(), sploCl,
					edgeCreator, spots,
					settings, featureModel,
					spotComparator(), graph.vertices() );
			this.cancelable = linker;
			linker.mutate1( edgeCreator, spots );

			ok = linker.isSuccessful();
			errorMessage = linker.getErrorMessage();
		}
		else
		{
			errorMessage = "[AbstractSpotLinkerOp] Could not find a suitable parent class for linker " + cl;
			return;
		}

		final long end = System.currentTimeMillis();
		processingTime = end - start;
		cancelable = null;
	}

	protected EdgeCreator< Spot > edgeCreator( final ModelGraph graph, final DoublePropertyMap< Link > pm )
	{
		return new MyEdgeCreator( graph, pm );
	}

	protected Comparator< Spot > spotComparator()
	{
		return SPOT_COMPARATOR;
	}

	/**
	 * The edge linking cost feature provided by this particle-linker.
	 */
	@Parameter( type = ItemIO.OUTPUT )
	protected WritableDoubleScalarFeature< Link > linkCostFeature;

	@Override
	public WritableDoubleScalarFeature< Link > getLinkCostFeature()
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
		if ( reason != null && cancelable != null )
		{
			cancelReason = reason;
			cancelable.cancel( reason );
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

	private static class MyEdgeCreator implements EdgeCreator< Spot >
	{

		private final ModelGraph graph;

		private final Link ref;

		private final DoublePropertyMap< Link > pm;

		public MyEdgeCreator( final ModelGraph graph, final DoublePropertyMap< Link > pm )
		{
			this.graph = graph;
			this.pm = pm;
			this.ref = graph.edgeRef();
		}

		@Override
		public void createEdge( final Spot source, final Spot target, final double edgeCost )
		{
			final Link link = graph.addEdge( source, target, ref ).init();
			pm.set( link, edgeCost );
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
