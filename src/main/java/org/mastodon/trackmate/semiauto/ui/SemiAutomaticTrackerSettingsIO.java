package org.mastodon.trackmate.semiauto.ui;

import java.util.LinkedHashMap;
import java.util.Map;

import org.mastodon.revised.io.yaml.AbstractWorkaroundConstruct;
import org.mastodon.revised.io.yaml.WorkaroundConstructor;
import org.mastodon.revised.io.yaml.WorkaroundRepresent;
import org.mastodon.revised.io.yaml.WorkaroundRepresenter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
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
		public Object construct( final Node node )
		{
			try
			{
				final Map< Object, Object > mapping = constructMapping( ( MappingNode  ) node );
				final String name = ( String ) mapping.get( "name" );
				final SemiAutomaticTrackerSettings s = SemiAutomaticTrackerSettings.defaultSettings().copy( name );

				s.setName( ( String ) mapping.get( "name") );

				s.setSetupID( ( int ) mapping.get( "setupID" ) );
				s.setQualityFactor( ( double ) mapping.get( "qualityFactor" ) );
				s.setDistanceFactor( ( double ) mapping.get( "distanceFactor" ) );
				s.setNTimepoints( ( int ) mapping.get( "nTimepoints" ) );
				s.setForwardInTime( ( boolean ) mapping.get( "forwardInTime" ) );
				s.setAllowLinkingToExisting( ( boolean ) mapping.get( "allowLinkingToExisting" ) );
				s.setAllowIfIncomingLinks( ( boolean ) mapping.get( "allowIfIncomingLinks" ) );
				s.setAllowIfOutgoingLinks( ( boolean ) mapping.get( "allowIfOutgoingLinks" ) );
				s.setContinueIfLinked( ( boolean ) mapping.get( "continueIfLinked" ) );

				return s;
			}
			catch( final Exception e )
			{
				e.printStackTrace();
			}
			return null;
		}
	}
}
