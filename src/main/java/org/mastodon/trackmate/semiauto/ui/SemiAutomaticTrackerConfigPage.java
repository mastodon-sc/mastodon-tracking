package org.mastodon.trackmate.semiauto.ui;

import javax.swing.Action;
import javax.swing.JPanel;

import org.mastodon.app.ui.settings.ModificationListener;
import org.mastodon.app.ui.settings.SelectAndEditProfileSettingsPage;
import org.mastodon.app.ui.settings.style.StyleProfile;
import org.mastodon.app.ui.settings.style.StyleProfileManager;
import org.mastodon.grouping.GroupHandle;
import org.mastodon.revised.bdv.SharedBigDataViewerData;
import org.mastodon.util.Listeners;
import org.mastodon.util.Listeners.SynchronizedList;

public class SemiAutomaticTrackerConfigPage extends SelectAndEditProfileSettingsPage< StyleProfile< SemiAutomaticTrackerSettings > >
{

	public SemiAutomaticTrackerConfigPage( final String treePath, final SemiAutomaticTrackerSettingsManager settingsManager, final SharedBigDataViewerData data, final GroupHandle groupHandle, final Action trackAction, final Action cancelAction )
	{
		super( treePath,
				new StyleProfileManager<>( settingsManager, new SemiAutomaticTrackerSettingsManager( false ) ),
				new SemiAutomaticTrackerSettingsEditPanel( settingsManager.getDefaultStyle(), data, groupHandle, trackAction, cancelAction ) );
	}

	static class SemiAutomaticTrackerSettingsEditPanel implements SemiAutomaticTrackerSettings.UpdateListener, SelectAndEditProfileSettingsPage.ProfileEditPanel< StyleProfile< SemiAutomaticTrackerSettings > >
	{

		private final SemiAutomaticTrackerSettings editedSettings;

		private final SemiAutomaticTrackerConfigPanel settingsPanel;

		private boolean trackModifications = true;

		private final SynchronizedList< ModificationListener > modificationListeners;

		public SemiAutomaticTrackerSettingsEditPanel( final SemiAutomaticTrackerSettings initialSettings, final SharedBigDataViewerData data, final GroupHandle groupHandle, final Action trackAction, final Action cancelAction )
		{
			editedSettings = initialSettings.copy( "Edited" );
			settingsPanel = new SemiAutomaticTrackerConfigPanel( data, editedSettings, groupHandle, trackAction, cancelAction );
			modificationListeners = new Listeners.SynchronizedList<>();
			editedSettings.updateListeners().add( this );
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
				modificationListeners.list.forEach( ModificationListener::modified );
		}
	}
}
