package org.mastodon.detection.mamut;

import org.mastodon.detection.DetectorOp;
import org.mastodon.detection.VertexCreator;
import org.mastodon.graph.Graph;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.feature.Feature;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.plugin.Parameter;
import org.scijava.thread.ThreadService;

import bdv.spimdata.SpimDataMinimal;
import net.imagej.ops.special.hybrid.AbstractUnaryHybridCF;
import net.imagej.ops.special.inplace.Inplaces;
import net.imglib2.algorithm.Benchmark;

public abstract class AbstractSpotDetectorOp extends AbstractUnaryHybridCF< SpimDataMinimal, ModelGraph > implements SpotDetectorOp, Benchmark
{

	private static final VertexCreator< Spot > VERTEX_CREATOR_INSTANCE = new VertexCreator< Spot >()
	{

		@Override
		public Spot createVertex( final Graph< Spot, ? > graph, final Spot ref, final double[] pos, final double radius, final int timepoint, final double quality )
		{
			return graph.addVertex( ref ).init( timepoint, pos, radius );
		}

	};

	@Parameter
	private ThreadService threadService;

	@Parameter
	private StatusService statusService;

	/**
	 * The id of the setup in the provided SpimData object to process.
	 */
	@Parameter( required = true )
	private int setup = 0;

	/**
	 * the expected radius (in units of the global coordinate system) of blobs
	 * to detect.
	 */
	@Parameter( required = true )
	private double radius = 5.;

	/**
	 * The quality threshold below which spots will be rejected.
	 */
	@Parameter
	private double threshold = 0.;

	/**
	 * The min time-point to process, inclusive.
	 */
	@Parameter
	private int minTimepoint = 0;

	/**
	 * The max time-point to process, inclusive.
	 */
	@Parameter
	private int maxTimepoint = 0;

	private long processingTime;

	protected void exec( final SpimDataMinimal spimData, final ModelGraph graph, @SuppressWarnings( "rawtypes" ) final Class< ? extends DetectorOp > cl )
	{
		final long start = System.currentTimeMillis();
		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final DetectorOp< Spot > detector = ( DetectorOp ) Inplaces.binary1( ops(), cl,
				graph, spimData,
				setup, radius, threshold, minTimepoint, maxTimepoint, vertexCreator() );
		detector.mutate1( graph, spimData );
		qualityFeature = detector.getQualityFeature();
		final long end = System.currentTimeMillis();
		this.processingTime = end - start;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Parameter( type = ItemIO.OUTPUT )
	protected Feature< Spot, Double, DoublePropertyMap< Spot > > qualityFeature;

	protected VertexCreator< Spot > vertexCreator()
	{
		return VERTEX_CREATOR_INSTANCE;
	}

	@Override
	public ModelGraph createOutput( final SpimDataMinimal input )
	{
		return new ModelGraph();
	}

	@Override
	public Feature< Spot, Double, DoublePropertyMap< Spot > > getQualityFeature()
	{
		return qualityFeature;
	}

}
