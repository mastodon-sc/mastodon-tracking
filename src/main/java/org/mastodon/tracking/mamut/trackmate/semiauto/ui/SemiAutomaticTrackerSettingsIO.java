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
package org.mastodon.tracking.mamut.trackmate.semiauto.ui;

import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_ALLOW_LINKING_IF_HAS_INCOMING;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_ALLOW_LINKING_IF_HAS_OUTGOING;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_ALLOW_LINKING_TO_EXISTING;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_CONTINUE_IF_LINK_EXISTS;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_DETECT_SPOT;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_DISTANCE_FACTOR;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_FORWARD_IN_TIME;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_N_TIMEPOINTS;
import static org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerKeys.DEFAULT_QUALITY_FACTOR;

import java.util.LinkedHashMap;
import java.util.Map;

import org.mastodon.io.yaml.AbstractWorkaroundConstruct;
import org.mastodon.io.yaml.WorkaroundConstructor;
import org.mastodon.io.yaml.WorkaroundRepresent;
import org.mastodon.io.yaml.WorkaroundRepresenter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Facilities to dump / load {@link SemiAutomaticTrackerSettings} to / from a YAML file.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class SemiAutomaticTrackerSettingsIO
{
	private static class SemiAutomaticTrackerSettingsRepresenter extends WorkaroundRepresenter
	{
		public SemiAutomaticTrackerSettingsRepresenter()
		{
			putRepresent( new RepresentSemiAutomaticTrackerSettings( this ) );
		}
	}

	private static class SemiAutomaticTrackerSettingsConstructor extends WorkaroundConstructor
	{
		public SemiAutomaticTrackerSettingsConstructor()
		{
			super( Object.class );
			putConstruct( new ConstructSemiAutomaticTrackerSettings( this ) );
		}
	}

	/**
	 * Returns a YAML instance that can dump / load a collection of
	 * {@link SemiAutomaticTrackerSettings} to / from a .yaml file.
	 *
	 * @return a new YAML instance.
	 */
	static Yaml createYaml()
	{
		final DumperOptions dumperOptions = new DumperOptions();
		final Representer representer = new SemiAutomaticTrackerSettingsRepresenter();
		final Constructor constructor = new SemiAutomaticTrackerSettingsConstructor();
		final Yaml yaml = new Yaml( constructor, representer, dumperOptions );
		return yaml;
	}

	private static final Tag SEMIAUTOTRACKERSETTINGS_TAG = new Tag( "!semiautomatictrackersettings" );

	private static class RepresentSemiAutomaticTrackerSettings extends WorkaroundRepresent
	{
		public RepresentSemiAutomaticTrackerSettings( final WorkaroundRepresenter r )
		{
			super( r, SEMIAUTOTRACKERSETTINGS_TAG, SemiAutomaticTrackerSettings.class );
		}

		@Override
		public Node representData( final Object data )
		{
			final SemiAutomaticTrackerSettings s = ( SemiAutomaticTrackerSettings ) data;
			final Map< String, Object > mapping = new LinkedHashMap< >();

			mapping.put( "name", s.getName() );

			mapping.put( "setupID", s.getSetupID() );
			mapping.put( "qualityFactor", s.getQualityFactor() );
			mapping.put( "distanceFactor", s.getDistanceFactor() );
			mapping.put( "nTimepoints", s.getnTimepoints() );
			mapping.put( "forwardInTime", s.isForwardInTime() );
			mapping.put( "allowLinkingToExisting", s.allowLinkingToExisting() );
			mapping.put( "allowIfIncomingLinks", s.allowIfIncomingLinks() );
			mapping.put( "allowIfOutgoingLinks", s.allowIfOutgoingLinks() );
			mapping.put( "continueIfLinked", s.continueIfLinked() );
			mapping.put( "detectSpot", s.detectSpot() );

			final Node node = representMapping( getTag(), mapping, getDefaultFlowStyle() );
			return node;
		}
	}

	private static class ConstructSemiAutomaticTrackerSettings extends AbstractWorkaroundConstruct
	{
		public ConstructSemiAutomaticTrackerSettings( final WorkaroundConstructor c )
		{
			super( c, SEMIAUTOTRACKERSETTINGS_TAG );
		}

		@Override
		public Object construct( final Node node ) throws YAMLException
		{
			final Map< Object, Object > mapping = constructMapping( ( MappingNode ) node );
			final String name = ( String ) mapping.get( "name" );
			final SemiAutomaticTrackerSettings s = SemiAutomaticTrackerSettings.defaultSettings().copy( name );

			s.setName( getStringOrDefault( mapping, "name", "NameNotFound" ) );
			s.setSetupID( getIntOrDefault( mapping, "setupID", 0 ) );
			s.setQualityFactor( getDoubleOrDefault( mapping, "qualityFactor", DEFAULT_QUALITY_FACTOR ) );
			s.setDistanceFactor( getDoubleOrDefault( mapping, "distanceFactor", DEFAULT_DISTANCE_FACTOR ) );
			s.setNTimepoints( getIntOrDefault( mapping, "nTimepoints", DEFAULT_N_TIMEPOINTS ) );
			s.setForwardInTime( getBooleanOrDefault( mapping, "forwardInTime", DEFAULT_FORWARD_IN_TIME ) );
			s.setAllowLinkingToExisting( ( getBooleanOrDefault( mapping, "allowLinkingToExisting", DEFAULT_ALLOW_LINKING_TO_EXISTING ) ) );
			s.setAllowIfIncomingLinks( getBooleanOrDefault( mapping, "allowIfIncomingLinks", DEFAULT_ALLOW_LINKING_IF_HAS_INCOMING ) );
			s.setAllowIfOutgoingLinks( getBooleanOrDefault( mapping, "allowIfOutgoingLinks", DEFAULT_ALLOW_LINKING_IF_HAS_OUTGOING ) );
			s.setContinueIfLinked( getBooleanOrDefault( mapping, "continueIfLinked", DEFAULT_CONTINUE_IF_LINK_EXISTS ) );
			s.setDetectSpot( getBooleanOrDefault( mapping, "detectSpot", DEFAULT_DETECT_SPOT ) );

			return s;
		}
	}
}
