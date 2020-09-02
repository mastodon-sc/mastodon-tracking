package org.mastodon.tracking.motionmodel;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;

public class RandomMotionTracker implements Tracker
{

	private final RealPoint current;

	public RandomMotionTracker(final int numDimensions)
	{
		this.current = new RealPoint( numDimensions );
	}

	@Override
	public void update( final RealLocalizable detection )
	{
		current.setPosition( detection );
	}

	@Override
	public RealLocalizable predict()
	{
		return current;
	}

	@Override
	public double costTo( final RealLocalizable position )
	{
		double squ_len = 0.0;
		for ( int d = 0; d < current.numDimensions(); ++d )
			squ_len += ( position.getDoublePosition( d ) - current.getDoublePosition( d ) ) * ( position.getDoublePosition( d ) - current.getDoublePosition( d ) );
		return squ_len;
	}

}
