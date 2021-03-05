/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2021 Tobias Pietzsch, Jean-Yves Tinevez
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
package org.mastodon.tracking.linking.sequential.lap.costmatrix;

import java.util.Comparator;

import org.mastodon.collection.RefCollection;
import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.tracking.linking.sequential.lap.costfunction.CostFunction;
import org.mastodon.tracking.linking.sequential.lap.linker.SparseCostMatrix;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import gnu.trove.list.array.TDoubleArrayList;
import net.imagej.ops.special.function.AbstractNullaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imglib2.algorithm.Benchmark;

/**
 * A {@link CostMatrixCreatorOp} that can generate a cost matrix from a list of
 * sources, a list of targets and a {@link CostFunction} that can generate a
 * cost for any combination.
 *
 * @author Jean-Yves Tinevez - 2014
 *
 * @param <K>
 *            the type of the source objects.
 * @param <J>
 *            the type of the target objects.
 */
@Plugin( type = CostMatrixCreatorOp.class )
public class JaqamanLinkingCostMatrixCreator< K, J >
		extends AbstractNullaryFunctionOp< SparseCostMatrix >
		implements CostMatrixCreatorOp< K, J >, Benchmark
{

	private static final String BASE_ERROR_MSG = "[JaqamanLinkingCostMatrixCreator] ";

	@Parameter( type = ItemIO.INPUT )
	private  Iterable< K > sources;

	@Parameter( type = ItemIO.INPUT )
	private  Iterable< J > targets;

	@Parameter( type = ItemIO.INPUT )
	private  CostFunction< K, J > costFunction;

	@Parameter( type = ItemIO.INPUT )
	private  double costThreshold;

	@Parameter( type = ItemIO.INPUT )
	private  double alternativeCostFactor;

	@Parameter( type = ItemIO.INPUT )
	private  double percentile;

	@Parameter( type = ItemIO.INPUT )
	private  RefCollection< K > sourcePool;

	@Parameter( type = ItemIO.INPUT )
	private  RefCollection< J > targetPool;

	@Parameter( type = ItemIO.INPUT )
	private  Comparator< K > sourceComparator;

	@Parameter( type = ItemIO.INPUT )
	private  Comparator< J > targetComparator;

	@Parameter( type = ItemIO.OUTPUT)
	private RefList< K > sourceList;

	@Parameter( type = ItemIO.OUTPUT)
	private RefList< J > targetList;

	@Parameter( type = ItemIO.OUTPUT)
	private double alternativeCost;

	private long processingTime;

	private String errorMessage;

	@Override
	public SparseCostMatrix calculate()
	{
		final long start = System.currentTimeMillis();

		if ( null == sources || !sources.iterator().hasNext() )
		{
			errorMessage = BASE_ERROR_MSG + "The source list is empty or null.";
			return null;
		}
		if ( null == targets || !targets.iterator().hasNext() )
		{
			errorMessage = BASE_ERROR_MSG + "The target list is empty or null.";
			return null;
		}

		final RefList< K > accSources = RefCollections.createRefList( sourcePool );
		final RefList< J > accTargets = RefCollections.createRefList( targetPool );
		final TDoubleArrayList costs = new TDoubleArrayList();

		for ( final K source : sources )
		{
			for ( final J target : targets )
			{

				final double cost = costFunction.linkingCost( source, target );
				if ( cost < costThreshold )
				{
					accSources.add( source );
					accTargets.add( target );
					costs.add( cost );
				}
			}
		}
		costs.trimToSize();

		sourceList = RefCollections.createRefList( sourcePool );
		targetList = RefCollections.createRefList( targetPool );

		/*
		 * Check if accepted source or target lists are empty and deal with it.
		 */

		final SparseCostMatrix scm;
		if ( accSources.isEmpty() || accTargets.isEmpty() )
		{

			alternativeCost = Double.NaN;
			scm = new SparseCostMatrix();
			/*
			 * CAREFUL! We return an empty matrix if no acceptable links are found.
			 */
		}
		else
		{

			@SuppressWarnings( "unchecked" )
			final DefaultCostMatrixCreatorOp< K, J > cmCreator = ( DefaultCostMatrixCreatorOp< K, J > ) Functions.nullary( ops(),
					DefaultCostMatrixCreatorOp.class, SparseCostMatrix.class,
					accSources,
					accTargets,
					costs.toArray(),
					alternativeCostFactor,
					percentile,
					sourceComparator,
					targetComparator );
			scm = cmCreator.calculate();
			if ( null == scm)
			{
				errorMessage = cmCreator.getErrorMessage();
				return null;
			}

			sourceList = cmCreator.getSourceList();
			targetList = cmCreator.getTargetList();
			alternativeCost = cmCreator.computeAlternativeCosts();
		}

		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return scm;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public RefList< K > getSourceList()
	{
		return sourceList;
	}

	@Override
	public RefList< J > getTargetList()
	{
		return targetList;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public double getAlternativeCostForSource( final K source )
	{
		return alternativeCost;
	}

	@Override
	public double getAlternativeCostForTarget( final J target )
	{
		return alternativeCost;
	}
}
