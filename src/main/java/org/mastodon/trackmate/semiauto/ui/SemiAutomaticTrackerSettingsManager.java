package org.mastodon.trackmate.semiauto.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mastodon.app.ui.settings.style.AbstractStyleManager;
import org.yaml.snakeyaml.Yaml;

public class SemiAutomaticTrackerSettingsManager extends AbstractStyleManager< SemiAutomaticTrackerSettingsManager, SemiAutomaticTrackerSettings >
{

	private static final String SETTINGS_FILE = System.getProperty( "user.home" ) + "/.mastodon/semiautomatictrackersettings.yaml";

	private final SemiAutomaticTrackerSettings forwardDefaultSettings;

	private final SemiAutomaticTrackerSettings.UpdateListener updateForwardDefaultListeners;

	public SemiAutomaticTrackerSettingsManager()
	{
		this( true );
	}

	public SemiAutomaticTrackerSettingsManager( final boolean loadSettings )
	{
		forwardDefaultSettings = SemiAutomaticTrackerSettings.defaultSettings().copy();
		updateForwardDefaultListeners = () -> forwardDefaultSettings.set( defaultStyle );
		defaultStyle.updateListeners().add( updateForwardDefaultListeners );
		if ( loadSettings )
			loadStyles();
	}

	/**
	 * Returns a final {@link SemiAutomaticTrackerSettings} instance that always has the same
	 * properties as the default style.
	 */
	public SemiAutomaticTrackerSettings getForwardDefaultStyle()
	{
		return forwardDefaultSettings;
	}

	@Override
	public void saveStyles()
	{
		saveStyles( SETTINGS_FILE );
	}

	public void saveStyles( final String filename )
	{
		try
		{
			new File( filename ).getParentFile().mkdirs();
			final FileWriter output = new FileWriter( filename );
			final Yaml yaml = SemiAutomaticTrackerSettingsIO.createYaml();
			final ArrayList< Object > objects = new ArrayList<>();
			objects.add( defaultStyle.getName() );
			objects.addAll( userStyles );
			yaml.dumpAll( objects.iterator(), output );
			output.close();
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
	}

	public void loadStyles()
	{
		loadStyles( SETTINGS_FILE );
	}

	public void loadStyles( final String filename )
	{
		userStyles.clear();
		final Set< String > names = builtinStyles.stream().map( SemiAutomaticTrackerSettings::getName ).collect( Collectors.toSet() );
		try
		{
			final FileReader input = new FileReader( filename );
			final Yaml yaml = SemiAutomaticTrackerSettingsIO.createYaml();
			final Iterable< Object > objs = yaml.loadAll( input );
			String defaultStyleName = null;
			for ( final Object obj : objs )
			{
				if ( obj instanceof String )
				{
					defaultStyleName = ( String ) obj;
				}
				else if ( obj instanceof SemiAutomaticTrackerSettings )
				{
					final SemiAutomaticTrackerSettings ts = ( SemiAutomaticTrackerSettings ) obj;
					if ( null != ts )
					{
						// sanity check: settings names must be unique
						if ( names.add( ts.getName() ) )
							userStyles.add( ts );
						else
							System.out.println( "Discarded settings with duplicate name \"" + ts.getName() + "\"." );
					}
				}
			}
			setDefaultStyle( styleForName( defaultStyleName ).orElseGet( () -> builtinStyles.get( 0 ) ) );
		}
		catch ( final FileNotFoundException e )
		{
			System.out.println( "SemiAutomaticTrackerSettings file " + filename + " not found. Using builtin settings." );
		}
	}

	@Override
	public synchronized void setDefaultStyle( final SemiAutomaticTrackerSettings renderSettings )
	{
		defaultStyle.updateListeners().remove( updateForwardDefaultListeners );
		defaultStyle = renderSettings;
		forwardDefaultSettings.set( defaultStyle );
		defaultStyle.updateListeners().add( updateForwardDefaultListeners );
	}

	@Override
	protected List< SemiAutomaticTrackerSettings > loadBuiltinStyles()
	{
		return Arrays.asList( new SemiAutomaticTrackerSettings[] { SemiAutomaticTrackerSettings.defaultSettings() } );
	}
}
