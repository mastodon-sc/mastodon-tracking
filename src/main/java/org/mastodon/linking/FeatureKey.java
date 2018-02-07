package org.mastodon.linking;

/**
 * A specialized String value pair immutable class used as a key into feature penalties.
 * <p>
 * Since linking penalties relies on feature projections, we need to specify a
 * feature projection by a pair of two strings: one string for the feature key
 * in the feature model, and one string for the projection key in the feature.
 * <p>
 * This class is made so that two {@link FeatureKey} instances with the same
 * string keys will return <code>true</code> when checked for equality.
 *
 * @author Jean-Yves Tinevez
 */
public class FeatureKey
{

	/**
	 * The feature ley string.
	 */
	public final String featureKey;

	/**
	 * The projection key in the feature.
	 */
	public final String projectionKey;

	/**
	 * Constructs a feature key with the specified feature and projection keys.
	 *
	 * @param featureKey
	 *            the feature key.
	 * @param projectionKey
	 *            the projection key.
	 */
	public FeatureKey( final String featureKey, final String projectionKey )
	{
		this.featureKey = featureKey;
		this.projectionKey = projectionKey;
	}

	/**
	 * Constructs a feature key for scalar features (have one projection with identical key).
	 * @param featureKey the feature and projection key.
	 */
	public FeatureKey( final String featureKey )
	{
		this( featureKey, featureKey );
	}

	@Override
	public String toString()
	{
		if ( featureKey.equals( projectionKey ) )
			return featureKey;

		return featureKey + " → " + projectionKey;
	}

	/*
	 * Inspired from Imglib2 ValuePair.
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( featureKey == null ) ? 0 : featureKey.hashCode() );
		result = prime * result + ( ( projectionKey == null ) ? 0 : projectionKey.hashCode() );
		return result;
	}

	/*
	 * Inspired from Imglib2 ValuePair.
	 */
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
