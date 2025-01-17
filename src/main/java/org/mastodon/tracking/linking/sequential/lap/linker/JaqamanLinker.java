/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2025 Tobias Pietzsch, Jean-Yves Tinevez
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
package org.mastodon.tracking.linking.sequential.lap.linker;

import java.util.Arrays;

import org.mastodon.Ref;
import org.mastodon.collection.RefCollection;
import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefDoubleMap;
import org.mastodon.collection.RefList;
import org.mastodon.collection.RefMaps;
import org.mastodon.collection.RefRefMap;
import org.mastodon.collection.RefSet;
import org.mastodon.tracking.linking.sequential.lap.costmatrix.CostMatrixCreatorOp;

import net.imglib2.algorithm.BenchmarkAlgorithm;
import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.util.Util;

/**
 * Links two lists of objects based on the LAP framework described in Jaqaman
 * <i>et al.</i>, Nature Methods, <b>2008</b>.
 *
 * @author Jean-Yves Tinevez - 2014
 *
 * @param <K>
 *            the type of the source objects to link.
 * @param <J>
 *            the type of the target objects to link.
 */
public class JaqamanLinker< K, J > extends BenchmarkAlgorithm implements OutputAlgorithm< RefRefMap< K, J > >
{
	private RefRefMap< K, J > assignments;

	private RefDoubleMap< K > costs;

	private final CostMatrixCreatorOp< K, J > costMatrixCreator;

	private final RefCollection< K > keyPool;

	private final RefCollection< J > valuePool;

	/**
	 * Creates a new linker for the specified cost matrix creator. See Jaqaman
	 * <i>et al.</i>, Nature Methods, <b>2008</b>, Figure 1b.
	 *
	 * @param costMatrixCreator
	 *            the class in charge of creating linking costs.
	 * @param keyPool
	 *            a {@link RefCollection} of keys in the linker. This instance
	 *            might be used to return optimized collections in the case
	 *            where keys are {@link Ref} objects.
	 * @param valuePool
	 *            a {@link RefCollection} of values in the linker. This instance
	 *            might be used to return optimized collections in the case
	 *            where values are {@link Ref} objects.
	 */
	public JaqamanLinker( final CostMatrixCreatorOp< K, J > costMatrixCreator, final RefCollection< K > keyPool, final RefCollection< J > valuePool )
	{
		this.costMatrixCreator = costMatrixCreator;
		this.keyPool = keyPool;
		this.valuePool = valuePool;
	}

	/**
	 * Returns the resulting assignments from this algorithm.
	 * <p>
	 * It takes the shape of a map, such that if <code>source</code> is a key of
	 * the map, it is assigned to <code>target = map.get(source)</code>.
	 *
	 * @return the assignment map.
	 * @see #getAssignmentCosts()
	 */
	@Override
	public RefRefMap< K, J > getResult()
	{
		return assignments;
	}

	/**
	 * Returns the costs associated to the assignment results.
	 * <p>
	 * It takes the shape of a map, such that if <code>source</code> is a key of
	 * the map, its assignment as a cost <code>cost = map.get(source)</code>.
	 *
	 * @return the assignment costs.
	 * @see #getResult()
	 */
	public RefDoubleMap< K > getAssignmentCosts()
	{
		return costs;
	}

