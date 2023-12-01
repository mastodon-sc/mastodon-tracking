/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2023 Tobias Pietzsch, Jean-Yves Tinevez
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

import java.util.List;

import org.mastodon.app.ui.AbstractStyleManagerYaml;
import org.yaml.snakeyaml.Yaml;

public class SemiAutomaticTrackerSettingsManager extends AbstractStyleManagerYaml< SemiAutomaticTrackerSettingsManager, SemiAutomaticTrackerSettings >
{

	private static final String SETTINGS_FILE = System.getProperty( "user.home" ) + "/.mastodon/plugins/tracking/semi-automatic-tracker-settings.yaml";
	private static final String LEGACY_SETTINGS_FILE = System.getProperty( "user.home" ) + "/.mastodon/Plugins/Tracking/semiautomatictrackersettings.yaml";

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
	 * Returns a final {@link SemiAutomaticTrackerSettings} instance that always
	 * has the same properties as the default style.
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

	public void loadStyles()
	{
		loadStyles( SETTINGS_FILE );
		handleLegacyFile( LEGACY_SETTINGS_FILE );
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

	@Override
	protected Yaml createYaml()
	{
		return SemiAutomaticTrackerSettingsIO.createYaml();
	}
}
