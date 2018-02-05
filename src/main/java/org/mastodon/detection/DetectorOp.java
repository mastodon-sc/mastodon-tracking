package org.mastodon.detection;

import org.mastodon.HasErrorMessage;
import org.mastodon.graph.Graph;
import org.mastodon.graph.Vertex;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.feature.Feature;
import org.scijava.Cancelable;

import bdv.spimdata.SpimDataMinimal;
import net.imagej.ops.special.inplace.BinaryInplace1OnlyOp;

public interface DetectorOp< V extends Vertex< ? > > extends BinaryInplace1OnlyOp< Graph< V, ? >, SpimDataMinimal >, Cancelable, HasErrorMessage
{

	/**
	 * Returns the quality feature calculated by this detector.
	 * <p>
	 * The quality feature is defined for all vertices created by the last call
	 * to the detector and only them. By convention, quality values are real
	 * positive <code>double</code>s, with large values indicating higher
	 * confidence in the detection result.
	 *
	 * @return the spot quality feature.
	 */
	public Feature< V, DoublePropertyMap< V > > getQualityFeature();
}
