/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2024 Tobias Pietzsch, Jean-Yves Tinevez
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
package org.mastodon.tracking.linking;

import java.util.HashMap;
import java.util.Map;

import org.mastodon.feature.FeatureProjectionKey;
import org.mastodon.tracking.detection.DetectorKeys;

public class LinkerKeys
{
	/**
	 * Key for the parameter specifying the maximal linking distance. The
	 * expected value must be a Double and should be expressed in physical
	 * units.
	 */
	public static final String KEY_LINKING_MAX_DISTANCE = "LINKING_MAX_DISTANCE";

	/** A default value for the {@value #KEY_LINKING_MAX_DISTANCE} parameter. */
	public static final double DEFAULT_LINKING_MAX_DISTANCE = 15.0;

	/**
	 * Key for the parameter specifying the feature penalties when linking
	 * particles. Expected values should be a
	 * <code>Map&lt;FeatureKey, Double&gt;</code> instance.
	 */
	public static final String KEY_LINKING_FEATURE_PENALTIES = "LINKING_FEATURE_PENALTIES";

	/**
	 * A default value for the {@value #KEY_LINKING_FEATURE_PENALTIES}
	 * parameter.
	 */
	public static final Map< FeatureProjectionKey, Double > DEFAULT_LINKING_FEATURE_PENALTIES = new HashMap<>();

	/**
	 * Key for the parameter specifying whether we allow the detection of
	 * gap-closing events. Expected values are {@link Boolean}s.
	 */
	public static final String KEY_ALLOW_GAP_CLOSING = "ALLOW_GAP_CLOSING";

	/** A default value for the {@value #KEY_ALLOW_GAP_CLOSING} parameter. */
	public static final boolean DEFAULT_ALLOW_GAP_CLOSING = true;

	/**
	 * Key for the parameter that specify the maximal number of frames to bridge
	 * when detecting gap closing. Expected values are {@link Integer}s greater
	 * than 0. A value of 1 means that a detection might be missed in 1 frame,
	 * and the track will not be broken. And so on.
	 */
	public static final String KEY_GAP_CLOSING_MAX_FRAME_GAP = "MAX_FRAME_GAP";

	/**
	 * A default value for the {@value #KEY_GAP_CLOSING_MAX_FRAME_GAP}
	 * parameter.
	 */
	public static final int DEFAULT_GAP_CLOSING_MAX_FRAME_GAP = 2;

	/**
	 * Key for the parameter specifying the max gap-closing distance. Expected
	 * values are {@link Double}s and should be expressed in physical units. If
	 * two spots, candidate for a gap-closing event, are found separated by a
	 * distance larger than this parameter value, gap-closing will not occur.
	 */
	public static final String KEY_GAP_CLOSING_MAX_DISTANCE = "GAP_CLOSING_MAX_DISTANCE";

	/**
	 * A default value for the {@value #KEY_GAP_CLOSING_MAX_DISTANCE} parameter.
	 */
	public static final double DEFAULT_GAP_CLOSING_MAX_DISTANCE = 15.0;

	/**
	 * Key for the parameter specifying the feature penalties when detecting
	 * gap-closing events. Expected values should be a
	 * <code>Map&lt;FeatureKey, Double&gt;</code> instance.
	 */
	public static final String KEY_GAP_CLOSING_FEATURE_PENALTIES = "GAP_CLOSING_FEATURE_PENALTIES";

	/**
	 * A default value for the {@value #KEY_GAP_CLOSING_FEATURE_PENALTIES}
	 * parameter.
	 */
	public static final Map< FeatureProjectionKey, Double > DEFAULT_GAP_CLOSING_FEATURE_PENALTIES = new HashMap<>();

	/**
	 * Key for the parameter specifying whether we allow the detection of
	 * merging events. Expected values are {@link Boolean}s.
	 */
	public static final String KEY_ALLOW_TRACK_MERGING = "ALLOW_TRACK_MERGING";

	/** A default value for the {@value #KEY_ALLOW_TRACK_MERGING} parameter. */
	public static final boolean DEFAULT_ALLOW_TRACK_MERGING = false;

	/**
	 * Key for the parameter specifying the max merging distance. Expected
	 * values are {@link Double}s and should be expressed in physical units. If
	 * two spots, candidate for a merging event, are found separated by a
	 * distance larger than this parameter value, track merging will not occur.
	 */
	public static final String KEY_MERGING_MAX_DISTANCE = "MERGING_MAX_DISTANCE";

	/** A default value for the {@value #KEY_MERGING_MAX_DISTANCE} parameter. */
	public static final double DEFAULT_MERGING_MAX_DISTANCE = 15.0;

