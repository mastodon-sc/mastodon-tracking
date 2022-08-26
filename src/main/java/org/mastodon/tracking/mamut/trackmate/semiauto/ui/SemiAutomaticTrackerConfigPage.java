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

import javax.swing.Action;
import javax.swing.JPanel;

import org.mastodon.grouping.GroupHandle;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.scijava.listeners.Listeners;
import org.scijava.listeners.Listeners.SynchronizedList;

import bdv.ui.settings.ModificationListener;
import bdv.ui.settings.SelectAndEditProfileSettingsPage;
import bdv.ui.settings.style.StyleProfile;
import bdv.ui.settings.style.StyleProfileManager;

public class SemiAutomaticTrackerConfigPage extends SelectAndEditProfileSettingsPage< StyleProfile< SemiAutomaticTrackerSettings > >
{

	public SemiAutomaticTrackerConfigPage( final String treePath, final SemiAutomaticTrackerSettingsManager settingsManager, final SharedBigDataViewerData data, final GroupHandle groupHandle, final Action trackAction, final Action cancelAction )
	{
		super( treePath,
				new StyleProfileManager<>( settingsManager, new SemiAutomaticTrackerSettingsManager( false ) ),
				new SemiAutomaticTrackerSettingsEditPanel(
						settingsManager.getSelectedStyle(),
						data,
						groupHandle,
						trackAction,
						cancelAction ) );
	}

	static class SemiAutomaticTrackerSettingsEditPanel implements SemiAutomaticTrackerSettings.UpdateListener, SelectAndEditProfileSettingsPage.ProfileEditPanel< StyleProfile< SemiAutomaticTrackerSettings > >
	{

		private final SemiAutomaticTrackerSettings editedSettings;

		private final SemiAutomaticTrackerConfigPanel settingsPanel;

		private boolean trackModifications = true;

		private final SynchronizedList< ModificationListener > modificationListeners;

		public SemiAutomaticTrackerSettingsEditPanel(
				final SemiAutomaticTrackerSettings initialSettings,
				final SharedBigDataViewerData data,
				final GroupHandle groupHandle,
				final Action trackAction,
				final Action cancelAction )
		{
			editedSettings = initialSettings.copy( "Edited" );
			settingsPanel = new SemiAutomaticTrackerConfigPanel( data, editedSettings, groupHandle );
			modificationListeners = new Listeners.SynchronizedList<>();
			editedSettings.updateListeners().add( this );

			settingsPanel.btnCancel.setAction( cancelAction );
			settingsPanel.btnCancel.setText( "Cancel" );
			settingsPanel.btnTrack.setAction( trackAction );
			settingsPanel.btnTrack.setText( "Track" );
		}

		@Override
		public Listeners< ModificationListener > modificationListeners()
		{
			return modificationListeners;
		}

		@Override
		public void loadProfile( final StyleProfile< SemiAutomaticTrackerSettings > profile )
		{
			trackModifications = false;
			editedSettings.set( profile.getStyle() );
			trackModifications = true;
		}

		@Override
		public void storeProfile( final StyleProfile< SemiAutomaticTrackerSettings > profile )
		{
			trackModifications = false;
			editedSettings.setName( profile.getStyle().getName() );
			trackModifications = true;
			profile.getStyle().set( editedSettings );
		}

		@Override
		public JPanel getJPanel()
		{
			return settingsPanel;
		}

		@Override
		public void settingsChanged()
		{
			if ( trackModifications )
				modificationListeners.list.forEach( ModificationListener::setModified );
		}
	}
}
