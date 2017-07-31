package org.mastodon.trackmate;

import java.util.Map;

import org.mastodon.HasErrorMessage;
import org.mastodon.detection.mamut.SpotDetectorOp;
import org.mastodon.linking.mamut.SpotLinkerOp;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
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
import net.imglib2.algorithm.Benchmark;

public class TrackMate extends ContextCommand implements HasErrorMessage
{

	@Parameter
	private OpService ops;

	@Parameter
	private StatusService statusService;

	@Parameter
	LogService log;

	private final Settings settings;

	private final Model model;

	private Op currentOp;

	private boolean succesful;

	private String errorMessage;

	public TrackMate( final Settings settings )
	{
		this.settings = settings;
		this.model = createModel();
	}

	public TrackMate( final Settings settings, final Model model )
	{
		this.settings = settings;
		this.model = model;
	}

	protected Model createModel()
	{
		return new Model();
	}

	public Model getModel()
	{
		return model;
	}

	public Settings getSettings()
	{
		return settings;
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
			errorMessage = "Cannot start detection: SpimData obect is null.";
			log.error( errorMessage );
			succesful = false;
			return false;
		}

		/*
		 * Clear previous content.
		 */

		for ( final Spot spot : model.getGraph().vertices() )
			model.getGraph().remove( spot );

		/*
		 * Exec detection.
		 */

		final Class< ? extends SpotDetectorOp > cl = settings.values.getDetector();
		final Map< String, Object > detectorSettings = settings.values.getDetectorSettings();

		final SpotDetectorOp detector = ( SpotDetectorOp ) Hybrids.unaryCF( ops, cl,
				graph, spimData,
				detectorSettings );
		this.currentOp = detector;
		log.info( "Detection with " + detector );
		detector.compute( spimData, graph );

		if ( !detector.isSuccessful() )
		{
			log.error( "Detection failed:\n" + detector.getErrorMessage() );
			succesful = false;
			errorMessage = detector.getErrorMessage();
			return false;
		}
		currentOp = null;

		model.getGraphFeatureModel().declareFeature( detector.getQualityFeature() );
		if ( detector instanceof Benchmark )
		{
			final Benchmark bm = ( Benchmark ) detector;
			log.info( "Detection completed in " + bm.getProcessingTime() + " ms." );
		}
		else
		{
			log.info( "Detection completed." );
		}
		log.info( "Found " + graph.vertices().size() + " spots." );

		model.getGraph().notifyGraphChanged();
		return true;
	}

	public boolean execParticleLinking()
	{
		succesful = true;
		errorMessage = null;
		if ( isCanceled() )
			return true;

		/*
		 * Clear previous content.
		 */

		for ( final Link link : model.getGraph().edges() )
			model.getGraph().remove( link );

		/*
		 * Exec particle linking.
		 */

		final Class< ? extends SpotLinkerOp > linkerCl = settings.values.getLinker();
		final Map< String, Object > linkerSettings = settings.values.getLinkerSettings();

		final SpotLinkerOp linker =
				( SpotLinkerOp ) Inplaces.binary1( ops, linkerCl, model.getGraph(), model.getSpatioTemporalIndex(),
						linkerSettings, model.getGraphFeatureModel() );

		log.info( "Particle-linking with " + linker );
		this.currentOp = linker;
		linker.mutate1( model.getGraph(), model.getSpatioTemporalIndex() );
		if ( !linker.isSuccessful() )
		{
			log.error( "Particle-linking failed:\n" + linker.getErrorMessage() );
			succesful = false;
			errorMessage = linker.getErrorMessage();
			return false;
		}

		currentOp = null;
		model.getGraphFeatureModel().declareFeature( linker.getLinkCostFeature() );
		if ( linker instanceof Benchmark )
			log.info( "Particle-linking completed in " + ( ( Benchmark ) linker ).getProcessingTime() + " ms." );
		else
			log.info( "Particle-linking completed." );

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
