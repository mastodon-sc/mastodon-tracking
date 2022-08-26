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
package org.mastodon.tracking.mamut.trackmate.semiauto.ui;

import static org.mastodon.tracking.detection.DetectorKeys.DEFAULT_SETUP_ID;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_SETUP_ID;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_ALLOW_LINKING_IF_HAS_INCOMING;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_ALLOW_LINKING_IF_HAS_OUTGOING;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_ALLOW_LINKING_TO_EXISTING;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_CONTINUE_IF_LINK_EXISTS;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_DETECT_SPOT;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_DISTANCE_FACTOR;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_FORWARD_IN_TIME;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_N_TIMEPOINTS;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_QUALITY_FACTOR;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_ALLOW_LINKING_IF_HAS_INCOMING;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_ALLOW_LINKING_IF_HAS_OUTGOING;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_ALLOW_LINKING_TO_EXISTING;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_CONTINUE_IF_LINK_EXISTS;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_DETECT_SPOT;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_DISTANCE_FACTOR;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_FORWARD_IN_TIME;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_N_TIMEPOINTS;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_QUALITY_FACTOR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.scijava.listeners.Listeners;

import bdv.ui.settings.style.Style;

public class SemiAutomaticTrackerSettings implements Style< SemiAutomaticTrackerSettings >
{

	public interface UpdateListener
	{
		public void settingsChanged();
	}

	private final Listeners.List< UpdateListener > updateListeners;

	private String name;

	private int setupID;

	private double qualityFactor;

	private double distanceFactor;

	private int nTimepoints;

	private boolean forwardInTime;

	private boolean allowLinkingToExisting;

	private boolean allowIfIncomingLinks;

	private boolean allowIfOutgoingLinks;

	private boolean continueIfLinked;

	private boolean detectSpot;

	private static final SemiAutomaticTrackerSettings defSats;
	static
	{
		defSats = new SemiAutomaticTrackerSettings();
		defSats.setupID = DEFAULT_SETUP_ID;
		defSats.qualityFactor = DEFAULT_QUALITY_FACTOR;
		defSats.distanceFactor = DEFAULT_DISTANCE_FACTOR;
		defSats.nTimepoints = DEFAULT_N_TIMEPOINTS;
		defSats.forwardInTime = DEFAULT_FORWARD_IN_TIME;
		defSats.allowLinkingToExisting = DEFAULT_ALLOW_LINKING_TO_EXISTING;
		defSats.allowIfIncomingLinks = DEFAULT_ALLOW_LINKING_IF_HAS_INCOMING;
		defSats.allowIfOutgoingLinks = DEFAULT_ALLOW_LINKING_IF_HAS_OUTGOING;
		defSats.continueIfLinked = DEFAULT_CONTINUE_IF_LINK_EXISTS;
		defSats.detectSpot = DEFAULT_DETECT_SPOT;
		defSats.name = "Forward";
	}

	private static final SemiAutomaticTrackerSettings backSats;
	static
	{
		backSats = new SemiAutomaticTrackerSettings();
		backSats.setupID = DEFAULT_SETUP_ID;
		backSats.qualityFactor = DEFAULT_QUALITY_FACTOR;
		backSats.distanceFactor = DEFAULT_DISTANCE_FACTOR;
		backSats.nTimepoints = 100 * DEFAULT_N_TIMEPOINTS;
		backSats.forwardInTime = Boolean.valueOf( false );
		backSats.allowLinkingToExisting = Boolean.valueOf( true );
		backSats.allowIfIncomingLinks = Boolean.valueOf( true );
		backSats.allowIfOutgoingLinks = Boolean.valueOf( true );
		backSats.continueIfLinked = Boolean.valueOf( false );
		backSats.detectSpot = DEFAULT_DETECT_SPOT;
		backSats.name = "Backtracking";
	}

	public static SemiAutomaticTrackerSettings defaultSettings()
	{
		return defSats;
	}


	public static List< SemiAutomaticTrackerSettings > defaults()
	{
		final List< SemiAutomaticTrackerSettings > df = new ArrayList<>( 2 );
		df.add( defSats );
		df.add( backSats );
		return Collections.unmodifiableList( df );
	}

	private SemiAutomaticTrackerSettings()
	{
		this.updateListeners = new Listeners.SynchronizedList<>();
	}

	private void notifyListeners()
	{
		for ( final UpdateListener l : updateListeners.list )
			l.settingsChanged();
	}

	public Listeners< UpdateListener > updateListeners()
	{
		return updateListeners;
	}

	public void set( final SemiAutomaticTrackerSettings stas )
	{
		this.name = stas.name;
		this.setupID = stas.setupID;
		this.distanceFactor = stas.distanceFactor;
		this.qualityFactor = stas.qualityFactor;
		this.nTimepoints = stas.nTimepoints;
		this.forwardInTime = stas.forwardInTime;
		this.allowLinkingToExisting = stas.allowLinkingToExisting;
		this.allowIfIncomingLinks = stas.allowIfIncomingLinks;
		this.allowIfOutgoingLinks = stas.allowIfOutgoingLinks;
		this.continueIfLinked = stas.continueIfLinked;
		this.detectSpot = stas.detectSpot;
		notifyListeners();
	}

