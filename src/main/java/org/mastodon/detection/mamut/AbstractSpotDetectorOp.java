package org.mastodon.detection.mamut;

import static org.mastodon.detection.DetectorKeys.KEY_ADD_BEHAVIOR;
import static org.mastodon.detection.DetectorKeys.KEY_ROI;

import java.util.Map;

import org.mastodon.detection.DetectionCreatorFactory;
import org.mastodon.detection.DetectionUtil;
import org.mastodon.detection.DetectorOp;
import org.mastodon.detection.mamut.MamutDetectionCreatorFactories.DetectionBehavior;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.feature.Feature;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.plugin.Parameter;
import org.scijava.thread.ThreadService;

import bdv.spimdata.SpimDataMinimal;
import net.imagej.ops.special.hybrid.AbstractUnaryHybridCF;
import net.imagej.ops.special.inplace.Inplaces;
import net.imglib2.Interval;
import net.imglib2.algorithm.Benchmark;

public abstract class AbstractSpotDetectorOp extends AbstractUnaryHybridCF< SpimDataMinimal, ModelGraph > implements SpotDetectorOp, Benchmark
{

	@Parameter
	private ThreadService threadService;

	@Parameter
	private StatusService statusService;

	@Parameter( type = ItemIO.INPUT )
	private Map< String, Object > settings;

	@Parameter( type = ItemIO.INPUT )
	private SpatioTemporalIndex< Spot > sti;

	@Parameter( type = ItemIO.OUTPUT )
	protected String errorMessage;

	@Parameter( type = ItemIO.OUTPUT )
	protected boolean ok;

	@Parameter( type = ItemIO.OUTPUT )
	protected Feature< Spot, DoublePropertyMap< Spot > > qualityFeature;

	private long processingTime;

	protected DetectorOp detector;

	protected void exec( final SpimDataMinimal spimData, final ModelGraph graph, final Class< ? extends DetectorOp > cl )
	{
		ok = false;
		final long start = System.currentTimeMillis();

		final DoublePropertyMap< Spot > pm = new DoublePropertyMap<>( graph.vertices(), Double.NaN );
		qualityFeature = DetectionUtil.getQualityFeature( pm, Spot.class );

		/*
		 * Resolve add detection behavior.
		 */
		final DetectionCreatorFactory detectionCreator;
		if ( null == sti )
		{
			detectionCreator = MamutDetectionCreatorFactories.getAddDetectionCreatorFactory( graph, pm );
		}
		else
		{
			DetectionBehavior detectionBehavior = DetectionBehavior.ADD;
			final String addBehavior = ( String ) settings.get( KEY_ADD_BEHAVIOR );
			if ( null != addBehavior )
			{
				try
				{
					detectionBehavior = MamutDetectionCreatorFactories.DetectionBehavior.valueOf( addBehavior );
				}
				catch ( final IllegalArgumentException e )
				{}
			}
			final Interval roi = ( Interval ) settings.get( KEY_ROI );
			detectionCreator = detectionBehavior.getFactory( graph, pm, sti, roi );
		}

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
		if ( reason != null )
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
