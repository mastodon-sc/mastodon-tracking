package org.mastodon.linking.graph;

import org.mastodon.graph.Edge;
import org.mastodon.graph.ReadOnlyGraph;
import org.mastodon.graph.Vertex;
import org.mastodon.linking.EdgeCreator;
import org.mastodon.linking.ParticleLinker;
import org.mastodon.spatial.SpatioTemporalIndex;

import net.imagej.ops.special.inplace.BinaryInplace1OnlyOp;
import net.imglib2.RealLocalizable;

/**
 * Base interface for particle-linking algorithms that require a graph to operate.
 * <p>
 * Note that the input class is that of a {@link ReadOnlyGraph}. Adding edges to the
 * graph is still done via a {@link EdgeCreator} passed as extra input.
 */
public interface GraphParticleLinkerOp< V extends Vertex< E > & RealLocalizable, E extends Edge< V > >
		extends BinaryInplace1OnlyOp< ReadOnlyGraph< V, E >, SpatioTemporalIndex< V > >, ParticleLinker
{}