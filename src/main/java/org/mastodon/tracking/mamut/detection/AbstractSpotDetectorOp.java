/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2025 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.tracking.mamut.detection;

import static org.mastodon.tracking.detection.DetectorKeys.KEY_ADD_BEHAVIOR;

import java.util.List;
import java.util.Map;

import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.tracking.detection.DetectionCreatorFactory;
import org.mastodon.tracking.detection.DetectorOp;
import org.mastodon.tracking.mamut.detection.MamutDetectionCreatorFactories.DetectionBehavior;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.thread.ThreadService;

import bdv.viewer.SourceAndConverter;
import net.imagej.ops.special.hybrid.AbstractUnaryHybridCF;
import net.imagej.ops.special.inplace.Inplaces;
import net.imglib2.algorithm.Benchmark;

public abstract class AbstractSpotDetectorOp extends AbstractUnaryHybridCF< List< SourceAndConverter< ? > >, ModelGraph > implements SpotDetectorOp, Benchmark
{

	@Parameter( type = ItemIO.INPUT )
	protected Map< String, Object > settings;

	@Parameter( type = ItemIO.INPUT )
	protected SpatioTemporalIndex< Spot > sti;

	@Parameter( type = ItemIO.BOTH, required = false )
	protected DetectionQualityFeature qualityFeature;

	@Parameter( type = ItemIO.OUTPUT )
	protected String errorMessage;

	@Parameter( type = ItemIO.OUTPUT )
	protected boolean ok;

	@Parameter
	protected ThreadService threadService;

	@Parameter( required = false )
	protected StatusService statusService;

	@Parameter( required = false )
	protected Logger log;

	protected long processingTime;

	protected DetectorOp detector;

	/**
	 * Default execution routine for a Mamut spot detector.
	 * <p>
	 * Calling this method will run the {@link DetectorOp} specified by its
	 * class (3rd input) on the specified sources (1st input). The positions
	 * found by the detector op will be then converted to {@link Spot}s and
	 * added to the specified {@link ModelGraph} (2nd input).
	 * 
	 * @param sources
	 *            the souces to operate on.
	 * @param graph
	 *            the {@link ModelGraph} to add the spots to.
	 * @param cl
	 *            the {@link DetectorOp} to run.
	 */
	protected void exec( final List< SourceAndConverter< ? > > sources, final ModelGraph graph, final Class< ? extends DetectorOp > cl )
	{
		ok = false;
		final long start = System.currentTimeMillis();

		if ( null == qualityFeature )
			qualityFeature = new DetectionQualityFeature( graph.vertices().getRefPool() );

		// Resolve add detection behavior.
		final DetectionCreatorFactory detectionCreator = getDetectorFactory( graph );

		this.detector = ( DetectorOp ) Inplaces.binary1( ops(), cl,
				detectionCreator, sources, settings );
		detector.setLogger( log );
		detector.setStatusService( statusService );
		try
		{
			detector.mutate1( detectionCreator, sources );
			ok = detector.isSuccessful();
			if ( !ok )
				errorMessage = detector.getErrorMessage();
		}
		catch ( final OutOfMemoryError oome )
		{
			errorMessage = "Not enough memory to process the image.";
			ok = false;
			return;
		}
		finally
		{
			final long end = System.currentTimeMillis();
			processingTime = end - start;
			this.detector = null;
		}
	}

	/**
	 * Instantiates a default {@link DetectionCreatorFactory} configured to add
	 * spots to the specified {@link ModelGraph}.
	 * <p>
	 * If a setting key 'ADD_BEHAVIOR' exists in the {@link #settings} field,
	 * the corresponding factory will be returned. Otherwise, the factory
	 * created adds spots to the graph, regardless of possibly pre-existing
	 * spots.
	 * 
	 * @param graph
	 *            the {@link ModelGraph}.
	 * @return a new {@link DetectionCreatorFactory}.
	 */
	protected DetectionCreatorFactory getDetectorFactory( final ModelGraph graph )
	{
		/*
		 * Resolve add detection behavior.
		 */
		final DetectionCreatorFactory detectionCreator;
		if ( null == sti )
		{
			detectionCreator = MamutDetectionCreatorFactories.getAddDetectionCreatorFactory( graph, qualityFeature );
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
			detectionCreator = detectionBehavior.getFactory( graph, qualityFeature, sti );
		}
		return detectionCreator;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public ModelGraph createOutput( final List< SourceAndConverter< ? > > input )
	{
		return new ModelGraph();
	}

	@Override
	public DetectionQualityFeature getQualityFeature()
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

	@Override
	public void setLogger( final Logger logger )
	{
		this.log = logger;
	}

	@Override
	public void setStatusService( final StatusService statusService )
	{
		this.statusService = statusService;
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
