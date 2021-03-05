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
package org.mastodon.spatial;

import java.util.Arrays;
import java.util.Iterator;
import java.util.PrimitiveIterator.OfInt;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.mastodon.RefPool;
import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.graph.Edge;
import org.mastodon.graph.GraphListener;
import org.mastodon.graph.ListenableReadOnlyGraph;
import org.mastodon.graph.Vertex;
import org.mastodon.graph.algorithm.Assigner;
import org.mastodon.model.SelectionModel;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.imglib2.RealLocalizable;

/**
 * Wraps a {@link SelectionModel} and exposes it as a
 * {@link SpatioTemporalIndex}.
 *
 * TODO: Merge with {@link SpatioTemporalIndexImp}?
 *
 * @author Jean-Yves Tinevez
 *
 * @param <V>
 *            the type of vertices in the graph.
 * @param <E>
 *            the type of edges in the graph.
 */
public class SpatioTemporalIndexSelection< V extends Vertex< E > & RealLocalizable & HasTimepoint, E extends Edge< V > >
		implements GraphListener< V, E >, VertexPositionListener< V >, SpatioTemporalIndex< V >
{
	private final static int NO_ENTRY_KEY = -1;

	private final ListenableReadOnlyGraph< V, E > graph;

	private final TIntObjectHashMap< SpatialIndexImp< V > > timepointToSpatialIndex;

	private final Lock readLock;

	private final Lock writeLock;

	private final SelectionModel< V, E > selectionModel;

	private final RefPool< V > vertexPool;

	public SpatioTemporalIndexSelection( final ListenableReadOnlyGraph< V, E > graph, final SelectionModel< V, E > selectionModel, final RefPool< V > vertexPool )
	{
		this.graph = graph;
		this.selectionModel = selectionModel;
		this.vertexPool = vertexPool;
		timepointToSpatialIndex = new TIntObjectHashMap<>( 10, 0.5f, NO_ENTRY_KEY );
		graph.addGraphListener( this );
		if ( graph instanceof VertexPositionChangeProvider )
		{
			@SuppressWarnings( "unchecked" )
			final VertexPositionChangeProvider< V > p = ( VertexPositionChangeProvider< V > ) graph;
			p.addVertexPositionListener( this );
		}
		final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
		readLock = rwl.readLock();
		writeLock = rwl.writeLock();
		init();
	}

	private void init()
	{
		final TIntObjectHashMap< RefList< V > > timepointToVertices = new TIntObjectHashMap<>( 10, 0.5f, NO_ENTRY_KEY );
		for ( final V v : selectionModel.getSelectedVertices() )
		{
			RefList< V > vs = timepointToVertices.get( v.getTimepoint() );
			if ( vs == null )
			{
				vs = RefCollections.createRefList( graph.vertices() );
				timepointToVertices.put( v.getTimepoint(), vs );
			}
			vs.add( v );
		}

		final TIntObjectIterator< RefList< V > > i = timepointToVertices.iterator();
		while ( i.hasNext() )
		{
			i.advance();
			final int timepoint = i.key();
			final SpatialIndexImp< V > data = new SpatialIndexImp<>( i.value(), vertexPool );
			timepointToSpatialIndex.put( timepoint, data );
		}
	}

	@Override
	public Lock readLock()
	{
		return readLock;
	}

	@Override
	public Iterator< V > iterator()
	{
		return new SpatioTemporalIndexIterator();
	}

	@Override
	public SpatialIndex< V > getSpatialIndex( final int timepoint )
	{
		return getSpatialIndexImp( timepoint );
	}

	@Override
	public SpatialIndex< V > getSpatialIndex( final int fromTimepoint, final int toTimepoint )
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "not implemented yet" );
	}

	@Override
	public void vertexAdded( final V vertex )
	{
		if ( !selectionModel.getSelectedVertices().contains( vertex ) )
			return;

		writeLock.lock();
		try
		{
			getSpatialIndexImp( vertex.getTimepoint() ).add( vertex );
		}
		finally
		{
			writeLock.unlock();
		}
	}

	@Override
	public void vertexRemoved( final V vertex )
	{
		if ( !selectionModel.getSelectedVertices().contains( vertex ) )
			return;

		writeLock.lock();
		try
		{
			final SpatialIndexImp< V > index = timepointToSpatialIndex.get( vertex.getTimepoint() );
			index.remove( vertex );
		}
		finally
		{
			writeLock.unlock();
		}
	}

	@Override
	public void edgeAdded( final E edge )
	{}

	@Override
	public void edgeRemoved( final E edge )
	{}

	@Override
	public void graphRebuilt()
	{
		writeLock.lock();
		try
		{
			init();
		}
		finally
		{
			writeLock.unlock();
		}
	}

	@Override
	public void vertexPositionChanged( final V vertex )
	{
		vertexAdded( vertex );
	}

	private SpatialIndexImp< V > getSpatialIndexImp( final int timepoint )
	{
		SpatialIndexImp< V > index = timepointToSpatialIndex.get( timepoint );
		if ( index == null )
		{
			index = new SpatialIndexImp<>( RefCollections.createRefSet( graph.vertices() ), vertexPool );
			timepointToSpatialIndex.put( timepoint, index );
		}
		return index;
	}

	private class SpatioTemporalIndexIterator implements Iterator< V >
	{

		private final OfInt timeIterator;

		private Iterator< V > valueIterator;

		private boolean hasNext;

		private final Assigner< V > assigner;

		private V next;

		private V current;

		public SpatioTemporalIndexIterator()
		{
			final int[] keys = timepointToSpatialIndex.keys();
			Arrays.sort( keys );
			this.timeIterator = Arrays.stream( keys ).iterator();
			this.current = vertexPool.createRef();
			this.assigner = Assigner.getFor( current );
			hasNext = timeIterator.hasNext();
			if ( hasNext )
			{
				valueIterator = timepointToSpatialIndex.get( timeIterator.nextInt() ).iterator();
				prefetch();
			}
		}

		private void prefetch()
		{
			if ( valueIterator.hasNext() )
			{
				next = valueIterator.next();
				return;
			}
			if ( timeIterator.hasNext() )
			{
				valueIterator = timepointToSpatialIndex.get( timeIterator.nextInt() ).iterator();
				prefetch();
				return;
			}
			hasNext = false;
			next = null;
		}

		@Override
		public boolean hasNext()
		{
			return hasNext;
		}

		@Override
		public V next()
		{
			current = assigner.assign( next, current );
			prefetch();
			return current;
		}
	}
}
