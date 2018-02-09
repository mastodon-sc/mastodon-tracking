package org.mastodon.linking.sequential.lap.costmatrix;

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