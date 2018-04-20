package org.mastodon.trackmate.semiauto;

import static org.mastodon.detection.DetectorKeys.KEY_SETUP_ID;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_ALLOW_LINKING_IF_HAS_INCOMING;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_ALLOW_LINKING_IF_HAS_OUTGOING;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_ALLOW_LINKING_TO_EXISTING;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_CONTINUE_IF_LINK_EXISTS;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_DISTANCE_FACTOR;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_FORWARD_IN_TIME;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_N_TIMEPOINTS;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_QUALITY_FACTOR;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_RESOLUTION_LEVEL;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.NEIGHBORHOOD_FACTOR;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.checkSettingsValidity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.mastodon.HasErrorMessage;
import org.mastodon.detection.DetectionUtil;
import org.mastodon.detection.DoGDetectorOp;
import org.mastodon.linking.LinkingUtils;
import org.mastodon.model.NavigationHandler;
import org.mastodon.model.SelectionModel;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.bdv.overlay.util.JamaEigenvalueDecomposition;
import org.mastodon.revised.model.feature.Feature;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.util.EllipsoidInsideTest;
import org.scijava.Cancelable;
import org.scijava.ItemIO;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import bdv.spimdata.SpimDataMinimal;
import bdv.util.Affine3DHelpers;
import mpicbg.spim.data.sequence.TimePoint;
import net.imagej.ops.OpService;
import net.imagej.ops.special.computer.AbstractBinaryComputerOp;
import net.imglib2.FinalInterval;
import net.imglib2.Point;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.dog.DogDetection.ExtremaType;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.neighborsearch.NearestNeighborSearch;
import net.imglib2.position.transform.Round;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