	@Override
	public boolean checkInput()
	{
		return true;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		/*
		 * Generate the cost matrix
		 */

		final SparseCostMatrix tl = costMatrixCreator.calculate();
		if ( null == tl )
		{
			errorMessage = costMatrixCreator.getErrorMessage();
			return false;
		}
		final RefList< K > matrixRows = costMatrixCreator.getSourceList();
		final RefList< J > matrixCols = costMatrixCreator.getTargetList();

		assignments = RefMaps.createRefRefMap( keyPool, valuePool );
		costs = RefMaps.createRefDoubleMap( keyPool, Double.NaN );

		if ( matrixCols.isEmpty() || matrixRows.isEmpty() )
		{
			final long end = System.currentTimeMillis();
			processingTime = end - start;
			return true;
		}

		/*
		 * Complement the cost matrix with alternative no linking cost matrix.
		 */

		final int nCols = tl.getNCols();
		final int nRows = tl.getNRows();

		/*
		 * Top right
		 */

		final double[] cctr = new double[ nRows ];
		final int[] kktr = new int[ nRows ];
		for ( int i = 0; i < nRows; i++ )
		{
			kktr[ i ] = i;
			cctr[ i ] = costMatrixCreator.getAlternativeCostForSource( matrixRows.get( i ) );
		}
		final int[] numbertr = new int[ nRows ];
		Arrays.fill( numbertr, 1 );
		final SparseCostMatrix tr = new SparseCostMatrix( cctr, kktr, numbertr, nRows );

		/*
		 * Bottom left
		 */
		final double[] ccbl = new double[ nCols ];
		final int[] kkbl = new int[ nCols ];
		for ( int i = 0; i < kkbl.length; i++ )
		{
			kkbl[ i ] = i;
			ccbl[ i ] = costMatrixCreator.getAlternativeCostForTarget( matrixCols.get( i ) );
		}
		final int[] numberbl = new int[ nCols ];
		Arrays.fill( numberbl, 1 );
		final SparseCostMatrix bl = new SparseCostMatrix( ccbl, kkbl, numberbl, nCols );

		/*
		 * Bottom right.
		 *
		 * Alt. cost is the overall min of alternative costs. This deviate or
		 * extend a bit the u-track code.
		 */
		final double minCost = Math.min( Util.min( ccbl ), Util.min( cctr ) );
		final SparseCostMatrix br = tl.transpose();
		br.fillWith( minCost );

		/*
		 * Stitch them together
		 */
		final SparseCostMatrix full = ( tl.hcat( tr ) ).vcat( bl.hcat( br ) );

		/*
		 * Solve the full cost matrix.
		 */
		final LAPJV solver = new LAPJV( full );
		if ( !solver.checkInput() || !solver.process() )
		{
			errorMessage = solver.getErrorMessage();
			return false;
		}

		final int[] assgn = solver.getResult();
		for ( int i = 0; i < assgn.length; i++ )
		{
			final int j = assgn[ i ];
			if ( i < matrixRows.size() && j < matrixCols.size() )
			{
				final K source = matrixRows.get( i );
				final J target = matrixCols.get( j );
				assignments.put( source, target );

				final double cost = full.get( i, j, Double.POSITIVE_INFINITY );
				costs.put( source, Double.valueOf( cost ) );
			}
		}

		final long end = System.currentTimeMillis();
		processingTime = end - start;

		return true;
	}

	public String resultToString()
	{
		if ( null == assignments ) { return "Not solved yet. Process the algorithm prior to calling this method."; }

		final RefSet< K > unassignedSources = RefCollections.createRefSet( keyPool );
		unassignedSources.addAll( costMatrixCreator.getSourceList() );

		final RefSet< J > unassignedTargets = RefCollections.createRefSet( valuePool );
		unassignedTargets.addAll( costMatrixCreator.getTargetList() );

		int sw = -1;
		for ( final K source : unassignedSources )
			if ( source.toString().length() > sw )
				sw = source.toString().length();
		sw = sw + 1;

		int tw = -1;
		for ( final J target : unassignedTargets )
			if ( target.toString().length() > tw )
				tw = target.toString().length();
		tw = tw + 1;

		int cw = 0;
		for ( final K source : assignments.keySet() )
		{
			final double cost = costs.get( source );
			if ( Math.log10( cost ) > cw )
				cw = ( int ) Math.log10( cost );
		}
		cw = cw + 1;

		final StringBuilder str = new StringBuilder();
		str.append( "Found " + assignments.size() + " assignments:\n" );
		final J jref = valuePool.createRef();
		for ( final K source : assignments.keySet() )
		{
			final J target = assignments.get( source, jref );

			unassignedSources.remove( source );
			unassignedTargets.remove( target );

			final double cost = costs.get( source );
			str.append( String.format( "%1$-" + sw + "s → %2$" + tw + "s, cost = %3$" + cw + ".1f\n", source.toString(), target.toString(), cost ) );
		}
		valuePool.releaseRef( jref );

		if ( !unassignedSources.isEmpty() )
		{
			str.append( "Found " + unassignedSources.size() + " unassigned sources:\n" );
			for ( final K us : unassignedSources )
				str.append( String.format( "%1$-" + sw + "s → %2$" + tw + "s\n", us.toString(), 'ø' ) );
		}

		if ( !unassignedTargets.isEmpty() )
		{
			str.append( "Found " + unassignedTargets.size() + " unassigned targets:\n" );
			for ( final J ut : unassignedTargets )
				str.append( String.format( "%1$-" + sw + "s → %2$" + tw + "s\n", 'ø', ut.toString() ) );
		}

		return str.toString();
	}
}
