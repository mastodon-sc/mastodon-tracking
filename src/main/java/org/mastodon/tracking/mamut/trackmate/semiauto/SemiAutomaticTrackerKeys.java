/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2021 Tobias Pietzsch, Jean-Yves Tinevez
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
package org.mastodon.tracking.mamut.trackmate.semiauto;

import static org.mastodon.tracking.detection.DetectorKeys.DEFAULT_SETUP_ID;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_SETUP_ID;
import static org.mastodon.tracking.linking.LinkingUtils.checkMapKeys;
import static org.mastodon.tracking.linking.LinkingUtils.checkParameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SemiAutomaticTrackerKeys
{

	/**
	 * Returns a new settings map filled with default values suitable for the
	 * semi-automatic detector.
	 *
	 * @return a new map.
	 */
	public static final Map< String, Object > getDefaultDetectorSettingsMap()
	{
		final Map< String, Object > settings = new HashMap< String, Object >();
		settings.put( KEY_SETUP_ID, DEFAULT_SETUP_ID );
		settings.put( KEY_N_TIMEPOINTS, DEFAULT_N_TIMEPOINTS );
		settings.put( KEY_QUALITY_FACTOR, DEFAULT_QUALITY_FACTOR );
		settings.put( KEY_DISTANCE_FACTOR, DEFAULT_DISTANCE_FACTOR );
		settings.put( KEY_FORWARD_IN_TIME, DEFAULT_FORWARD_IN_TIME );
		settings.put( KEY_ALLOW_LINKING_TO_EXISTING, DEFAULT_ALLOW_LINKING_TO_EXISTING );
		settings.put( KEY_ALLOW_LINKING_IF_HAS_INCOMING, DEFAULT_ALLOW_LINKING_IF_HAS_INCOMING );
		settings.put( KEY_ALLOW_LINKING_IF_HAS_OUTGOING, DEFAULT_ALLOW_LINKING_IF_HAS_OUTGOING );
		settings.put( KEY_CONTINUE_IF_LINK_EXISTS, DEFAULT_CONTINUE_IF_LINK_EXISTS );
		settings.put( KEY_DETECT_SPOT, DEFAULT_DETECT_SPOT );
		return settings;
	}

	/**
	 * Checks whether the provided settings map is suitable for use with the
	 * semi-automatic tracker.
	 *
	 * @param settings
	 *            the map to test.
	 * @param errorHolder
	 *            a {@link StringBuilder} that will contain an error message if
	 *            the check is not successful.
	 * @return true if the settings map can be used with the semi-automatic
	 *         tracker.
	 */
	public static final boolean checkSettingsValidity( final Map< String, Object > settings, final StringBuilder errorHolder )
	{
		if ( null == settings )
		{
			errorHolder.append( "Settings map is null.\n" );
			return false;
		}

		boolean ok = true;
		// Check proper class.
		ok = ok & checkParameter( settings, KEY_SETUP_ID, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_QUALITY_FACTOR, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_DISTANCE_FACTOR, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_FORWARD_IN_TIME, Boolean.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_N_TIMEPOINTS, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_ALLOW_LINKING_TO_EXISTING, Boolean.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_ALLOW_LINKING_IF_HAS_INCOMING, Boolean.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_ALLOW_LINKING_IF_HAS_OUTGOING, Boolean.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_CONTINUE_IF_LINK_EXISTS, Boolean.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_DETECT_SPOT, Boolean.class, errorHolder );

		// Check key presence.
		final List< String > mandatoryKeys = new ArrayList< String >();
		mandatoryKeys.add( KEY_SETUP_ID );
		mandatoryKeys.add( KEY_QUALITY_FACTOR );
		mandatoryKeys.add( KEY_DISTANCE_FACTOR );
		mandatoryKeys.add( KEY_FORWARD_IN_TIME );
		mandatoryKeys.add( KEY_N_TIMEPOINTS );
		mandatoryKeys.add( KEY_ALLOW_LINKING_TO_EXISTING );
		mandatoryKeys.add( KEY_ALLOW_LINKING_IF_HAS_INCOMING );
		mandatoryKeys.add( KEY_ALLOW_LINKING_IF_HAS_OUTGOING );
		mandatoryKeys.add( KEY_CONTINUE_IF_LINK_EXISTS );
		mandatoryKeys.add( KEY_DETECT_SPOT );
		final List< String > optionalKeys = new ArrayList< String >();
		optionalKeys.add( KEY_RESOLUTION_LEVEL );
		ok = ok & checkMapKeys( settings, mandatoryKeys, optionalKeys, errorHolder );

		// Check some parameter values.
		if ( ok )
		{
			final double qualityFactor = ( double ) settings.get( KEY_QUALITY_FACTOR );
			if ( qualityFactor < 0 )
			{
				errorHolder.append( String.format( "Value for key %s must be larger than or equal to 0. Was %d.\n", KEY_QUALITY_FACTOR, qualityFactor ) );
				ok = false;
			}

			final double distanceFactor = ( double ) settings.get( KEY_DISTANCE_FACTOR );
			if ( distanceFactor <= 0 )
			{
				errorHolder.append( String.format( "Value for key %s must be larger than 0. Was %d.\n", KEY_DISTANCE_FACTOR, distanceFactor ) );
				ok = false;
			}

			final int nt = ( int ) settings.get( KEY_N_TIMEPOINTS );
			if ( ok & nt < 1 )
			{
				errorHolder.append( String.format( "Value for key %s must be larger than 0. Was %d.\n", KEY_N_TIMEPOINTS, nt ) );
				ok = false;
			}
		}

		return ok;
	}

	/**
	 * Key for the parameter that specifies the tolerance on quality for
	 * discovered spots. A target spot may be linked to the source spot only if
	 * the target spot has a quality higher than the source spot quality times
	 * this factor.
	 * <p>
	 * Expected values are {@link Double}s larger than or equal to 0.
	 */
	public static final String KEY_QUALITY_FACTOR = "QUALITY_FACTOR";

	/**
	 * Default value for the {@link #KEY_QUALITY_FACTOR} parameter.
	 */
	public static final double DEFAULT_QUALITY_FACTOR = 0.5;

	/**
	 * Key for the parameter that specifies the tolerance on distance for
	 * discovered spots. A target spot may be linked to the source spot only if
	 * they are not father than the source spot radius times this factor.
	 * <p>
	 * Expected values are {@link Double}s larger than 0.
	 */
	public static final String KEY_DISTANCE_FACTOR = "DISTANCE_FACTOR";

	/**
	 * Default value for the {@link #KEY_DISTANCE_FACTOR} parameter.
	 */
	public static final double DEFAULT_DISTANCE_FACTOR = 1.2;

	/**
	 * Key for the parameter that specifies whether we track forward or backward
	 * in time. If <code>true</code>, we track forward in time. If
	 * <code>false</code>, backward.
	 * <p>
	 * Expected values are {@link Boolean}s.
	 */
	public static final String KEY_FORWARD_IN_TIME = "FORWARD_IN_TIME";

	/**
	 * Default value for the {@link #KEY_FORWARD_IN_TIME} parameter.
	 */
	public static final boolean DEFAULT_FORWARD_IN_TIME = true;

	/**
	 * Key for the parameter specifying how many time-points are processed at
	 * most, from the input source spot.
	 * <p>
	 * Expected values are {@link Integer}s larger than 0.
	 */
	public static final String KEY_N_TIMEPOINTS = "N_TIMEPOINTS";

	/**
	 * Default value for the {@link #KEY_N_TIMEPOINTS} parameter.
	 */
	public static final int DEFAULT_N_TIMEPOINTS = 10;

	/**
	 * Key for the parameter specifying whether we allow linking to a spot
	 * already existing in the model.
	 * <p>
	 * If the best candidate spot is found near a spot already existing in the
	 * model (within radius), semi-automatic tracking will stop, unless this
	 * parameter is set to <code>true</code>. In that case the source spot might
	 * be linked to the pre-existing spot, depending on whether it has incoming
	 * or outgoing links already (see {@link #KEY_ALLOW_LINKING_IF_HAS_INCOMING}
	 * and {@link #KEY_ALLOW_LINKING_IF_HAS_OUTGOING}).
	 * <p>
	 * Expected values are {@link Boolean}s.
	 */
	public static final String KEY_ALLOW_LINKING_TO_EXISTING = "ALLOW_LINKING_TO_EXISTING";

	/**
	 * Default value for the {@link #KEY_ALLOW_LINKING_TO_EXISTING} parameter.
	 */
	public static final boolean DEFAULT_ALLOW_LINKING_TO_EXISTING = true;

	/**
	 * Key for the parameter specifying whether we allow linking to spot already
	 * existing in the model if it has already incoming links.
	 * <p>
	 * If this parameter is set to <code>true</code>, we allow linking to
	 * existing spots that already have incoming links (more than 0). For this
	 * parameter to be taken into account,
	 * {@link #KEY_ALLOW_LINKING_TO_EXISTING} must be <code>true</code>.
	 * <p>
	 * Expected values are {@link Boolean}s.
	 */
	public static final String KEY_ALLOW_LINKING_IF_HAS_INCOMING = "ALLOW_LINKING_IF_HAS_INCOMING";

	/**
	 * Default value for the {@link #KEY_ALLOW_LINKING_IF_HAS_INCOMING}
	 * parameter.
	 */
	public static final boolean DEFAULT_ALLOW_LINKING_IF_HAS_INCOMING = false;

	/**
	 * Key for the parameter specifying whether we allow linking to spot already
	 * existing in the model if it has already outgoing links.
	 * <p>
	 * If this parameter is set to <code>true</code>, we allow linking to
	 * existing spots that already have outgoing links (more than 0). For this
	 * parameter to be taken into account,
	 * {@link #KEY_ALLOW_LINKING_TO_EXISTING} must be <code>true</code>.
	 * <p>
	 * Expected values are {@link Boolean}s.
	 */
	public static final String KEY_ALLOW_LINKING_IF_HAS_OUTGOING = "ALLOW_LINKING_IF_HAS_OUTGOING";

	/**
	 * Default value for the {@link #KEY_ALLOW_LINKING_IF_HAS_INCOMING}
	 * parameter.
	 */
	public static final boolean DEFAULT_ALLOW_LINKING_IF_HAS_OUTGOING = true;

	/**
	 * Key for the parameter that specifies whether we continue semi-automatic
	 * tracking if a link already exists in the model between the source and the
	 * target spots. For this parameter to be taken into account,
	 * {@link #KEY_ALLOW_LINKING_TO_EXISTING} must be <code>true</code>.
	 * <p>
	 * Expected values are {@link Boolean}s.
	 */
	public static final String KEY_CONTINUE_IF_LINK_EXISTS = "CONTINUE_IF_LINK_EXISTS";

	/**
	 * Default value for the {@link #KEY_CONTINUE_IF_LINK_EXISTS} parameter.
	 */
	public static final boolean DEFAULT_CONTINUE_IF_LINK_EXISTS = true;

	/**
	 * Key for the parameter that specifies what resolution level to use for
	 * detection, in the case of a multi-resolution level image. This parameter
	 * is not mandatory.
	 * <p>
	 * Expected values are {@link Integer}s, larger than or equal to 0. If it is
	 * <code>null</code> or negative, the optimal resolution level is determined
	 * automatically from the spot radius.
	 */
	public static final String KEY_RESOLUTION_LEVEL = "RESOLUTION_LEVEL";

	/**
	 * Default value for the {@link #KEY_RESOLUTION_LEVEL} parameter.
	 */
	public static final Integer DEFAULT_RESOLUTION_LEVEL = null;

	/**
	 * Key for the parameter that specifies whether we will perform spot
	 * detection. If <code>false</code>, the semi-automatic tracker will stop if
	 * no existing spots cannot be found as target for linking. If
	 * <code>true</code>, the detection process will be run on the neighborhood
	 * to create spots to link to from the image data.
	 */
	public static final String KEY_DETECT_SPOT = "DETECT_SPOT";

	/**
	 * Default value for the {@link #KEY_DETECT_SPOT} parameter.
	 */
	public static final boolean DEFAULT_DETECT_SPOT = true;

	/** Minimal size of neighborhoods, in spot diameter units. */
	public static final double NEIGHBORHOOD_FACTOR = 2.;

	private SemiAutomaticTrackerKeys()
	{}
}
