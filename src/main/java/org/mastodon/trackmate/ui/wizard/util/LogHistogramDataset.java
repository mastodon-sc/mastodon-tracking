package org.mastodon.trackmate.ui.wizard.util;

import org.jfree.data.statistics.HistogramDataset;

/**
 * A {@link HistogramDataset} that returns the log of the count in each bin
 * (plus one), so as to have a logarithmic plot.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class LogHistogramDataset extends HistogramDataset
{

	private static final long serialVersionUID = 4407722939138628972L;

	@Override
	public Number getY( final int series, final int item )
	{
		final Number val = super.getY( series, item );
		return Math.log( 1 + val.doubleValue() );
	}
}
