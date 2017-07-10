package org.mastodon.trackmate.ui.wizard;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

/**
 * Taken from
 * https://www.java-forums.org/blogs/ozzyman/1141-simple-frame-transitions-without-complex-code.html
 *
 * @author Ozzy
 */
public class Framinator
{

	public static void transitionComponents( final Container parent, final CardLayout cl, final Component from, final Component to, final int direction )
	{

		// Create the combined image depending on the direction
		BufferedImage combined;
		switch ( direction )
		{
		default:
		case TRANSITION_LEFT:
			combined = ImageHelper.combineImages(
					ImageHelper.captureComponent( to ),
					ImageHelper.captureComponent( from ),
					ImageHelper.SIDE_BY_SIDE );
			break;
		case TRANSITION_RIGHT:
			combined = ImageHelper.combineImages(
					ImageHelper.captureComponent( from ),
					ImageHelper.captureComponent( to ),
					ImageHelper.SIDE_BY_SIDE );
			break;
		}

		// Create the intermediary transition panel
		final JPanel transition = new JPanel( new GridLayout() );
		transition.add( new JLabel( new ImageIcon( combined ) ) );
		final JScrollPane scroller = new JScrollPane( transition );
		scroller.setBorder( null );
		scroller.setViewportBorder( null );
		// Hide the scroll bars
		scroller.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER );
		scroller.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );

		// Add the scroller to the CardLayout and show it
		parent.add( scroller, "transitionCard" );
		cl.show( parent, "transitionCard" );

		final long delay = 1000; // ms
		final long period = 30; // ms;

		System.out.println( "Start" ); // DEBUG

		final ScrollTransition2 scrollTransition2 = new ScrollTransition2( scroller, ( int ) ( delay / period ), direction );
		final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		final ScheduledFuture< ? > scrollMoverScheduled = scheduler.scheduleAtFixedRate( scrollTransition2, 0, period, TimeUnit.MILLISECONDS );
		scheduler.schedule( new Runnable()
		{

			@Override
			public void run()
			{
				scrollMoverScheduled.cancel( false );
				// Once the ScrollTransition has finished:
				// Remove the intermediary transition Card and show the next
				// Card
				parent.remove( scroller );
				parent.add( to, "newCard" );
				cl.show( parent, "newCard" );
			}
		}, delay, TimeUnit.MILLISECONDS );
		System.out.println( "Done" ); // DEBUG
	}

	// Some constants which define the transition direction
	public static final int TRANSITION_LEFT = 0;

	public static final int TRANSITION_RIGHT = 1;

	private static class ScrollTransition2 implements Runnable
	{

		private JScrollPane scroller;

		private int step;

		private int direction;

		private int $offset = 0;

		private int $max = 0;

		private boolean $runOnce = true;

		ScrollTransition2( final JScrollPane scroller, final int step, final int direction )
		{
			this.scroller = scroller;
			this.step = step;
			this.direction = direction;
			$max = scroller.getHorizontalScrollBar().getMaximum();
		}

		@Override
		public void run()
		{
			SwingUtilities.invokeLater( new Runnable()
			{

				@Override
				public void run()
				{

					switch ( direction )
					{
					default:
					case TRANSITION_LEFT:
						// Going Right-to-Left
						// Start at the end, scroll backwards
						if ( $runOnce )
						{
							scroller.getHorizontalScrollBar().setValue( $max );
							$offset = $max;
							$runOnce = false;
						}

						// If there is space for a step, scroll left one step
						// Else scroll right to the left
						if ( $offset > ( 0 + step ) )
						{
							$offset -= step;
							scroller.getHorizontalScrollBar().setValue( $offset );
						}
						else
						{
							scroller.getHorizontalScrollBar().setValue( 0 );
						}
						break;

					case TRANSITION_RIGHT:
						// Going Left-to-Right
						// If there is space for a step, scroll right one step
						// Else scroll right to the end
						if ( $offset < ( $max - step ) )
						{
							$offset += step;
							scroller.getHorizontalScrollBar().setValue( $offset );
						}
						else
						{
							scroller.getHorizontalScrollBar().setValue( $max );
						}
						break;
					}
				}
			} );
		}
	}
}
