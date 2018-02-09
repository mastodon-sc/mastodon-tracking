package org.mastodon.linking.sequential.kalman;

import org.mastodon.pool.ByteMappedElement;
import org.mastodon.pool.PoolObject;
import org.mastodon.pool.attributes.RealPointAttributeValue;
import org.mastodon.util.DelegateRealLocalizable;
import org.mastodon.util.DelegateRealPositionable;

class Prediction extends PoolObject< Prediction, PredictionPool, ByteMappedElement >
implements DelegateRealLocalizable, DelegateRealPositionable, Comparable< Prediction >
{
	private final RealPointAttributeValue position;

	Prediction( final PredictionPool pool )
	{
		super( pool );
		position = pool.position.createAttributeValue( this );
	}

	public Prediction init( final double... pos )
	{
		pool.position.setPositionQuiet( this, pos );
		return this;
	}

	@Override
	protected void setToUninitializedState()
	{}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		char c = '(';
		for ( int i = 0; i < numDimensions(); i++ )
		{
			sb.append( c );
			sb.append( getDoublePosition( i ) );
			c = ',';
		}
		sb.append( ")" );
		return sb.toString();
	}

	@Override
	public RealPointAttributeValue delegate()
	{
		return position;
	}

	@Override
	public int compareTo( final Prediction o )
	{
		/*
		 * Sort based on X, Y, Z
		 */
		int i = 0;
		while ( i < numDimensions() )
		{
			if ( getDoublePosition( i ) != o.getDoublePosition( i ) ) { return ( int ) Math.signum( getDoublePosition( i ) - o.getDoublePosition( i ) ); }
			i++;
		}
		return hashCode() - o.hashCode();
	}
}
