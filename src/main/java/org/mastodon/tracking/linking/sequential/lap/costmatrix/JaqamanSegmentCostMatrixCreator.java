package org.mastodon.tracking.linking.sequential.lap.costmatrix;

import static org.mastodon.tracking.linking.LinkerKeys.KEY_ALLOW_GAP_CLOSING;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_ALLOW_TRACK_MERGING;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_CUTOFF_PERCENTILE;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_MERGING_MAX_DISTANCE;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_SPLITTING_MAX_DISTANCE;
import static org.mastodon.tracking.linking.LinkingUtils.checkFeatureMap;
import static org.mastodon.tracking.linking.LinkingUtils.checkMapKeys;
import static org.mastodon.tracking.linking.LinkingUtils.checkParameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.feature.FeatureModel;
import org.mastodon.feature.FeatureProjectionKey;
import org.mastodon.graph.Edge;
import org.mastodon.graph.ReadOnlyGraph;
import org.mastodon.graph.Vertex;
import org.mastodon.spatial.HasTimepoint;
import org.mastodon.tracking.linking.LinkingUtils;
import org.mastodon.tracking.linking.sequential.lap.costfunction.CostFunction;
import org.mastodon.tracking.linking.sequential.lap.linker.SparseCostMatrix;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import gnu.trove.list.array.TDoubleArrayList;
import net.imagej.ops.special.function.AbstractNullaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.Benchmark;

/**
 * This class generates the top-left quadrant of the LAP segment linking cost
 * matrix, following <code>Jaqaman et al., 2008 Nature Methods</code>. It can
 * also computes the alternative cost value, to use to complete this quadrant
 * with the 3 others in the final LAP cost matrix.
 * <p>
 * Warning: we changed and simplified some things compared to the original paper
 * and the MATLAB implementation by Khulud Jaqaman:
 * <ul>
 * <li>There is only one alternative cost for all segment linking, and it
 * calculated as <code>alternativeCostFactor x 90% percentile</code> of all the
 * non-infinite costs.
 * <li>Costs are based on square distance +/- feature penalties.
 * </ul>
 *
 * @author Jean-Yves Tinevez - 2014 - 2017
 * @param <V>
 *            the type of vertices in the graph.
 * @param <E>
 *            the type of edges in the graph.
 *
 */
