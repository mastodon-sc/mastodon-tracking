package org.mastodon.tracking.lap;


import static org.mastodon.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static org.mastodon.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static org.mastodon.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static org.mastodon.tracking.TrackerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static org.mastodon.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static org.mastodon.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static org.mastodon.tracking.lap.LAPUtils.checkFeatureMap;
import static org.mastodon.tracking.lap.LAPUtils.checkParameter;

import java.util.Comparator;
import java.util.Map;

import org.mastodon.collection.RefRefMap;
import org.mastodon.graph.Edge;
import org.mastodon.graph.Graph;
import org.mastodon.graph.Vertex;
import org.mastodon.revised.mamut.ProgressListener;
import org.mastodon.revised.model.feature.FeatureModel;
import org.mastodon.spatial.HasTimepoint;
import org.mastodon.tracking.ProgressListeners;
import org.mastodon.tracking.lap.costmatrix.JaqamanSegmentCostMatrixCreator;
import org.mastodon.tracking.lap.linker.JaqamanLinker;

import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;

/**
 * This class tracks deals with the second step of tracking according to the LAP
 * tracking framework formulated by Jaqaman, K. et al.
 * "Robust single-particle tracking in live-cell time-lapse sequences." Nature
 * Methods, 2008.
 *
 * <p>
 * In this tracking framework, tracking is divided into two steps:
 *
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
 * <li>Split (the start of one track is linked to the middle of another track)</li>
 * <li>Merged (the end of one track is linked to the middle of another track</li>
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
 */
public class SparseLAPSegmentTracker< V extends Vertex< E > & HasTimepoint & RealLocalizable, E extends Edge< V > > implements Algorithm, Benchmark, MultiThreaded
{

	private static final String BASE_ERROR_MESSAGE = "[SparseLAPSegmentTracker] ";

	private final FeatureModel< V, E > featureModel;

	private final Graph< V, E > graph;

	private final Map< String, Object > settings;

	private final Comparator< V > spotComparator;

	private String errorMessage;

	private long processingTime;

	private int numThreads;

	private ProgressListener logger = ProgressListeners.voidLogger();



	public SparseLAPSegmentTracker( final Graph< V, E > graph, final FeatureModel< V, E > featureModel, final Map< String, Object > settings, final Comparator< V > spotComparator )
	{
		this.graph = graph;
		this.featureModel = featureModel;
		this.settings = settings;
		this.spotComparator = spotComparator;
		setNumThreads();
	}

	@Override
	public boolean checkInput()
	{
		return true;
	}

	@Override
	public boolean process()
	{
		/*
		 * Check input now.
		 */

		// Check that the objects list itself isn't null
		if ( null == graph )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The input graph is null.";
			return false;
		}

		// Check parameters
		final StringBuilder errorHolder = new StringBuilder();
		if ( !checkSettingsValidity( settings, errorHolder ) )
		{
			errorMessage = BASE_ERROR_MESSAGE + errorHolder.toString();
			return false;
		}

		/*
		 * Process.
		 */

		final long start = System.currentTimeMillis();

		/*
		 * Top-left costs.
		 */

		logger.showStatus( "Creating the segment linking cost matrix..." );
		final JaqamanSegmentCostMatrixCreator< V, E > costMatrixCreator = new JaqamanSegmentCostMatrixCreator<>(
				graph, featureModel, settings, spotComparator );
		costMatrixCreator.setNumThreads( numThreads );
		final JaqamanLinker< V, V > linker = new JaqamanLinker<>( costMatrixCreator, graph.vertices(), graph.vertices() );
		if ( !linker.checkInput() || !linker.process() )
		{
			errorMessage = linker.getErrorMessage();
			return false;
		}


		/*
		 * Create links in graph.
		 */

		logger.showProgress( 9, 10 );
		logger.showStatus( "Creating links..." );

		final RefRefMap< V, V > assignment = linker.getResult();
		final E eref = graph.edgeRef();
		final V vref = graph.vertexRef();
		for ( final V source : assignment.keySet() )
		{
			final V target = assignment.get( source, vref );
			graph.addEdge( source, target, eref );
		}
		graph.releaseRef( eref );
		graph.releaseRef( vref );

		logger.showProgress( 1, 1 );
		logger.showStatus( "" );
		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
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
	public void setNumThreads()
	{
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}

	public void setProgressListener( final ProgressListener logger )
	{
		this.logger = logger;
	}
}
