package org.mastodon.trackmate.ui.wizard;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;

import org.mastodon.trackmate.ui.wizard.TransitionAnimator.Direction;

import net.imglib2.ui.PainterThread;
import net.imglib2.ui.PainterThread.Paintable;

public class WizardPanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	static final ImageIcon LOG_ICON = new ImageIcon( WizardPanel.class.getResource( "book.png" ) );

	static final ImageIcon NEXT_ICON = new ImageIcon( WizardPanel.class.getResource( "arrow_right.png" ) );

	static final ImageIcon PREVIOUS_ICON = new ImageIcon( WizardPanel.class.getResource( "arrow_left.png" ) );

	static final ImageIcon CANCEL_ICON = new ImageIcon( WizardPanel.class.getResource( "cancel.png" ) );

	private final CardLayout cardLayout;

	private final AnimatorPanel animatorPanel;

	final JPanel panelMain;

	final JToggleButton btnLog;

	final JButton btnPrevious;

	final JButton btnNext;

	final JButton btnCancel;

	public WizardPanel()
	{
		setLayout( new BorderLayout( 0, 0 ) );
		this.animatorPanel = new AnimatorPanel();

		final JPanel panelButtons = new JPanel();
		panelButtons.setBorder( new EmptyBorder( 3, 3, 3, 3 ) );
		add( panelButtons, BorderLayout.SOUTH );
		panelButtons.setLayout( new BoxLayout( panelButtons, BoxLayout.X_AXIS ) );

		this.btnCancel = new JButton();
		panelButtons.add( btnCancel );

		final Component horizontalGlue_1 = Box.createHorizontalGlue();
		panelButtons.add( horizontalGlue_1 );

		this.btnLog = new JToggleButton();
		panelButtons.add( btnLog );

		final Component horizontalGlue = Box.createHorizontalGlue();
		panelButtons.add( horizontalGlue );

		this.btnPrevious = new JButton();
		panelButtons.add( btnPrevious );

		btnNext = new JButton();
		panelButtons.add( btnNext );

		this.panelMain = new JPanel();
		add( panelMain, BorderLayout.CENTER );
		this.cardLayout = new CardLayout( 0, 0 );
		panelMain.setLayout( cardLayout );
	}

	public void display( final WizardPanelDescriptor current )
	{
		panelMain.add( current.getPanelComponent(), current.getPanelDescriptorIdentifier() );
		cardLayout.show( panelMain, current.getPanelDescriptorIdentifier() );
	}

	public void transition( final WizardPanelDescriptor to, final WizardPanelDescriptor from, final Direction direction )
	{
		animatorPanel.start( from, to, direction );
	}

	private class AnimatorPanel extends JPanel implements Paintable
	{
		private static final long serialVersionUID = 1L;

		private static final long duration = 200; // ms

		private final JLabel label;

		private TransitionAnimator animator;

		private WizardPanelDescriptor to;

		private final PainterThread painterThread;

		public AnimatorPanel()
		{
			super( new GridLayout() );
			this.label = new JLabel();
			add( label );
			this.painterThread = new PainterThread( this );
			painterThread.start();
		}

		public void start( final WizardPanelDescriptor from, final WizardPanelDescriptor to, final Direction direction )
		{
			this.to = to;
			this.animator = new TransitionAnimator( from.getPanelComponent(), to.getPanelComponent(), direction, duration );
			label.setIcon( new ImageIcon( animator.getCurrent( System.currentTimeMillis() ) ) );

			panelMain.add( this, "transitionCard" );
			cardLayout.show( panelMain, "transitionCard" );
		}

		private void stop()
		{
			animator = null;
			panelMain.remove( this );
			panelMain.add( to.getPanelComponent(), to.getPanelDescriptorIdentifier() );
			to.displayingPanel();
			cardLayout.show( panelMain, to.getPanelDescriptorIdentifier() );
		}

		@Override
		public void paint()
		{
			synchronized ( this )
			{
				if ( animator != null )
				{
					label.setIcon( new ImageIcon( animator.getCurrent( System.currentTimeMillis() ) ) );
					if ( animator.isComplete() )
						stop();

					repaint();
				}
			}
		}

		@Override
		public void repaint()
		{
			if ( null != painterThread )
				painterThread.requestRepaint();
		}
	}

}
