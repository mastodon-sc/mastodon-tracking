package org.mastodon.trackmate.ui.wizard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mastodon.detection.mamut.SpotDetectorOp;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.trackmate.PluginProvider;
import org.mastodon.trackmate.TrackMate;
import org.mastodon.trackmate.ui.wizard.descriptors.BoundingBoxDescriptor;
import org.mastodon.trackmate.ui.wizard.descriptors.ChooseDetectorDescriptor;
import org.mastodon.trackmate.ui.wizard.descriptors.ExecuteDetectionDescriptor;
import org.mastodon.trackmate.ui.wizard.descriptors.SetupIdDecriptor;
import org.mastodon.trackmate.ui.wizard.descriptors.SpotDetectorDescriptor;
import org.scijava.Context;

public class DetectionSequence implements WizardSequence
{

	private final TrackMate trackmate;

	private final WindowManager windowManager;

	private final SetupIdDecriptor setupIdDecriptor;

	private final BoundingBoxDescriptor boundingBoxDescriptor;

	private final ChooseDetectorDescriptor chooseDetectorDescriptor;

	private final ExecuteDetectionDescriptor executeDetectionDescriptor;

	/**
	 * The previously chosen SpotDetectorDescriptor (<code>null</code> if it is
	 * the first time).
	 */
	private SpotDetectorDescriptor previousDetectorPanel = null;

	private WizardPanelDescriptor current;

	private final Map< WizardPanelDescriptor, WizardPanelDescriptor > next;

	private final Map< WizardPanelDescriptor, WizardPanelDescriptor > previous;

	private final PluginProvider< SpotDetectorDescriptor > descriptorProvider;

	public DetectionSequence( final TrackMate trackmate, final WindowManager windowManager, final WizardLogService logService )
	{
		this.trackmate = trackmate;
		this.windowManager = windowManager;

		this.setupIdDecriptor = new SetupIdDecriptor( trackmate.getSettings(), logService );
		setupIdDecriptor.setContext( windowManager.getContext() );
		this.boundingBoxDescriptor = new BoundingBoxDescriptor( trackmate.getSettings(), windowManager, logService );
		boundingBoxDescriptor.setContext( windowManager.getContext() );
		this.chooseDetectorDescriptor = new ChooseDetectorDescriptor( trackmate, windowManager );
		this.executeDetectionDescriptor = new ExecuteDetectionDescriptor( trackmate, logService.getPanel() );
		this.current = init();

		this.next = getForwardSequence();
		this.previous = getBackwardSequence();

		descriptorProvider = new PluginProvider<>( SpotDetectorDescriptor.class );
		final Context context = windowManager.getContext();
		context.inject( descriptorProvider );
	}

	private Map< WizardPanelDescriptor, WizardPanelDescriptor > getBackwardSequence()
	{
		final Map< WizardPanelDescriptor, WizardPanelDescriptor > map = new HashMap<>( 5 );
		map.put( setupIdDecriptor, null );
		map.put( boundingBoxDescriptor, setupIdDecriptor );
		map.put( chooseDetectorDescriptor, boundingBoxDescriptor );
		return map;
	}

	private Map< WizardPanelDescriptor, WizardPanelDescriptor > getForwardSequence()
	{
		final Map< WizardPanelDescriptor, WizardPanelDescriptor > map = new HashMap<>( 5 );
		map.put( setupIdDecriptor, boundingBoxDescriptor );
		map.put( boundingBoxDescriptor, chooseDetectorDescriptor );
		map.put( executeDetectionDescriptor, null );
		return map;
	}

	/**
	 * Determines and registers the descriptor used to configure the detector
	 * chosen in the {@link ChooseDetectorDescriptor}.
	 *
	 * @return a suitable {@link SpotDetectorDescriptor}.
	 */
	private SpotDetectorDescriptor getConfigDescriptor()
	{
		final Class< ? extends SpotDetectorOp > detectorClass = trackmate.getSettings().values.getDetector();


		final List< String > linkerPanelNames = descriptorProvider.getNames();
		for ( final String key : linkerPanelNames )
		{
			final SpotDetectorDescriptor detectorConfigDescriptor = descriptorProvider.getInstance( key );
			if ( detectorConfigDescriptor.getTargetClasses().contains( detectorClass ) )
			{
				if ( detectorConfigDescriptor == previousDetectorPanel )
					return detectorConfigDescriptor;

				previousDetectorPanel = detectorConfigDescriptor;
				if ( detectorConfigDescriptor.getContext() == null )
					windowManager.getContext().inject( detectorConfigDescriptor );

				/*
				 * Copy as much settings as we can to the potentially new config descriptor.
				 */
				final Map< String, Object > oldSettings = new HashMap<>( trackmate.getSettings().values.getDetectorSettings() );
				final Map< String, Object > defaultSettings = detectorConfigDescriptor.getDefaultSettings();
				for ( final String skey : defaultSettings.keySet() )
					defaultSettings.put( skey, oldSettings.get( skey ) );
				trackmate.getSettings().detectorSettings( defaultSettings );

				detectorConfigDescriptor.setTrackMate( trackmate );
				detectorConfigDescriptor.setWindowManager( windowManager );
				detectorConfigDescriptor.getPanelComponent().setSize( chooseDetectorDescriptor.getPanelComponent().getSize() );
				return detectorConfigDescriptor;
			}
		}
		throw new RuntimeException( "Could not find a descriptor that can configure " + detectorClass );
	}

	@Override
	public WizardPanelDescriptor next()
	{
		if ( current == chooseDetectorDescriptor )
		{
			final SpotDetectorDescriptor configDescriptor = getConfigDescriptor();
			next.put( chooseDetectorDescriptor, configDescriptor );
			next.put( configDescriptor, executeDetectionDescriptor );
			previous.put( configDescriptor, chooseDetectorDescriptor );
			previous.put( executeDetectionDescriptor, configDescriptor );
		}

		current = next.get( current );
		return current;
	}

	@Override
	public boolean hasNext()
	{
		return current != executeDetectionDescriptor;
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
		return current != setupIdDecriptor;
	}

	@Override
	public WizardPanelDescriptor init()
	{
		return setupIdDecriptor;
	}
}
