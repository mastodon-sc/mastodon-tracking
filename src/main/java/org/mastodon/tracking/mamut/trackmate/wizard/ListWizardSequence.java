package org.mastodon.tracking.mamut.trackmate.wizard;

import java.util.Collections;
import java.util.List;

/**
 * A simple {@link WizardSequence} based on a {@link List}.
 *
 * @author Jean-Yves Tinevez
 */
public class ListWizardSequence implements WizardSequence
{

	/**
	 * The list of {@link WizardPanelDescriptor}s iterated in this sequence.
	 */
	protected final List< WizardPanelDescriptor > list;

	/**
	 * The current position in the sequence.
	 */
	protected int pos;

	/**
	 * Instantiates a new sequence with a copy of the specified list.
	 * 
	 * @param list
	 *            the list to iterate in this sequence.
	 */
	public ListWizardSequence( final List< WizardPanelDescriptor > list )
	{
		this.list = Collections.unmodifiableList( list );
		this.pos = 0;
		init();
	}

	@Override
	public WizardPanelDescriptor current()
	{
		return list.get( pos );
	}

	@Override
	public WizardPanelDescriptor next()
	{
		if ( pos >= list.size() - 1 )
			return null;
		return list.get( ++pos );
	}

	@Override
	public boolean hasNext()
	{
		return pos < list.size() - 1;
	}

	@Override
	public WizardPanelDescriptor previous()
	{
		if ( pos <= 0 )
			return null;
		return list.get( --pos );
	}

	@Override
	public boolean hasPrevious()
	{
		return pos > 0;
	}

	@Override
	public WizardPanelDescriptor init()
	{
		pos = 0;
		return current();
	}
}
