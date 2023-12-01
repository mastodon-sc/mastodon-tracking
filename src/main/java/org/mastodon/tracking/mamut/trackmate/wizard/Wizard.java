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

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;

import org.mastodon.app.MastodonIcons;
import org.scijava.Context;
import org.scijava.plugin.Parameter;

public class Wizard
{

	@Parameter
	private Context context;

	private final JFrame frame;

	private final WizardLogService wizardLogService;

	public Wizard( final StyledDocument log )
	{
		this.frame = new JFrame();
		this.wizardLogService = new WizardLogService( log );
	}

	public Wizard()
	{
		this( new DefaultStyledDocument() );
	}

	public WizardLogService getLogService()
	{
		return wizardLogService;
	}

	public void show( final WizardSequence sequence, final String title )
	{
		context.inject( wizardLogService );
		final WizardController controller = new WizardController( sequence, wizardLogService );
		frame.getContentPane().removeAll();
		frame.getContentPane().add( controller.getWizardPanel() );
		frame.setSize( 300, 600 );
		frame.setTitle( title );
		controller.init();
		frame.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
		frame.addWindowListener( controller );
		frame.setIconImages( MastodonIcons.MASTODON_ICON );
		frame.setLocationByPlatform( true );
		frame.setLocationRelativeTo( null );
		frame.setVisible( true );
	}
}
