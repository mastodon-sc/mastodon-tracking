package org.mastodon.trackmate.ui.wizard;

import java.util.List;
import java.util.ListIterator;

/**
 * A simple {@link WizardSequence} based on a {@link List}.
 *
 * @author Jean-Yves Tinevez
 */
public class ListWizardSequence implements WizardSequence
{

	private ListIterator< WizardPanelDescriptor > it;

	private WizardPanelDescriptor current;

	private final List< WizardPanelDescriptor > list;

	public ListWizardSequence( final List< WizardPanelDescriptor > list )
	{
		this.list = list;
		assert !list.isEmpty(): "Sequence list is empty.";
		init();
	}

	@Override
	public WizardPanelDescriptor current()
	{
		return current;
	}

	@Override
	public WizardPanelDescriptor next()
	{
		if ( !it.hasNext() )
			return null;
		current = it.next();
		System.out.println( "Moving to next: " + current ); // DEBUG
		return current;
	}

	@Override
	public boolean hasNext()
	{
		return it.hasNext();
	}

	@Override
	public WizardPanelDescriptor previous()
	{
		if ( !it.hasPrevious() )
			return null;
		current = it.previous();
		System.out.println( "Moving to previous: " + current ); // DEBUG
		return current;
	}

	@Override
	public boolean hasPrevious()
	{
		return it.hasPrevious();
	}

	@Override
	public WizardPanelDescriptor init()
	{
		it = list.listIterator();
		current = it.next();
		return current;
	}
}
