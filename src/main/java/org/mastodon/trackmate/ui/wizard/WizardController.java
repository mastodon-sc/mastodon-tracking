package org.mastodon.trackmate.ui.wizard;

import java.awt.event.ActionEvent;
import java.util.Locale;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mastodon.trackmate.ui.wizard.TransitionAnimator.Direction;
import org.mastodon.trackmate.ui.wizard.descriptors.Descriptor1;
import org.mastodon.trackmate.ui.wizard.descriptors.Descriptor2;
import org.mastodon.trackmate.ui.wizard.descriptors.Descriptor3;
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

	protected void log()
	{
		System.out.println( "log" ); // DEBUG
	}

	protected void previous()
	{
		System.out.println( "previous" ); // DEBUG
		final WizardPanelDescriptor current = wizardModel.getCurrent();
		if ( current == null || null == current.getBackPanelDescriptorIdentifier() )
			return;

		final String backId = current.getBackPanelDescriptorIdentifier();
		final WizardPanelDescriptor back = wizardModel.getDescriptor( backId );
		if ( null == back )
			return;

		display( back, current, false );
		wizardModel.setCurrent( back );
		exec( back.getBackwardRunnable() );
	}

	protected void next()
	{
		System.out.println( "next" ); // DEBUG
		final WizardPanelDescriptor current = wizardModel.getCurrent();
		if ( current == null || null == current.getNextPanelDescriptorIdentifier() )
			return;

		final String nextId = current.getNextPanelDescriptorIdentifier();
		final WizardPanelDescriptor next = wizardModel.getDescriptor( nextId );
		if ( null == next )
			return;

		display( next, current, true );
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
	}

	private void init()
	{
		final WizardPanelDescriptor descriptor = wizardModel.getCurrent();
		wizardPanel.btnPrevious.setEnabled( null != descriptor.getBackPanelDescriptorIdentifier() );
		wizardPanel.btnNext.setEnabled( null != descriptor.getNextPanelDescriptorIdentifier() );
		wizardPanel.display( descriptor );
	}

	private void display( final WizardPanelDescriptor to, final WizardPanelDescriptor from, final boolean forward )
	{
		if ( null == to )
			return;

		wizardPanel.btnPrevious.setEnabled( null != to.getBackPanelDescriptorIdentifier() );
		wizardPanel.btnNext.setEnabled( null != to.getNextPanelDescriptorIdentifier() );

		from.aboutToHidePanel();
		wizardPanel.transition( to, from, forward ? Direction.RIGHT : Direction.LEFT );
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
				log();
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

	/*
	 * MAIN METHOD
	 */

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		Locale.setDefault( Locale.ROOT );
		final JFrame frame = new JFrame( "Wrapper frame" );
		final WizardModel model = new WizardModel();
		final WizardController controller = new WizardController( model );
		final Descriptor1 d1 = new Descriptor1();
		controller.registerWizardPanel( d1 );
		controller.registerWizardPanel( new Descriptor2() );
		controller.registerWizardPanel( new Descriptor3() );
		model.setCurrent( d1 );
		controller.init();
		frame.getContentPane().add( controller.getWizardPanel() );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.setSize( 300, 500 );
		frame.setVisible( true );
	}

}
