/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2024 Tobias Pietzsch, Jean-Yves Tinevez
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

import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.Action;
import javax.swing.JLabel;

import org.mastodon.tracking.mamut.trackmate.wizard.TransitionAnimator.Direction;
import org.mastodon.tracking.mamut.trackmate.wizard.descriptors.LogDescriptor;
import org.mastodon.tracking.mamut.trackmate.wizard.util.EverythingDisablerAndReenabler;
import org.scijava.Cancelable;
import org.scijava.ui.behaviour.util.AbstractNamedAction;

public class WizardController implements WindowListener
{

	private final WizardSequence sequence;

	private final WizardPanel wizardPanel;

	private final LogDescriptor logDescriptor;

	private final WizardLogService wizardLogService;

	public WizardController( final WizardSequence sequence, final WizardLogService wizardLogService )
	{
		this.sequence = sequence;
		this.wizardLogService = wizardLogService;
		this.logDescriptor = new LogDescriptor( wizardLogService.getPanel() );
		this.wizardPanel = new WizardPanel();
		wizardPanel.btnLog.setAction( getLogAction() );
		wizardPanel.btnNext.setAction( getNextAction() );
		wizardPanel.btnPrevious.setAction( getPreviousAction() );
		wizardPanel.btnCancel.setAction( getCancelAction() );
		wizardPanel.btnCancel.setVisible( false );
		wizardPanel.btnFinish.setAction( getFinishAction() );
		wizardPanel.btnFinish.setVisible( false );
		registerWizardPanel( logDescriptor );
	}

	public WizardPanel getWizardPanel()
	{
		return wizardPanel;
	}

	public void registerWizardPanel( final WizardPanelDescriptor panel )
	{
		panel.setLogger( wizardLogService );
		panel.setStatusService( wizardLogService );
		wizardPanel.panelMain.add( panel.getPanelComponent(), panel.getPanelDescriptorIdentifier() );
	}

	protected void log( final boolean show )
	{
		final WizardPanelDescriptor current = sequence.current();

		if ( show )
		{
			display( logDescriptor, current, Direction.TOP );
			wizardPanel.btnNext.setEnabled( false );
			wizardPanel.btnPrevious.setEnabled( false );
		}
		else
		{
			display( current, logDescriptor, Direction.BOTTOM );
		}
	}

	protected synchronized void previous()
	{
		final WizardPanelDescriptor current = sequence.current();
		if ( current == null )
			return;

		current.aboutToHidePanel();
		final WizardPanelDescriptor back = sequence.previous();
		if ( null == back )
			return;

		back.targetPanel.setSize( current.targetPanel.getSize() );
		back.aboutToDisplayPanel();
		display( back, current, Direction.LEFT );
		back.displayingPanel();
		exec( back.getBackwardRunnable() );
	}

	protected synchronized void next()
	{
		final WizardPanelDescriptor current = sequence.current();
		if ( current == null )
			return;

		current.aboutToHidePanel();
		final WizardPanelDescriptor next = sequence.next();
		if ( null == next)
			return;

		next.targetPanel.setSize( current.targetPanel.getSize() );
		next.aboutToDisplayPanel();
		display( next, current, Direction.RIGHT );
		next.displayingPanel();
		exec( next.getForwardRunnable() );
	}

	protected void cancel()
	{
		final Cancelable cancelable = sequence.current().getCancelable();
		if (null != cancelable)
			cancelable.cancel( "User pressed cancel button." );
	}

	protected void finish()
	{
		Container container = wizardPanel;
		while ( !( container instanceof Frame ) )
			container = container.getParent();

		( ( Frame ) container ).dispose();
	}

	private void exec( final Runnable runnable )
	{
		if ( null == runnable )
			return;

		final EverythingDisablerAndReenabler reenabler = new EverythingDisablerAndReenabler(
				wizardPanel.panelButtons, new Class[] { JLabel.class } );
		new Thread( "Wizard exec thread" )
		{
			@Override
			public void run()
			{
				try
				{
					reenabler.disable();
					wizardPanel.btnCancel.setVisible( true );
					wizardPanel.btnCancel.setEnabled( true );
					runnable.run();
				}
				finally
				{
					wizardPanel.btnCancel.setVisible( false);
					reenabler.reenable();
				}
			};
		}.start();
	}

	public void init()
	{
		final WizardPanelDescriptor descriptor = sequence.init();
		wizardPanel.btnPrevious.setEnabled( sequence.hasPrevious() );
		wizardPanel.btnNext.setEnabled( sequence.hasNext() );
		descriptor.aboutToDisplayPanel();
		wizardPanel.display( descriptor );
		descriptor.displayingPanel();
	}

	private void display( final WizardPanelDescriptor to, final WizardPanelDescriptor from, final Direction direction )
	{
		if ( null == to )
			return;

		wizardPanel.btnPrevious.setEnabled( sequence.hasPrevious() );
		wizardPanel.btnNext.setVisible( sequence.hasNext() );
		wizardPanel.btnFinish.setVisible( !sequence.hasNext() );
		wizardPanel.btnNext.setEnabled( sequence.hasNext() );
		wizardPanel.btnFinish.setEnabled( !sequence.hasNext() );
		wizardPanel.transition( to, from, direction );
		if ( !sequence.hasNext() )
			wizardPanel.btnFinish.requestFocusInWindow();
	}

	private Action getNextAction()
	{
		final AbstractNamedAction nextAction = new AbstractNamedAction( "Next" )
		{

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				next();
			}
		};
		nextAction.putValue( Action.SMALL_ICON, WizardPanel.NEXT_ICON );
		return nextAction;
	}

	private Action getPreviousAction()
	{
		final AbstractNamedAction previousAction = new AbstractNamedAction( "Previous" )
		{

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				previous();
			}
		};
		previousAction.putValue( Action.SMALL_ICON, WizardPanel.PREVIOUS_ICON );
		return previousAction;
	}

	private Action getLogAction()
	{
		final AbstractNamedAction logAction = new AbstractNamedAction( "Log" )
		{

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				log( wizardPanel.btnLog.isSelected() );
			}
		};
		logAction.putValue( Action.SMALL_ICON, WizardPanel.LOG_ICON );
		return logAction;
	}

	private Action getCancelAction()
	{
		final AbstractNamedAction cancelAction = new AbstractNamedAction( "Cancel" )
		{

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				cancel();
			}
		};
		cancelAction.putValue( Action.SMALL_ICON, WizardPanel.CANCEL_ICON );
		return cancelAction;
	}

	private Action getFinishAction()
	{
		final AbstractNamedAction cancelAction = new AbstractNamedAction( "Finish" )
		{

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				finish();
			}
		};
		cancelAction.putValue( Action.SMALL_ICON, WizardPanel.FINISH_ICON );
		return cancelAction;
	}

	@Override
	public void windowOpened( final WindowEvent e )
	{}

	@Override
	public void windowClosing( final WindowEvent e )
	{
		final Cancelable cancelable = sequence.current().getCancelable();
		if (null != cancelable)
			cancelable.cancel( "User closed the wizard." );

		finish();
	}

	@Override
	public void windowClosed( final WindowEvent e )
	{}

	@Override
	public void windowIconified( final WindowEvent e )
	{}

	@Override
	public void windowDeiconified( final WindowEvent e )
	{}

	@Override
	public void windowActivated( final WindowEvent e )
	{}

	@Override
	public void windowDeactivated( final WindowEvent e )
	{}
}
