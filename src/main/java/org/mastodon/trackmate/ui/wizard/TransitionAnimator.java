package org.mastodon.trackmate.ui.wizard;

import java.awt.Component;
import java.awt.image.BufferedImage;

import bdv.viewer.animate.AbstractAnimator;

public class TransitionAnimator extends AbstractAnimator
{

	public enum Direction
	{
		LEFT, RIGHT;
	}

	private final BufferedImage combined;

	private final int width;

	private final int height;

	private final Direction direction;


	public TransitionAnimator( final Component from, final Component to, final Direction direction, final long duration )
	{
		super( duration );
		this.direction = direction;
		switch ( direction )
		{
		default:
		case LEFT:
			combined = ImageHelper.combineImages(
					ImageHelper.captureComponent( to ),
					ImageHelper.captureComponent( from ),
					ImageHelper.SIDE_BY_SIDE );
			break;
		case RIGHT:
			combined = ImageHelper.combineImages(
					ImageHelper.captureComponent( from ),
					ImageHelper.captureComponent( to ),
					ImageHelper.SIDE_BY_SIDE );
			break;
		}

		this.width = from.getWidth();
		this.height = from.getHeight();
	}

	public BufferedImage getCurrent( final long time )
	{
		setTime( time );
		return get( ratioComplete() );
	}

	public  BufferedImage get( final double t )
	{
		final int y = 0;
		final int x;
		switch ( direction )
		{
		default:
		case LEFT:
			x = width - ( int ) Math.round( t * width );
			break;
		case RIGHT:
			x = ( int ) Math.round( t * width );
			break;
		}
		return combined.getSubimage( x, y, width, height );
	}

}
