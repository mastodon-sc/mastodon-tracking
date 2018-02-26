package org.mastodon.trackmate.ui.boundingbox;

import static org.mastodon.trackmate.ui.boundingbox.BoundingBoxOverlay.BoxDisplayMode.FULL;
import static org.mastodon.trackmate.ui.boundingbox.BoundingBoxOverlay.BoxDisplayMode.SECTION;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.WindowConstants;

import org.mastodon.trackmate.ui.boundingbox.BoundingBoxOverlay.BoxDisplayMode;
import org.mastodon.trackmate.ui.boundingbox.tobdv.BoxSelectionPanel;
import org.mastodon.trackmate.ui.boundingbox.tobdv.BoxSelectionPanel.Box;

import net.imglib2.Interval;

class BoundingBoxDialog extends JDialog
{

	private static final long serialVersionUID = 1L;

	final BoxSelectionPanel boxSelectionPanel;

	final BoxModePanel boxModePanel;

	private boolean contentCreated = false;

	public BoundingBoxDialog(
			final Frame owner,
			final String title,
			final Box interval,
			final Interval rangeInterval )
	{
		super( owner, title, false );

		// create a JPanel with sliders to modify the bounding box interval
		// (boxRealRandomAccessible.getInterval())
		boxSelectionPanel = new BoxSelectionPanel( interval, rangeInterval );

		boxModePanel = new BoxModePanel();

		setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
	}

	@Override
	public void setVisible( final boolean b )
	{
		if ( b && !contentCreated )
		{
			createContent();
			contentCreated = true;
		}
		super.setVisible( b );
	}

	// Override in subclasses
	public void createContent()
	{
		getContentPane().add( boxSelectionPanel, BorderLayout.NORTH );
		getContentPane().add( boxModePanel, BorderLayout.SOUTH );
		pack();
	}


	public static class BoxModePanel extends JPanel
	{
		private static final long serialVersionUID = 1L;

		public interface ModeChangeListener
		{
			void boxDisplayModeChanged();
		}

		private final ArrayList< ModeChangeListener > listeners;

		private BoxDisplayMode mode;

		public BoxModePanel()
		{
			listeners = new ArrayList<>();
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
			listeners.forEach( ModeChangeListener::boxDisplayModeChanged );
		}

		@Override
		public void setEnabled( final boolean b )
		{
			super.setEnabled( b );
			for ( final Component c : getComponents() )
				c.setEnabled( b );
		}

		public void addListener( final ModeChangeListener l )
		{
			listeners.add( l );
		}

		public BoxDisplayMode getBoxDisplayMode()
		{
			return mode;
		}
	}
}
