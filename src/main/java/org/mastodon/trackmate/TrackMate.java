package org.mastodon.trackmate;

import java.util.Map;

import org.mastodon.detection.mamut.SpotDetectorOp;
import org.mastodon.linking.mamut.SpotLinkerOp;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.scijava.command.ContextCommand;
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

	public void execDetection()
	{
		final Class< ? extends SpotDetectorOp > cl = settings.values.getDetector();
		final ModelGraph graph = model.getGraph();
		final SpimDataMinimal spimData = settings.values.getSpimData();

		/*
		 * TODO: Make a general settings map for detectors.
		 */
		final int setup= 0;
		final double radius = 6.;
		final double threshold = 1000.;
		final int maxTimepoint = spimData.getSequenceDescription().getTimePoints().size() - 1;
		final int minTimepoint = 0;

		final SpotDetectorOp detector = ( SpotDetectorOp ) Hybrids.unaryCF( ops, cl, graph, spimData,
				setup, radius, threshold, minTimepoint, maxTimepoint );
		detector.compute( spimData, graph );
		model.getGraphFeatureModel().declareFeature( detector.getQualityFeature() );
		if ( detector instanceof Benchmark )
		{
			final Benchmark bm = ( Benchmark ) detector;
			System.out.println( "Detection completed in " + bm.getProcessingTime() + " ms." );
		}
		System.out.println( "Found " + graph.vertices().size() + " spots." );
	}

	public void execParticleLinking()
	{
		final SpimDataMinimal spimData = settings.values.getSpimData();
		final int maxTimepoint = spimData.getSequenceDescription().getTimePoints().size() - 1;
		final int minTimepoint = 0;

		final Class< ? extends SpotLinkerOp > linkerCl = settings.values.getLinker();
		final Map< String, Object > linkerSettings = settings.values.getLinkerSettings();

		final SpotLinkerOp linker =
				( SpotLinkerOp ) Inplaces.binary1( ops, linkerCl, model.getGraph(), model.getSpatioTemporalIndex(),
						linkerSettings, model.getGraphFeatureModel(), minTimepoint, maxTimepoint );

		System.out.println( "\n\nParticle-linking with " + linker );
		linker.mutate1( model.getGraph(), model.getSpatioTemporalIndex() );
		if ( !linker.wasSuccessful() )
		{
			System.out.println( "Tracking failed: " + linker.getErrorMessage() );
			return;
		}
		model.getGraphFeatureModel().declareFeature( linker.getLinkCostFeature() );
		if ( linker instanceof Benchmark )
			System.out.println( "Tracking completed in " + ( ( Benchmark ) linker ).getProcessingTime() + " ms." );
		else
			System.out.println( "Tracking completed." );
	}

	@Override
	public void run()
	{
		execDetection();
		execParticleLinking();
	}

}
