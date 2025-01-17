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
package org.mastodon.tracking.mamut.trackmate;

import static org.mastodon.tracking.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_DO_LINK_SELECTION;

import java.util.List;
import java.util.Map;

import org.mastodon.HasErrorMessage;
import org.mastodon.graph.algorithm.RootFinder;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.SelectionModel;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.spatial.SpatioTemporalIndexSelection;
import org.mastodon.tracking.mamut.detection.DetectionQualityFeature;
import org.mastodon.tracking.mamut.detection.SpotDetectorOp;
import org.mastodon.tracking.mamut.linking.LinkCostFeature;
import org.mastodon.tracking.mamut.linking.SpotLinkerOp;
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
		 * Exec detection (or not).
		 */

		final long start = System.currentTimeMillis();
		final Class< ? extends SpotDetectorOp > cl = settings.values.getDetector();
		if ( cl == null )
		{
			logger.info( "No detector specified. Skipping detection.\n" );
			return true;
		}

		final Map< String, Object > detectorSettings = settings.values.getDetectorSettings();
		final DetectionQualityFeature qualityFeature = DetectionQualityFeature.getOrRegister(
				model.getFeatureModel(), graph.vertices().getRefPool() );

		final SpotDetectorOp detector = ( SpotDetectorOp ) Hybrids.unaryCF( ops, cl,
				graph, sources,
				detectorSettings,
				model.getSpatioTemporalIndex(),
				qualityFeature );
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

		model.getFeatureModel().declareFeature(  detector.getQualityFeature() );
		final long end = System.currentTimeMillis();
		logger.info( String.format( "Detection completed in %.1f s.\n", ( end - start ) / 1000. ) );
		logger.info( "There is now " + graph.vertices().size() + " spots.\n" );

		model.setUndoPoint();
		graph.notifyGraphChanged();

		return true;
	}

	public boolean execParticleLinking()
	{
		succesful = true;
		errorMessage = null;
		if ( isCanceled() )
			return true;

		final Class< ? extends SpotLinkerOp > linkerCl = settings.values.getLinker();
		if ( linkerCl == null )
		{
			logger.info( "No linker specified. Skipping linking.\n" );
			return true;
		}

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
		final LinkCostFeature linkCostFeature = LinkCostFeature.getOrRegister(
				model.getFeatureModel(), model.getGraph().edges().getRefPool() );

		final SpotLinkerOp linker =
				( SpotLinkerOp ) Inplaces.binary1( ops, linkerCl, model.getGraph(), target,
						linkerSettings,
						model.getFeatureModel(),
						linkCostFeature );
		linker.setLogger( logger );
		linker.setStatusService( statusService );

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

		model.setUndoPoint();
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
