package org.mastodon.tracking.mamut.detection;

import java.util.List;

import org.mastodon.mamut.model.ModelGraph;
import org.scijava.Cancelable;
import org.scijava.app.StatusService;
import org.scijava.log.Logger;

import bdv.viewer.SourceAndConverter;
import net.imagej.ops.special.hybrid.UnaryHybridCF;

public interface SpotDetectorOp extends UnaryHybridCF< List< SourceAndConverter< ? > >, ModelGraph >, Cancelable
{

	/**
	 * Returns the quality feature provided by this detector.
	 *
	 * @return the quality feature.
	 */
	public DetectionQualityFeature getQualityFeature();

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
