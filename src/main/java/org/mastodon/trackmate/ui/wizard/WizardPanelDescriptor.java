package org.mastodon.trackmate.ui.wizard;

import java.awt.Component;

import org.scijava.Cancelable;

public class WizardPanelDescriptor
{

	protected Component targetPanel;

	protected String panelIdentifier;

	public final Component getPanelComponent()
	{
		return targetPanel;
	}

	public final void setPanelComponent( final Component panel )
	{
		targetPanel = panel;
	}

	public final String getPanelDescriptorIdentifier()
	{
		return panelIdentifier;
	}

	public final void setPanelDescriptorIdentifier( final String id )
	{
		panelIdentifier = id;
	}

	public String getNextPanelDescriptorIdentifier()
	{
		return null;
	}

	public void aboutToHidePanel()
	{}

	public void aboutToDisplayPanel()
	{}

	public void displayingPanel()
	{}

	public Runnable getForwardRunnable()
	{
		return null;
	}

	public Runnable getBackwardRunnable()
	{
		return null;
	}

	public Cancelable getCancelable()
	{
		return null;
	}

}
