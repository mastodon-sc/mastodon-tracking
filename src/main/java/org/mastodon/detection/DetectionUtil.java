package org.mastodon.detection;

import static org.mastodon.detection.DetectorKeys.DEFAULT_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.DEFAULT_MIN_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.DEFAULT_RADIUS;
import static org.mastodon.detection.DetectorKeys.DEFAULT_ROI;
import static org.mastodon.detection.DetectorKeys.DEFAULT_SETUP_ID;
import static org.mastodon.detection.DetectorKeys.DEFAULT_THRESHOLD;
import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_RADIUS;
import static org.mastodon.detection.DetectorKeys.KEY_ROI;
import static org.mastodon.detection.DetectorKeys.KEY_SETUP_ID;
import static org.mastodon.detection.DetectorKeys.KEY_THRESHOLD;
import static org.mastodon.linking.LinkingUtils.checkMapKeys;
import static org.mastodon.linking.LinkingUtils.checkParameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.feature.Feature;
import org.mastodon.revised.model.feature.FeatureProjectors;
import org.mastodon.revised.model.feature.FeatureTarget;

import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.util.Affine3DHelpers;
import mpicbg.spim.data.generic.sequence.BasicMultiResolutionImgLoader;
import mpicbg.spim.data.generic.sequence.BasicMultiResolutionSetupImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.generic.sequence.ImgLoaderHints;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.Point;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.localextrema.LocalExtrema;
import net.imglib2.algorithm.localextrema.LocalExtrema.LocalNeighborhoodCheck;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * Static utilities for detection classes.
 *
 * @author Tobias Pietzsch
 * @author Jean-Yves Tinevez
 *
 */
public class DetectionUtil
{

	/**
	 * The key of the quality feature and projection returned by
	 * {@link #getQualityFeature(DoublePropertyMap)}.
	 */
	public static final String QUALITY_FEATURE_NAME = "Detection quality";

	/**
	 * Returns a new feature wrapping the specified property map, that serves as
	 * a Quality feature for the detectors of Mastodon. This feature is expected
	 * to be common to all detectors.
	 *
	 * @param quality
	 *            the property map containing the quality values of all spots in
	 *            the model.
	 * @return the quality feature.
	 */
	public static final < V > Feature< V, Double, DoublePropertyMap< V > > getQualityFeature( final DoublePropertyMap< V > quality )
	{
		return new Feature< V, Double, DoublePropertyMap< V > >(
				QUALITY_FEATURE_NAME, FeatureTarget.VERTEX, quality,
				Collections.singletonMap( QUALITY_FEATURE_NAME, FeatureProjectors.project( quality ) ) );
	}

	/**
	 * Returns <code>true</code> if the there is some data at the specified
	 * time-point for the specified setup id.
	 *
	 * @param spimData
	 *            the {@link SpimDataMinimal} linking to the image data.
	 * @param setup
	 *            the setup id.
	 * @param timepoint
	 *            the time-point.
	 * @return <code>true</code> if there are some data at the specified
	 *         time-point for the specified setup id.
	 */
	public static final boolean isPresent( final SpimDataMinimal spimData, final int setup, final int timepoint )
	{
		final BasicViewDescription< BasicViewSetup > vd = spimData.getSequenceDescription().getViewDescriptions().get( new ViewId( timepoint, setup ) );
		return ( null != vd && vd.isPresent() );
	}

	/**
	 * Determines the optimal resolution level.
	 * <p>
	 * This optimal resolution is the largest resolution level for which an
	 * object with the specified radius (measured at level 0) has a radius at
	 * least larger the specified limit.
	 * <p>
	 * If the data does not ship multiple resolution levels, this methods return
	 * 0.
	 *
	 * @param spimData
	 *            the {@link SpimDataMinimal} linking to the image data.
	 * @param size
	 *            the size of an object measured at resolution level 0.
	 * @param minSizePixel
	 *            the desired minimal size of the same object in higher
	 *            resolution levels.
	 * @param timepoint
	 *            the time-point to query.
	 * @param setup
	 *            the setup id to query.
	 * @return the largest resolution level at which the object size is still
	 *         larger than the minimal desired size. Returns 0 if the data does
	 *         not ship multiple resolution levels.
	 */
	public static final int determineOptimalResolutionLevel( final SpimDataMinimal spimData, final double size, final double minSizePixel, final int timepoint, final int setup )
	{
		final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
		if ( seq.getImgLoader() instanceof BasicMultiResolutionImgLoader )
		{
			final BasicMultiResolutionSetupImgLoader< ? > loader = ( ( BasicMultiResolutionImgLoader ) seq.getImgLoader() ).getSetupImgLoader( setup );
			final int numMipmapLevels = loader.numMipmapLevels();
			final AffineTransform3D[] mipmapTransforms = loader.getMipmapTransforms();

			int level = 0;
			while ( level < numMipmapLevels - 1 )
			{

				/*
				 * Scan all axes. The "worst" one is the one with the largest
				 * scale. If at this scale the spot is too small, then we stop.
				 */

				final AffineTransform3D t = mipmapTransforms[ level ];
				double scale = Affine3DHelpers.extractScale( t, 0 );
				for ( int axis = 1; axis < t.numDimensions(); axis++ )
				{
					final double sc = Affine3DHelpers.extractScale( t, axis );
					if ( sc > scale )
						scale = sc;
				}

				final double sizeInPix = size / scale;
				if ( sizeInPix < minSizePixel )
					break;

				level++;
			}
			return level;
		}
		else
		{
			return 0;
		}

	}

