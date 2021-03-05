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
package org.mastodon.tracking.mamut.trackmate.wizard.util;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.Layer;

public class FilterPanel extends javax.swing.JPanel
{

	private static final long serialVersionUID = 1L;

	private final ChangeEvent CHANGE_EVENT = new ChangeEvent( this );

	private final ArrayList< ChangeListener > listeners = new ArrayList< ChangeListener >();

	private final ChartPanel chartPanel;

	private final double[] values;

	private final JRadioButton jRadioButtonBelow;

	private final JRadioButton jRadioButtonAbove;

	private double threshold;

	/*
	 * CONSTRUCTOR
	 */

	public FilterPanel( final double[] values )
	{
		this.values = values;
		this.threshold = HistogramUtil.otsuThreshold( values );

		final GridBagLayout layout = new GridBagLayout();
		layout.rowWeights = new double[] { 1.0, 0.0, 0.0 };
		layout.rowHeights = new int[] { 0, 0, 0 };
		layout.columnWeights = new double[] { 0.0, 0.0, 1.0 };
		layout.columnWidths = new int[] { 0, 0, 0 };
		setLayout( layout );
		setBorder( new LineBorder( HistogramUtil.ANNOTATION_COLOR, 1, true ) );
		final GridBagConstraints gbc = new GridBagConstraints();

		/*
		 * Create the histogram plot and wire it to treshold thingie.
		 */

		chartPanel = HistogramUtil.createHistogramPlot( values, true );
		final XYPlot plot = chartPanel.getChart().getXYPlot();
		final XYTextSimpleAnnotation annotation = ( XYTextSimpleAnnotation ) plot.getAnnotations().get( 0 );

		final MouseListener[] mls = chartPanel.getMouseListeners();
		for ( final MouseListener ml : mls )
			chartPanel.removeMouseListener( ml );

		chartPanel.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseClicked( final MouseEvent e )
			{
				chartPanel.requestFocusInWindow();
				threshold = getXFromChartEvent( e );
				redrawThresholdMarker();
			}
		} );
		chartPanel.addMouseMotionListener( new MouseMotionListener()
		{
			@Override
			public void mouseMoved( final MouseEvent e )
			{}

			@Override
			public void mouseDragged( final MouseEvent e )
			{
				threshold = getXFromChartEvent( e );
				redrawThresholdMarker();
			}
		} );
		chartPanel.setFocusable( true );
		chartPanel.addFocusListener( new FocusListener()
		{

			@Override
			public void focusLost( final FocusEvent e )
			{
				annotation.setColor( HistogramUtil.ANNOTATION_COLOR.darker() );
			}

			@Override
			public void focusGained( final FocusEvent e )
			{
				annotation.setColor( Color.RED.darker() );
			}
		} );
		chartPanel.addKeyListener( new MyKeyListener() );

		chartPanel.setOpaque( false );
		gbc.gridwidth = 3;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 0;
		add( chartPanel, gbc );

		final JButton jButtonAutoThreshold = new JButton();
		jButtonAutoThreshold.setText( "Auto" );
		jButtonAutoThreshold.setFont( getFont().deriveFont( getFont().getSize2D() - 2f ) );
		jButtonAutoThreshold.addActionListener( ( e ) -> autoThreshold() );
		gbc.gridy++;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets( 0, 0, 0, 10 );
		add( jButtonAutoThreshold, gbc );

		jRadioButtonAbove = new JRadioButton();
		jRadioButtonAbove.setText( "Above" );
		jRadioButtonAbove.setFont( getFont().deriveFont( getFont().getSize2D() - 2f ) );
		jRadioButtonAbove.addActionListener( ( e ) -> redrawThresholdMarker() );
		gbc.gridx++;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets( 0, 10, 0, 0 );
		add( jRadioButtonAbove, gbc );

		jRadioButtonBelow = new JRadioButton();
		jRadioButtonBelow.setText( "Below" );
		jRadioButtonBelow.addActionListener( ( e ) -> redrawThresholdMarker() );
		jRadioButtonBelow.setFont( getFont().deriveFont( getFont().getSize2D() - 2f ) );
		gbc.gridx++;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.insets = new Insets( 0, 5, 0, 0 );
		add( jRadioButtonBelow, gbc );

		final ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add( jRadioButtonAbove );
		buttonGroup.add( jRadioButtonBelow );
		jRadioButtonAbove.setSelected( true );

		redrawThresholdMarker();
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Sets the threshold currently selected for the data displayed in this
	 * panel.
	 *
	 * @param threshold
	 *            the threshold to set.
	 *
	 * @see #isAboveThreshold()
	 */
	public void setThreshold( final double threshold )
	{
		this.threshold = threshold;
		redrawThresholdMarker();
	}

	/**
	 * Sets whether the current threshold should be taken above or below its
	 * value.
	 *
	 * @param isAbove
	 *            if <code>true</code>, the threshold will be related as above
	 *            its value.
	 */
	public void setAboveThreshold( final boolean isAbove )
	{
		jRadioButtonAbove.setSelected( isAbove );
		jRadioButtonBelow.setSelected( !isAbove );
		redrawThresholdMarker();
	}

	/**
	 * Returns the threshold currently selected for the data displayed in this
	 * panel.
	 *
	 * @return the threshold.
	 * @see #isAboveThreshold()
	 */
	public double getThreshold()
	{
		return threshold;
	}

	/**
	 * Returns <code>true</code> if the user selected the above threshold option
	 * for the data displayed in this panel.
	 *
	 * @return <code>true</code> if the thresholding is made for values larger
	 *         than the threshold, <code>false</code> otherwise.
	 * @see #getThreshold()
	 */
	public boolean isAboveThreshold()
	{
		return jRadioButtonAbove.isSelected();
	}

	/**
	 * Adds an {@link ChangeListener} to this panel. The {@link ChangeListener}
	 * will be notified when a change happens to the threshold displayed by this
	 * panel, whether due to the slider being move, the auto-threshold button
	 * being pressed, or the combo-box selection being changed.
	 *
	 * @param listener
	 *            the listener to add.
	 */
	public void addChangeListener( final ChangeListener listener )
	{
		listeners.add( listener );
	}

	/**
	 * Removes a ChangeListener.
	 *
	 * @param listener
	 *            the listener to add.
	 * @return true if the listener was in listener collection of this instance.
	 */
	public boolean removeChangeListener( final ChangeListener listener )
	{
		return listeners.remove( listener );
	}

	public Collection< ChangeListener > getChangeListeners()
	{
		return listeners;
	}

	/*
	 * PRIVATE METHODS
	 */

	private void fireThresholdChanged()
	{
		for ( final ChangeListener al : listeners )
			al.stateChanged( CHANGE_EVENT );
	}

	private void autoThreshold()
	{
		threshold = HistogramUtil.otsuThreshold( values );
		redrawThresholdMarker();
	}

	private double getXFromChartEvent( final MouseEvent mouseEvent )
	{
		final Rectangle2D plotArea = chartPanel.getScreenDataArea();
		final XYPlot plot = chartPanel.getChart().getXYPlot();
		return plot.getDomainAxis().java2DToValue( mouseEvent.getX(), plotArea, plot.getDomainAxisEdge() );
	}

	private void redrawThresholdMarker()
	{
		final XYPlot plot = chartPanel.getChart().getXYPlot();
		final IntervalMarker intervalMarker = ( IntervalMarker ) plot.getDomainMarkers( Layer.FOREGROUND ).iterator().next();
		if ( jRadioButtonAbove.isSelected() )
		{
			intervalMarker.setStartValue( threshold );
			intervalMarker.setEndValue( plot.getDomainAxis().getUpperBound() );
		}
		else
		{
			intervalMarker.setStartValue( plot.getDomainAxis().getLowerBound() );
			intervalMarker.setEndValue( threshold );
		}
		float x, y;
		if ( threshold > 0.85 * plot.getDomainAxis().getUpperBound() )
		{
			x = ( float ) ( threshold - 0.15 * plot.getDomainAxis().getRange().getLength() );
		}
		else
		{
			x = ( float ) ( threshold + 0.05 * plot.getDomainAxis().getRange().getLength() );
		}

		y = ( float ) ( 0.85 * plot.getRangeAxis().getUpperBound() );
		final XYTextSimpleAnnotation annotation = ( XYTextSimpleAnnotation ) plot.getAnnotations().get( 0 );
		annotation.setText( String.format( "%.2f", threshold ) );
		annotation.setLocation( x, y );
		fireThresholdChanged();
	}

	/**
	 * A class that listen to the user typing a number, building a string
	 * representation as he types, then converting the string to a double after
	 * a wait time. The number typed is used to set the threshold in the chart
	 * panel.
	 *
	 * @author Jean-Yves Tinevez
	 */
	private final class MyKeyListener implements KeyListener
	{

		private static final long WAIT_DELAY = 1; // s

		private static final double INCREASE_FACTOR = 0.1;

		private String strNumber = "";

		private ScheduledExecutorService ex;

		private ScheduledFuture< ? > future;

		private boolean dotAdded = false;

		private final Runnable command = new Runnable()
		{
			@Override
			public void run()
			{
				// Convert to double and pass it to threshold value
				try
				{
					final double typedThreshold = NumberFormat.getInstance().parse( strNumber ).doubleValue();
					threshold = typedThreshold;
					redrawThresholdMarker();
				}
				catch ( final ParseException nfe )
				{}
				// Reset
				ex = null;
				strNumber = "";
				dotAdded = false;
			}
		};

		@Override
		public void keyPressed( final KeyEvent e )
		{
			final XYPlot plot = chartPanel.getChart().getXYPlot();
			// Is it arrow keys?
			if ( e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_KP_LEFT )
			{
				threshold -= INCREASE_FACTOR * plot.getDomainAxis().getRange().getLength();
				redrawThresholdMarker();
				return;
			}
			else if ( e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_KP_RIGHT )
			{
				threshold += INCREASE_FACTOR * plot.getDomainAxis().getRange().getLength();
				redrawThresholdMarker();
				return;
			}
			else if ( e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_KP_UP )
			{
				threshold = plot.getDomainAxis().getRange().getUpperBound();
				redrawThresholdMarker();
				return;
			}
			else if ( e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_KP_DOWN )
			{
				threshold = plot.getDomainAxis().getRange().getLowerBound();
				redrawThresholdMarker();
				return;
			}
		}

		@Override
		public void keyReleased( final KeyEvent e )
		{}

		@Override
		public void keyTyped( final KeyEvent e )
		{

			if ( e.getKeyChar() < '0' || e.getKeyChar() > '9' )
			{
				// Ok then it's number

				if ( !dotAdded && e.getKeyChar() == '.' )
				{
					// User added a decimal dot for the first and only time
					dotAdded = true;
				}
				else
				{
					return;
				}
			}

			if ( ex == null )
			{
				// Create new waiting line
				ex = Executors.newSingleThreadScheduledExecutor();
				future = ex.schedule( command, WAIT_DELAY, TimeUnit.SECONDS );
			}
			else
			{
				// Reset waiting line
				future.cancel( false );
				future = ex.schedule( command, WAIT_DELAY, TimeUnit.SECONDS );
			}
			strNumber += e.getKeyChar();
		}
	}

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		Locale.setDefault( Locale.ROOT );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		// Prepare fake data
		final int N_ITEMS = 100;
		final Random ran = new Random();
		double mean;

		final double[] val = new double[ N_ITEMS ];
		mean = ran.nextDouble() * 10;
		for ( int j = 0; j < val.length; j++ )
			val[ j ] = ran.nextGaussian() + 5 + mean;

		// Create GUI
		final FilterPanel tp = new FilterPanel( val );
		final JFrame frame = new JFrame();
		frame.getContentPane().add( tp );
		frame.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		frame.pack();
		frame.setVisible( true );
	}
}
