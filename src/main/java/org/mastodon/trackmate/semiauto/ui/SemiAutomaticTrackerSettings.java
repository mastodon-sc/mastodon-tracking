package org.mastodon.trackmate.semiauto.ui;

import static org.mastodon.detection.DetectorKeys.DEFAULT_SETUP_ID;
import static org.mastodon.detection.DetectorKeys.KEY_SETUP_ID;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_ALLOW_LINKING_IF_HAS_INCOMING;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_ALLOW_LINKING_IF_HAS_OUTGOING;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_ALLOW_LINKING_TO_EXISTING;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_CONTINUE_IF_LINK_EXISTS;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_DISTANCE_FACTOR;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_FORWARD_IN_TIME;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_N_TIMEPOINTS;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_QUALITY_FACTOR;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_ALLOW_LINKING_IF_HAS_INCOMING;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_ALLOW_LINKING_IF_HAS_OUTGOING;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_ALLOW_LINKING_TO_EXISTING;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_CONTINUE_IF_LINK_EXISTS;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_DISTANCE_FACTOR;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_FORWARD_IN_TIME;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_N_TIMEPOINTS;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_QUALITY_FACTOR;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.mastodon.app.ui.settings.style.Style;
import org.mastodon.util.Listeners;

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
		defSats.name = "Default";
	}

	public static SemiAutomaticTrackerSettings defaultSettings()
	{
		return defSats;
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
		map.put( KEY_CONTINUE_IF_LINK_EXISTS, Boolean.valueOf( allowIfIncomingLinks ) );
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

	@Override
	public SemiAutomaticTrackerSettings copy()
	{
		return copy( null );
	}

	@Override
	public SemiAutomaticTrackerSettings copy( final String newName )
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
