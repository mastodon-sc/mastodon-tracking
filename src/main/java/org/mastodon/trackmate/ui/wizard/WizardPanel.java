package org.mastodon.trackmate.ui.wizard;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;

public class WizardPanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	static final ImageIcon LOG_ICON = new ImageIcon( WizardPanel.class.getResource( "book.png" ) );

	static final ImageIcon NEXT_ICON = new ImageIcon( WizardPanel.class.getResource( "arrow_right.png" ) );

	static final ImageIcon PREVIOUS_ICON = new ImageIcon( WizardPanel.class.getResource( "arrow_left.png" ) );

	static final ImageIcon CANCEL_ICON = new ImageIcon( WizardPanel.class.getResource( "cancel.png" ) );

	private final CardLayout cardLayout;

	final JPanel panelMain;

	final JToggleButton btnLog;

	final JButton btnPrevious;

	final JButton btnNext;

	final JButton btnCancel;

	public WizardPanel()
	{
		setLayout( new BorderLayout( 0, 0 ) );

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
}
