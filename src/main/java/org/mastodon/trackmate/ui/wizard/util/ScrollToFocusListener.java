package org.mastodon.trackmate.ui.wizard.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JPanel;

/**
 * A focus listener that will scroll a master {@link JPanel} to make it visible.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class ScrollToFocusListener extends FocusAdapter
{
	private final Rectangle rect = new Rectangle();

	private final JPanel master;

	public ScrollToFocusListener( final JPanel master )
	{
		this.master = master;
	}

	@Override
	public void focusGained( final FocusEvent evt )
	{
		final Component focusedComponent = evt.getComponent();
		focusedComponent.getBounds( rect );
		getRelativeBounds( focusedComponent );
		master.scrollRectToVisible( rect );
		master.repaint();
	}

	private void getRelativeBounds( final Component component )
	{
		Container parent = component.getParent();
		while ( parent != master )
		{
			rect.x += parent.getX();
			rect.y += parent.getY();
			parent = parent.getParent();
		}
	}
}
