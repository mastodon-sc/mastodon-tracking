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
package org.mastodon.tracking.linking.sequential.lap.costmatrix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.collection.RefSet;
import org.mastodon.tracking.linking.sequential.lap.linker.SparseCostMatrix;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.special.function.AbstractNullaryFunctionOp;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.util.Util;

/**
 * A {@link CostMatrixCreatorOp} that build a cost matrix from 3 lists
 * containing the sources, the targets and the associated costs.
 *
 * @author Jean-Yves Tinevez - 2014
 *
 * @param <K>
 *            the type of row elements.
 * @param <J>
 *            the type of column elements.
 */
@Plugin( type = CostMatrixCreatorOp.class )
public class DefaultCostMatrixCreatorOp< K, J >
		extends AbstractNullaryFunctionOp< SparseCostMatrix >
		implements CostMatrixCreatorOp< K, J >, Benchmark
{

	private static final String BASE_ERROR_MESSAGE = "[DefaultCostMatrixCreatorOp] ";

	@Parameter( type = ItemIO.INPUT )
	private RefList< K > rows;

	@Parameter( type = ItemIO.INPUT )
	private RefList< J > cols;

	@Parameter( type = ItemIO.INPUT )
	private double[] costs;

	@Parameter( type = ItemIO.INPUT )
	private double alternativeCostFactor;

	@Parameter( type = ItemIO.INPUT )
	private double percentile;

	@Parameter( type = ItemIO.INPUT )
	private Comparator< K > rowComparator;

	@Parameter( type = ItemIO.INPUT )
	private Comparator< J > colComparator;

	@Parameter( type = ItemIO.OUTPUT )
	private RefList< K > uniqueRows;

	@Parameter( type = ItemIO.OUTPUT )
	private RefList< J > uniqueCols;

	@Parameter( type = ItemIO.OUTPUT )
	private double alternativeCost;

	private String errorMessage;

	private long processingTime;

	@Override
	public SparseCostMatrix calculate()
	{
		if ( rows == null || rows.isEmpty() )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The row list is null or empty.";
			return null;
		}
		if ( rows.size() != cols.size() )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Row and column lists do not have the same number of elements. Found " + rows.size() + " and " + cols.size() + ".";
			return null;
		}
		if ( rows.size() != costs.length )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Row list and cost array do not have the same number of elements. Found " + rows.size() + " and " + costs.length + ".";
			return null;
		}
		if ( alternativeCostFactor <= 0 )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The alternative cost factor must be greater than 0. Was: " + alternativeCostFactor + ".";
			return null;
		}
		if ( percentile < 0 || percentile > 1 )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The percentile must no be smaller than 0 or greater than 1. Was: " + percentile;
			return null;
		}

		final RefSet< K > tmpSet1 = RefCollections.createRefSet( rows, rows.size() );
		tmpSet1.addAll( rows );
		uniqueRows = RefCollections.createRefList( rows, tmpSet1.size() );
		uniqueRows.addAll( tmpSet1 );
		uniqueRows.sort( rowComparator );

		final RefSet< J > tmpSet2 = RefCollections.createRefSet( cols, cols.size() );
		tmpSet2.addAll( cols );
		uniqueCols = RefCollections.createRefList( cols, tmpSet2.size() );
		uniqueCols.addAll( tmpSet2 );
		uniqueCols.sort( colComparator );

		final List< Assignment > assignments = new ArrayList< Assignment >( costs.length );
		for ( int i = 0; i < costs.length; i++ )
		{
			final K rowObj = rows.get( i );
			final J colObj = cols.get( i );
			final int r = Collections.binarySearch( uniqueRows, rowObj, rowComparator );
			final int c = Collections.binarySearch( uniqueCols, colObj, colComparator );
			assignments.add( new Assignment( r, c, costs[ i ] ) );
		}
		Collections.sort( assignments );

		// Test we do not have duplicates.
		Assignment previousAssgn = assignments.get( 0 );
		for ( int i = 1; i < assignments.size(); i++ )
		{
			final Assignment assgn = assignments.get( i );
			if ( assgn.equals( previousAssgn ) )
			{
				errorMessage = BASE_ERROR_MESSAGE + "Found duplicate assignment at index: " + assgn + ".";
				return null;
			}
			previousAssgn = assgn;
		}

		final int nRows = uniqueRows.size();
		final int nCols = uniqueCols.size();
		final int[] kk = new int[ costs.length ];
		final int[] number = new int[ nRows ];
		final double[] cc = new double[ costs.length ];

		Assignment a = assignments.get( 0 );
		kk[ 0 ] = a.c;
		cc[ 0 ] = a.cost;
		int currentRow = a.r;
		int nOfEl = 0;
		for ( int i = 1; i < assignments.size(); i++ )
		{
			a = assignments.get( i );

			kk[ i ] = a.c;
			cc[ i ] = a.cost;
			nOfEl++;

			if ( a.r != currentRow )
			{
				number[ currentRow ] = nOfEl;
				nOfEl = 0;
				currentRow = a.r;
			}
		}
		number[ currentRow ] = nOfEl + 1;

		final SparseCostMatrix scm = new SparseCostMatrix( cc, kk, number, nCols );
		alternativeCost = computeAlternativeCosts();
		return scm;
	}

	protected double computeAlternativeCosts()
	{
		if ( percentile == 1 ) { return alternativeCostFactor * Util.max( costs ); }
		return alternativeCostFactor * Util.percentile( costs, percentile );
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public RefList< K > getSourceList()
	{
		return uniqueRows;
	}

	@Override
	public RefList< J > getTargetList()
	{
		return uniqueCols;
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

	public final static class Assignment implements Comparable< Assignment >
	{
		private final int r;

		private final int c;

		private final double cost;

		public Assignment( final int r, final int c, final double cost )
		{
			this.r = r;
			this.c = c;
			this.cost = cost;
		}

		@Override
		public int compareTo( final Assignment o )
		{
			if ( r == o.r ) { return c - o.c; }
			return r - o.r;
		}

		@Override
		public boolean equals( final Object obj )
		{
			if ( obj instanceof Assignment )
			{
				final Assignment o = ( Assignment ) obj;
				return r == o.r && c == o.c;
			}
			return false;
		}

		@Override
		public int hashCode()
		{
			int hash = 23;
			hash = hash * 31 + r;
			hash = hash * 31 + c;
			return hash;
		}

		@Override
		public String toString()
		{
			return "Assignment r = " + r + ", c = " + c + ", cost = " + cost;
		}

		public int getC()
		{
			return c;
		}

		public int getR()
		{
			return r;
		}

		public double getCost()
		{
			return cost;
		}
	}
}
