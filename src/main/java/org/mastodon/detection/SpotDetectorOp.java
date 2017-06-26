package org.mastodon.detection;

import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.feature.Feature;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;

import bdv.spimdata.SpimDataMinimal;
import net.imagej.ops.special.hybrid.UnaryHybridCF;

public interface SpotDetectorOp extends UnaryHybridCF< SpimDataMinimal, ModelGraph >
{

	/**
	 * Returns the quality feature calculated by this detector.
	 * <p>
	 * The quality feature is defined for all the spots created by the last call
	 * to the detector and only them. By convention, quality values are real
	 * positive <code>double</code>s, with large values indicating the
	 * confidence in the detection result.
	 *
	 * @return the spot quality feature.
	 */
	Feature< Spot, Double, DoublePropertyMap< Spot > > getQualityFeature();


}
