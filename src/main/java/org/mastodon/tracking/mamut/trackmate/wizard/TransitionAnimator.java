/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2021 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.tracking.mamut.trackmate.wizard;

import java.awt.Component;
import java.awt.image.BufferedImage;

import bdv.viewer.animate.AbstractAnimator;

public class TransitionAnimator extends AbstractAnimator
{

	public enum Direction
	{
		LEFT, RIGHT, TOP, BOTTOM;
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
		case BOTTOM:
			combined = ImageHelper.combineImages(
					ImageHelper.captureComponent( to ),
					ImageHelper.captureComponent( from ),
					ImageHelper.BOTTOM_TO_TOP );
			break;
		case TOP:
			combined = ImageHelper.combineImages(
					ImageHelper.captureComponent( from ),
					ImageHelper.captureComponent( to ),
					ImageHelper.BOTTOM_TO_TOP );
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

	public BufferedImage get( final double t )
	{
		final int y;
		final int x;
		switch ( direction )
		{
		default:
		case LEFT:
			x = width - ( int ) Math.round( t * width );
			y = 0;
			break;
		case RIGHT:
			x = ( int ) Math.round( t * width );
			y = 0;
			break;
		case BOTTOM:
			x = 0;
			y = height - ( int ) Math.round( t * height );
			break;
		case TOP:
			x = 0;
			y = ( int ) Math.round( t * height );
			break;
		}
		return combined.getSubimage( x, y, width, height );
	}

}
