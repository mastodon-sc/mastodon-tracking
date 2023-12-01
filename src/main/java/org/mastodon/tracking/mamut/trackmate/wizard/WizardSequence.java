/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2023 Tobias Pietzsch, Jean-Yves Tinevez
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

/**
 * Interface for classes that allows specifying what descriptors are traversed
 * in the Wizard.
 *
 * @author Jean-Yves Tinevez
 */
public interface WizardSequence
{

	/**
	 * Performs initialization of the sequence and returns the first descriptor
	 * to display in the wizard. Calling this method again reinitializes the
	 * sequence.
	 *
	 * @return the first descriptor to show.
	 */
	public WizardPanelDescriptor init();

	/**
	 * Returns the descriptor currently displayed.
	 *
	 * @return the descriptor currently displayed
	 */
	public WizardPanelDescriptor current();

	/**
	 * Returns the next descriptor to display. Returns <code>null</code> if the
	 * sequence is finished and does not have a next descriptor.
	 *
	 * @return the next descriptor to display.
	 */
	public WizardPanelDescriptor next();

	/**
	 * Returns the previous descriptor to display. Returns <code>null</code> if
	 * the sequence is starting and does not have a previous descriptor.
	 *
	 * @return the previous descriptor to display.
	 */
	public WizardPanelDescriptor previous();

	/**
	 * Returns <code>true</code> if the sequence has an element after the
	 * current one.
	 *
	 * @return <code>true</code> if the sequence has an element after the
	 *         current one.
	 */
	public boolean hasNext();

	/**
	 * Returns <code>true</code> if the sequence has an element before the
	 * current one.
	 *
	 * @return <code>true</code> if the sequence has an element before the
	 *         current one.
	 */
	public boolean hasPrevious();
}
