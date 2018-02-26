package org.mastodon.trackmate.ui.boundingbox;

import java.awt.BorderLayout;
import java.awt.Frame;

import javax.swing.JDialog;
import javax.swing.WindowConstants;

import org.mastodon.trackmate.ui.boundingbox.tobdv.BoxSelectionPanel;
import org.mastodon.trackmate.ui.boundingbox.tobdv.BoxSelectionPanel.Box;

import net.imglib2.Interval;

class BoundingBoxDialog extends JDialog
{
	private static final long serialVersionUID = 1L;

	final BoxSelectionPanel boxSelectionPanel;

	final BoxModePanel boxModePanel;

	public BoundingBoxDialog(
			final Frame owner,
			final String title,
			final Box interval,
			final Interval rangeInterval )
	{
		super( owner, title, false );

		boxSelectionPanel = new BoxSelectionPanel( interval, rangeInterval );
		boxModePanel = new BoxModePanel();

		getContentPane().add( boxSelectionPanel, BorderLayout.NORTH );
		getContentPane().add( boxModePanel, BorderLayout.SOUTH );
		pack();

		setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
	}
}
