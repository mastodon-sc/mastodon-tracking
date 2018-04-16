package org.mastodon.trackmate;

import java.util.HashMap;
import java.util.Map;

import org.mastodon.detection.DetectionUtil;
import org.mastodon.detection.mamut.DoGDetectorMamut;
import org.mastodon.detection.mamut.SpotDetectorOp;
import org.mastodon.linking.LinkingUtils;
import org.mastodon.linking.mamut.SimpleSparseLAPLinkerMamut;
import org.mastodon.linking.mamut.SpotLinkerOp;

import bdv.spimdata.SpimDataMinimal;

public class Settings
{

	public final Values values;

	public Settings()
	{
		this( new Values() );
	}

	private Settings( final Values values )
	{
		this.values = values;
	}

	public Settings detector( final Class< ? extends SpotDetectorOp > detector )
	{
		values.detector = detector;
		return this;
	}

	public Settings detectorSettings( final Map< String, Object > detectorSettings )
	{
		values.detectorSettings = detectorSettings;
		return this;
	}

	public Settings linker( final Class< ? extends SpotLinkerOp > linker )
	{
		values.linker = linker;
		return this;
	}

	public Settings linkerSettings( final Map< String, Object > linkerSettings )
	{
		values.linkerSettings = linkerSettings;
		return this;
	}

	public Settings spimData( final SpimDataMinimal spimData )
	{
		values.spimData = spimData;
		return this;
	}

	/**
	 * Returns a copy of this settings instance.
	 * 
	 * @return a copy of this settings instance.
	 */
	public Settings copy()
	{
		return new Settings( this.values.copy() );
	}

	@Override
	public String toString()
	{
		final StringBuffer str = new StringBuffer( super.toString() + ":\n" );
		str.append( " - spimData: " + values.spimData + "\n" );
		str.append( " - detector: " + values.detector + "\n" );
		str.append( " - detector settings: @" + values.detectorSettings.hashCode() + "\n" );
		for ( final String key : values.detectorSettings.keySet() )
			str.append( "    - " + key + " = " + values.detectorSettings.get( key ) + "\n" );
		str.append( " - linker: " + values.linker + "\n" );
		str.append( " - linker settings: @" + values.linkerSettings.hashCode() + "\n" );
		for ( final String key : values.linkerSettings.keySet() )
			str.append( "    - " + key + " = " + values.linkerSettings.get( key ) + "\n" );

		return str.toString();
	}

	public static class Values
	{
		private SpimDataMinimal spimData = null;

		private Class< ? extends SpotDetectorOp > detector = DoGDetectorMamut.class;

		private Map< String, Object > detectorSettings = DetectionUtil.getDefaultDetectorSettingsMap();

		private Class< ? extends SpotLinkerOp > linker = SimpleSparseLAPLinkerMamut.class;

		private Map< String, Object > linkerSettings = LinkingUtils.getDefaultLAPSettingsMap();

		public Class< ? extends SpotDetectorOp > getDetector()
		{
			return detector;
		}

		public Values copy()
		{
			final Values v = new Values();
			v.spimData = spimData;
			v.detector = detector;
			v.detectorSettings = new HashMap<>( detectorSettings );
			v.linker = linker;
			v.linkerSettings = new HashMap<>( linkerSettings );
			return v;
		}

		public Map< String, Object > getDetectorSettings()
		{
			return detectorSettings;
		}

		public Class< ? extends SpotLinkerOp > getLinker()
		{
			return linker;
		}

		public Map< String, Object > getLinkerSettings()
		{
			return linkerSettings;
		}

		public SpimDataMinimal getSpimData()
		{
			return spimData;
		}
	}
}
