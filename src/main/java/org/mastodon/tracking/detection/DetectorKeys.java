/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2025 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.tracking.detection;

import bdv.spimdata.SpimDataMinimal;
import net.imglib2.Interval;

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
	 * Key for the parameter specifying whether to detect maxima or minima.
	 * Expected values are {@link String}s defining a behavior that can be
	 * interpreted by the detector implementation (e.g., "MAXIMA", "MINIMA").
	 */
	public static final String KEY_DETECTION_TYPE = "DETECTION_TYPE";

	/**
	 * Default value for the {@link #DEFAULT_DETECTION_TYPE} parameter.
	 */
	public static final String DEFAULT_DETECTION_TYPE = null;

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

	/**
	 * Key for the parameter specifying what portion of the source image to
	 * process, specified as {@link Interval}. A value of <code>null</code>
	 * indicates that the whole image is to be processed.
	 */
	public static final String KEY_ROI = "ROI";

	/**
	 * Default value for the {@link #KEY_ROI} parameter.
	 */
	public static final Interval DEFAULT_ROI = null;

	/**
	 * Key for the parameter specifying what to do when adding a detection to a
	 * model that contains an existing detection in the vicinity of the new one.
	 * Expected values are {@link String}s defining a behavior that can be
	 * interpreted by the detector implementation. A <code>null</code> value is
	 * acceptable and will results in picking a default behavior.
	 */
	public static final String KEY_ADD_BEHAVIOR = "ADD_BEHAVIOR";

	/**
	 * Default value for the {@link #KEY_ADD_BEHAVIOR} parameter.
	 */
	public static final String DEFAULT_ADD_BEHAVIOR = null;

	private DetectorKeys()
	{}
}