	/**
	 * Returns the transformation that maps the image coordinates to the global
	 * coordinate system for the specified time-point, setup id and resolution
	 * level.
	 * <p>
	 * If the data does not ship multiple resolution levels, the {@code level}
	 * parameter is ignored.
	 *
	 * @param spimData
	 *            the {@link SpimDataMinimal} linking to the image data.
	 * @param timepoint
	 *            the time-point to query.
	 * @param setup
	 *            the setup id to query.
	 * @param level
	 *            the resolution level.
	 * @return a new transform.
	 */
	public static AffineTransform3D getTransform( final SpimDataMinimal spimData, final int timepoint, final int setup, final int level )
	{
		final ViewId viewId = new ViewId( timepoint, setup );
		final AffineTransform3D transform = new AffineTransform3D();
		transform.set( spimData.getViewRegistrations().getViewRegistration( viewId ).getModel() );

		transform.concatenate( getMipmapTransform( spimData, timepoint, setup, level ) );
		return transform;
	}

	/**
	 * Returns the transformation that maps the image coordinates at level 0 to
	 * the image coordinates at the level specified, for the specified
	 * time-point, setup id.
	 * <p>
	 * If the data does not ship multiple resolution levels, the identity
	 * transform is returned.
	 *
	 * @param spimData
	 *            the {@link SpimDataMinimal} linking to the image data.
	 * @param timepoint
	 *            the time-point to query.
	 * @param setup
	 *            the setup id to query.
	 * @param level
	 *            the resolution level.
	 * @return a new transform.
	 */
	public static AffineTransform3D getMipmapTransform( final SpimDataMinimal spimData, final int timepoint, final int setup, final int level )
	{
		final AffineTransform3D transform = new AffineTransform3D();
		final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
		if ( seq.getImgLoader() instanceof BasicMultiResolutionImgLoader )
		{
			final BasicMultiResolutionSetupImgLoader< ? > loader = ( ( BasicMultiResolutionImgLoader ) seq.getImgLoader() ).getSetupImgLoader( setup );
			final AffineTransform3D[] mipmapTransforms = loader.getMipmapTransforms();
			final AffineTransform3D mipmapTransform = mipmapTransforms[ level ];
			transform.set( mipmapTransform );
		}
		return transform;
	}

	public static double getResolutionLevelScale( final SpimDataMinimal spimData, final int timepoint, final int setup, final int level )
	{
		final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
		if ( seq.getImgLoader() instanceof BasicMultiResolutionImgLoader )
		{
			final BasicMultiResolutionSetupImgLoader< ? > loader = ( ( BasicMultiResolutionImgLoader ) seq.getImgLoader() ).getSetupImgLoader( setup );
			final AffineTransform3D[] mipmapTransforms = loader.getMipmapTransforms();
			final AffineTransform3D t = mipmapTransforms[ level ];
			double scale = Affine3DHelpers.extractScale( t, 0 );
			for ( int axis = 1; axis < t.numDimensions(); axis++ )
			{
				final double sc = Affine3DHelpers.extractScale( t, axis );
				if ( sc > scale )
					scale = sc;
			}
			return scale;
		}
		else
		{
			return 1.;
		}
	}

	/**
	 * Returns the image data for the specified time-point, setup id and
	 * resolution level. The image is loaded completely at once.
	 * <p>
	 * If the data does not ship multiple resolution levels, the {@code level}
	 * parameter is ignored.
	 *
	 * @param spimData
	 *            the {@link SpimDataMinimal} linking to the image data.
	 * @param timepoint
	 *            the time-point to query.
	 * @param setup
	 *            the setup id to query.
	 * @param level
	 *            the resolution level.
	 * @return a new transform.
	 */
	public static RandomAccessibleInterval< ? > getImage( final SpimDataMinimal spimData, final int timepoint, final int setup, final int level )
	{
		final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
		if ( seq.getImgLoader() instanceof BasicMultiResolutionImgLoader )
		{
			final BasicMultiResolutionSetupImgLoader< ? > loader = ( ( BasicMultiResolutionImgLoader ) seq.getImgLoader() ).getSetupImgLoader( setup );
			final RandomAccessibleInterval< ? > img = loader
					.getImage( timepoint, level, ImgLoaderHints.LOAD_COMPLETELY );
			return img;
		}
		else
		{
			final RandomAccessibleInterval< ? > img = spimData.getSequenceDescription().getImgLoader().getSetupImgLoader( setup )
					.getImage( timepoint, ImgLoaderHints.LOAD_COMPLETELY );
			return img;
		}
	}

