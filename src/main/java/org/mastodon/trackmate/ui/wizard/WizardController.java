package org.mastodon.trackmate.ui.wizard;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.mastodon.trackmate.ui.wizard.TransitionAnimator.Direction;
import org.mastodon.trackmate.ui.wizard.descriptors.LogDescriptor;
import org.scijava.ui.behaviour.util.AbstractNamedAction;

public class WizardController
{

	private final WizardModel wizardModel;

	private final WizardPanel wizardPanel;

	public WizardController( final WizardModel wizardModel )
	{
		this.wizardModel = wizardModel;
		this.wizardPanel = new WizardPanel();
		wizardPanel.btnLog.setAction( getLogAction() );
		wizardPanel.btnNext.setAction( getNextAction() );
		wizardPanel.btnPrevious.setAction( getPreviousAction() );
		wizardPanel.btnCancel.setAction( getCancelAction() );
		wizardPanel.btnCancel.setVisible( false );
	}

	public WizardPanel getWizardPanel()
	{
		return wizardPanel;
	}

	public void registerWizardPanel( final WizardPanelDescriptor panel )
	{
		wizardPanel.panelMain.add( panel.getPanelComponent(), panel.getPanelDescriptorIdentifier() );
		wizardModel.registerPanel( panel );
	}

	protected void log( final boolean show )
	{
		System.out.println( "log " + show ); // DEBUG
		final WizardPanelDescriptor logDescriptor = wizardModel.getDescriptor( LogDescriptor.IDENTIFIER );
		final WizardPanelDescriptor current = wizardModel.getCurrent();

		wizardPanel.btnNext.setEnabled( !show );
		wizardPanel.btnPrevious.setEnabled( !show );
		if ( show )
			display( logDescriptor, current, Direction.TOP );
		else
			display( current, logDescriptor, Direction.BOTTOM );

	}

	protected void previous()
	{
		System.out.println( "previous" ); // DEBUG
		final WizardPanelDescriptor current = wizardModel.getCurrent();
		if ( current == null )
			return;

		current.aboutToHidePanel();
		if ( null == current.getBackPanelDescriptorIdentifier() )
			return;

		final String backId = current.getBackPanelDescriptorIdentifier();
		final WizardPanelDescriptor back = wizardModel.getDescriptor( backId );
		if ( null == back )
			return;

		back.aboutToDisplayPanel();
		display( back, current, Direction.LEFT );
		back.displayingPanel();
		wizardModel.setCurrent( back );
		exec( back.getBackwardRunnable() );
	}

	protected void next()
	{
		final WizardPanelDescriptor current = wizardModel.getCurrent();
		if ( current == null )
			return;

		current.aboutToHidePanel();
		if ( null == current.getNextPanelDescriptorIdentifier() )
			return;

		final String nextId = current.getNextPanelDescriptorIdentifier();
		final WizardPanelDescriptor next = wizardModel.getDescriptor( nextId );
		System.out.println( "next is " + next ); // DEBUG
		if ( null == next )
			return;

		next.aboutToDisplayPanel();
		display( next, current, Direction.RIGHT );
		next.displayingPanel();
		wizardModel.setCurrent( next );
		exec( next.getForwardRunnable() );
	}

	protected void cancel()
	{
		System.out.println( "cancel" ); // DEBUG
	}

	private void exec( final Runnable runnable )
	{
		System.out.println( "executing " + runnable ); // DEBUG
		if ( null == runnable )
			return;

		new Thread()
		{
			@Override
			public void run()
			{
				runnable.run();
			};
		}.start();
	}

	public void init( final WizardPanelDescriptor descriptor )
	{
		wizardModel.setCurrent( descriptor );
		wizardPanel.btnPrevious.setEnabled( null != descriptor.getBackPanelDescriptorIdentifier() );
		wizardPanel.btnNext.setEnabled( null != descriptor.getNextPanelDescriptorIdentifier() );
		descriptor.aboutToDisplayPanel();
		wizardPanel.display( descriptor );
		descriptor.displayingPanel();
	}

	private void display( final WizardPanelDescriptor to, final WizardPanelDescriptor from, final Direction direction )
	{
		if ( null == to )
			return;

		wizardPanel.btnPrevious.setEnabled( null != to.getBackPanelDescriptorIdentifier() );
		wizardPanel.btnNext.setEnabled( null != to.getNextPanelDescriptorIdentifier() );
		wizardPanel.transition( to, from, direction );
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
}
