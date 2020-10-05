package org.mastodon.tracking.linking.motionmodel;

import net.imglib2.RealLocalizable;

/**
 * Interface for tracker that implements a specific motion model.
 *
 * @author Jean-Yves Tinevez
 *
 */
public interface MotionModel
{

	/**
	 * Updates this tracker with the specified detection position.
	 *
	 * @param detection
	 *            the new detection position.
	 */
	public void update( RealLocalizable detection );

	/**
	 * Returns the candidate predicted position for the next step in tracking.
	 *
	 * @return the predicted position.
	 */
	public RealLocalizable predict();

	/**
	 * Returns the cost set by this tracker to link the predicted position to
	 * the specified position.
	 *
	 * @param position
	 *            the position to compute to cost to link to.
	 * @return the cost to link to the specified position.
	 */
	public double costTo( RealLocalizable position );

}