	public Map< String, Object > getAsSettingsMap()
	{
		final Map< String, Object > map = new HashMap<>();
		map.put( KEY_SETUP_ID, Integer.valueOf( setupID ) );
		map.put( KEY_DISTANCE_FACTOR, Double.valueOf( distanceFactor ) );
		map.put( KEY_QUALITY_FACTOR, Double.valueOf( qualityFactor ) );
		map.put( KEY_N_TIMEPOINTS, Integer.valueOf( nTimepoints ) );
		map.put( KEY_FORWARD_IN_TIME, Boolean.valueOf( forwardInTime ) );
		map.put( KEY_ALLOW_LINKING_TO_EXISTING, Boolean.valueOf( allowLinkingToExisting ) );
		map.put( KEY_ALLOW_LINKING_IF_HAS_INCOMING, Boolean.valueOf( allowIfIncomingLinks ) );
		map.put( KEY_ALLOW_LINKING_IF_HAS_OUTGOING, Boolean.valueOf( allowIfOutgoingLinks ) );
		map.put( KEY_CONTINUE_IF_LINK_EXISTS, Boolean.valueOf( continueIfLinked ) );
		map.put( KEY_DETECT_SPOT, Boolean.valueOf( detectSpot ) );
		return map;
	}

	public int getSetupID()
	{
		return setupID;
	}

	public void setSetupID( final int setupID )
	{
		if ( this.setupID != setupID )
		{
			this.setupID = setupID;
			notifyListeners();
		}
	}

	public double getQualityFactor()
	{
		return qualityFactor;
	}

	public void setQualityFactor( final double qualityFactor )
	{
		if ( this.qualityFactor != qualityFactor )
		{
			this.qualityFactor = qualityFactor;
			notifyListeners();
		}
	}

	public double getDistanceFactor()
	{
		return distanceFactor;
	}

	public void setDistanceFactor( final double distanceFactor )
	{
		if ( this.distanceFactor != distanceFactor )
		{
			this.distanceFactor = distanceFactor;
			notifyListeners();
		}
	}

	public int getnTimepoints()
	{
		return nTimepoints;
	}

	public void setNTimepoints( final int nTimepoints )
	{
		if ( this.nTimepoints != nTimepoints )
		{
			this.nTimepoints = nTimepoints;
			notifyListeners();
		}
	}

	public boolean isForwardInTime()
	{
		return forwardInTime;
	}

	public void setForwardInTime( final boolean forwardInTime )
	{
		if ( this.forwardInTime != forwardInTime )
		{
			this.forwardInTime = forwardInTime;
			notifyListeners();
		}
	}

	public boolean allowLinkingToExisting()
	{
		return allowLinkingToExisting;
	}

	public void setAllowLinkingToExisting( final boolean allowLinkingToExisting )
	{
		if ( this.allowLinkingToExisting != allowLinkingToExisting )
		{
			this.allowLinkingToExisting = allowLinkingToExisting;
			notifyListeners();
		}
	}

	public boolean allowIfIncomingLinks()
	{
		return allowIfIncomingLinks;
	}

	public void setAllowIfIncomingLinks( final boolean allowIfIncomingLinks )
	{
		if ( this.allowIfIncomingLinks != allowIfIncomingLinks )
		{
			this.allowIfIncomingLinks = allowIfIncomingLinks;
			notifyListeners();
		}
	}

	public boolean allowIfOutgoingLinks()
	{
		return allowIfOutgoingLinks;
	}

	public void setAllowIfOutgoingLinks( final boolean allowIfOutgoingLinks )
	{
		if ( this.allowIfOutgoingLinks != allowIfOutgoingLinks )
		{
			this.allowIfOutgoingLinks = allowIfOutgoingLinks;
			notifyListeners();
		}
	}

	public boolean continueIfLinked()
	{
		return continueIfLinked;
	}

	public void setContinueIfLinked( final boolean continueIfLinked )
	{
		if ( this.continueIfLinked != continueIfLinked )
		{
			this.continueIfLinked = continueIfLinked;
			notifyListeners();
		}
	}

	public boolean detectSpot()
	{
		return detectSpot;
	}

	public void setDetectSpot( final boolean detectSpot )
	{
		if ( this.detectSpot != detectSpot )
		{
			this.detectSpot = detectSpot;
			notifyListeners();
		}
	}

	@Override
	public SemiAutomaticTrackerSettings copy()
	{
		return copy( null );
	}

	@Override
	public SemiAutomaticTrackerSettings copy( final String name )
	{
		final SemiAutomaticTrackerSettings sats = new SemiAutomaticTrackerSettings();
		sats.set( this );
		if ( name != null )
			sats.setName( name );
		return sats;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public void setName( final String name )
	{
		if ( !Objects.equals( this.name, name ) )
		{
			this.name = name;
			notifyListeners();
		}
	}
}
