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

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.menu;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.mastodon.app.ui.ViewMenuBuilder;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.plugin.MamutPlugin;
import org.scijava.AbstractContextual;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.RunnableAction;

public abstract class WizardPlugin extends AbstractContextual implements MamutPlugin
{

	private final AbstractNamedAction runWizardAction;

	private ProjectModel appModel;

	private final String actionName;

	private final String commandName;

	private final String menuPath;

	private final String[] keyStrokes;

	/**
	 * The log document for this session.
	 */
	protected static final StyledDocument log = new DefaultStyledDocument();

	/**
	 * Instantiates a new wizard-based Mastodon plugin.
	 *
	 * @param actionName
	 *            the name of the action triggered by this plugin, and that will
	 *            launch the wizard. <i>E.g.</i> "[trackmate] run linking
	 *            wizard".
	 * @param commandName
	 *            the command name to show in the menus for this plugin.
	 *            <i>E.g.</i> "Linking wizard...".
	 * @param menuPath
	 *            the menu path in which to install this plugin. Menu path must
	 *            be specified as a string, separated bye the
	 *            '<code>&gt;</code>' char. <i>E.g.</i> "Plugins &gt;
	 *            TrackMate".
	 * @param keyStrokes
	 *            the keystrokes to associate to this plugin, as an array of
	 *            <code>String</code>s. <i>E.g.</i>
	 *            <code>new String[] { "not mapped" }</code>.
	 */
	protected WizardPlugin( final String actionName, final String commandName, final String menuPath, final String[] keyStrokes )
	{
		this.actionName = actionName;
		this.commandName = commandName;
		this.menuPath = menuPath;
		this.keyStrokes = keyStrokes;
		runWizardAction = new RunnableAction( actionName, this::runWizard );
		updateEnabledActions();
	}

	/**
	 * Returns the sequence to be used in the wizard that will be shown when
	 * this plugin is executed.
	 *
	 * @param pluginAppModel
	 *            the app model.
	 * @param wizard
	 *            the wizard that will display the sequence.
	 * @return the {@link WizardSequence}.
	 */
	public abstract WizardSequence getWizardSequence( ProjectModel pluginAppModel, Wizard wizard );


	@Override
	public void setAppPluginModel( final ProjectModel model )
	{
		this.appModel = model;
		updateEnabledActions();
	}

	@Override
	public List< ViewMenuBuilder.MenuItem > getMenuItems()
	{
		ViewMenuBuilder.MenuItem mi = item( actionName );
		final String[] split = menuPath.split( ">" );
		for ( int i = split.length - 1; i >= 0; i-- )
			mi = menu( split[ i ].trim(), mi );
		return Arrays.asList( mi );
	}

	@Override
	public Map< String, String > getMenuTexts()
	{
		final Map< String, String > menuTexts = new HashMap<>();
		menuTexts.put( actionName, commandName );
		return menuTexts;
	}

	@Override
	public void installGlobalActions( final Actions actions )
	{
		actions.namedAction( runWizardAction, keyStrokes );
	}

	private void updateEnabledActions()
	{
		runWizardAction.setEnabled( appModel != null );
	}

	private void runWizard()
	{
		if ( appModel != null )
		{
			try
			{
				final String str1 = "______________\n" + commandName;
				final String str2 = " @ " + new SimpleDateFormat( "HH:mm" ).format( new Date() ) + "\n  ";
				final SimpleAttributeSet normal = new SimpleAttributeSet();
				final SimpleAttributeSet bold = new SimpleAttributeSet( normal );
				StyleConstants.setBold( bold, true );
				log.insertString( log.getLength(), str1, bold );
				log.insertString( log.getLength(), str2, normal );
			}
			catch ( final BadLocationException e )
			{
				e.printStackTrace();
			}
			final Wizard wizard = new Wizard( log );
			appModel.getContext().inject( wizard );
			final WizardSequence sequence = getWizardSequence( appModel, wizard );
			wizard.show( sequence, commandName );
		}
	}
}
