package org.mastodon.detection.mamut;

import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.feature.Feature;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;

import bdv.spimdata.SpimDataMinimal;
import net.imagej.ops.special.hybrid.UnaryHybridCF;

public interface SpotDetectorOp extends UnaryHybridCF< SpimDataMinimal, ModelGraph >
{

	/**
	 * The quality feature provided by this detector.
	 */
	public Feature< Spot, Double, DoublePropertyMap< Spot > > getQualityFeature();
}
