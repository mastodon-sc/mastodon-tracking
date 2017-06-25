package org.mastodon.tracking.lap.costmatrix;

import java.util.Comparator;

import org.mastodon.collection.RefCollection;
import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.tracking.lap.costfunction.CostFunction;
import org.mastodon.tracking.lap.linker.SparseCostMatrix;

import gnu.trove.list.array.TDoubleArrayList;

/**
 * A {@link CostMatrixCreator} that can generate a cost matrix from a list of
 * sources, a list of targets and a {@link CostFunction} that can generate a
 * cost for any combination.
 *
 * @author Jean-Yves Tinevez - 2014
 *
 * @param <K>
 * @param <J>
 */
public class JaqamanLinkingCostMatrixCreator< K, J > implements CostMatrixCreator< K, J >
{

	private static final String BASE_ERROR_MSG = "[JaqamanLinkingCostMatrixCreator] ";

	private final Iterable< K > sources;

	private final Iterable< J > targets;

	private final CostFunction< K, J > costFunction;

	private SparseCostMatrix scm;

	private long processingTime;

	private String errorMessage;

	private final double costThreshold;

	private RefList< K > sourceList;

	private RefList< J > targetList;

	private double alternativeCost;

	private final double alternativeCostFactor;

	private final double percentile;

	private final RefCollection< K > sourcePool;

	private final RefCollection< J > targetPool;

	private final Comparator< K > sourceComparator;

	private final Comparator< J > targetComparator;

	public JaqamanLinkingCostMatrixCreator(
			final RefCollection< K > sources,
			final RefCollection< J > targets,
			final CostFunction< K, J > costFunction,
			final double costThreshold,
			final double alternativeCostFactor,
			final double percentile,
			final Comparator< K > sourceComparator,
			final Comparator< J > targetComparator )
	{
		this( sources, targets, costFunction, costThreshold, alternativeCostFactor, percentile, sources, targets, sourceComparator, targetComparator );
	}

	public JaqamanLinkingCostMatrixCreator( final Iterable< K > sources, final Iterable< J > targets, final CostFunction< K, J > costFunction, final double costThreshold, final double alternativeCostFactor, final double percentile, final RefCollection< K > sourcePool, final RefCollection< J > targetPool, final Comparator< K > sourceComparator, final Comparator< J > targetComparator )
	{
		this.sources = sources;
		this.targets = targets;
		this.costFunction = costFunction;
		this.costThreshold = costThreshold;
		this.alternativeCostFactor = alternativeCostFactor;
		this.percentile = percentile;
		this.sourcePool = sourcePool;
		this.targetPool = targetPool;
		this.sourceComparator = sourceComparator;
		this.targetComparator = targetComparator;
	}

	@Override
	public boolean checkInput()
	{
		if ( null == sources || !sources.iterator().hasNext() )
		{
			errorMessage = BASE_ERROR_MSG + "The source list is empty or null.";
			return false;
		}
		if ( null == targets || !targets.iterator().hasNext() )
		{
			errorMessage = BASE_ERROR_MSG + "The target list is empty or null.";
			return false;
		}
		return true;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

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

		if ( accSources.isEmpty() || accTargets.isEmpty() )
		{

			alternativeCost = Double.NaN;
			scm = null;
			/*
			 * CAREFUL! We return null if no acceptable links are found.
			 */
		}
		else
		{

			final DefaultCostMatrixCreator< K, J > cmCreator = new DefaultCostMatrixCreator< K, J >(
					accSources,
					accTargets,
					costs.toArray(),
					alternativeCostFactor,
					percentile,
					sourceComparator,
					targetComparator );
			if ( !cmCreator.checkInput() || !cmCreator.process() )
			{
				errorMessage = cmCreator.getErrorMessage();
				return false;
			}

			scm = cmCreator.getResult();
			sourceList = cmCreator.getSourceList();
			targetList = cmCreator.getTargetList();
			alternativeCost = cmCreator.computeAlternativeCosts();
		}


		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	/**
	 * Returns the cost matrix generated.
	 * <p>
	 * Careful, it can be <code>null</code> if not acceptable costs have been
	 * found for the specified configuration. In that case, the lists returned
	 * by {@link #getSourceList()} and {@link #getTargetList()} are empty.
	 *
	 * @return a new {@link SparseCostMatrix} or <code>null</code>.
	 */
	@Override
	public SparseCostMatrix getResult()
	{
		return scm;
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
