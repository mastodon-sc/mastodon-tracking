package org.mastodon.trackmate;

import java.util.Map;

import org.mastodon.detection.mamut.SpotDetectorOp;
import org.mastodon.linking.mamut.SpotLinkerOp;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.scijava.app.StatusService;
import org.scijava.command.ContextCommand;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import bdv.spimdata.SpimDataMinimal;
import net.imagej.ops.OpService;
import net.imagej.ops.special.hybrid.Hybrids;
import net.imagej.ops.special.inplace.Inplaces;
import net.imglib2.algorithm.Benchmark;

public class TrackMate extends ContextCommand
{

	@Parameter
	private OpService ops;

	@Parameter
	private StatusService statusService;

	@Parameter
	LogService log;

	private final Settings settings;

	private final Model model;

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

	public boolean execDetection()
	{
		final ModelGraph graph = model.getGraph();
		final SpimDataMinimal spimData = settings.values.getSpimData();
		if ( null == spimData )
		{
			log.error( "Cannot start detection: SpimData obect is null." );
			return false;
		}

		final Class< ? extends SpotDetectorOp > cl = settings.values.getDetector();
		final Map< String, Object > detectorSettings = settings.values.getDetectorSettings();

		final SpotDetectorOp detector = ( SpotDetectorOp ) Hybrids.unaryCF( ops, cl,
				graph, spimData,
				detectorSettings );
		log.info( "Detection with " + detector );
		detector.compute( spimData, graph );

		if ( !detector.wasSuccessful() )
		{
			log.error( "Detection failed:\n" + detector.getErrorMessage() );
			return false;
		}

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
		return true;
	}

	public boolean execParticleLinking()
	{
		final Class< ? extends SpotLinkerOp > linkerCl = settings.values.getLinker();
		final Map< String, Object > linkerSettings = settings.values.getLinkerSettings();

		final SpotLinkerOp linker =
				( SpotLinkerOp ) Inplaces.binary1( ops, linkerCl, model.getGraph(), model.getSpatioTemporalIndex(),
						linkerSettings, model.getGraphFeatureModel() );

		log.info( "Particle-linking with " + linker );
		linker.mutate1( model.getGraph(), model.getSpatioTemporalIndex() );
		if ( !linker.wasSuccessful() )
		{
			log.error( "Particle-linking failed:\n" + linker.getErrorMessage() );
			return false;
		}
		model.getGraphFeatureModel().declareFeature( linker.getLinkCostFeature() );
		if ( linker instanceof Benchmark )
			log.info( "Particle-linking completed in " + ( ( Benchmark ) linker ).getProcessingTime() + " ms." );
		else
			log.info( "Particle-linking completed." );
		return true;
	}

	@Override
	public void run()
	{
		if ( execDetection() && execParticleLinking() );
	}

}
