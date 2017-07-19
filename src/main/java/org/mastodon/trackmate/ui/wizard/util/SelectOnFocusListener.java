package org.mastodon.trackmate.ui.wizard.util;

import java.awt.event.FocusAdapter;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * A focus listener that selects all the content of a {@link JTextField} when it
 * gains the focus.
 *
 * @author Jean-Yves Tinevez
 */
public class SelectOnFocusListener extends FocusAdapter
{

	@Override
	public void focusGained( final java.awt.event.FocusEvent e )
	{
		final Object source = e.getSource();
		if ( source instanceof JTextField )
		{
			final JTextField tf = ( JTextField ) source;
			SwingUtilities.invokeLater( () -> tf.selectAll() );
		}
	}
}
