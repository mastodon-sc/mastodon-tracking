package org.mastodon.detection;

import static org.mastodon.detection.DetectorKeys.DEFAULT_ADD_BEHAVIOR;
import static org.mastodon.detection.DetectorKeys.DEFAULT_DETECTION_TYPE;
import static org.mastodon.detection.DetectorKeys.DEFAULT_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.DEFAULT_MIN_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.DEFAULT_RADIUS;
import static org.mastodon.detection.DetectorKeys.DEFAULT_ROI;
import static org.mastodon.detection.DetectorKeys.DEFAULT_SETUP_ID;
import static org.mastodon.detection.DetectorKeys.DEFAULT_THRESHOLD;
import static org.mastodon.detection.DetectorKeys.KEY_ADD_BEHAVIOR;
import static org.mastodon.detection.DetectorKeys.KEY_DETECTION_TYPE;
import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_RADIUS;
import static org.mastodon.detection.DetectorKeys.KEY_ROI;
import static org.mastodon.detection.DetectorKeys.KEY_SETUP_ID;
import static org.mastodon.detection.DetectorKeys.KEY_THRESHOLD;
import static org.mastodon.linking.LinkingUtils.checkMapKeys;
import static org.mastodon.linking.LinkingUtils.checkParameter;

import bdv.viewer.Source;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import mpicbg.spim.data.sequence.VoxelDimensions;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import bdv.BigDataViewer;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.transformation.ManualTransformation;
import bdv.util.Affine3DHelpers;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.Point;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.localextrema.LocalExtrema;
import net.imglib2.algorithm.localextrema.LocalExtrema.LocalNeighborhoodCheck;
import net.imglib2.algorithm.neighborhood.RectangleShape;
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
	 * Returns <code>true</code> if the there is some data at the specified
	 * time-point for the specified setup id.
	 *
	 * @param sources
	 *            the image data.
	 * @param setup
	 *            the setup id.
	 * @param timepoint
	 *            the time-point.
	 * @return <code>true</code> if there are some data at the specified
	 *         time-point for the specified setup id.
	 */
	public static final boolean isPresent( final List< SourceAndConverter< ? > > sources, final int setup, final int timepoint )
	{
		return sources.get( setup ).getSpimSource().isPresent( timepoint );
	}

	/**
	 * Tries to determine if the data is <b>really</b> there. (Largest dimension
	 * larger than 1 pixel.) It might not if some partition files are missing.
	 * Then we want to fail gracefully.
	 *
	 * @param img
	 *            the image to test presence of.
	 * @return <code>true</code> if the image is not really there.
	 */
	public static final boolean isReallyPresent( final RandomAccessibleInterval< ? > img )
	{
		final long[] dims = new long[ img.numDimensions() ];
		img.dimensions( dims );
		return Arrays.stream( dims ).max().orElse( -1l ) > 1;
	}

	/**
	 * Determines the optimal resolution level for detection of an object of a
	 * given size (in physical units).
	 * <p>
	 * The size here is specified in <b>physical units</b>. The calibration
	 * information is retrieved from the spimData to estimate the object size in
	 * pixel units.
	 * <p>
	 *
	 * Typically, even with LSFMs, the Z sampling can be much lower than in X
	 * and Y. The pixel size in Z is them much larger than in X and Y. For
	 * instance on a 25x water objective imaging on a 2048x2048 sCMOS camera the
	 * pixel size in X, Y and Z are respectively
	 * <code>[ 0.35, 0.35, 1.5 ] µm</code>. This is going to be a common case
	 * for microscopists using modern cameras.
	 * <p>
	 * There is a factor 4 between X and Z pixel sizes. The BDV conversion tool
	 * picks this up correctly, and proposes the following mipmap scales:
	 *
	 * <pre>
	 *0: [ 1 1 1
	 *1:   2 2 1
	 *2:   4 4 1
	 *3:   8 8 2 ]
	 * </pre>
	 *
	 * If we are to detect nuclei that are about 3µm in radius, we would like
	 * them to have be at most 2.5 pixels in all directions at the optimal
	 * resolution level.
	 * <p>
	 * This algorithm deals with this by doing the following:
	 * <ul>
	 * <li>Iterate to level i.
	 * <li>Compute the size of object in all dimensions.
	 * <li>Iterate to dimension d.
	 * <li>If the size of object at this dimension is smaller than the limit,
	 * then we stop at this level, but only if:
	 * <ul>
	 * <li>we lower the size of the object in this dimension even more (compared
	 * with previous level).
	 * <li>all dimensions are below the limit for the first time.
	 * </ul>
	 * </ul>
	 * With the previous example, the algorithm performs as follow:
	 * <ul>
	 * <li>Iterate to level 0.
	 * <li>At this level, the size of my object is <code>[ 9.6, 9.6, 2.0 ]
	 * pixels</code>.
	 * <li>The Z dimension has a size 2.0 pixels, below 2.5 pixels. But:
	 * <ul>
	 * <li>this is the first time,
	 * <li>and the other dimensions are above the limit.
	 * </ul>
	 * <li>Iterate to level 1.
	 * <li>At this level, the size of my object is
	 * <code>[ 4.8, 4.8, 2.0 ] pixels</code>.
	 * <li>The Z dimension has a size 2.0 pixels, below 2.5 pixels. But:
	 * <ul>
	 * <li>this is NOT the first time, but we did not decrease its size more.
	 * <li>and the other dimensions are still above the limit.
	 * </ul>
	 * <li>Iterate to level 2.
	 * <li>At this level, the size of my object is
	 * <code>[ 2.4, 2.4, 2.0 ] pixels.</code>
	 * <li>All dimensions are below the limit -&gt; we stop there.
	 * </ul>
	 * <p>
	 * If the data does not ship multiple resolution levels, this methods return
	 * 0.
	 *
	 * @param sources
	 *            the image data.
	 * @param size
	 *            the size of an object measured at resolution level 0, <b>in
	 *            physical units</b>.
	 * @param minSizePixel
	 *            the desired minimal size in pixel units of the same object in
	 *            higher resolution levels.
	 * @param timepoint
	 *            the time-point to query.
	 * @param setup
	 *            the setup id to query.
	 * @return the largest resolution level at which the object size is still
	 *         larger than the minimal desired size. Returns 0 if the data does
	 *         not ship multiple resolution levels.
	 */
	public static final int determineOptimalResolutionLevel( final List< SourceAndConverter< ? > > sources, final double size, final double minSizePixel, final int timepoint, final int setup )
	{
		final int numMipmapLevels = sources.get( setup ).getSpimSource().getNumMipmapLevels();
		int level = 0;
		final double[] previousSizeInPix = new double[ 3 ];
		Arrays.fill( previousSizeInPix, Double.POSITIVE_INFINITY );
		final boolean[] belowLimit = new boolean[ 3 ];
		Arrays.fill( belowLimit, false );
		while ( level < numMipmapLevels - 1 )
		{
			/*
			 * There is probably a more compact way to implement this algorithm,
			 * but this one expresses what we have in mind.
			 */

			final double[] calibration = getPhysicalCalibration( sources, timepoint, setup, level );
			final double[] sizeInPix = new double[ 3 ];
			for ( int d = 0; d < sizeInPix.length; d++ )
			{
				sizeInPix[ d ] = size / calibration[ d ];
				// Are we below the limit?
				if ( sizeInPix[ d ] < minSizePixel )
				{
					// Yes! Was it the case the previous level?
					if ( belowLimit[ d ] )
					{
						// Yes! But with this level, are we getting even
						// smaller?
						if ( sizeInPix[ d ] < previousSizeInPix[ d ] )
						{
							// Yes! This is not ok. We stop at the previous
							// level.
							break;
						}
						else
						{
							/*
							 * No. But know we are. If the others dimensions are
							 * fine we are fine, but we won't allow going
							 * smaller than this.
							 */
						}
					}
					// Remember that we are below limit for this dimension.
					belowLimit[ d ] = true;
				}
				else
				{
					// We are not below limit. Let's go deeper.
				}
				previousSizeInPix[ d ] = sizeInPix[ d ];
			}
			// Now that we check all dimensions, are they all below limit?
			if ( isAllTrue( belowLimit ) )
			{
				// Yes! We stop there.
				break;
			}

			level++;
		}
		return level;
	}

	private static final boolean isAllTrue( final boolean[] array )
	{
		for ( final boolean b : array )
			if ( !b )
				return false;
		return true;
	}

	/**
	 * Returns the transformation that maps the image coordinates to the global
	 * coordinate system for the specified time-point, setup id and resolution
	 * level.
	 *
	 * @param sources
	 *            the image data.
	 * @param timepoint
	 *            the time-point to query.
	 * @param setup
	 *            the setup id to query.
	 * @param level
	 *            the resolution level.
	 * @return a new transform.
	 */
	public static AffineTransform3D getTransform( final List< SourceAndConverter< ? > > sources, final int timepoint, final int setup, final int level )
	{
		final AffineTransform3D transform = new AffineTransform3D();
		sources.get( setup ).getSpimSource().getSourceTransform( timepoint, level, transform );
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
	 * @param sources
	 *            the image data.
	 * @param timepoint
	 *            the time-point to query.
	 * @param setup
	 *            the setup id to query.
	 * @param level
	 *            the resolution level.
	 * @return a new transform.
	 */
	public static AffineTransform3D getMipmapTransform( final List< SourceAndConverter< ? > > sources, final int timepoint, final int setup, final int level )
	{
		// Get transform at level L -> global coords.
		final AffineTransform3D levelL = getTransform( sources, timepoint, setup, level );
		// Get transform at level 0 -> global coords.
		final AffineTransform3D level0 = getTransform( sources, timepoint, setup, 0 );
		final AffineTransform3D transform = new AffineTransform3D();
		for ( int d = 0; d < 3; d++ )
		{
			final double scale = Affine3DHelpers.extractScale( levelL, d ) / Affine3DHelpers.extractScale( level0, d );
			transform.set( scale, d, d );
		}
		return transform;
	}

	/**
	 * Returns the physical calibration of the specified setup at the specified
	 * resolution level.
	 * <p>
	 * The physical calibration is the pixel size in whatever physical units,
	 * for each axis, possibly scaled by the resolution level. If the specified
	 * spimData does not have multiple resolution level, or if the specifed
	 * resolution level does not exist, then the physical calibration at level 0
	 * is returned.
	 *
	 * @param sources
	 *            the image data.
	 * @param timepoint
	 *            the timepoint to query.
	 * @param setup
	 *            the setup id to query.
	 * @param level
	 *            the resolution level.
	 * @return a new <code>double[]</code> array containing the pixel physical
	 *         size.
	 */
	public static double[] getPhysicalCalibration( final List< SourceAndConverter< ? > > sources, final int timepoint, final int setup, final int level )
	{
		final AffineTransform3D transform = getTransform( sources, timepoint, setup, level );
		final double physicalSizeOfGlobalUnit = getPhysicalSizeOfGlobalUnit( sources, timepoint, setup );

		final double[] calibration = new double[ 3 ];
		for ( int d = 0; d < calibration.length; d++ )
			calibration[ d ] = physicalSizeOfGlobalUnit * Affine3DHelpers.extractScale( transform, d );
		return calibration;
	}

	/**
	 * Translate the physical size of the global coordinate system unit length,
	 * as determined by the voxel dimensions of the given setup at the given timepoint.
	 *
	 * @param sources
	 *            the image data.
	 * @param timepoint
	 *            the timepoint to query.
	 * @param setup
	 *            the setup id to query.
	 * @return the physical size of the global coordinate system unit length.
	 */
	public static double getPhysicalSizeOfGlobalUnit( final List< SourceAndConverter< ? > > sources, final int timepoint, final int setup )
	{
		final Source< ? > spimSource = sources.get( setup ).getSpimSource();
		final VoxelDimensions voxelDimensions = spimSource.getVoxelDimensions();
		final double pixelWidth = ( voxelDimensions == null ) ? 1 : voxelDimensions.dimension( 0 );

		final AffineTransform3D transform = getTransform( sources, timepoint, setup, 0 );
		return pixelWidth / Affine3DHelpers.extractScale( transform, 0 );
	}

	/**
	 * Returns the image data for the specified time-point, setup id and
	 * resolution level. The image is loaded completely at once.
	 * <p>
	 * If the data does not ship multiple resolution levels, the {@code level}
	 * parameter is ignored.
	 *
	 * @param sources
	 *            the image data.
	 * @param timepoint
	 *            the time-point to query.
	 * @param setup
	 *            the setup id to query.
	 * @param level
	 *            the resolution level.
	 * @return a new transform.
	 */
	public static RandomAccessibleInterval< ? > getImage( final List< SourceAndConverter< ? > > sources, final int timepoint, final int setup, final int level )
	{
		return sources.get( setup ).getSpimSource().getSource( timepoint, level );
	}

	/**
	 * Possibly wraps and extends the specified image in a view over floats.
	 *
	 * @param <T>
	 *            the type of the pixels in the image. Must extend
	 *            {@link RealType}.
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
		final LocalNeighborhoodCheck< Point, FloatType > localNeighborhoodCheck = new LocalExtrema.MaximumCheck<>( val );
		final IntervalView< FloatType > extended = Views.interval( Views.extendMirrorSingle( source ), Intervals.expand( source, 1 ) );
		final RectangleShape shape = new RectangleShape( 1, true );
		final int numTasks = Runtime.getRuntime().availableProcessors() / 2;
		List< Point > peaks = new ArrayList<>();
		try
		{
			peaks = LocalExtrema.findLocalExtrema( extended, localNeighborhoodCheck, shape, service, numTasks );
		}
		catch ( InterruptedException | ExecutionException e )
		{
			e.printStackTrace();
		}
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
		final Map< String, Object > settings = new HashMap<>();
		settings.put( KEY_MIN_TIMEPOINT, DEFAULT_MIN_TIMEPOINT );
		settings.put( KEY_MAX_TIMEPOINT, DEFAULT_MAX_TIMEPOINT );
		settings.put( KEY_SETUP_ID, DEFAULT_SETUP_ID );
		settings.put( KEY_RADIUS, DEFAULT_RADIUS );
		settings.put( KEY_THRESHOLD, DEFAULT_THRESHOLD );
		settings.put( KEY_ROI, DEFAULT_ROI );
		settings.put( KEY_ADD_BEHAVIOR, DEFAULT_ADD_BEHAVIOR );
		settings.put( KEY_DETECTION_TYPE, DEFAULT_DETECTION_TYPE );
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
//		ok = ok & checkParameter( settings, KEY_ADD_BEHAVIOR, String.class, errorHolder );

		// Check key presence.
		final List< String > mandatoryKeys = new ArrayList<>();
		mandatoryKeys.add( KEY_SETUP_ID );
		mandatoryKeys.add( KEY_MIN_TIMEPOINT );
		mandatoryKeys.add( KEY_MAX_TIMEPOINT );
		mandatoryKeys.add( KEY_RADIUS );
		mandatoryKeys.add( KEY_THRESHOLD );
		final List< String > optionalKeys = new ArrayList<>();
		optionalKeys.add( KEY_ADD_BEHAVIOR );
		optionalKeys.add( KEY_ROI );
		optionalKeys.add( KEY_DETECTION_TYPE );
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

	public static List< SourceAndConverter< ? > > loadData( final String bdvFile ) throws SpimDataException
	{
		// Try to emulate what SharedBigDataViewerData does, without the viewer
		// thingies.
		final SpimDataMinimal spimData = new XmlIoSpimDataMinimal().load( bdvFile );
		final ArrayList< ConverterSetup > converterSetups = new ArrayList<>();
		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList<>();
		BigDataViewer.initSetups( spimData, converterSetups, sources );
		// Manual transformation.
		final ManualTransformation manualTransformation = new ManualTransformation( sources );
		if ( bdvFile.startsWith( "http://" ) )
		{
			// load settings.xml from the BigDataServer
			final String settings = bdvFile + "settings";
			{
				try
				{
					final SAXBuilder sax = new SAXBuilder();
					final Document doc = sax.build( settings );
					final Element root = doc.getRootElement();
					manualTransformation.restoreFromXml( root );
				}
				catch ( final FileNotFoundException e )
				{}
				catch ( final Exception e )
				{
					e.printStackTrace();
				}
			}
		}
		else if ( bdvFile.endsWith( ".xml" ) )
		{
			final String settings = bdvFile.substring( 0, bdvFile.length() - ".xml".length() ) + ".settings" + ".xml";
			final File proposedSettingsFile = new File( settings );
			if ( proposedSettingsFile.isFile() )
			{
				try
				{
					final SAXBuilder sax = new SAXBuilder();
					final Document doc = sax.build( proposedSettingsFile );
					final Element root = doc.getRootElement();
					manualTransformation.restoreFromXml( root );
				}
				catch ( final Exception e )
				{
					e.printStackTrace();
				}
			}
		}
		return sources;
	}

	private DetectionUtil()
	{}
}
