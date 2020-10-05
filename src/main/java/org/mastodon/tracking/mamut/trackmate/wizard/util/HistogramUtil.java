package org.mastodon.tracking.mamut.trackmate.wizard.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;

import net.imglib2.util.Util;

public class HistogramUtil
{
	static final Color ANNOTATION_COLOR = new java.awt.Color( 252, 117, 0 );

	private static final String DATA_SERIES_NAME = "Data";

	public static ChartPanel createHistogramPlot( final double[] values, final boolean withAnnotation )
	{
		final LogHistogramDataset dataset = new LogHistogramDataset();
		final double threshold;
		if ( values.length > 0 )
		{
			final int nBins = getNBins( values, 8, 100 );
			if ( nBins > 1 )
				dataset.addSeries( DATA_SERIES_NAME, values, nBins );
			threshold = otsuThreshold( values );
		}
		else
		{
			threshold = 0.;
		}

		final JFreeChart chart = ChartFactory.createHistogram( null, null, null, dataset, PlotOrientation.VERTICAL, false, false, false );
		final XYPlot plot = chart.getXYPlot();
		final XYBarRenderer renderer = ( XYBarRenderer ) plot.getRenderer();
		renderer.setShadowVisible( false );
		renderer.setMargin( 0 );
		renderer.setBarPainter( new StandardXYBarPainter() );
		renderer.setDrawBarOutline( true );
		renderer.setSeriesOutlinePaint( 0, Color.BLACK );
		renderer.setSeriesPaint( 0, new Color( 1, 1, 1, 0 ) );

		plot.setBackgroundPaint( new Color( 1, 1, 1, 0 ) );
		plot.setOutlineVisible( false );
		plot.setDomainCrosshairVisible( false );
		plot.setDomainGridlinesVisible( false );
		plot.setRangeCrosshairVisible( false );
		plot.setRangeGridlinesVisible( false );

		plot.getRangeAxis().setVisible( false );
		plot.getDomainAxis().setVisible( false );

		chart.setBorderVisible( false );
		chart.setBackgroundPaint( new Color( 0.6f, 0.6f, 0.7f ) );
		final ChartPanel chartPanel = new ChartPanel( chart );

		if ( withAnnotation )
		{

			final IntervalMarker intervalMarker = new IntervalMarker(
					0., threshold,
					new Color( 0.3f, 0.5f, 0.8f ),
					new BasicStroke(),
					new Color( 0, 0, 0.5f ),
					new BasicStroke( 1.5f ), 0.5f );
			plot.addDomainMarker( intervalMarker );

			final XYTextSimpleAnnotation annotation = new XYTextSimpleAnnotation( chartPanel );
			final Font smallFont = chartPanel.getFont().deriveFont( chartPanel.getFont().getSize2D() - 2f );
			annotation.setFont( smallFont.deriveFont( Font.BOLD ) );
			annotation.setColor( ANNOTATION_COLOR.darker() );
			annotation.setText( String.format( "%.2f", threshold ) );
			plot.addAnnotation( annotation );
		}

		return chartPanel;
	}

	/**
	 * Return the optimal bin number for a histogram of the data given in array,
	 * using the Freedman and Diaconis rule (bin_space = 2*IQR/n^(1/3)). It is
	 * ensured that the bin number returned is not smaller and no bigger than
	 * the bounds given in argument.
	 *
	 * @param values
	 *            the values to bin.
	 * @param minBinNumber
	 *            the minimal desired number of bins.
	 * @param maxBinNumber
	 *            the maximal desired number of bins.
	 * @return the number of bins.
	 */
	public static final int getNBins( final double[] values, final int minBinNumber, final int maxBinNumber )
	{
		final int size = values.length;
		final double q1 = Util.percentile( values, 0.25 );
		final double q3 = Util.percentile( values, 0.75 );
		final double iqr = q3 - q1;
		final double binWidth = 2 * iqr * Math.pow( size, -0.33 );

		final double max = Util.max( values );
		final double min = Util.min( values );
		final double range = max - min;

		int nBin = ( int ) ( range / binWidth + 1 );
		if ( nBin > maxBinNumber )
			nBin = maxBinNumber;
		else if ( nBin < minBinNumber )
			nBin = minBinNumber;
		return nBin;
	}

	private static final int getNBins( final double[] values )
	{
		return getNBins( values, 8, 256 );
	}

	/**
	 * Creates a histogram from the data given.
	 */
	private static final int[] histogram( final double[] data, final int nBins )
	{
		final double max = Util.max( data );
		final double min = Util.min( data );
		final double range = max - min;
		final double binWidth = range / nBins;
		final int[] hist = new int[ nBins ];
		int index;

		if ( nBins > 0 )
		{
			for ( int i = 0; i < data.length; i++ )
			{
				index = Math.min( ( int ) Math.floor( ( data[ i ] - min ) / binWidth ), nBins - 1 );
				hist[ index ]++;
			}
		}
		return hist;
	}

	/**
	 * Returns a threshold for the given data, using an Otsu histogram
	 * thresholding method.
	 *
	 * @param data
	 *            the data.
	 * @return the Otsu threshold.
	 */
	public static final double otsuThreshold( final double[] data )
	{
		return otsuThreshold( data, getNBins( data ) );
	}

	/**
	 * Return a threshold for the given data, using an Otsu histogram
	 * thresholding method with a given bin number.
	 */
	private static final double otsuThreshold( final double[] data, final int nBins )
	{
		final int[] hist = histogram( data, nBins );
		final int thresholdIndex = otsuThresholdIndex( hist, data.length );
		final double max = Util.max( data );
		final double min = Util.min( data );
		final double range = max - min;
		final double binWidth = range / nBins;
		return min + binWidth * thresholdIndex;
	}

	/**
	 * Given a histogram array <code>hist</code>, built with an initial amount
	 * of <code>nPoints</code> data item, this method return the bin index that
	 * thresholds the histogram in 2 classes. The threshold is performed using
	 * the Otsu Threshold Method, {@link http
	 * ://www.labbookpages.co.uk/software/imgProc/otsuThreshold.html}.
	 *
	 * @param hist
	 *            the histogram array
	 * @param nPoints
	 *            the number of data items this histogram was built on
	 * @return the bin index of the histogram that thresholds it
	 */
	private static final int otsuThresholdIndex( final int[] hist, final int nPoints )
	{
		final int total = nPoints;

		double sum = 0;
		for ( int t = 0; t < hist.length; t++ )
		{
			sum += t * hist[ t ];
		}

		double sumB = 0;
		int wB = 0;
		int wF = 0;

		double varMax = 0;
		int threshold = 0;

		for ( int t = 0; t < hist.length; t++ )
		{
			wB += hist[ t ]; // Weight Background
			if ( wB == 0 )
			{
				continue;
			}

			wF = total - wB; // Weight Foreground
			if ( wF == 0 )
			{
				break;
			}

			sumB += ( t * hist[ t ] );

			final double mB = sumB / wB; // Mean Background
			final double mF = ( sum - sumB ) / wF; // Mean Foreground

			// Calculate Between Class Variance
			final double varBetween = wB * wF * ( mB - mF ) * ( mB - mF );

			// Check if new maximum found
			if ( varBetween > varMax )
			{
				varMax = varBetween;
				threshold = t;
			}
		}
		return threshold;
	}

	private HistogramUtil()
	{}
}
