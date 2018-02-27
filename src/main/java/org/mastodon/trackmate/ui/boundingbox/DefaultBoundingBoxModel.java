package org.mastodon.trackmate.ui.boundingbox;

import org.mastodon.util.Listeners;

import bdv.tools.boundingbox.BoxSelectionPanel.Box;
import bdv.util.ModifiableInterval;
import net.imglib2.Interval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

public class DefaultBoundingBoxModel implements BoundingBoxModel, Box
{
	private final ModifiableInterval interval;

	private final AffineTransform3D transform;

	public interface IntervalChangedListener
	{
		void intervalChanged();
	}

	private final Listeners.List< IntervalChangedListener > listeners;

	public DefaultBoundingBoxModel(
			final ModifiableInterval interval,
			final AffineTransform3D transform )
	{
		this.interval = interval;
		this.transform = transform;
		listeners = new Listeners.SynchronizedList<>();
	}

	@Override
	public Interval getInterval()
	{
		return interval;
	}

	@Override
	public void setInterval( final Interval i )
	{
		if ( ! Intervals.equals( interval, i ) )
		{
			interval.set( i );
			listeners.list.forEach( IntervalChangedListener::intervalChanged );
		}
	}

	@Override
	public void setInterval( final RealInterval i )
	{
		setInterval( Intervals.smallestContainingInterval( i ) );
	}

	@Override
	public void getTransform( final AffineTransform3D t )
	{
		t.set( transform );
	}

	public Listeners< IntervalChangedListener > intervalChangedListeners()
	{
		return listeners;
	}
}
