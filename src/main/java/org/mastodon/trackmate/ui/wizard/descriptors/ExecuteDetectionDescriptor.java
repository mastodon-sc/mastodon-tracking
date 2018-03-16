package org.mastodon.trackmate.ui.wizard.descriptors;

import org.mastodon.trackmate.TrackMate;
import org.mastodon.trackmate.ui.wizard.WizardPanelDescriptor;
import org.scijava.Cancelable;

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
				trackmate.execDetection();
			}
		};
	}

	@Override
	public Cancelable getCancelable()
	{
		return trackmate;
	}
}
