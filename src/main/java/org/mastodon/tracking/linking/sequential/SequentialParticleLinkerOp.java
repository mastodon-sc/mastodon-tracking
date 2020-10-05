package org.mastodon.tracking.linking.sequential;

import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.tracking.linking.EdgeCreator;
import org.mastodon.tracking.linking.ParticleLinker;

import net.imagej.ops.special.inplace.BinaryInplace1OnlyOp;
import net.imglib2.RealLocalizable;

/**
 * Base interface for particle-linking algorithms that perform particle-linking
 * frame by frame.
 * <p>
 * The creation of edge is done through the {@link EdgeCreator} only.
 * 
 * @param <V>
 *            the type of vertices.
 */
public interface SequentialParticleLinkerOp< V extends RealLocalizable >
		extends BinaryInplace1OnlyOp< EdgeCreator< V >, SpatioTemporalIndex< V > >, ParticleLinker
{}
