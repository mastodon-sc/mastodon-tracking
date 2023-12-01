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
package org.mastodon.tracking.mamut.trackmate.wizard.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.annotations.AbstractXYAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AnnotationChangeEvent;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;

public class XYTextSimpleAnnotation extends AbstractXYAnnotation
{

	private static final long serialVersionUID = 1L;

	private float x, y;

	private String text;

	private Font font;

	private Color color;

	private ChartPanel chartPanel;

	public XYTextSimpleAnnotation( final ChartPanel chartPanel )
	{
		this.chartPanel = chartPanel;
	}

	/*
	 * PUBLIC METHOD
	 */

	@Override
	public void draw( final Graphics2D g2, final XYPlot plot, final Rectangle2D dataArea,
			final ValueAxis domainAxis, final ValueAxis rangeAxis, final int rendererIndex,
			final PlotRenderingInfo info )
	{

		final Rectangle2D box = chartPanel.getScreenDataArea();
		float sx = ( float ) plot.getDomainAxis().valueToJava2D( x, box, plot.getDomainAxisEdge() );
		final float maxXLim = ( float ) box.getWidth() - g2.getFontMetrics().stringWidth( text );
		if ( sx > maxXLim )
		{
			sx = maxXLim;
		}
		if ( sx < box.getMinX() )
		{
			sx = ( float ) box.getMinX();
		}

		final float sy = ( float ) plot.getRangeAxis().valueToJava2D( y, chartPanel.getScreenDataArea(), plot.getRangeAxisEdge() );
		g2.setTransform( new AffineTransform() );
		g2.setColor( color );
		g2.setFont( font );
		g2.drawString( text, sx, sy );
	}

	public void setLocation( final float x, final float y )
	{
		this.x = x;
		this.y = y;
		notifyListeners( new AnnotationChangeEvent( this, this ) );
	}

	public void setText( final String text )
	{
		this.text = text;
	}

	public void setFont( final Font font )
	{
		this.font = font;
	}

	public void setColor( final Color color )
	{
		this.color = color;
		notifyListeners( new AnnotationChangeEvent( this, this ) );
	}

}
