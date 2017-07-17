package org.mastodon.trackmate.ui.wizard.descriptors;

import org.mastodon.trackmate.TrackMate;
import org.mastodon.trackmate.ui.wizard.WizardPanelDescriptor;

public class ExecuteDetectionDescriptor extends WizardPanelDescriptor
{

	public static final String IDENTIFIER = "Execute detection";

	private final TrackMate trackmate;

	public ExecuteDetectionDescriptor( final TrackMate trackmate, final LogPanel logPanel )
	{
		this.trackmate = trackmate;
		this.targetPanel = logPanel;
		this.panelIdentifier = IDENTIFIER;
	}

	@Override
	public String getNextPanelDescriptorIdentifier()
	{
		return Descriptor1.ID;
	}

	@Override
	public String getBackPanelDescriptorIdentifier()
	{
		return DogDetectorDescriptor.IDENTIFIER;
	}

	@Override
	public Runnable getForwardRunnable()
	{
		return new Runnable()
		{

			@Override
			public void run()
			{
				trackmate.execDetection();
			}
		};
	}

}