	/**
	 * Possibly wraps and extends the specified image in a view over floats.
	 *
	 * @param img
	 *            the image to wrap.
	 * @return an extend view of the image over floats.
	 */
	@SuppressWarnings( "unchecked" )
	public static final < T extends RealType< T > > RandomAccessible< FloatType > asExtendedFloat( final RandomAccessibleInterval< T > img )
	{
		if ( Util.getTypeFromInterval( img ) instanceof FloatType )
		{
			return Views.extendMirrorSingle( ( RandomAccessibleInterval< FloatType > ) img );
		}
		else
		{
			final RealFloatConverter< T > converter = new RealFloatConverter<>();
			return Views.extendMirrorSingle( Converters.convert( img, converter, new FloatType() ) );
		}
	}

	public static final List< Point > findLocalMaxima(
			final RandomAccessibleInterval< FloatType > source,
			final double threshold,
			final ExecutorService service )
	{
		final FloatType val = new FloatType();
		val.setReal( threshold );
		final LocalNeighborhoodCheck< Point, FloatType > localNeighborhoodCheck = new LocalExtrema.MaximumCheck< FloatType >( val );
		final IntervalView< FloatType > extended = Views.interval( Views.extendMirrorSingle( source ), Intervals.expand( source, 1 ) );
		final List< Point > peaks = LocalExtrema.findLocalExtrema( extended, localNeighborhoodCheck, service );
		return peaks;
	}

	/**
	 * Returns a new settings map filled with default values suitable for the
	 * default detectors.
	 *
	 * @return a new map.
	 */
	public static final Map< String, Object > getDefaultDetectorSettingsMap()
	{
		final Map< String, Object > settings = new HashMap< String, Object >();
		settings.put( KEY_MIN_TIMEPOINT, DEFAULT_MIN_TIMEPOINT );
		settings.put( KEY_MAX_TIMEPOINT, DEFAULT_MAX_TIMEPOINT );
		settings.put( KEY_SETUP_ID, DEFAULT_SETUP_ID );
		settings.put( KEY_RADIUS, DEFAULT_RADIUS );
		settings.put( KEY_THRESHOLD, DEFAULT_THRESHOLD );
		settings.put( KEY_ROI, DEFAULT_ROI );
		return settings;
	}

	/**
	 * Checks whether the provided settings map is suitable for use with the
	 * default detectors.
	 *
	 * @param settings
	 *            the map to test.
	 * @param errorHolder
	 *            a {@link StringBuilder} that will contain an error message if
	 *            the check is not successful.
	 * @return true if the settings map can be used with the default detectors.
	 */
	public static final boolean checkSettingsValidity( final Map< String, Object > settings, final StringBuilder errorHolder )
	{
		if ( null == settings )
		{
			errorHolder.append( "Settings map is null.\n" );
			return false;
		}

		boolean ok = true;
		// Check proper class.
		ok = ok & checkParameter( settings, KEY_SETUP_ID, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_MIN_TIMEPOINT, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_MAX_TIMEPOINT, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_RADIUS, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_THRESHOLD, Double.class, errorHolder );

		// Check key presence.
		final List< String > mandatoryKeys = new ArrayList< String >();
		mandatoryKeys.add( KEY_SETUP_ID );
		mandatoryKeys.add( KEY_MIN_TIMEPOINT );
		mandatoryKeys.add( KEY_MAX_TIMEPOINT );
		mandatoryKeys.add( KEY_RADIUS );
		mandatoryKeys.add( KEY_THRESHOLD );
		final List< String > optionalKeys = new ArrayList< String >();
		optionalKeys.add( KEY_ROI );
		ok = ok & checkMapKeys( settings, mandatoryKeys, optionalKeys, errorHolder );

		// Check min & max time-point.
		final int minTimepoint = ( int ) settings.get( KEY_MIN_TIMEPOINT );
		final int maxTimepoint = ( int ) settings.get( KEY_MAX_TIMEPOINT );
		if ( maxTimepoint < minTimepoint )
		{
			ok = false;
			errorHolder.append( "Min time-point should smaller than or equal to max time-point, be was min = "
					+ minTimepoint + " and max = " + maxTimepoint + "\n" );
		}

		return ok;
	}

	private DetectionUtil()
	{}

}
