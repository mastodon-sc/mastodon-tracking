/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2024 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.tracking.mamut.linking;

import java.util.Comparator;
import java.util.Map;

import org.mastodon.feature.FeatureModel;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.tracking.linking.EdgeCreator;
import org.mastodon.tracking.linking.ParticleLinker;
import org.mastodon.tracking.linking.graph.GraphParticleLinkerOp;
import org.mastodon.tracking.linking.sequential.SequentialParticleLinkerOp;
import org.scijava.Cancelable;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;

import net.imagej.ops.special.inplace.AbstractBinaryInplace1Op;
import net.imagej.ops.special.inplace.Inplaces;
import net.imglib2.algorithm.Benchmark;

public abstract class AbstractSpotLinkerOp
		extends AbstractBinaryInplace1Op< ModelGraph, SpatioTemporalIndex< Spot > >
		implements SpotLinkerOp, Benchmark
{

	@Parameter
	protected StatusService statusService;

	@Parameter( type = ItemIO.INPUT )
	protected Map< String, Object > settings;

	@Parameter( type = ItemIO.INPUT )
	protected FeatureModel featureModel;

	/**
	 * The edge linking cost feature provided by this particle-linker.
	 */
	@Parameter( type = ItemIO.BOTH, required = false )
	protected LinkCostFeature linkCostFeature;

	@Parameter( required = false )
	protected Logger logger;

	protected long processingTime;

	protected boolean ok;

	protected String errorMessage;

	protected Cancelable cancelable;

	protected void exec( final ModelGraph graph, final SpatioTemporalIndex< Spot > spots, final Class< ? extends ParticleLinker > cl )
	{
		ok = false;
		if ( null == linkCostFeature )
			linkCostFeature = new LinkCostFeature( graph.edges().getRefPool() );

		final long start = System.currentTimeMillis();

		final EdgeCreator< Spot > edgeCreator = edgeCreator( graph );

		if ( GraphParticleLinkerOp.class.isAssignableFrom( cl ) )
		{
			@SuppressWarnings( "rawtypes" )
			final Class< ? extends GraphParticleLinkerOp > gploCl = cl.asSubclass( GraphParticleLinkerOp.class );
			@SuppressWarnings( { "rawtypes", "unchecked" } )
			final GraphParticleLinkerOp< Spot, Link > linker = ( GraphParticleLinkerOp ) Inplaces.binary1( ops(), gploCl,
					graph, spots,
					settings, featureModel,
					spotComparator(), edgeCreator );
			linker.setLogger( logger );
			linker.setStatusService( statusService );
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

	protected EdgeCreator< Spot > edgeCreator( final ModelGraph graph )
	{
		return new MyEdgeCreator( graph, linkCostFeature );
	}

	protected Comparator< Spot > spotComparator()
	{
		return SPOT_COMPARATOR;
	}

	@Override
	public LinkCostFeature getLinkCostFeature()
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

	@Override
	public void setLogger( final Logger logger )
	{
		this.logger = logger;
	}

	@Override
	public void setStatusService( final StatusService statusService )
	{
		this.statusService = statusService;
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

		private final LinkCostFeature linkCostFeature;

		public MyEdgeCreator( final ModelGraph graph, final LinkCostFeature linkCostFeature )
		{
			this.graph = graph;
			this.linkCostFeature = linkCostFeature;
			this.ref = graph.edgeRef();
		}

		@Override
		public void createEdge( final Spot source, final Spot target, final double edgeCost )
		{
			final Link link = graph.addEdge( source, target, ref ).init();
			linkCostFeature.set( link, edgeCost );
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
