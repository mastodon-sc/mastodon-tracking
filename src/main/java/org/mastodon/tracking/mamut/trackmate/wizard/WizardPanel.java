/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2021 Tobias Pietzsch, Jean-Yves Tinevez
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

import org.mastodon.tracking.mamut.trackmate.wizard.TransitionAnimator.Direction;

import bdv.viewer.render.PainterThread;
import bdv.viewer.render.PainterThread.Paintable;

public class WizardPanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	static final ImageIcon LOG_ICON = new ImageIcon( WizardPanel.class.getResource( "book.png" ) );

	static final ImageIcon NEXT_ICON = new ImageIcon( WizardPanel.class.getResource( "arrow_right.png" ) );

	static final ImageIcon PREVIOUS_ICON = new ImageIcon( WizardPanel.class.getResource( "arrow_left.png" ) );

	static final ImageIcon CANCEL_ICON = new ImageIcon( WizardPanel.class.getResource( "cancel.png" ) );

	static final ImageIcon FINISH_ICON = new ImageIcon( WizardPanel.class.getResource( "accept-icon.png" ) );

	private final CardLayout cardLayout;

	private final AnimatorPanel animatorPanel;

	final JPanel panelMain;

	final JToggleButton btnLog;

	final JButton btnPrevious;

	final JButton btnNext;

	final JButton btnCancel;

	final JButton btnFinish;

	final JPanel panelButtons;

	public WizardPanel()
	{
		setLayout( new BorderLayout( 0, 0 ) );
		this.animatorPanel = new AnimatorPanel();

		this.panelButtons = new JPanel();
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

		btnFinish = new JButton();
		panelButtons.add( btnFinish );

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
