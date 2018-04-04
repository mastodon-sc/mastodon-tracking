package org.mastodon.trackmate;

import static org.mastodon.linking.LinkerKeys.KEY_DO_LINK_SELECTION;

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
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import bdv.spimdata.SpimDataMinimal;
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

	@Parameter
	private LogService log;

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

	public boolean execDetection()
	{
		succesful = true;
		errorMessage = null;
		if ( isCanceled() )
			return true;

		final ModelGraph graph = model.getGraph();
		final SpimDataMinimal spimData = settings.values.getSpimData();
		if ( null == spimData )
		{
			errorMessage = "Cannot start detection: SpimData object is null.\n";
			log.error( errorMessage + '\n' );
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
				graph, spimData,
				detectorSettings,
				model.getSpatioTemporalIndex() );
		this.currentOp = detector;
		log.info( "Detection with " + cl.getSimpleName() + '\n' );
		detector.compute( spimData, graph );

		if ( !detector.isSuccessful() )
		{
			log.error( "Detection failed:\n" + detector.getErrorMessage() + '\n' );
			succesful = false;
			errorMessage = detector.getErrorMessage();
			return false;
		}
		currentOp = null;

		model.getFeatureModel().declareFeature( detector.getQualityFeature() );
		final long end = System.currentTimeMillis();
		log.info( String.format( "Detection completed in %.1f s.\n", ( end - start ) / 1000. ) );
		log.info( "Found " + graph.vertices().size() + " spots.\n" );

		graph.notifyGraphChanged();
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
		final boolean doLinkSelection = ( boolean ) linkerSettings.get( KEY_DO_LINK_SELECTION );
		if ( doLinkSelection )
			target = new SpatioTemporalIndexSelection<>( model.getGraph(), selectionModel, model.getGraph().vertices().getRefPool() );
		else
			target = model.getSpatioTemporalIndex();

		/*
		 * Clear previous content.
		 */

		model.getGraph().getLock().writeLock().lock();
		try
		{
			for ( final Spot spot : target )
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

		log.info( "Particle-linking with " + linkerCl.getSimpleName() + '\n' );
		this.currentOp = linker;
		linker.mutate1( model.getGraph(), model.getSpatioTemporalIndex() );
		if ( !linker.isSuccessful() )
		{
			log.error( "Particle-linking failed:\n" + linker.getErrorMessage() + '\n' );
			succesful = false;
			errorMessage = linker.getErrorMessage();
			return false;
		}

		currentOp = null;
		model.getFeatureModel().declareFeature( linker.getLinkCostFeature() );
		final long end = System.currentTimeMillis();
		log.info( String.format( "Particle-linking completed in %.1f s.\n", ( end - start ) / 1000. ) );
		final int nTracks = RootFinder.getRoots( model.getGraph() ).size();
		log.info( String.format( "Found %d tracks.\n", nTracks ) );

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