@Plugin( type = SemiAutomaticTracker.class )
public class SemiAutomaticTracker
		extends AbstractBinaryComputerOp< Collection< Spot >, Map< String, Object >, Model >
		implements HasErrorMessage, Cancelable
{

	@Parameter
	private ThreadService threadService;

	@Parameter
	private OpService ops;

	@Parameter( type = ItemIO.INPUT )
	private SpimDataMinimal spimData;

	@Parameter
	private NavigationHandler< Spot, Link > navigationHandler;

	@Parameter
	private SelectionModel< Spot, Link > selectionModel;

	@Parameter( required = false )
	private Logger log;

	@Parameter
	private LogService logService;

	@Parameter( type = ItemIO.OUTPUT )
	protected String errorMessage;

	@Parameter( type = ItemIO.OUTPUT )
	protected boolean ok;

	private final Comparator< RefinedPeak< ? > > refinedPeakComparator = new RefinedPeakComparator();

	private final JamaEigenvalueDecomposition eig = new JamaEigenvalueDecomposition( 3 );

	@Override
	public void compute( final Collection< Spot > input, final Map< String, Object > settings, final Model model )
	{
		if ( null == log )
			log = logService;

		if ( null == input || input.isEmpty() )
		{
			log.info( "Spot collection to track is empty. Stopping." );
			return;
		}

		final ModelGraph graph = model.getGraph();
		final SpatioTemporalIndex< Spot > spatioTemporalIndex = model.getSpatioTemporalIndex();

		/*
		 * Clean selection if we have one.
		 */

		if ( null != selectionModel )
			selectionModel.clearSelection();

		/*
		 * Quality and link-cost features. If they do not exist, create them and
		 * register them.
		 */

		@SuppressWarnings( "unchecked" )
		Feature< Spot, DoublePropertyMap< Spot > > qualityFeature =
				( Feature< Spot, DoublePropertyMap< Spot > > ) model.getFeatureModel().getFeature( DetectionUtil.QUALITY_FEATURE_NAME );
		if ( null == qualityFeature )
		{
			final DoublePropertyMap< Spot > quality = new DoublePropertyMap<>( graph.vertices(), Double.NaN );
			qualityFeature = DetectionUtil.getQualityFeature( quality, Spot.class );
			model.getFeatureModel().declareFeature( qualityFeature );
		}

		@SuppressWarnings( "unchecked" )
		Feature< Link, DoublePropertyMap< Link > > linkCostFeature =
				( Feature< Link, DoublePropertyMap< Link > > ) model.getFeatureModel().getFeature( LinkingUtils.LINK_COST_FEATURE_NAME );
		if ( null == linkCostFeature )
		{
			final DoublePropertyMap< Link > linkcost = new DoublePropertyMap<>( graph.edges(), Double.NaN );
			linkCostFeature = LinkingUtils.getLinkCostFeature( linkcost, Link.class );
			model.getFeatureModel().declareFeature( linkCostFeature );
		}

		/*
		 * Check settings map.
		 */

		ok = false;
		final StringBuilder errorHandler = new StringBuilder();
		if ( !checkSettingsValidity( settings, errorHandler ) )
		{
			errorMessage = errorHandler.toString();
			return;
		}

		/*
		 * Extract parameters from map.
		 */

		final int setup = ( int ) settings.get( KEY_SETUP_ID );
		final double qualityFactor = ( double ) settings.get( KEY_QUALITY_FACTOR );
		final double distanceFactor = ( double ) settings.get( KEY_DISTANCE_FACTOR );
		final boolean forward = ( boolean ) settings.get( KEY_FORWARD_IN_TIME );
		final int nTimepoints = ( int ) settings.get( KEY_N_TIMEPOINTS );
		final boolean allowLinkingToExisting = ( boolean ) settings.get( KEY_ALLOW_LINKING_TO_EXISTING );
		final boolean allowLinkingIfIncoming = ( boolean ) settings.get( KEY_ALLOW_LINKING_IF_HAS_INCOMING );
		final boolean allowLinkingIfOutgoing = ( boolean ) settings.get( KEY_ALLOW_LINKING_IF_HAS_OUTGOING );
		final boolean continueIfLinkExists = ( boolean ) settings.get( KEY_CONTINUE_IF_LINK_EXISTS );
		final double neighborhoodFactor = Math.max( NEIGHBORHOOD_FACTOR, distanceFactor + 1. );
		int resolutionLevel = -1;
		if ( settings.containsKey( KEY_RESOLUTION_LEVEL ) )
			resolutionLevel = ( int ) settings.get( KEY_RESOLUTION_LEVEL );

		/*
		 * Units.
		 */

		final String units = spimData.getSequenceDescription().getViewSetups().get( Integer.valueOf( setup ) ).getVoxelSize().unit();

		/*
		 * First and last time-points.
		 */

		final List< TimePoint > tps = spimData.getSequenceDescription().getTimePoints().getTimePointsOrdered();
		final int minTimepoint = tps.get( 0 ).getId();
		final int maxTimepoint = tps.get( tps.size() - 1 ).getId();

		/*
		 * Loop over each spot input.
		 */

		final double[][] cov = new double[ 3 ][ 3 ];
		final EllipsoidInsideTest test = new EllipsoidInsideTest();

		INPUT: for ( final Spot first : input )
		{
			final int firstTimepoint = first.getTimepoint();
			int tp = firstTimepoint;
			final Spot source = model.getGraph().vertexRef();
			source.refTo( first );

			log.info( "Semi-automatic tracking from spot " + first.getLabel() + ", going " + ( forward ? "forward" : "backward" ) + " in time." );
			TIME: while ( Math.abs( tp - firstTimepoint ) < nTimepoints
					&& ( forward ? tp < maxTimepoint : tp > minTimepoint ) )
			{
				// Are we canceled?
				if ( isCanceled() )
					return;

				tp = ( forward ? tp + 1 : tp - 1 );

				// Check if there is some data at this timepoint.
				if ( !DetectionUtil.isPresent( spimData, setup, tp ) )
					continue TIME;

				/*
				 * Determine optimal level for detection.
				 */

				// Best radius is smallest radius of ellipse.
				source.getCovariance( cov );
				eig.decomposeSymmetric( cov );
				final double[] eigVals = eig.getRealEigenvalues();
				double minEig = Double.POSITIVE_INFINITY;
				for ( int k = 0; k < eigVals.length; k++ )
					minEig = Math.min( minEig, eigVals[ k ] );
				final double physicalRadius = Math.sqrt( minEig );

				final double[] calibration = DetectionUtil.getPhysicalCalibration( spimData, setup );
				final double radius = physicalRadius / calibration[ 0 ]; // pixels

				final int nResolutionLevels = DetectionUtil.getNResolutionLevels( spimData, setup );
				final int level;
				if ( resolutionLevel < 0 )
					level = DetectionUtil.determineOptimalResolutionLevel( spimData, radius, DoGDetectorOp.MIN_SPOT_PIXEL_SIZE / 2., tp, setup );
				else
					level = Math.min( nResolutionLevels - 1, resolutionLevel );
				final AffineTransform3D transform = DetectionUtil.getTransform( spimData, tp, setup, level );

				/*
				 * Load and extends image data.
				 */

				final RandomAccessibleInterval< ? > img = DetectionUtil.getImage( spimData, tp, setup, level );
				// If 2D, the 3rd dimension will be dropped here.
				final RandomAccessibleInterval< ? > zeroMin = Views.dropSingletonDimensions( Views.zeroMin( img ) );

				@SuppressWarnings( { "unchecked", "rawtypes" } )
				final RandomAccessible< FloatType > ra = DetectionUtil.asExtendedFloat( ( RandomAccessibleInterval ) zeroMin );

				/*
				 * Build ROI to process.
				 */

				final double xs = Affine3DHelpers.extractScale( transform, 0 );
				final double ys = Affine3DHelpers.extractScale( transform, 1 );
				final double zs = Affine3DHelpers.extractScale( transform, 2 );

				final Point center = new Point( 3 );
				transform.applyInverse( new Round<>( center ), source );
				final long x = center.getLongPosition( 0 );
				final long y = center.getLongPosition( 1 );
				final long z = center.getLongPosition( 2 );
				final long rx = ( long ) Math.ceil( neighborhoodFactor * radius / xs );
				final long ry = ( long ) Math.ceil( neighborhoodFactor * radius / ys );
				final long rz = ( long ) Math.ceil( neighborhoodFactor * radius / zs );

				final long[] min = new long[] { x - rx, y - ry, z - rz };
				final long[] max = new long[] { x + rx, y + ry, z + rz };

				// Only take 2D or 3D version of the transformed interval.
				final long[] tmin = new long[ zeroMin.numDimensions() ];
				final long[] tmax = new long[ zeroMin.numDimensions() ];
				for ( int d = 0; d < zeroMin.numDimensions(); d++ )
				{
					tmin[ d ] = min[ d ];
					tmax[ d ] = max[ d ];
				}
				final FinalInterval transformedRoi = new FinalInterval( tmin, tmax );
				final FinalInterval roi = Intervals.intersect( transformedRoi, zeroMin );

				/*
				 * Perform detection.
				 */

				final double[] pixelSize = new double[] { 1, ys / xs, zs / xs };
				final double scale = xs;

				final int stepsPerOctave = 4;
				final double k = Math.pow( 2.0, 1.0 / stepsPerOctave );
				final double sigma = radius / Math.sqrt( zeroMin.numDimensions() ) / scale;
				final double sigmaSmaller = sigma;
				final double sigmaLarger = k * sigmaSmaller;
				final double normalization = 1.0 / ( sigmaLarger / sigmaSmaller - 1.0 );

				// Does the source have a quality value?
				final double threshold;
				if ( qualityFeature.getPropertyMap().isSet( source ) )
					threshold = qualityFeature.getPropertyMap().get( source ) * qualityFactor;
				else
					threshold = 0.;

				final DogDetection< FloatType > dog = new DogDetection<>(
						ra,
						roi,
						pixelSize,
						sigmaSmaller,
						sigmaLarger,
						ExtremaType.MINIMA,
						threshold,
						true );
				dog.setExecutorService( threadService.getExecutorService() );
				final ArrayList< RefinedPeak< Point > > refinedPeaks = dog.getSubpixelPeaks();
				if ( refinedPeaks.isEmpty() )
				{
					log.info( String.format( " - No target spot found at t=%d for spot %s above desired quality threshold.",
							tp, source.getLabel() ) );
					continue INPUT;
				}

				/*
				 * Find a suitable candidate with largest quality.
				 */

				// Sort peaks by quality.
				refinedPeaks.sort( refinedPeakComparator );

				boolean found = false;
				RefinedPeak< Point > candidate = null;
				final RealPoint p3d = new RealPoint( 3 );
				final double[] pos = new double[ 3 ];
				final RealPoint sp = RealPoint.wrap( pos );
				double sqDist = 0.;
				for ( final RefinedPeak< Point > p : refinedPeaks )
				{
					// Compute square distance.
					p3d.setPosition( p );
					transform.apply( p3d, sp );
					sqDist = 0.;
					for ( int d = 0; d < 3; d++ )
					{
						final double dx = pos[ d ] - source.getDoublePosition( d );
						sqDist += dx * dx;
					}

					if ( sqDist < distanceFactor * distanceFactor * radius * radius )
					{
						found = true;
						candidate = p;
						break;
					}
				}

				if ( !found )
				{
					log.info( String.format(
							" - Suitable spot found at t=%d, but outside the tolerance radius for spot %s (at a distance of %.1f %s).",
							tp, source.getLabel(), Math.sqrt( sqDist ), units ) );
					log.info( " - Stopping semi-automatic tracking for spot " + first.getLabel() + "." );
					continue INPUT;
				}

				// Deselect source spot.
				if ( null != selectionModel )
					selectionModel.setSelected( source, false );

				/*
				 * Check whether candidate is close to an existing spot.
				 */

				spatioTemporalIndex.readLock().lock();
				Spot target = null;
				double distance = 0.;
				try
				{
					final SpatialIndex< Spot > spatialIndex = spatioTemporalIndex.getSpatialIndex( tp );
					final NearestNeighborSearch< Spot > nn = spatialIndex.getNearestNeighborSearch();
					nn.search( sp );
					target = nn.getSampler().get();
					distance = nn.getDistance();
				}
				finally
				{
					spatioTemporalIndex.readLock().unlock();
				}

				if ( target != null && ( test.isPointInside( pos, target ) || test.isCenterWithin( target, pos, radius ) ) )
				{

					/*
					 * We have an existing spot close to our candidate.
					 */

					// Select it.
					if ( null != selectionModel )
						selectionModel.setSelected( target, true );

					log.info( String.format( " - Found an exising spot at t=%d for spot %s close to candidate: %s.",
							tp, source.getLabel(), target.getLabel() ) );
					if ( !allowLinkingToExisting )
					{
						log.info( " - Stopping semi-automatic tracking for spot " + first.getLabel() + "." );
						continue INPUT;
					}

					// Are they connected?
					final Link eref = graph.edgeRef();
					final boolean connected = forward
							? graph.getEdge( source, target, eref ) != null
							: graph.getEdge( target, source, eref ) != null;
					if ( !connected )
					{
						// They are not connected.
						// Should we link them?
						if ( !allowLinkingIfIncoming && !target.incomingEdges().isEmpty() )
						{
							log.info( " - Existing spot has incoming links. Stopping semi-automatic tracking for spot " + first.getLabel() + "." );
							continue INPUT;
						}
						if ( !allowLinkingIfOutgoing && !target.outgoingEdges().isEmpty() )
						{
							log.info( " - Existing spot has outgoing links. Stopping semi-automatic tracking for spot " + first.getLabel() + "." );
							continue INPUT;
						}

						// Yes.
						final Link edge;
						if ( forward )
							edge = graph.addEdge( source, target, eref ).init();
						else
							edge = graph.addEdge( target, source, eref ).init();

						final double cost = distance * distance;
						log.info( String.format( " - Linking spot %s at t=%d to spot %s at t=%d with linking cost %.1f.",
								source.getLabel(), source.getTimepoint(), target.getLabel(), target.getTimepoint(), cost ) );
						linkCostFeature.getPropertyMap().set( edge, cost );

						if ( null != navigationHandler )
							navigationHandler.notifyNavigateToVertex( target );
					}
					else
					{
						// They are connected.
						log.info( String.format( " - Spots %s at t=%d and %s at t=%d are already linked.",
								source.getLabel(), source.getTimepoint(), target.getLabel(), target.getTimepoint() ) );
						if ( !continueIfLinkExists )
						{
							log.info( " - Stopping semi-automatic tracking for spot " + first.getLabel() + "." );
							continue INPUT;
						}
					}

					source.refTo( target );
					graph.releaseRef( eref );
				}
				else
				{

					/*
					 * We do NOT have an existing spot near out candidate.
					 */

					final Spot vref = graph.vertexRef();
					final Link eref = graph.edgeRef();
					target = graph.addVertex( vref ).init( tp, pos, physicalRadius );
					final Link edge;
					if ( forward )
						edge = graph.addEdge( source, target, eref ).init();
					else
						edge = graph.addEdge( target, source, eref ).init();

					double cost = 0.;
					for ( int d = 0; d < 3; d++ )
					{
						final double dx = source.getDoublePosition( d ) - target.getDoublePosition( d );
						cost += dx * dx;
					}
					final double quality = -candidate.getValue() * normalization;
					log.info( String.format( " - Linking spot %s at t=%d to spot %s at t=%d with linking cost %.1f.",
							source.getLabel(), source.getTimepoint(), target.getLabel(), target.getTimepoint(), cost ) );
					linkCostFeature.getPropertyMap().set( edge, cost );
					qualityFeature.getPropertyMap().set( target, quality );

					if ( null != navigationHandler )
						navigationHandler.notifyNavigateToVertex( target );

					// Select new spot.
					if ( null != selectionModel )
						selectionModel.setSelected( target, true );

					source.refTo( target );
					graph.releaseRef( eref );
					graph.releaseRef( vref );
				}

				graph.notifyGraphChanged();
			}

			log.info( " - Finished semi-automatic tracking for spot " + first.getLabel() + "." );
		}

		/*
		 * Return.
		 */

		ok = true;
	}

	@Override
	public boolean isSuccessful()
	{
		return ok;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	// -- Cancelable methods --

	/** Reason for cancelation, or null if not canceled. */
	private String cancelReason;

	@Override
	public boolean isCanceled()
	{
		return cancelReason != null;
	}

	/** Cancels the command execution, with the given reason for doing so. */
	@Override
	public void cancel( final String reason )
	{
		cancelReason = reason == null ? "" : reason;
	}

	@Override
	public String getCancelReason()
	{
		return cancelReason;
	}

	private static class RefinedPeakComparator implements Comparator< RefinedPeak< ? > >
	{

		@Override
		public int compare( final RefinedPeak< ? > o1, final RefinedPeak< ? > o2 )
		{
			return Double.compare( o1.getValue(), o2.getValue() );
		}

	}

}
