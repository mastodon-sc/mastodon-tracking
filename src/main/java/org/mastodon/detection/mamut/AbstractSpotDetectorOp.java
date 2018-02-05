package org.mastodon.detection.mamut;

import java.util.Map;

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

	@Parameter
	private ThreadService threadService;

	@Parameter
	private StatusService statusService;

	@Parameter( type = ItemIO.INPUT )
	private Map< String, Object > settings;

	@Parameter( type = ItemIO.OUTPUT )
	protected String errorMessage;

	@Parameter( type = ItemIO.OUTPUT )
	protected boolean ok;

	@Parameter( type = ItemIO.OUTPUT )
	protected Feature< Spot, DoublePropertyMap< Spot > > qualityFeature;

	private long processingTime;

	protected DetectorOp< Spot > detector;

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	protected void exec( final SpimDataMinimal spimData, final ModelGraph graph, final Class< ? extends DetectorOp > cl )
	{
		ok = false;
		final long start = System.currentTimeMillis();
		this.detector = ( DetectorOp ) Inplaces.binary1( ops(), cl,
				graph, spimData,
				settings, vertexCreator() );
		detector.mutate1( graph, spimData );
		final long end = System.currentTimeMillis();
		processingTime = end - start;
		qualityFeature = detector.getQualityFeature();
		ok = detector.isSuccessful();
		if ( !ok )
			errorMessage = detector.getErrorMessage();

		this.detector = null;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public ModelGraph createOutput( final SpimDataMinimal input )
	{
		return new ModelGraph();
	}

	@Override
	public Feature< Spot, DoublePropertyMap< Spot > > getQualityFeature()
	{
		return qualityFeature;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public boolean isSuccessful()
	{
		return ok;
	}

	private static final VertexCreator< Spot > VERTEX_CREATOR_INSTANCE = new VertexCreator< Spot >()
	{

		@Override
		public Spot createVertex( final Graph< Spot, ? > graph, final Spot ref, final double[] pos, final double radius, final int timepoint, final double quality )
		{
			return graph.addVertex( ref ).init( timepoint, pos, radius );
		}

	};

	protected VertexCreator< Spot > vertexCreator()
	{
		return VERTEX_CREATOR_INSTANCE;
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
		if (reason!=null)
		{
			cancelReason = reason;
			detector.cancel( reason );
		}
		else
		{
			cancelReason = "";
		}
	}

	@Override
	public String getCancelReason()
	{
		return cancelReason;
	}
}
