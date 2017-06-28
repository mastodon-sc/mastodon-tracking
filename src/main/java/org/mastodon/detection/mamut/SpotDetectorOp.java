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
	 * Returns the quality feature provided by this detector.
	 *
	 * @return the quality feature.
	 */
	public Feature< Spot, Double, DoublePropertyMap< Spot > > getQualityFeature();

	/**
	 * Returns <code>true</code> if the particle-linking process completed
	 * successfully. If not, a meaningful error message can be obtained with
	 * {@link #getErrorMessage()}.
	 *
	 * @return <code>true</code> if the particle-linking process completed
	 *         successfully.
	 * @see #getErrorMessage()
	 */
	public boolean wasSuccessful();

	/**
	 * Returns a meaningful error message after the particle-linking process
	 * failed to complete.
	 *
	 * @return an error message.
	 */
	public String getErrorMessage();
}
