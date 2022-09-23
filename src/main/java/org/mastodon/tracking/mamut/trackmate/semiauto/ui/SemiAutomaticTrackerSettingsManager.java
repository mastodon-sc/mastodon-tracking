/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2022 Tobias Pietzsch, Jean-Yves Tinevez
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mastodon.tracking.mamut.trackmate.semiauto.SemiAutomaticTrackerPlugin;
import org.yaml.snakeyaml.Yaml;

import bdv.ui.settings.style.AbstractStyleManager;

public class SemiAutomaticTrackerSettingsManager extends AbstractStyleManager< SemiAutomaticTrackerSettingsManager, SemiAutomaticTrackerSettings >
{

	private static final String SETTINGS_FILE;
	static
	{
		String sfPath = System.getProperty( "user.home" ) + "/.mastodon";
		for ( final String pathElement : SemiAutomaticTrackerPlugin.MENU_PATH )
			sfPath += '/' + pathElement;
		sfPath += "/semiautomatictrackersettings.yaml";
		SETTINGS_FILE = sfPath;
	}

	private final SemiAutomaticTrackerSettings forwardDefaultSettings;

	private final SemiAutomaticTrackerSettings.UpdateListener updateForwardDefaultListeners;

	public SemiAutomaticTrackerSettingsManager()
	{
		this( true );
	}

	public SemiAutomaticTrackerSettingsManager( final boolean loadSettings )
	{
		forwardDefaultSettings = SemiAutomaticTrackerSettings.defaultSettings().copy();
		updateForwardDefaultListeners = () -> forwardDefaultSettings.set( selectedStyle );
		selectedStyle.updateListeners().add( updateForwardDefaultListeners );
		if ( loadSettings )
			loadStyles();
	}

	/**
	 * Returns a final {@link SemiAutomaticTrackerSettings} instance that always has
	 * the same properties as the default style.
	 * 
	 * @return the forward settings.
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
			objects.add( selectedStyle.getName() );
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
			setSelectedStyle( styleForName( defaultStyleName ).orElseGet( () -> builtinStyles.get( 0 ) ) );
		}
		catch ( final FileNotFoundException e )
		{
			System.out.println( "SemiAutomaticTrackerSettings file " + filename + " not found. Using builtin settings." );
		}
	}

	@Override
	public synchronized void setSelectedStyle( final SemiAutomaticTrackerSettings renderSettings )
	{
		selectedStyle.updateListeners().remove( updateForwardDefaultListeners );
		selectedStyle = renderSettings;
		forwardDefaultSettings.set( selectedStyle );
		selectedStyle.updateListeners().add( updateForwardDefaultListeners );
	}

	@Override
	protected List< SemiAutomaticTrackerSettings > loadBuiltinStyles()
	{
		return SemiAutomaticTrackerSettings.defaults();
	}
}
