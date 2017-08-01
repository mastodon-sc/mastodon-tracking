package org.mastodon.trackmate.ui.wizard.descriptors;

import org.mastodon.trackmate.TrackMate;
import org.mastodon.trackmate.ui.wizard.WizardPanelDescriptor;
import org.scijava.Cancelable;

public class ExecuteLinkingDescriptor extends WizardPanelDescriptor
{

	public static final String IDENTIFIER = "Execute linking";

	private final TrackMate trackmate;

	public ExecuteLinkingDescriptor( final TrackMate trackmate, final LogPanel logPanel )
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
	public Runnable getForwardRunnable()
	{
		return new Runnable()
		{

			@Override
			public void run()
			{
				// Reset cancel status.
				trackmate.cancel( null );
				// Run detection.
				trackmate.execParticleLinking();
			}
		};
	}

	@Override
	public Cancelable getCancelable()
	{
		return trackmate;
	}

}