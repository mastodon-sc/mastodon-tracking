package org.mastodon.trackmate.ui.wizard;

import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mastodon.linking.mamut.SpotLinkerOp;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.trackmate.PluginProvider;
import org.mastodon.trackmate.TrackMate;
import org.mastodon.trackmate.ui.wizard.descriptors.ChooseLinkerDescriptor;
import org.mastodon.trackmate.ui.wizard.descriptors.ExecuteLinkingDescriptor;
import org.mastodon.trackmate.ui.wizard.descriptors.LogPanel;
import org.mastodon.trackmate.ui.wizard.descriptors.SpotLinkerDescriptor;
import org.scijava.Context;

public class LinkingSequence implements WizardSequence
{

	private final TrackMate trackmate;

	private final WindowManager windowManager;

	/**
	 * The previously chosen SpotLinkerDescriptor (<code>null</code> if it is
	 * the first time).
	 */
	private SpotLinkerDescriptor previousLinkerPanel = null;

	private WizardPanelDescriptor current;

	private final Map< WizardPanelDescriptor, WizardPanelDescriptor > next;

	private final Map< WizardPanelDescriptor, WizardPanelDescriptor > previous;

	private final PluginProvider< SpotLinkerDescriptor > descriptorProvider;

	private ChooseLinkerDescriptor chooseLinkerDescriptor;

	private ExecuteLinkingDescriptor executeLinkingDescriptor;

	public LinkingSequence( final TrackMate trackmate, final WindowManager windowManager, final LogPanel logPanel )
	{
		this.trackmate = trackmate;
		this.windowManager = windowManager;

		this.chooseLinkerDescriptor = new ChooseLinkerDescriptor( trackmate, windowManager );
		this.executeLinkingDescriptor = new ExecuteLinkingDescriptor( trackmate, logPanel );

		this.next = getForwardSequence();
		this.previous = getBackwardSequence();

		descriptorProvider = new PluginProvider<>( SpotLinkerDescriptor.class );
		final Context context = windowManager.getContext();
		context.inject( descriptorProvider );

		this.current = init();
	}

	private Map< WizardPanelDescriptor, WizardPanelDescriptor > getBackwardSequence()
	{
		final Map< WizardPanelDescriptor, WizardPanelDescriptor > map = new HashMap<>( 3 );
		map.put( chooseLinkerDescriptor, null );
		return map;
	}

	private Map< WizardPanelDescriptor, WizardPanelDescriptor > getForwardSequence()
	{
		final Map< WizardPanelDescriptor, WizardPanelDescriptor > map = new HashMap<>( 5 );
		map.put( executeLinkingDescriptor, null );
		return map;
	}

	/**
	 * Determines and registers the descriptor used to configure the linker
	 * chosen in the {@link ChooseLinkerDescriptor}.
	 *
	 * @return a suitable {@link SpotLinkerDescriptor}.
	 */
	private SpotLinkerDescriptor getConfigDescriptor()
	{
		final Class< ? extends SpotLinkerOp > linkerClass = trackmate.getSettings().values.getLinker();
		final List< String > linkerPanelNames = descriptorProvider.getNames();
		for ( final String key : linkerPanelNames )
		{
			final SpotLinkerDescriptor linkerConfigDescriptor = descriptorProvider.getInstance( key );
			if ( linkerConfigDescriptor.getTargetClasses().contains( linkerClass ) )
			{
				if ( linkerConfigDescriptor == previousLinkerPanel )
					return linkerConfigDescriptor;

				previousLinkerPanel = linkerConfigDescriptor;
				if ( linkerConfigDescriptor.getContext() == null )
					windowManager.getContext().inject( linkerConfigDescriptor );
				final Map< String, Object > defaultSettings = linkerConfigDescriptor.getDefaultSettings();

				// Pass the old ROI to the new settings.
				final Map< String, Object > oldSettings = trackmate.getSettings().values.getLinkerSettings();
				defaultSettings.put( KEY_MAX_TIMEPOINT, oldSettings.get( KEY_MAX_TIMEPOINT ) );
				defaultSettings.put( KEY_MIN_TIMEPOINT, oldSettings.get( KEY_MIN_TIMEPOINT ) );

				trackmate.getSettings().linkerSettings( defaultSettings );
				linkerConfigDescriptor.setTrackMate( trackmate );
				linkerConfigDescriptor.setWindowManager( windowManager );
				linkerConfigDescriptor.getPanelComponent().setSize( chooseLinkerDescriptor.getPanelComponent().getSize() );
				return linkerConfigDescriptor;
			}
		}
		throw new RuntimeException( "Could not find a descriptor that can configure " + linkerClass );
	}

	@Override
	public WizardPanelDescriptor next()
	{
		if ( current == chooseLinkerDescriptor )
		{
			final SpotLinkerDescriptor configDescriptor = getConfigDescriptor();
			next.put( chooseLinkerDescriptor, configDescriptor );
			next.put( configDescriptor, executeLinkingDescriptor );
			previous.put( configDescriptor, chooseLinkerDescriptor );
			previous.put( executeLinkingDescriptor, configDescriptor );
		}

		current = next.get( current );
		return current;
	}

	@Override
	public boolean hasNext()
	{
		return current != executeLinkingDescriptor;
	}

	@Override
	public WizardPanelDescriptor current()
	{
		return current;
	}

	@Override
	public WizardPanelDescriptor previous()
	{
		current = previous.get( current );
		return current;
	}

	@Override
	public boolean hasPrevious()
	{
		return current != chooseLinkerDescriptor;
	}

	@Override
	public WizardPanelDescriptor init()
	{
		return chooseLinkerDescriptor;
	}
}
