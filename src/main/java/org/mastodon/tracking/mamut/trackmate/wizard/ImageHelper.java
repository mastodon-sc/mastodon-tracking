/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2023 Tobias Pietzsch, Jean-Yves Tinevez
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
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * Taken from
 * https://www.java-forums.org/blogs/ozzyman/1141-simple-frame-transitions-without-complex-code.html
 *
 * @author Ozzy
 */
public class ImageHelper
{

	/**
	 * Capture a Swing Component and return as a BufferedImage
	 *
	 * @param component
	 *            the component to capture.
	 * @return a new image.
	 */
	public static BufferedImage captureComponent( final Component component )
	{
		final BufferedImage image = new BufferedImage( component.getWidth(), component.getHeight(),
				BufferedImage.TYPE_INT_RGB );
		component.paint( image.getGraphics() );
		return image;
	}

	/**
	 * Some constant to define how I'd like to merge my images
	 */
	public static final int SIDE_BY_SIDE = 0;
	public static final int BOTTOM_TO_TOP = 1;

	/**
	 * Helper method to combine two images, in the specified format.
	 *
	 * @param img1
	 *            the first image to combine.
	 * @param img2
	 *            the second image to combine.
	 * @param renderHint
	 *            how to combine them.
	 * @return a new image.
	 * @see #SIDE_BY_SIDE
	 * @see #BOTTOM_TO_TOP
	 */
	public static BufferedImage combineImages( final BufferedImage img1, final BufferedImage img2, final int renderHint )
	{
		switch ( renderHint )
		{
		default:
		case SIDE_BY_SIDE:
		{
			/*
			 * Create a new image that is the width of img1+img2. Take the
			 * height of the taller image Paint the two images side-by-side.
			 */
			final BufferedImage combined = new BufferedImage( img1.getWidth() + img2.getWidth(),
					Math.max( img1.getHeight(), img2.getHeight() ), BufferedImage.TYPE_INT_RGB );
			final Graphics g = combined.getGraphics();
			g.drawImage( img1, 0, 0, null );
			g.drawImage( img2, img1.getWidth(), 0, null );
			return combined;
		}
		case BOTTOM_TO_TOP:
		{
			final BufferedImage combined = new BufferedImage(
					Math.max( img1.getWidth(), img2.getWidth() ),
					img1.getHeight() + img2.getHeight(),
					BufferedImage.TYPE_INT_RGB );
			final Graphics g = combined.getGraphics();
			g.drawImage( img1, 0, 0, null );
			g.drawImage( img2, 0, img1.getHeight(), null );
			return combined;
		}
		}
	}

}
