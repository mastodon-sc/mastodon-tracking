package org.mastodon.trackmate;

import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.linking.LinkerKeys.KEY_DO_LINK_SELECTION;

import java.util.List;
import java.util.Map;

import org.mastodon.HasErrorMessage;
import org.mastodon.detection.mamut.SpotDetectorOp;
import org.mastodon.graph.algorithm.RootFinder;
import org.mastodon.linking.mamut.SpotLinkerOp;
import org.mastodon.model.SelectionModel;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.spatial.SpatioTemporalIndexSelection;
import org.scijava.Cancelable;
import org.scijava.app.StatusService;
import org.scijava.command.ContextCommand;
import org.scijava.log.Logger;
import org.scijava.log.StderrLogService;
import org.scijava.plugin.Parameter;

import bdv.viewer.SourceAndConverter;
import net.imagej.ops.Op;
import net.imagej.ops.OpService;
import net.imagej.ops.special.hybrid.Hybrids;
import net.imagej.ops.special.inplace.Inplaces;

public class TrackMate extends ContextCommand implements HasErrorMessage
{

	@Parameter
	private OpService ops;

	@Parameter
	private StatusService statusService;

	@Parameter(required = false)
	private Logger logger = new StderrLogService();

	private final Settings settings;

	private final Model model;

	private final SelectionModel< Spot, Link > selectionModel;

	private Op currentOp;

	private boolean succesful;

	private String errorMessage;

	public TrackMate( final Settings settings, final Model model, final SelectionModel< Spot, Link > selectionModel )
	{
		this.settings = settings;
		this.model = model;
		this.selectionModel = selectionModel;
	}

	public Model getModel()
	{
		return model;
	}

	public Settings getSettings()
	{
		return settings;
	}

	public SelectionModel< Spot, Link > getSelectionModel()
	{
		return selectionModel;
	}

	public void setLogger( final Logger logger )
	{
		this.logger = logger;
	}

	public void setStatusService( final StatusService statusService )
	{
		this.statusService = statusService;
	}

	public boolean execDetection()
	{
		succesful = true;
		errorMessage = null;
		if ( isCanceled() )
			return true;

		final ModelGraph graph = model.getGraph();
		final List< SourceAndConverter< ? > > sources = settings.values.getSources();
		if ( null == sources || sources.isEmpty() )
		{
			errorMessage = "Cannot start detection: No sources.\n";
			logger.error( errorMessage + '\n' );
			succesful = false;
			return false;
		}

		/*
		 * Exec detection.
		 */

		final long start = System.currentTimeMillis();
		final Class< ? extends SpotDetectorOp > cl = settings.values.getDetector();
		final Map< String, Object > detectorSettings = settings.values.getDetectorSettings();

		final SpotDetectorOp detector = ( SpotDetectorOp ) Hybrids.unaryCF( ops, cl,
				graph, sources,
				detectorSettings,
				model.getSpatioTemporalIndex() );
		detector.setLogger( logger );
		detector.setStatusService( statusService );
		this.currentOp = detector;
		logger.info( "Detection with " + cl.getSimpleName() + '\n' );
		detector.compute( sources, graph );

		if ( !detector.isSuccessful() )
		{
			logger.error( "Detection failed:\n" + detector.getErrorMessage() + '\n' );
			succesful = false;
			errorMessage = detector.getErrorMessage();
			return false;
		}
		currentOp = null;

		model.getFeatureModel().declareFeature( detector.getQualityFeature() );
		final long end = System.currentTimeMillis();
		logger.info( String.format( "Detection completed in %.1f s.\n", ( end - start ) / 1000. ) );
		logger.info( "There is now " + graph.vertices().size() + " spots.\n" );

		graph.notifyGraphChanged();
		model.setUndoPoint();

		return true;
	}

