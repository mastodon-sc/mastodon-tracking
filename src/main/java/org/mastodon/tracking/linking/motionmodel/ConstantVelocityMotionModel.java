package org.mastodon.tracking.linking.motionmodel;

import org.mastodon.tracking.linking.sequential.kalman.CVMKalmanFilter;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;

public class ConstantVelocityMotionModel implements MotionModel
{

	private RealPoint current;

	private CVMKalmanFilter kalmanFilter;

	private final double positionProcessStd;

	private final double velocityProcessStd;

	private final double positionMeasurementStd;

	private final double[] pos = new double[ 3 ];

	public ConstantVelocityMotionModel( final double positionProcessStd, final double velocityProcessStd, final double positionMeasurementStd )
	{
		this.positionProcessStd = positionProcessStd;
		this.velocityProcessStd = velocityProcessStd;
		this.positionMeasurementStd = positionMeasurementStd;
	}

	@Override
	public void update( final RealLocalizable detection )
	{
		if ( null == current )
		{
			current = new RealPoint( detection.numDimensions() );
			current.setPosition( detection );
			return;
		}

		if ( kalmanFilter == null )
		{
			final double[] initialState = new double[ 6 ];
			for ( int d = 0; d < 3; d++ )
			{
				initialState[ d ] = detection.getDoublePosition( d );
				initialState[ 3 + d ] = current.getDoublePosition( d - 3 ) - detection.getDoublePosition( d - 3 );
			}
			kalmanFilter = new CVMKalmanFilter( initialState, Double.MIN_NORMAL, positionProcessStd, velocityProcessStd, positionMeasurementStd );
			return;
		}

		detection.localize( pos );
		kalmanFilter.update( pos );
	}

	@Override
	public RealLocalizable predict()
	{
		if (null == current)
			throw new IllegalStateException( "Cannot predict a position before the tracker state has been updated at last once." );

		if (null == kalmanFilter)
			return current;

		current.setPosition( kalmanFilter.predict() );
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

	public static double estimatePositionProcessStd( final double maxSearchRadius )
	{
		return maxSearchRadius / 3d;
	}

	public static double estimatePositionMeasurementStd( final double maxSearchRadius )
	{
		return maxSearchRadius / 10d;
	}

	public static double estimateVelocityProcessStd( final double maxSearchRadius )
	{
		return maxSearchRadius / 3d;
	}
}
