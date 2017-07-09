package org.mastodon.trackmate.ui.wizard;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;

public class WizardPanel extends JPanel
{
	private static final ImageIcon LOG_ICON = new ImageIcon( WizardPanel.class.getResource( "book.png" ) );
	private static final ImageIcon NEXT_ICON = new ImageIcon( WizardPanel.class.getResource( "arrow_right.png" ) );
	private static final ImageIcon PREVIOUS_ICON = new ImageIcon( WizardPanel.class.getResource( "arrow_left.png" ) );
	private static final ImageIcon CANCEL_ICON = new ImageIcon( WizardPanel.class.getResource( "cancel.png" ) );

	public WizardPanel()
	{
		setLayout( new BorderLayout( 0, 0 ) );

		final JPanel panelButtons = new JPanel();
		panelButtons.setBorder( new EmptyBorder( 3, 3, 3, 3 ) );
		add( panelButtons, BorderLayout.SOUTH );
		panelButtons.setLayout( new BoxLayout( panelButtons, BoxLayout.X_AXIS ) );

		final Component horizontalGlue_1 = Box.createHorizontalGlue();
		panelButtons.add( horizontalGlue_1 );

		final JToggleButton tglbtnLog = new JToggleButton( "Log", LOG_ICON, false );
		panelButtons.add( tglbtnLog );

		final Component horizontalGlue = Box.createHorizontalGlue();
		panelButtons.add( horizontalGlue );

		final JButton btnPrevious = new JButton( PREVIOUS_ICON );
		panelButtons.add( btnPrevious );

		final JButton btnNext = new JButton( "Next", NEXT_ICON );
		panelButtons.add( btnNext );

		final JPanel panelMain = new JPanel();
		add(panelMain, BorderLayout.CENTER);
		panelMain.setLayout(new CardLayout(0, 0));
	}

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		Locale.setDefault( Locale.ROOT );
		final JFrame frame = new JFrame( "Wrapper frame" );
		frame.getContentPane().add( new WizardPanel());
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.setSize( 300, 500 );
		frame.setVisible( true );
	}

}
