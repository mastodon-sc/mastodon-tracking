package org.mastodon.linking;

/**
 * A specialized String value pair class used as a key into feature penalties.
 *
 * @author Jean-Yves Tinevez
 */
public class FeatureKey
{
	public final String featureKey;

	public final String projectionKey;

	public FeatureKey( final String featureKey, final String projectionKey )
	{
		this.featureKey = featureKey;
		this.projectionKey = projectionKey;
	}

	@Override
	public String toString()
	{
		if ( featureKey.equals( projectionKey ) )
			return featureKey;

		return featureKey + " → " + projectionKey;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( featureKey == null ) ? 0 : featureKey.hashCode() );
		result = prime * result + ( ( projectionKey == null ) ? 0 : projectionKey.hashCode() );
		return result;
	}

	@Override
	public boolean equals( final Object obj )
	{
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( !( obj instanceof FeatureKey ) )
			return false;
		final FeatureKey other = ( FeatureKey ) obj;
		if ( featureKey == null )
		{
			if ( other.featureKey != null )
				return false;
		}
		else if ( !featureKey.equals( other.featureKey ) )
			return false;
		if ( projectionKey == null )
		{
			if ( other.projectionKey != null )
				return false;
		}
		else if ( !projectionKey.equals( other.projectionKey ) )
			return false;
		return true;
	}
}
