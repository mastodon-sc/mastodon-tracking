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
