package org.mastodon.trackmate.semiauto;

import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_RADIUS;
import static org.mastodon.detection.DetectorKeys.KEY_ROI;
import static org.mastodon.detection.DetectorKeys.KEY_SETUP_ID;
import static org.mastodon.detection.DetectorKeys.KEY_THRESHOLD;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_ALLOW_LINKING_IF_HAS_INCOMING;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_ALLOW_LINKING_IF_HAS_OUTGOING;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_ALLOW_LINKING_TO_EXISTING;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_CONTINUE_IF_LINK_EXISTS;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_DISTANCE_FACTOR;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_FORWARD_IN_TIME;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_N_TIMEPOINTS;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.KEY_QUALITY_FACTOR;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.NEIGHBORHOOD_FACTOR;
import static org.mastodon.trackmate.semiauto.SemiAutomaticTrackerKeys.checkSettingsValidity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.mastodon.HasErrorMessage;
import org.mastodon.detection.DetectionCreatorFactory;
import org.mastodon.detection.DetectionUtil;
import org.mastodon.detection.DetectorOp;
import org.mastodon.detection.DoGDetectorOp;
import org.mastodon.detection.mamut.DetectionQualityFeature;
import org.mastodon.linking.mamut.LinkCostFeature;
import org.mastodon.model.FocusModel;
import org.mastodon.model.NavigationHandler;
import org.mastodon.model.SelectionModel;
import org.mastodon.revised.bdv.SharedBigDataViewerData;
import org.mastodon.revised.bdv.overlay.util.JamaEigenvalueDecomposition;
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

