package org.mastodon.trackmate.ui.boundingbox;

import static org.mastodon.trackmate.ui.boundingbox.BoundingBoxOverlay.BoxDisplayMode.FULL;
import static org.mastodon.trackmate.ui.boundingbox.BoundingBoxOverlay.BoxDisplayMode.SECTION;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.mastodon.trackmate.ui.boundingbox.BoundingBoxOverlay.BoxDisplayMode;
import org.mastodon.util.Listeners;

/**
 * Panel with radio-buttons to switch between {@link BoxDisplayMode}s.
 *
 * @author Tobias Pietzsch
 * @author Jean-Yves Tinevez
 */
public class BoxModePanel extends JPanel
{
	public interface ModeChangeListener
	{
		void boxDisplayModeChanged();
	}

	private static final long serialVersionUID = 1L;

	private final Listeners.List< ModeChangeListener > listeners;

	private BoxDisplayMode mode;

	public BoxModePanel()
	{
		listeners = new Listeners.SynchronizedList<>();
		mode = FULL;

		final GridBagLayout layout = new GridBagLayout();
		layout.columnWidths = new int[] { 80, 80 };
		layout.columnWeights = new double[] { 0.5, 0.5 };
		setLayout( layout );
		final GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridy = 0;
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc.insets = new Insets( 5, 5, 5, 5 );

		final JLabel overlayLabel = new JLabel( "Overlay:", JLabel.LEFT );
		overlayLabel.setFont( getFont().deriveFont( Font.BOLD ) );
		add( overlayLabel, gbc );

		gbc.gridy++;
		gbc.gridwidth = 1;
		final JRadioButton full = new JRadioButton( "Full", mode == FULL );
		full.addActionListener( e -> setBoxDisplayMode( FULL ) );
		add( full, gbc );

		gbc.gridx++;
		final JRadioButton section = new JRadioButton( "Section", mode == SECTION );
		section.addActionListener( e -> setBoxDisplayMode( SECTION ) );
		add( section, gbc );

		final ButtonGroup group = new ButtonGroup();
		group.add( full );
		group.add( section );
	}

	private void setBoxDisplayMode( final BoxDisplayMode mode )
	{
		this.mode = mode;
		listeners.list.forEach( ModeChangeListener::boxDisplayModeChanged );
	}

	@Override
	public void setEnabled( final boolean b )
	{
		super.setEnabled( b );
		for ( final Component c : getComponents() )
			c.setEnabled( b );
	}

	public BoxDisplayMode getBoxDisplayMode()
	{
		return mode;
	}

	public Listeners< ModeChangeListener > modeChangeListeners()
	{
		return listeners;
	}
}
