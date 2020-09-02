package org.mastodon.tracking.mamut.trackmate.wizard.descriptors;

import org.mastodon.tracking.mamut.trackmate.wizard.WizardPanelDescriptor;

public class LogDescriptor extends WizardPanelDescriptor
{

	public static final String IDENTIFIER = "Log";

	public LogDescriptor( final LogPanel logPanel )
	{
		panelIdentifier = IDENTIFIER;
		targetPanel = logPanel;
	}

}
