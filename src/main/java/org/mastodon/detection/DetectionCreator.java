package org.mastodon.detection;

public interface DetectionCreator
{

	/**
	 * Creates a detection object from the coordinates specified by the
	 * detector. All coordinates are specified with respect to the global
	 * coordinate system.
	 * 
	 * @param pos
	 *            the position of the detection.
	 * @param radius
	 *            the radius of the detection.
	 * 
	 * @param timepoint
	 *            the time-point of the detection.
	 * @param quality
	 *            the quality of the detection.
	 */
	public void createDetection( double[] pos, double radius, int timepoint, double quality );

	/**
	 * Method called before a batch of detections is added to the output via the
	 * {@link #createDetection(double[], double, int, double)} method.
	 */
	public void preAddition();

	/**
	 * Method called after a batch of detections is added to the output via the
	 * {@link #createDetection(double[], double, int, double)} method.
	 */
	public void postAddition();

}
