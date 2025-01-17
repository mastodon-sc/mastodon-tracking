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
import java.util.Set;

import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.collection.RefSet;
import org.mastodon.graph.Edge;
import org.mastodon.graph.ReadOnlyGraph;
import org.mastodon.graph.Vertex;
import org.mastodon.graph.algorithm.ConnectedComponents;
import org.mastodon.spatial.HasTimepoint;

public class GraphSegmentSplitter< V extends Vertex< E > & HasTimepoint, E extends Edge< V > >
{
	private static final int MINIMAL_SIZE = 2;

	private final Comparator< V > timepointComparator = new Comparator< V >()
	{
		@Override
		public int compare( final V v1, final V v2 )
		{
			return v1.getTimepoint() - v2.getTimepoint();
		}
	};

	private final RefList< V > segmentStarts;

	private final RefList< V > segmentEnds;

	private final List< RefList< V > > segmentMiddles;

	public GraphSegmentSplitter( final ReadOnlyGraph< V, E > graph, final boolean findMiddlePoints )
	{
		final ConnectedComponents< V, E > ccInspector = new ConnectedComponents<>( graph, MINIMAL_SIZE );
		final Set< RefSet< V > > ccs = ccInspector.get();

		this.segmentStarts = RefCollections.createRefList( graph.vertices(), ccs.size() );
		this.segmentEnds = RefCollections.createRefList( graph.vertices(), ccs.size() );
		this.segmentMiddles = findMiddlePoints ? new ArrayList<>( ccs.size() ) : Collections.emptyList();

		for ( final RefSet< V > cc : ccs )
		{
			final RefList< V > list = RefCollections.createRefList( graph.vertices() );
			list.addAll( cc );
			list.sort( timepointComparator );

			segmentEnds.add( list.remove( list.size()-1 ) );
			segmentStarts.add( list.remove( 0 ) );
			if ( findMiddlePoints )
				segmentMiddles.add( list );
		}

	}

	public RefList< V > getSegmentEnds()
	{
		return segmentEnds;
	}

	public List< RefList< V > > getSegmentMiddles()
	{
		return segmentMiddles;
	}

	public RefList< V > getSegmentStarts()
	{
		return segmentStarts;
	}

}
