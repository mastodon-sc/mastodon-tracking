package org.mastodon.trackmate.ui.boundingbox;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;

import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.OverlayRenderer;
import net.imglib2.ui.TransformListener;

public class BoundingBoxOverlay implements OverlayRenderer, TransformListener< AffineTransform3D >
{
	public static enum DisplayMode
	{
		FULL, SECTION;
	}

	public static interface BoundingBoxOverlaySource
	{
		public Interval getInterval();

		public void getIntervalTransform( final AffineTransform3D transform );
	}

	private final BoundingBoxModel model;

	private final Color backColor = new Color( 0x00994499 );// Color.MAGENTA;

	private final Color frontColor = Color.GREEN;

	private final Stroke normalStroke = new BasicStroke();

	private final Stroke intersectionStroke = new BasicStroke( 1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] { 10f, 10f }, 0f );

	private final Paint intersectionColor = Color.WHITE.darker();

	private Color cornerColor = frontColor;

	private final AffineTransform3D viewerTransform;

	final AffineTransform3D transform;

	final RenderBoxHelper renderBoxHelper;

	private double perspective = 100.;

	private int canvasWidth;

	private int canvasHeight;

	private DisplayMode displayMode = DisplayMode.FULL;

	private boolean editMode;

	private GeneralPath front;

	private GeneralPath back;

	private GeneralPath intersection;

	private Ellipse2D cornerHandle;

	int cornerId;

	public BoundingBoxOverlay( final BoundingBoxModel model )
	{
		this.model = model;
		this.viewerTransform = new AffineTransform3D();
		this.transform = new AffineTransform3D();
		this.renderBoxHelper = new RenderBoxHelper();
	}

	/**
	 * Sets the perspective value. {@code perspective < 0} means parallel
	 * projection.
	 *
	 * @param perspective
	 *            the perspective value.
	 */
	public void setPerspective( final double perspective )
	{
		this.perspective = perspective;
	}

	public void setEditMode( final boolean editMode )
	{
		this.editMode = editMode;
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		final Graphics2D graphics = ( Graphics2D ) g;
		final AffineTransform t = graphics.getTransform();

		front = new GeneralPath();
		back = new GeneralPath();
		intersection = new GeneralPath();

		final Interval interval = model.getInterval();
		final double sourceSize = Math.max( Math.max( interval.dimension( 0 ), interval.dimension( 1 ) ), interval.dimension( 2 ) );
		final double ox = canvasWidth / 2;
		final double oy = canvasHeight / 2;
		model.getIntervalTransform( transform );
		transform.preConcatenate( viewerTransform );
		renderBoxHelper.setPerspectiveProjection( perspective > 0 );
		renderBoxHelper.setDepth( perspective * sourceSize );
		renderBoxHelper.setOrigin( ox, oy );
		renderBoxHelper.setScale( 1 );
		renderBoxHelper.renderBox( interval, transform, front, back, intersection );

		final AffineTransform translate = new AffineTransform( 1, 0, 0, 1, ox, oy );
		translate.preConcatenate( t );
		graphics.setTransform( translate );
		graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

		graphics.setPaint( intersectionColor );
		graphics.setStroke( intersectionStroke );
		graphics.draw( intersection );
		if ( displayMode == DisplayMode.FULL )
		{
			graphics.setStroke( normalStroke );
			graphics.setPaint( backColor );
			graphics.draw( back );
			graphics.setPaint( frontColor );
			graphics.draw( front );
		}

		graphics.setTransform( t );

		if ( cornerHandle != null )
		{
			graphics.setColor( cornerColor );
			graphics.fill( cornerHandle );
			graphics.setColor( cornerColor.darker().darker() );
			graphics.draw( cornerHandle );
		}
	}

	@Override
	public void setCanvasSize( final int width, final int height )
	{
		this.canvasWidth = width;
		this.canvasHeight = height;
	}

	@Override
	public void transformChanged( final AffineTransform3D t )
	{
		viewerTransform.set( t );
	}

	public void setDisplayMode( final DisplayMode mode )
	{
		this.displayMode = mode;

	}

	public class CornerHighlighter extends MouseMotionAdapter
	{
		private static final double DISTANCE_TOLERANCE = 20.;

		private static final double HANDLE_RADIUS = DISTANCE_TOLERANCE / 2.;

		private static final double SQU_DISTANCE_TOLERANCE = DISTANCE_TOLERANCE * DISTANCE_TOLERANCE;

		@Override
		public void mouseMoved( final MouseEvent e )
		{
			final int x = e.getX();
			final int y = e.getY();
			cornerHandle = null;
			cornerId = -1;
			for ( int i = 0; i < renderBoxHelper.corners.length; i++ )
			{
				final double[] corner = renderBoxHelper.corners[ i ];
				final double dx = x - corner[ 0 ];
				final double dy = y - corner[ 1 ];
				final double dr2 = dx * dx + dy * dy;
				if ( dr2 < SQU_DISTANCE_TOLERANCE )
				{
					cornerId = i;
					cornerColor = ( corner[ 2 ] > 0 ) ? backColor : frontColor;
					regenFrom( corner );
					return;
				}
			}
		}

		@Override
		public void mouseDragged( final MouseEvent e )
		{
			if ( cornerId < 0 )
				return;

			regenFrom( renderBoxHelper.corners[ cornerId ] );
		}

		private void regenFrom( final double[] corner )
		{
			cornerHandle = new Ellipse2D.Double(
					corner[ 0 ] - HANDLE_RADIUS,
					corner[ 1 ] - HANDLE_RADIUS,
					2 * HANDLE_RADIUS, 2 * HANDLE_RADIUS );
			cornerColor = ( corner[ 2 ] > 0 ) ? backColor : frontColor;
		}
	}
}
