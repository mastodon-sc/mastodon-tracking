package org.mastodon.trackmate.ui.wizard.descriptors;

import org.mastodon.trackmate.ui.wizard.WizardPanelDescriptor;

public class LogDescriptor extends WizardPanelDescriptor
{

	public static final String IDENTIFIER = "Log";

	public LogDescriptor( final LogPanel logPanel )
	{
		panelIdentifier = IDENTIFIER;
		targetPanel = logPanel;
	}

}