	/**
	 * Key for the parameter specifying the feature penalties when dealing with
	 * merging events. Expected values should be a
	 * <code>Map&lt;FeatureKey, Double&gt;</code> instance.
	 */
	public static final String KEY_MERGING_FEATURE_PENALTIES = "MERGING_FEATURE_PENALTIES";

	/**
	 * A default value for the {@value #KEY_MERGING_FEATURE_PENALTIES}
	 * parameter.
	 */
	public static final Map< FeatureProjectionKey, Double > DEFAULT_MERGING_FEATURE_PENALTIES = new HashMap<>();

	/**
	 * Key for the parameter specifying whether we allow the detection of
	 * splitting events. Expected values are {@link Boolean}s.
	 */
	public static final String KEY_ALLOW_TRACK_SPLITTING = "ALLOW_TRACK_SPLITTING";

	/**
	 * A default value for the {@value #KEY_ALLOW_TRACK_SPLITTING} parameter.
	 */
	public static final boolean DEFAULT_ALLOW_TRACK_SPLITTING = false;

	/**
	 * Key for the parameter specifying the max splitting distance. Expected
	 * values are {@link Double}s and should be expressed in physical units. If
	 * two spots, candidate for a merging event, are found separated by a
	 * distance larger than this parameter value, track splitting will not
	 * occur.
	 */
	public static final String KEY_SPLITTING_MAX_DISTANCE = "SPLITTING_MAX_DISTANCE";

	/**
	 * A default value for the {@link #KEY_SPLITTING_MAX_DISTANCE} parameter.
	 */
	public static final double DEFAULT_SPLITTING_MAX_DISTANCE = 15.0;

	/**
	 * Key for the parameter specifying the feature penalties when dealing with
	 * splitting events. Expected values should be a
	 * <code>Map&lt;FeatureKey, Double&gt;</code> instance.
	 */
	public static final String KEY_SPLITTING_FEATURE_PENALTIES = "SPLITTING_FEATURE_PENALTIES";

	/**
	 * A default value for the {@value #KEY_SPLITTING_FEATURE_PENALTIES}
	 * parameter.
	 */
	public static final Map< FeatureProjectionKey, Double > DEFAULT_SPLITTING_FEATURE_PENALTIES = new HashMap<>();

	/**
	 * Key for the parameter specifying the factor used to compute alternative
	 * linking costs. Expected values are {@link Double}s.
	 */
	public static final String KEY_ALTERNATIVE_LINKING_COST_FACTOR = "ALTERNATIVE_LINKING_COST_FACTOR";

	/**
	 * A default value for the {@value #KEY_ALTERNATIVE_LINKING_COST_FACTOR}
	 * parameter.
	 */
	public static final double DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR = 1.05d;

	/**
	 * Key for the cutoff percentile parameter. Expected values are
	 * {@link Double}s.
	 */
	public static final String KEY_CUTOFF_PERCENTILE = "CUTOFF_PERCENTILE";

	/** A default value for the {@value #KEY_CUTOFF_PERCENTILE} parameter. */
	public static final double DEFAULT_CUTOFF_PERCENTILE = 0.9d;

	/**
	 * Key for the parameter that stores the blocking value: cost for
	 * non-physical, forbidden links. Expected values are {@link Double}s, and
	 * are typically very large.
	 */
	public static final String KEY_BLOCKING_VALUE = "BLOCKING_VALUE";

	/** A default value for the {@value #KEY_BLOCKING_VALUE} parameter. */
	public static final double DEFAULT_BLOCKING_VALUE = Double.POSITIVE_INFINITY;

	public static final String KEY_KALMAN_SEARCH_RADIUS = "KALMAN_SEARCH_RADIUS";

	public static final double DEFAULT_MAX_SEARCH_RADIUS = 10.;

	/**
	 * The position accuracy or standard deviation on positions, used to
	 * initialize the Kalman filter.
	 */
	public static final String KEY_POSITION_SIGMA = "POSITION_SIGMA";

	public static final double DEFAULT_POSITION_SIGMA = DEFAULT_MAX_SEARCH_RADIUS / 10.;

	/**
	 * Key for the parameter that specifies whether linking should occur only on
	 * the content of the selection model. If <code>false</code>, linking will
	 * be performed on the whole model between the time-points specified by
	 * {@link DetectorKeys#KEY_MIN_TIMEPOINT} and
	 * {@link DetectorKeys#KEY_MAX_TIMEPOINT}. Expected values are
	 * {@link Boolean}s.
	 */
	public static final String KEY_DO_LINK_SELECTION = "DO_LINK_SELECTION";

	/**
	 * Default value for the {@link #KEY_DO_LINK_SELECTION} parameter.
	 */
	public static final boolean DEFAULT_DO_LINK_SELECTION = false;

	private LinkerKeys()
	{}
}
