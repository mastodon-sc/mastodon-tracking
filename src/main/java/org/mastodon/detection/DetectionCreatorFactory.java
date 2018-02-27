package org.mastodon.detection;

public interface DetectionCreatorFactory
{

	/**
	 * Creates a {@link DetectionCreator} for the specified time-point.
	 *
	 * @param timepoint
	 *            the time-point in which the detection are created.
	 * @return a new {@link DetectionCreator} instance.
	 */
	public DetectionCreator create( int timepoint );

	/**
	 * Interface for classes that can add the detections of a single time-point
	 * to some data structure.
	 */
	public static interface DetectionCreator
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
		 * @param quality
		 *            the quality of the detection.
		 */
		public void createDetection( double[] pos, double radius, double quality );

		/**
		 * Method called before a batch of detections is added to the output via
		 * the {@link #createDetection(double[], double, double)} method.
		 */
		public void preAddition();

		/**
		 * Method called after a batch of detections is added to the output via
		 * the {@link #createDetection(double[], double, double)} method.
		 */
		public void postAddition();
	}

}
