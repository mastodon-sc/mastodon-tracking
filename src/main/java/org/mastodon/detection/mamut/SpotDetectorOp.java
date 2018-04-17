package org.mastodon.detection.mamut;

import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.feature.Feature;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
import org.scijava.Cancelable;
import org.scijava.app.StatusService;
import org.scijava.log.Logger;

import bdv.spimdata.SpimDataMinimal;
import net.imagej.ops.special.hybrid.UnaryHybridCF;

public interface SpotDetectorOp extends UnaryHybridCF< SpimDataMinimal, ModelGraph >, Cancelable
{

	/**
	 * Returns the quality feature provided by this detector.
	 *
	 * @return the quality feature.
	 */
	public Feature< Spot, DoublePropertyMap< Spot > > getQualityFeature();

	/**
	 * Returns <code>true</code> if the detection process completed
	 * successfully. If not, a meaningful error message can be obtained with
	 * {@link #getErrorMessage()}.
	 *
	 * @return <code>true</code> if the particle-linking process completed
	 *         successfully.
	 * @see #getErrorMessage()
	 */
	public boolean isSuccessful();

	/**
	 * Returns a meaningful error message after the detection process
	 * failed to complete.
	 *
	 * @return an error message.
	 */
	public String getErrorMessage();

	public void setStatusService( StatusService statusService );

	public void setLogger( Logger logger );
}
