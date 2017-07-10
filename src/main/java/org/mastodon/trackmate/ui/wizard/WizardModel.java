package org.mastodon.trackmate.ui.wizard;

import java.util.HashMap;
import java.util.Map;

public class WizardModel
{

	private final Map< String, WizardPanelDescriptor > descriptors;

	private WizardPanelDescriptor current;

	public WizardModel()
	{
		this.descriptors = new HashMap<>();
	}

	public void registerPanel( final WizardPanelDescriptor panelDescriptor )
	{
		descriptors.put(panelDescriptor.getPanelDescriptorIdentifier(), panelDescriptor );
	}

	public WizardPanelDescriptor getCurrent()
	{
		return current;
	}

	public void setCurrent( final WizardPanelDescriptor panelDescriptor )
	{
		current = panelDescriptor;
	}

	public WizardPanelDescriptor getDescriptor( final String id )
	{
		return descriptors.get( id );
	}

}
