package org.mastodon.detection.mamut;

import java.util.Map;

import org.mastodon.detection.DetectionCreator;
import org.mastodon.detection.DetectionUtil;
import org.mastodon.detection.DetectorOp;
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

	protected DetectorOp< DetectionCreator > detector;

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	protected void exec( final SpimDataMinimal spimData, final ModelGraph graph, final Class< ? extends DetectorOp > cl )
	{
		ok = false;
		final long start = System.currentTimeMillis();

		final DoublePropertyMap< Spot > pm = new DoublePropertyMap<>( graph.vertices(), Double.NaN );
		qualityFeature = DetectionUtil.getQualityFeature( pm, Spot.class );

		final DetectionCreator detectionCreator = detectionCreator( graph, pm );

		this.detector = ( DetectorOp ) Inplaces.binary1( ops(), cl,
				detectionCreator, spimData, settings );
		detector.mutate1( detectionCreator, spimData );

		final long end = System.currentTimeMillis();
		processingTime = end - start;
		ok = detector.isSuccessful();
		if ( !ok )
			errorMessage = detector.getErrorMessage();

		this.detector = null;
	}

	protected DetectionCreator detectionCreator( final ModelGraph graph, final DoublePropertyMap< Spot > pm )
	{
		return new MyDetectionCreator( graph, pm );
	}

	/**
	 * Default detection creator suitable to create {@link Spot} vertices in a
	 * {@link ModelGraph} from the detection returned by the detector. Takes
	 * care of acquiring the writing lock before adding all the detections of a
	 * time-point and returning it after. Also resets and feeds the quality
	 * value to a quality feature.
	 * 
	 * @author Jean-Yves Tinevez
	 *
	 */
	private static class MyDetectionCreator implements DetectionCreator
	{

		private final Spot ref;

		private final DoublePropertyMap< Spot > pm;

		private final ModelGraph graph;

		public MyDetectionCreator( final ModelGraph graph, final DoublePropertyMap< Spot > pm )
		{
			this.graph = graph;
			this.pm = pm;
			this.ref = graph.vertexRef();
		}

		@Override
		public void preAddition()
		{
			graph.getLock().writeLock().lock();
		}

		@Override
		public void postAddition()
		{
			graph.getLock().writeLock().unlock();
		}

		@Override
		public void createDetection( final double[] pos, final double radius, final int timepoint, final double quality )
		{
			final Spot spot = graph.addVertex( ref ).init( timepoint, pos, radius );
			pm.set( spot, quality );
		}
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