@Plugin( type = JaqamanSegmentCostMatrixCreator.class )
public class JaqamanSegmentCostMatrixCreator< V extends Vertex< E > & HasTimepoint & RealLocalizable, E extends Edge< V > >
		extends AbstractNullaryFunctionOp< SparseCostMatrix >
		implements CostMatrixCreatorOp< V, V >, Benchmark
{

	private static String BASE_ERROR_MESSAGE = "[JaqamanSegmentCostMatrixCreator] ";

	@Parameter( type = ItemIO.INPUT )
	private ReadOnlyGraph< V, E > graph;

	@Parameter( type = ItemIO.INPUT )
	private FeatureModel featureModel;

	@Parameter( type = ItemIO.INPUT )
	private Map< String, Object > settings;

	@Parameter( type = ItemIO.INPUT )
	private Comparator< V > spotComparator;

	private String errorMessage;

	private long processingTime;

	@Parameter( type = ItemIO.OUTPUT )
	private RefList< V > uniqueSources;

	@Parameter( type = ItemIO.OUTPUT )
	private RefList< V > uniqueTargets;

	@Parameter( type = ItemIO.OUTPUT )
	private double alternativeCost = -1;

	@Override
	public SparseCostMatrix calculate()
	{
		final long start = System.currentTimeMillis();

		final StringBuilder str = new StringBuilder();
		if ( !checkSettingsValidity( settings, str ) )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Incorrect settings map:\n" + str.toString();
			return null;
		}

		/*
		 * Extract parameters
		 */

		@SuppressWarnings( "unchecked" )
		final Class< V > vertexClass = ( Class< V > ) graph.vertexRef().getClass();
		// Gap closing.
		@SuppressWarnings( "unchecked" )
		final Map< FeatureProjectionKey, Double > gcFeaturePenalties = ( Map< FeatureProjectionKey, Double > ) settings.get( KEY_GAP_CLOSING_FEATURE_PENALTIES );
		final CostFunction< V, V > gcCostFunction = LinkingUtils.getCostFunctionFor( gcFeaturePenalties, featureModel, vertexClass );
		final int maxFrameInterval = ( Integer ) settings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP );
		final double gcMaxDistance = ( Double ) settings.get( KEY_GAP_CLOSING_MAX_DISTANCE );
		final double gcCostThreshold = gcMaxDistance * gcMaxDistance;
		final boolean allowGapClosing = ( Boolean ) settings.get( KEY_ALLOW_GAP_CLOSING );

		// Merging
		@SuppressWarnings( "unchecked" )
		final Map< FeatureProjectionKey, Double > mFeaturePenalties = ( Map< FeatureProjectionKey, Double > ) settings.get( KEY_MERGING_FEATURE_PENALTIES );
		final CostFunction< V, V > mCostFunction = LinkingUtils.getCostFunctionFor( mFeaturePenalties, featureModel, vertexClass );
		final double mMaxDistance = ( Double ) settings.get( KEY_MERGING_MAX_DISTANCE );
		final double mCostThreshold = mMaxDistance * mMaxDistance;
		final boolean allowMerging = ( Boolean ) settings.get( KEY_ALLOW_TRACK_MERGING );

		// Splitting
		@SuppressWarnings( "unchecked" )
		final Map< FeatureProjectionKey, Double > sFeaturePenalties = ( Map< FeatureProjectionKey, Double > ) settings.get( KEY_SPLITTING_FEATURE_PENALTIES );
		final CostFunction< V, V > sCostFunction = LinkingUtils.getCostFunctionFor( sFeaturePenalties, featureModel, vertexClass );
		final boolean allowSplitting = ( Boolean ) settings.get( KEY_ALLOW_TRACK_SPLITTING );
		final double sMaxDistance = ( Double ) settings.get( KEY_SPLITTING_MAX_DISTANCE );
		final double sCostThreshold = sMaxDistance * sMaxDistance;

		// Alternative cost
		final double alternativeCostFactor = ( Double ) settings.get( KEY_ALTERNATIVE_LINKING_COST_FACTOR );
		final double percentile = ( Double ) settings.get( KEY_CUTOFF_PERCENTILE );

		uniqueSources = RefCollections.createRefList( graph.vertices() );
		uniqueTargets = RefCollections.createRefList( graph.vertices() );

		// Do we have to work?
		if ( !allowGapClosing && !allowSplitting && !allowMerging )
			return new SparseCostMatrix( new double[ 0 ], new int[ 0 ], new int[ 0 ], 0 );

		/*
		 * Find segment ends, starts and middle points.
		 */

		final boolean mergingOrSplitting = allowMerging || allowSplitting;

		final GraphSegmentSplitter< V, E > segmentSplitter = new GraphSegmentSplitter< V, E >( graph, mergingOrSplitting );
		final RefList< V > segmentEnds = segmentSplitter.getSegmentEnds();
		final RefList< V > segmentStarts = segmentSplitter.getSegmentStarts();

		/*
		 * Generate all middle points list. We have to sort it by the same order
		 * we will sort the unique list of targets, otherwise the SCM will
		 * complains it does not receive columns in the right order.
		 */
		final List< V > allMiddles;
		if ( mergingOrSplitting )
		{
			final List< RefList< V > > segmentMiddles = segmentSplitter.getSegmentMiddles();
			allMiddles = new ArrayList< V >();
			for ( final RefList< V > segment : segmentMiddles )
				allMiddles.addAll( segment );
		}
		else
		{
			allMiddles = Collections.emptyList();
		}

		/*
		 * Sources and targets.
		 */
		final List< V > sources = RefCollections.createRefList( graph.vertices() );
		final List< V > targets = RefCollections.createRefList( graph.vertices() );
		// Corresponding costs.
		final TDoubleArrayList linkCosts = new TDoubleArrayList();

		/*
		 * A. We iterate over all segment ends, targeting 1st the segment starts
		 * (gap-closing) then the segment middles (merging).
		 */

		for ( final V source : segmentEnds )
		{
			final int sourceFrame = source.getTimepoint();

			/*
			 * Iterate over segment starts - GAP-CLOSING.
			 */

			if ( allowGapClosing )
			{
				for ( final V target : segmentStarts )
				{
					// Check frame interval, must be within user
					// specification.
					final int targetFrame = target.getTimepoint();
					final int tdiff = targetFrame - sourceFrame;
					if ( tdiff < 1 || tdiff > maxFrameInterval )
						continue;

					// Check max distance
					final double cost = gcCostFunction.linkingCost( source, target );
					if ( cost > gcCostThreshold )
						continue;

					sources.add( source );
					targets.add( target );
					linkCosts.add( cost );
				}

			}

			/*
			 * Iterate over middle points - MERGING.
			 */

			if ( allowMerging )
			{
				for ( final V target : allMiddles )
				{
					// Check frame interval, must be 1.
					final int targetFrame = target.getTimepoint();
					final int tdiff = targetFrame - sourceFrame;
					if ( tdiff != 1 )
						continue;

					// Check max distance
					final double cost = mCostFunction.linkingCost( source, target );
					if ( cost > mCostThreshold )
						continue;

					sources.add( source );
					targets.add( target );
					linkCosts.add( cost );
				}
			}
		}

		/*
		 * Iterate over middle points targeting segment starts - SPLITTING
		 */
		if ( allowSplitting )
		{
			for ( final V source : allMiddles )
			{

				final int sourceFrame = source.getTimepoint();
				for ( final V target : segmentStarts )
				{

					// Check frame interval, must be 1.
					final int targetFrame = target.getTimepoint();
					final int tdiff = targetFrame - sourceFrame;
					if ( tdiff != 1 )
						continue;

					// Check max distance
					final double cost = sCostFunction.linkingCost( source, target );
					if ( cost > sCostThreshold )
						continue;

					sources.add( source );
					targets.add( target );
					linkCosts.add( cost );
				}
			}
		}

		/*
		 * Build a sparse cost matrix from this. If the accepted costs are not
		 * empty.
		 */

		final SparseCostMatrix scm;
		if ( sources.isEmpty() || targets.isEmpty() )
		{

			uniqueSources.clear();
			uniqueTargets.clear();
			alternativeCost = Double.NaN;
			scm = new SparseCostMatrix();
			/*
			 * CAREFUL! We return an empty matrix if no acceptable links are
			 * found.
			 */
		}
		else
		{
			@SuppressWarnings( "unchecked" )
			final DefaultCostMatrixCreatorOp< V, V > creator = ( DefaultCostMatrixCreatorOp< V, V > ) Functions.nullary( ops(),
					DefaultCostMatrixCreatorOp.class, SparseCostMatrix.class,
					sources,
					targets,
					linkCosts.toArray(),
					alternativeCostFactor,
					percentile,
					spotComparator,
					spotComparator );
			scm = creator.calculate();
			if ( null == scm )
			{
				errorMessage = "Linking track segments: " + creator.getErrorMessage();
				return null;
			}
			/*
			 * Compute the alternative cost from the cost array
			 */
			alternativeCost = creator.computeAlternativeCosts();
			uniqueSources = creator.getSourceList();
			uniqueTargets = creator.getTargetList();
		}

		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return scm;
	}

	@Override
	public RefList< V > getSourceList()
	{
		return uniqueSources;
	}

	@Override
	public RefList< V > getTargetList()
	{
		return uniqueTargets;
	}

	@Override
	public double getAlternativeCostForSource( final V source )
	{
		return alternativeCost;
	}

	@Override
	public double getAlternativeCostForTarget( final V target )
	{
		return alternativeCost;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	private static final boolean checkSettingsValidity( final Map< String, Object > settings, final StringBuilder str )
	{
		if ( null == settings )
		{
			str.append( "Settings map is null.\n" );
			return false;
		}

		boolean ok = true;
		// Gap-closing
		ok = ok & checkParameter( settings, KEY_ALLOW_GAP_CLOSING, Boolean.class, str );
		ok = ok & checkParameter( settings, KEY_GAP_CLOSING_MAX_DISTANCE, Double.class, str );
		ok = ok & checkParameter( settings, KEY_GAP_CLOSING_MAX_FRAME_GAP, Integer.class, str );
		ok = ok & checkFeatureMap( settings, KEY_GAP_CLOSING_FEATURE_PENALTIES, str );
		// Splitting
		ok = ok & checkParameter( settings, KEY_ALLOW_TRACK_SPLITTING, Boolean.class, str );
		ok = ok & checkParameter( settings, KEY_SPLITTING_MAX_DISTANCE, Double.class, str );
		ok = ok & checkFeatureMap( settings, KEY_SPLITTING_FEATURE_PENALTIES, str );
		// Merging
		ok = ok & checkParameter( settings, KEY_ALLOW_TRACK_MERGING, Boolean.class, str );
		ok = ok & checkParameter( settings, KEY_MERGING_MAX_DISTANCE, Double.class, str );
		ok = ok & checkFeatureMap( settings, KEY_MERGING_FEATURE_PENALTIES, str );
		// Others
		ok = ok & checkParameter( settings, KEY_ALTERNATIVE_LINKING_COST_FACTOR, Double.class, str );
		ok = ok & checkParameter( settings, KEY_CUTOFF_PERCENTILE, Double.class, str );

		// Check keys
		final List< String > mandatoryKeys = new ArrayList< String >();
		mandatoryKeys.add( KEY_ALLOW_GAP_CLOSING );
		mandatoryKeys.add( KEY_GAP_CLOSING_MAX_DISTANCE );
		mandatoryKeys.add( KEY_GAP_CLOSING_MAX_FRAME_GAP );
		mandatoryKeys.add( KEY_ALLOW_TRACK_SPLITTING );
		mandatoryKeys.add( KEY_SPLITTING_MAX_DISTANCE );
		mandatoryKeys.add( KEY_ALLOW_TRACK_MERGING );
		mandatoryKeys.add( KEY_MERGING_MAX_DISTANCE );
		mandatoryKeys.add( KEY_ALTERNATIVE_LINKING_COST_FACTOR );
		mandatoryKeys.add( KEY_CUTOFF_PERCENTILE );
		final List< String > optionalKeys = new ArrayList< String >();
		optionalKeys.add( KEY_GAP_CLOSING_FEATURE_PENALTIES );
		optionalKeys.add( KEY_SPLITTING_FEATURE_PENALTIES );
		optionalKeys.add( KEY_MERGING_FEATURE_PENALTIES );
		ok = ok & checkMapKeys( settings, mandatoryKeys, optionalKeys, str );

		return ok;
	}
}
