/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2022 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
