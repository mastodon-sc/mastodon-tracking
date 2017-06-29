package org.mastodon.detection;

import bdv.spimdata.SpimDataMinimal;

public class DetectorKeys
{

	/**
	 * Key for the parameter specifying what setup on the
	 * {@link SpimDataMinimal} to perform detection on. Expected values must be
	 * {@link Integer} larger or equal to 0.
	 */
	public static final String KEY_SETUP_ID = "SETUP";

	/**
	 * Default value for the {@link #KEY_SETUP_ID} parameter.
	 */
	public static final int DEFAULT_SETUP_ID = 0;

	/**
	 * Key for the parameter specifying from what time-point on to perform
	 * tracking, inclusive. Expected values must be {@link Integer}s.
	 */
	public static final String KEY_MIN_TIMEPOINT = "MIN_TIMEPOINT";

	/**
	 * Default value for the {@link #KEY_MIN_TIMEPOINT} parameter.
	 */
	public static final int DEFAULT_MIN_TIMEPOINT = 0;

	/**
	 * Key for the parameter specifying up to what time-point on to perform
	 * tracking, inclusive. Expected values must be {@link Integer}s, larger
	 * than {@link #KEY_MIN_TIMEPOINT}.
	 */
	public static final String KEY_MAX_TIMEPOINT = "MAX_TIMEPOINT";

	/**
	 * Default value for the {@link #KEY_MAX_TIMEPOINT} parameter.
	 */
	public static final int DEFAULT_MAX_TIMEPOINT = 0;

	/**
	 * Key for the parameter specifying what is the expected radius of
	 * particles, in units of the global coordinate system. Expected value must
	 * be {@link Double}s larger than 0.
	 */
	public static final String KEY_RADIUS = "RADIUS";

	/**
	 * Default value for the {@link #KEY_RADIUS} parameter.
	 */
	public static final double DEFAULT_RADIUS = 5;

	/**
	 * Key for the parameter specifying the threshold on quality values above
	 * which to discard detections. Expected values are {@link Double}s, larger
	 * than 0.
	 */
	public static final String KEY_THRESHOLD = "THRESHOLD";

	/**
	 * Default value for the {@link #KEY_THRESHOLD} parameter.
	 */
	public static final double DEFAULT_THRESHOLD = 1000.;

	private DetectorKeys()
	{}
}
