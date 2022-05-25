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
package org.mastodon.tracking.mamut.trackmate.wizard;

import java.util.ArrayList;

import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.WindowManager;
import org.mastodon.mamut.plugin.MamutPluginAppModel;
import org.mastodon.tracking.mamut.trackmate.Settings;
import org.mastodon.tracking.mamut.trackmate.TrackMate;
import org.mastodon.ui.keymap.CommandDescriptionProvider;
import org.mastodon.ui.keymap.CommandDescriptions;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.scijava.plugin.Plugin;

import bdv.viewer.SourceAndConverter;

@Plugin( type = WizardDetectionPlugin.class )
public class WizardDetectionPlugin extends WizardPlugin
{

	public static final String RUN_WIZARD = "run spot detection wizard";

	private static final String[] RUN_WIZARD_KEYS = new String[] { "not mapped" };

	public static final Settings settings = new Settings();

	public WizardDetectionPlugin()
	{
		super( RUN_WIZARD, "Detection...", "Plugins > Tracking", RUN_WIZARD_KEYS );
	}

	@Override
	public WizardSequence getWizardSequence( final MamutPluginAppModel pluginAppModel, final Wizard wizard )
	{
		final MamutAppModel appModel = pluginAppModel.getAppModel();
		final WindowManager windowManager = pluginAppModel.getWindowManager();
		/*
		 * TODO Resolve later: SharedBdvData has AbstractSpimData<?>, we need
		 * SpimDataMinimal here. Using AbstractSpimData<?> everywhere should
		 * work, but requires additional casting when instantiation ops.
		 */
		final ArrayList< SourceAndConverter< ? > > sources = appModel.getSharedBdvData().getSources();
		settings.sources( sources );
		final TrackMate trackmate = new TrackMate( settings, appModel.getModel(), appModel.getSelectionModel() );
		getContext().inject( trackmate );
		trackmate.setLogger( wizard.getLogService() );
		trackmate.setStatusService( wizard.getLogService() );
		return new DetectionSequence( trackmate, windowManager, wizard.getLogService() );
	}

	/*
	 * Command descriptions for the detection wizard.
	 */
	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigContexts.MASTODON );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( RUN_WIZARD, RUN_WIZARD_KEYS, "Launch the detection wizard." );
		}
	}
}