	public boolean execParticleLinking()
	{
		succesful = true;
		errorMessage = null;
		if ( isCanceled() )
			return true;

		final Map< String, Object > linkerSettings = settings.values.getLinkerSettings();

		/*
		 * Operate on all spots or just the selection?
		 */

		final SpatioTemporalIndex< Spot > target;
		final Object dls = linkerSettings.get( KEY_DO_LINK_SELECTION );
		final boolean doLinkSelection = ( null == dls) ? false : (boolean) dls;
		if ( doLinkSelection )
			target = new SpatioTemporalIndexSelection<>( model.getGraph(), selectionModel, model.getGraph().vertices().getRefPool() );
		else
			target = model.getSpatioTemporalIndex();

		/*
		 * Clear previous content. We do not remove links that are incoming in
		 * the spots belonging to the first time-point, not the links that are
		 * outgoing of the spots in the last time-point.
		 */
		final int minT;
		final int maxT;
		if ( doLinkSelection && !selectionModel.isEmpty() )
		{
			int t1 = Integer.MAX_VALUE;
			int t2 = Integer.MIN_VALUE;
			for ( final Spot spot : selectionModel.getSelectedVertices() )
			{
				final int t = spot.getTimepoint();
				if ( t > t2 )
					t2 = t;
				if ( t < t1 )
					t1 = t;
			}
			minT = t1;
			maxT = t2;
		}
		else
		{
			minT = ( int ) linkerSettings.get( KEY_MIN_TIMEPOINT );
			maxT = ( int ) linkerSettings.get( KEY_MAX_TIMEPOINT );
		}

		model.getGraph().getLock().writeLock().lock();
		try
		{
			for ( final Spot spot : target.getSpatialIndex( minT ) )
				for ( final Link link : spot.outgoingEdges() )
					model.getGraph().remove( link );

			for ( final Spot spot : target.getSpatialIndex( maxT ) )
				for ( final Link link : spot.incomingEdges() )
					model.getGraph().remove( link );

			for ( int t = minT + 1; t < maxT; t++ )
				for ( final Spot spot : target.getSpatialIndex( t ) )
					for ( final Link link : spot.edges() )
						model.getGraph().remove( link );
		}
		finally
		{
			model.getGraph().getLock().writeLock().unlock();
		}

		/*
		 * Exec particle linking.
		 */

		final long start = System.currentTimeMillis();
		final Class< ? extends SpotLinkerOp > linkerCl = settings.values.getLinker();

		final SpotLinkerOp linker =
				( SpotLinkerOp ) Inplaces.binary1( ops, linkerCl, model.getGraph(), target,
						linkerSettings, model.getFeatureModel() );

		logger.info( "Particle-linking with " + linkerCl.getSimpleName() + '\n' );
		this.currentOp = linker;
		linker.mutate1( model.getGraph(), model.getSpatioTemporalIndex() );
		if ( !linker.isSuccessful() )
		{
			logger.error( "Particle-linking failed:\n" + linker.getErrorMessage() + '\n' );
			succesful = false;
			errorMessage = linker.getErrorMessage();
			return false;
		}

		currentOp = null;
		model.getFeatureModel().declareFeature( linker.getLinkCostFeature() );
		final long end = System.currentTimeMillis();
		logger.info( String.format( "Particle-linking completed in %.1f s.\n", ( end - start ) / 1000. ) );
		final int nTracks = RootFinder.getRoots( model.getGraph() ).size();
		logger.info( String.format( "There is now %d tracks.\n", nTracks ) );

		model.getGraph().notifyGraphChanged();
		return true;
	}

	@Override
	public void run()
	{
		if ( execDetection() && execParticleLinking() )
			;

	}

	// -- Cancelable methods --

	/** Reason for cancelation, or null if not canceled. */
	private String cancelReason;

	@Override
	public void cancel( final String reason )
	{
		cancelReason = reason;
		if ( null != currentOp && ( currentOp instanceof Cancelable ) )
		{
			final Cancelable cancelable = ( Cancelable ) currentOp;
			cancelable.cancel( reason );
		}
	}

	@Override
	public boolean isCanceled()
	{
		return cancelReason != null;
	}

	@Override
	public String getCancelReason()
	{
		return cancelReason;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public boolean isSuccessful()
	{
		return succesful;
	}
}
