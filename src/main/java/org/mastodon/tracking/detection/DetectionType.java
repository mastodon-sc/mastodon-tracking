package org.mastodon.tracking.detection;

/**
 * Specify whether intensity maxima or minima should be detected.
 */
public enum DetectionType
{
	MINIMA("bright blobs"),
	MAXIMA("dark blobs");

	private final String str;

	DetectionType( final String str )
	{
		this.str = str;
	}

	@Override
	public String toString()
	{
		return str;
	}

	public static DetectionType getOrDefault( final String name, final DetectionType defaultDetectionType )
	{
		try
		{
			return DetectionType.valueOf( name );
		}
		catch ( final IllegalArgumentException | NullPointerException e )
		{
			return defaultDetectionType;
		}
	}
}
