package org.mastodon.trackmate.ui.wizard;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class WizardModel
{

	private final Map< String, WizardPanelDescriptor > descriptors;

	private final Deque< String > descriptorStack;

	private WizardPanelDescriptor current;

	public WizardModel()
	{
		this.descriptors = new HashMap<>();
		this.descriptorStack = new ArrayDeque<>();
	}

	public void registerPanel( final WizardPanelDescriptor panelDescriptor )
	{
		descriptors.put( panelDescriptor.getPanelDescriptorIdentifier(), panelDescriptor );
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

	/**
	 * Stores the specified descriptor id on the previous descriptor id stack.
	 *
	 * @param descriptorId
	 *            the id to store.
	 */
	public void pushDescriptorId( final String descriptorId )
	{
		descriptorStack.push( descriptorId );
	}

	/**
	 * Pops and returns the previous descriptor id, or <code>null</code> if
	 * there are no previous descriptor id.
	 *
	 * @return the previous descriptor id.
	 */
	public String popPreviousDescriptorId()
	{
		return descriptorStack.pollFirst();
	}

	/**
	 * Returns <code>true</code> if there is at least one descriptor id stored
	 * in the previous stack, <code>false</code> otherwise.
	 *
	 * @return <code>true</code> if there is at least one descriptor id stored
	 *         in the previous stack.
	 */
	public boolean hasPrevious()
	{
		return !descriptorStack.isEmpty();
	}

}