import bdv.viewer.SourceAndConverter;
import net.imagej.ops.OpService;
import net.imagej.ops.special.computer.AbstractBinaryComputerOp;
import net.imagej.ops.special.inplace.Inplaces;
import net.imglib2.FinalInterval;
import net.imglib2.Point;
import net.imglib2.RealPoint;
import net.imglib2.neighborsearch.NearestNeighborSearch;
import net.imglib2.position.transform.Round;
import net.imglib2.realtransform.AffineTransform3D;

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
	private SharedBigDataViewerData data;

	@Parameter( required = false )
	private NavigationHandler< Spot, Link > navigationHandler;

	@Parameter( required = false )
	private SelectionModel< Spot, Link > selectionModel;

	@Parameter( required = false )
	private FocusModel< Spot, Link > focusModel;

	@Parameter( required = false )
	private Logger log;

	@Parameter
	private LogService logService;

	@Parameter( type = ItemIO.OUTPUT )
	protected String errorMessage;

	@Parameter( type = ItemIO.OUTPUT )
	protected boolean ok;

	private final Comparator< Detection > detectionComparator = new RefinedPeakComparator();

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

		final List< SourceAndConverter< ? > > sources = data.getSources();
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

		final DetectionQualityFeature qualityFeature = DetectionQualityFeature.getOrRegister( model.getFeatureModel(), graph.vertices().getRefPool() );
		final LinkCostFeature linkCostFeature = LinkCostFeature.getOrRegister( model.getFeatureModel(), graph.edges().getRefPool() );

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

		/*
		 * Units.
		 */

		final String units =  sources.get( setup ).getSpimSource().getVoxelDimensions().unit();

		/*
		 * First and last time-points.
		 */

		final int minTimepoint = 0;
		final int maxTimepoint = data.getNumTimepoints() -1;

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
				if ( !DetectionUtil.isPresent( sources, setup, tp ) )
					continue TIME;

				// Best radius is smallest radius of ellipse.
				source.getCovariance( cov );
				eig.decomposeSymmetric( cov );
				final double[] eigVals = eig.getRealEigenvalues();
				double minEig = Double.POSITIVE_INFINITY;
				for ( int k = 0; k < eigVals.length; k++ )
					minEig = Math.min( minEig, eigVals[ k ] );
				final double radius = Math.sqrt( minEig );

				// Does the source have a quality value?
				final double threshold;
				if ( qualityFeature.isSet( source ) )
					threshold = qualityFeature.value( source ) * qualityFactor;
				else
					threshold = 0.;

				/*
				 * Build ROI to process.
				 */
				final double[] calibration = DetectionUtil.getPhysicalCalibration( sources, tp, setup, 0 );
				final AffineTransform3D transform = DetectionUtil.getTransform( sources, tp, setup, 0 );
				final Point center = new Point( 3 );
				transform.applyInverse( new Round<>( center ), source );
				final long x = center.getLongPosition( 0 );
				final long y = center.getLongPosition( 1 );
				final long z = center.getLongPosition( 2 );
				final long rx = ( long ) Math.ceil( neighborhoodFactor * radius / calibration[ 0 ] );
				final long ry = ( long ) Math.ceil( neighborhoodFactor * radius / calibration[ 1 ] );
				final long rz = ( long ) Math.ceil( neighborhoodFactor * radius / calibration[ 2 ] );
				final long[] min = new long[] { x - rx, y - ry, z - rz };
				final long[] max = new long[] { x + rx, y + ry, z + rz };
				final FinalInterval roi = new FinalInterval( min, max );

				/*
				 * User built-in detector.
				 */

				final List< Detection > detections = new ArrayList<>();
				final DetectionCreatorFactory detectionCreator = new DetectionCreatorFactory()
				{

					@Override
					public DetectionCreator create( final int timepoint )
					{
						return new DetectionCreator()
						{

							@Override
							public void preAddition()
							{}

							@Override
							public void postAddition()
							{}

							@Override
							public void createDetection( final double[] pos, final double radius, final double quality )
							{
								detections.add( new Detection( pos, quality ) );
							}
						};
					}
				};

				// Configure detector.
				final Map<String, Object> detectorSettings = DetectionUtil.getDefaultDetectorSettingsMap();
				detectorSettings.put( KEY_RADIUS, Double.valueOf( radius ) );
				detectorSettings.put( KEY_THRESHOLD, Double.valueOf( threshold ) );
				detectorSettings.put( KEY_SETUP_ID, Integer.valueOf( setup ) );
				detectorSettings.put( KEY_MIN_TIMEPOINT, Integer.valueOf( tp ) );
				detectorSettings.put( KEY_MAX_TIMEPOINT, Integer.valueOf( tp ) );
				detectorSettings.put( KEY_ROI, roi );
				final DetectorOp detector = ( DetectorOp ) Inplaces.binary1( ops(), DoGDetectorOp.class,
						detectionCreator, sources, detectorSettings  );
				detector.mutate1( detectionCreator, sources );

				if ( detections.isEmpty() )
				{
					log.info( String.format( " - No target spot found at t=%d for spot %s above desired quality threshold.",
							tp, source.getLabel() ) );
					continue INPUT;
				}

				/*
				 * Find a suitable candidate with largest quality.
				 */

				// Sort peaks by quality.
				detections.sort( detectionComparator );

				boolean found = false;
				Detection candidate = null;
				double sqDist = 0.;
				for ( final Detection p : detections )
				{
					// Compute square distance.
					sqDist = 0.;
					for ( int d = 0; d < source.numDimensions(); d++ )
					{
						final double dx = p.getDoublePosition( d ) - source.getDoublePosition( d );
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
					nn.search( candidate );
					target = nn.getSampler().get();
					distance = nn.getDistance();
				}
				finally
				{
					spatioTemporalIndex.readLock().unlock();
				}

				final double[] pos = new double[ candidate.numDimensions()];
				candidate.localize( pos );
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
						graph.getLock().writeLock().lock();
						try
						{
							if ( forward )
								edge = graph.addEdge( source, target, eref ).init();
							else
								edge = graph.addEdge( target, source, eref ).init();
						}
						finally
						{
							graph.getLock().writeLock().unlock();
						}
						graph.notifyGraphChanged();

						final double cost = distance * distance;
						log.info( String.format( " - Linking spot %s at t=%d to spot %s at t=%d with linking cost %.1f.",
								source.getLabel(), source.getTimepoint(), target.getLabel(), target.getTimepoint(), cost ) );
						linkCostFeature.set( edge, cost );

						if ( null != navigationHandler )
							navigationHandler.notifyNavigateToVertex( target );
						if ( null != focusModel )
							focusModel.focusVertex( target );
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

					graph.getLock().writeLock().lock();
					final Link eref = graph.edgeRef();
					final Spot vref = graph.vertexRef();
					final Link edge;
					try
					{
						target = graph.addVertex( vref ).init( tp, pos, radius );
						if ( forward )
							edge = graph.addEdge( source, target, eref ).init();
						else
							edge = graph.addEdge( target, source, eref ).init();
					}
					finally
					{
						graph.getLock().writeLock().unlock();
					}
					graph.notifyGraphChanged();

					double cost = 0.;
					for ( int d = 0; d < 3; d++ )
					{
						final double dx = source.getDoublePosition( d ) - target.getDoublePosition( d );
						cost += dx * dx;
					}
					final double quality = candidate.quality;
					log.info( String.format( " - Linking spot %s at t=%d to spot %s at t=%d with linking cost %.1f.",
							source.getLabel(), source.getTimepoint(), target.getLabel(), target.getTimepoint(), cost ) );
					linkCostFeature.set( edge, cost );
					qualityFeature.set( target, quality );

					// Select, focus and navigate to new spot.
					if ( null != navigationHandler )
						navigationHandler.notifyNavigateToVertex( target );
					if ( null != selectionModel )
						selectionModel.setSelected( target, true );
					if ( null != focusModel )
						focusModel.focusVertex( target );

					source.refTo( target );
					graph.releaseRef( eref );
					graph.releaseRef( vref );
				}
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

	private static class RefinedPeakComparator implements Comparator< Detection >
	{

		@Override
		public int compare( final Detection o1, final Detection o2 )
		{
			return Double.compare( o2.quality, o1.quality );
		}
	}

	private static class Detection extends RealPoint
	{
		private final double quality;

		public Detection(final double[] pos, final double quality)
		{
			super( pos );
			this.quality = quality;
		}
	}

}
