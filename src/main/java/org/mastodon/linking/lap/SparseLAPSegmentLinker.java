package org.mastodon.linking.lap;

import static org.mastodon.linking.LinkerKeys.KEY_ALLOW_GAP_CLOSING;
import static org.mastodon.linking.LinkerKeys.KEY_ALLOW_TRACK_MERGING;
import static org.mastodon.linking.LinkerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static org.mastodon.linking.LinkerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static org.mastodon.linking.LinkerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static org.mastodon.linking.LinkerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static org.mastodon.linking.LinkingUtils.checkFeatureMap;
import static org.mastodon.linking.LinkingUtils.checkParameter;

import java.util.Map;

import org.mastodon.collection.RefDoubleMap;
import org.mastodon.collection.RefRefMap;
import org.mastodon.graph.Edge;
import org.mastodon.graph.Graph;
import org.mastodon.graph.Vertex;
import org.mastodon.linking.AbstractParticleLinkerOp;
import org.mastodon.linking.LinkingUtils;
import org.mastodon.linking.lap.costmatrix.JaqamanSegmentCostMatrixCreator;
import org.mastodon.linking.lap.linker.JaqamanLinker;
import org.mastodon.linking.lap.linker.SparseCostMatrix;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.spatial.HasTimepoint;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.scijava.plugin.Plugin;

import net.imagej.ops.special.function.Functions;
import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.Benchmark;

/**
 * This class tracks deals with the second step of tracking according to the LAP
 * tracking framework formulated by Jaqaman, K. et al. "Robust single-particle
 * tracking in live-cell time-lapse sequences." Nature Methods, 2008.
 * <p>
 * In this tracking framework, tracking is divided into two steps:
 * <ol>
 * <li>Identify individual track segments</li>
 * <li>Gap closing, merging and splitting</li>
 * </ol>
 * and this class does the second step.
 * <p>
 * It first extract track segment from a specified graph, and create a cost
 * matrix corresponding to the following events: Track segments can be:
 * <ul>
 * <li>Linked end-to-tail (gap closing)</li>
 * <li>Split (the start of one track is linked to the middle of another
 * track)</li>
 * <li>Merged (the end of one track is linked to the middle of another
 * track</li>
 * <li>Terminated (track ends)</li>
 * <li>Initiated (track starts)</li>
 * </ul>
 * The cost matrix for this step is illustrated in Figure 1c in the paper.
 * However, there is some important deviations from the paper: The alternative
 * costs that specify the cost for track termination or initiation are all
 * equals to the same fixed value.
 * <p>
 * The class itself uses a sparse version of the cost matrix and a solver that
 * can exploit it. Therefore it is optimized for memory usage rather than speed.
 *
 * @param <V>
 *            the type of vertices in the graph.
 * @param <E>
 *            the type of edges in the graph.
 */
@Plugin( type = SparseLAPSegmentLinker.class )
public class SparseLAPSegmentLinker< V extends Vertex< E > & HasTimepoint & RealLocalizable, E extends Edge< V > >
		extends AbstractParticleLinkerOp< V, E >
		implements Benchmark
{

	private static final String BASE_ERROR_MESSAGE = "[SparseLAPSegmentLinker] ";

	private long processingTime;

	@Override
	public void mutate1( final Graph< V, E > graph, final SpatioTemporalIndex< V > spots )
	{
		ok = false;
		final DoublePropertyMap< E > linkcost = new DoublePropertyMap<>( graph.edges(), Double.NaN );

		/*
		 * Check input now.
		 */

		// Check parameters
		final StringBuilder errorHolder = new StringBuilder();
		if ( !checkSettingsValidity( settings, errorHolder ) )
		{
			errorMessage = BASE_ERROR_MESSAGE + errorHolder.toString();
			return;
		}

		/*
		 * Process.
		 */

		final long start = System.currentTimeMillis();

		/*
		 * Top-left costs.
		 */

		statusService.showStatus( "Creating the segment linking cost matrix..." );

		/*
		 * TODO Make these processes cancelable? The cost matrix calculation can
		 * take a long time, and its solving too. It maybe makes sense to check
		 * whether the user canceled in between to qui calculation early. But
		 * then we have to make the JaqamanLinker cancelable.
		 */

		@SuppressWarnings( "unchecked" )
		final JaqamanSegmentCostMatrixCreator< V, E > costMatrixCreator =
				( JaqamanSegmentCostMatrixCreator< V, E > ) Functions.nullary( ops(), JaqamanSegmentCostMatrixCreator.class, SparseCostMatrix.class,
						graph, featureModel, settings, spotComparator );
		final JaqamanLinker< V, V > linker = new JaqamanLinker<>( costMatrixCreator, graph.vertices(), graph.vertices() );
		if ( !linker.checkInput() || !linker.process() )
		{
			errorMessage = linker.getErrorMessage();
			return;
		}

		/*
		 * Create links in graph.
		 */

		statusService.showProgress( 9, 10 );
		statusService.showStatus( "Creating links..." );
		final RefRefMap< V, V > assignment = linker.getResult();
		final RefDoubleMap< V > assignmentCosts = linker.getAssignmentCosts();
		synchronized ( graph )
		{

			final E eref = graph.edgeRef();
			final V vref = graph.vertexRef();
			for ( final V source : assignment.keySet() )
			{
				final V target = assignment.get( source, vref );
				final double cost = assignmentCosts.get( source );
				final E edge = edgeCreator.createEdge( graph, eref, source, target, cost );
				linkcost.set( edge, cost );
			}
			graph.releaseRef( eref );
			graph.releaseRef( vref );
		}

		statusService.clearStatus();
		final long end = System.currentTimeMillis();
		processingTime = end - start;
		this.linkCostFeature = LinkingUtils.getLinkCostFeature( linkcost );
		ok = true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	private static final boolean checkSettingsValidity( final Map< String, Object > settings, final StringBuilder str )
	{
		if ( null == settings )
		{
			str.append( "Settings map is null.\n" );
			return false;
		}

		/*
		 * In this class, we just need the following. We will check later for
		 * other parameters.
		 */

		boolean ok = true;
		// Gap-closing
		ok = ok & checkParameter( settings, KEY_ALLOW_GAP_CLOSING, Boolean.class, str );
		ok = ok & checkParameter( settings, KEY_GAP_CLOSING_MAX_DISTANCE, Double.class, str );
		ok = ok & checkParameter( settings, KEY_GAP_CLOSING_MAX_FRAME_GAP, Integer.class, str );
		ok = ok & checkFeatureMap( settings, KEY_GAP_CLOSING_FEATURE_PENALTIES, str );
		// Splitting
		ok = ok & checkParameter( settings, KEY_ALLOW_TRACK_SPLITTING, Boolean.class, str );
		// Merging
		ok = ok & checkParameter( settings, KEY_ALLOW_TRACK_MERGING, Boolean.class, str );
		return ok;
	}

	@Override
	public boolean isSuccessful()
	{
		return ok;
	}

}
