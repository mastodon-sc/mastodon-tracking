package org.mastodon.tracking.mamut.trackmate.wizard.descriptors;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import org.scijava.Context;
import org.scijava.plugin.Parameter;

public class LogPanel extends JPanel
{

	public static final Color NORMAL_COLOR = Color.BLACK;

	public static final Color ERROR_COLOR = new Color( 0.8f, 0, 0 );

	public static final Color WARN_COLOR = new Color( 0.6f, 0.7f, 0 );

	public static final Color GREEN_COLOR = new Color( 0, 0.6f, 0 );

	public static final Color BLUE_COLOR = new Color( 0, 0, 0.7f );

	private static final long serialVersionUID = 1L;

	public static final String DESCRIPTOR = "LogPanel";

	@Parameter
	private Context context;

	private final JScrollPane jScrollPaneLog;

	private final JTextPane jTextPaneLog;

	private final JProgressBar jProgressBar;

	public LogPanel( final StyledDocument log )
	{
		final GridBagLayout layout = new GridBagLayout();
		layout.columnWidths = new int[] { 64 };
		layout.columnWeights = new double[] { 1. };
		layout.rowHeights = new int[] { 20, 20, 128 };
		layout.rowWeights = new double[] { 0., 0., 1. };
		this.setLayout( layout );

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridy = 0;
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.insets = new Insets( 5, 5, 5, 5 );
		gbc.fill = GridBagConstraints.HORIZONTAL;

		final JLabel title = new JLabel( "Log and status." );
		title.setFont( getFont().deriveFont( Font.BOLD ) );
		add( title, gbc );

		final Font smallFont = getFont().deriveFont( getFont().getSize2D() - 1f );

		jProgressBar = new JProgressBar();
		jProgressBar.setStringPainted( true );
		jProgressBar.setFont( smallFont );
		gbc.gridy++;
		add( jProgressBar, gbc );

		jTextPaneLog = new JTextPane( log );
		jTextPaneLog.setEditable( true );
		jTextPaneLog.setForeground( NORMAL_COLOR );
		jTextPaneLog.setFont( smallFont );
		jScrollPaneLog = new JScrollPane( jTextPaneLog );
		jTextPaneLog.setBackground( getBackground() );
		gbc.gridy++;
		gbc.fill = GridBagConstraints.BOTH;
		add( jScrollPaneLog, gbc );
	}

	/*
	 * PUBLIC METHODS
	 */

	public void clearLog()
	{
		jTextPaneLog.setText( "" );
	}

	public void setStatus( final String status )
	{
		setStatus( status, NORMAL_COLOR );
	}

	public void setStatus( final String status, final Color color )
	{
		SwingUtilities.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				jProgressBar.setForeground( color );
				jProgressBar.setString( status );
			}
		} );
	}

	public void setProgress( double val )
	{
		val = Math.min( 1., val );
		val = Math.max( 0., val );
		final int intVal = ( int ) ( val * 100 );
		SwingUtilities.invokeLater( () -> jProgressBar.setValue( intVal ) );
	}

	public void clearStatus()
	{
		SwingUtilities.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				jProgressBar.setValue( 0 );
				jProgressBar.setString( "" );
			}
		} );
	}

	public void append( final String message, final Color color )
	{
		SwingUtilities.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				final StyleContext sc = StyleContext.getDefaultStyleContext();
				final AttributeSet aset = sc.addAttribute( SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color );
				final int len = jTextPaneLog.getDocument().getLength();
				jTextPaneLog.setCaretPosition( len );
				jTextPaneLog.setCharacterAttributes( aset, false );
				jTextPaneLog.replaceSelection( message );

				final AttributeSet def = sc.addAttribute( SimpleAttributeSet.EMPTY, StyleConstants.Foreground, NORMAL_COLOR );
				jTextPaneLog.setCharacterAttributes( def, false );
			}
		} );
	}
}
