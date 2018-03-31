package org.mastodon.detection.mamut;

import java.util.ArrayList;
import java.util.List;

import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.detection.DetectionCreatorFactory;
import org.mastodon.detection.DetectionCreatorFactory.DetectionCreator;
import org.mastodon.kdtree.ClipConvexPolytope;
import org.mastodon.kdtree.IncrementalNearestNeighborSearch;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.spatial.SpatioTemporalIndex;

import net.imglib2.Interval;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.kdtree.ConvexPolytope;
import net.imglib2.algorithm.kdtree.HyperPlane;
import net.imglib2.neighborsearch.NearestNeighborSearch;

/**
 * Collection of {@link DetectionCreatorFactory}s suitable to be used with a
 * MaMuT {@link Model}.
 * <p>
 * The factory instances of this collection implement different behaviors
 * related to what to do when trying to add a spot near an existing one.
 *
 * @author Jean-Yves Tinevez
 */
public class MamutDetectionCreatorFactories
{

	/**
	 * Enum for available detection creator behaviours.
	 * <p>
	 * {@link DetectionCreatorFactory}s deal with how spots are added to an
	 * existing model, depending on whether there are existing spots in the
	 * vicinity.
	 *
	 * @author Jean-Yves Tinevez
	 */
	public static enum DetectionBehavior
	{
		ADD( "Add", "Add a new spot even if an existing one is found within its radius." ),
		REPLACE( "Replace", "Replace existing spots found within radius of the new one." ),
		DONTADD( "Don't add", "Do not add a new spot if an existing spot is found within radius." ),
		REMOVEALL( "Remove all", "Remove all spots in ROI prior to detection." );

		private final String name;

		private final String info;

		private DetectionBehavior( final String name, final String info )
		{
			this.name = name;
			this.info = info;
		}

		@Override
		public String toString()
		{
			return name;
		}

		public String info()
		{
			return info;
		}

		public DetectionCreatorFactory getFactory( final ModelGraph graph, final DoublePropertyMap< Spot > pm, final SpatioTemporalIndex< Spot > sti, final Interval roi )
		{
			switch ( this )
			{
			case ADD:
				return getAddDetectionCreatorFactory( graph, pm );
			case DONTADD:
				return getDontAddDetectionCreatorFactory( graph, pm, sti );
			case REMOVEALL:
				return getRemoveAllDetectionCreatorFactory( graph, pm, sti, roi );
			case REPLACE:
				return getReplaceDetectionCreatorFactory( graph, pm, sti );
			default:
				throw new IllegalArgumentException( "Unknown detection bahaviour: " + this );
			}
		}
	}

	public static final DetectionCreatorFactory getAddDetectionCreatorFactory( final ModelGraph graph, final DoublePropertyMap< Spot > pm )
	{
		return new AddDetectionCreatorFactory( graph, pm );
	}

	public static final DetectionCreatorFactory getDontAddDetectionCreatorFactory( final ModelGraph graph, final DoublePropertyMap< Spot > pm, final SpatioTemporalIndex< Spot > sti )
	{
		return new DontAddDetectionCreatorFactory( graph, pm, sti );
	}

	public static final DetectionCreatorFactory getReplaceDetectionCreatorFactory( final ModelGraph graph, final DoublePropertyMap< Spot > pm, final SpatioTemporalIndex< Spot > sti )
	{
		return new ReplaceDetectionCreatorFactory( graph, pm, sti );
	}

	public static final DetectionCreatorFactory getRemoveAllDetectionCreatorFactory( final ModelGraph graph, final DoublePropertyMap< Spot > pm, final SpatioTemporalIndex< Spot > sti, final Interval roi )
	{
		return new RemoveAllDetectionCreatorFactory( graph, pm, sti, roi );
	}

	/**
	 * Default detection creator suitable to create {@link Spot} vertices in a
	 * {@link ModelGraph} from the detection returned by the detector. Takes
	 * care of acquiring the writing lock before adding all the detections of a
	 * time-point and returning it after. Also resets and feeds the quality
	 * value to a quality feature.
	 * <p>
	 * Add spots to the model, regardless of whether there is an existing one
	 * nearby.
	 *
	 * @author Jean-Yves Tinevez
	 *
	 */
	private static class AddDetectionCreatorFactory implements DetectionCreatorFactory
	{

		private final DoublePropertyMap< Spot > pm;

		private final ModelGraph graph;

		public AddDetectionCreatorFactory( final ModelGraph graph, final DoublePropertyMap< Spot > pm )
		{
			this.graph = graph;
			this.pm = pm;
		}

		@Override
		public DetectionCreator create( final int timepoint )
		{
			return new AddDetectionCreator( graph, pm, timepoint );
		}
	}

	private static class AddDetectionCreator implements DetectionCreator
	{

		protected final Spot ref;

		protected final int timepoint;

		protected final DoublePropertyMap< Spot > pm;

		protected final ModelGraph graph;

		private AddDetectionCreator( final ModelGraph graph, final DoublePropertyMap< Spot > pm, final int timepoint )
		{
			this.graph = graph;
			this.pm = pm;
			this.timepoint = timepoint;
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
		public void createDetection( final double[] pos, final double radius, final double quality )
		{
			final Spot spot = graph.addVertex( ref ).init( timepoint, pos, radius );
			pm.set( spot, quality );
		}
	}

	private static final class RemoveAllDetectionCreatorFactory implements DetectionCreatorFactory
	{

		private final ModelGraph graph;

		private final DoublePropertyMap< Spot > pm;

		private final SpatioTemporalIndex< Spot > sti;

		private final Interval roi;

		public RemoveAllDetectionCreatorFactory( final ModelGraph graph, final DoublePropertyMap< Spot > pm, final SpatioTemporalIndex< Spot > sti, final Interval roi )
		{
			this.graph = graph;
			this.pm = pm;
			this.sti = sti;
			this.roi = roi;
		}

		@Override
		public DetectionCreator create( final int timepoint )
		{
			return new RemoveAllDetectionCreator( graph, pm, sti.getSpatialIndex( timepoint ), roi, timepoint );
		}
	}

	private static class RemoveAllDetectionCreator implements DetectionCreator
	{

		private final ModelGraph graph;

		private final DoublePropertyMap< Spot > pm;

		private final SpatialIndex< Spot > spatialIndex;

		private final int timepoint;

		private final Interval roi;

		private final Spot ref;

		public RemoveAllDetectionCreator( final ModelGraph graph, final DoublePropertyMap< Spot > pm, final SpatialIndex< Spot > spatialIndex, final Interval roi, final int timepoint )
		{
			this.graph = graph;
			this.pm = pm;
			this.spatialIndex = spatialIndex;
			this.roi = roi;
			this.timepoint = timepoint;
			this.ref = graph.vertexRef();
		}

		@Override
		public void preAddition()
		{
			graph.getLock().readLock().lock();
			final RefList< Spot > toRemove = RefCollections.createRefList( graph.vertices() );
			if ( null == roi )
			{
				// Remove all in time-point.
				for ( final Spot spot : spatialIndex )
					toRemove.add( spot );
			}
			else
			{
				/*
				 * FIXME Does not work. Maybe the normal are not with the right orientation?
				 */

				// Remove spots in the ROI.
				final double[][] normals = new double[][] {
						new double[] { +1., 0., 0. }, // X min plane.
						new double[] { -1., 0., 0. }, // X max plane.
						new double[] { 0., +1., 0. }, // Y min plane.
						new double[] { 0., -1., 0. }, // Y max plane.
						new double[] { 0., 0., +1. }, // Z min plane.
						new double[] { 0., 0., -1. } // Z max plane.
				};
				final double[] distances = new double[] {
						roi.min( 0 ),
						roi.max( 0 ),
						roi.min( 1 ),
						roi.max( 1 ),
						roi.min( 2 ),
						roi.max( 2 )
				};
				final List< HyperPlane > hyperplanes = new ArrayList<>();
				for ( int i = 0; i < distances.length; i++ )
					hyperplanes.add( new HyperPlane( normals[ i ], distances[ i ] ) );
				final ConvexPolytope cp = new ConvexPolytope( hyperplanes );
				final ClipConvexPolytope< Spot > clip = spatialIndex.getClipConvexPolytope();
				clip.clip( cp );
				for ( final Spot spot : clip.getInsideValues() )
					toRemove.add( spot );
			}

			graph.getLock().readLock().unlock();
			graph.getLock().writeLock().lock();
			for ( final Spot spot : toRemove )
				graph.remove( spot );
		}

		@Override
		public void createDetection( final double[] pos, final double radius, final double quality )
		{
			final Spot spot = graph.addVertex( ref ).init( timepoint, pos, radius );
			pm.set( spot, quality );
		}

		@Override
		public void postAddition()
		{
			graph.getLock().writeLock().unlock();
		}
	}

	private static class ReplaceDetectionCreatorFactory implements DetectionCreatorFactory
	{

		private final DoublePropertyMap< Spot > pm;

		private final ModelGraph graph;

		private final SpatioTemporalIndex< Spot > sti;

		public ReplaceDetectionCreatorFactory( final ModelGraph graph, final DoublePropertyMap< Spot > pm, final SpatioTemporalIndex< Spot > sti )
		{
			this.graph = graph;
			this.pm = pm;
			this.sti = sti;
		}

		@Override
		public DetectionCreator create( final int timepoint )
		{
			return new ReplaceDetectionCreator( graph, pm, sti.getSpatialIndex( timepoint ).getIncrementalNearestNeighborSearch(), timepoint );
		}
	}

	private static class ReplaceDetectionCreator extends AddDetectionCreator
	{

		private final IncrementalNearestNeighborSearch< Spot > search;

		private ReplaceDetectionCreator( final ModelGraph graph, final DoublePropertyMap< Spot > pm, final IncrementalNearestNeighborSearch< Spot > search, final int timepoint )
		{
			super( graph, pm, timepoint );
			this.search = search;
		}

		@Override
		public void createDetection( final double[] pos, final double radius, final double quality )
		{
			final Spot spot = graph.addVertex( ref ).init( timepoint, pos, radius );
			pm.set( spot, quality );

			search.search( spot );
			while ( search.hasNext() )
			{
				final Spot next = search.next();
				if (search.getSquareDistance() > radius * radius )
					break;
				graph.remove( next );
			}

			/*
			 * TODO This is imperfect. There might be large existing spots that are further
			 * than the radius and still encompasses the new spot. We do not act on these.
			 * 
			 * To fix this, we need to know the max radius in a time-point, and therefore to
			 * access the BoundingSphereRadiusStatistics. But the instance is part of the
			 * app model and creating a new one might be costly. Maybe pre-compute the max
			 * radius of this time point "by hand" in the #preAddition method?
			 */
		}
	}

	private static class DontAddDetectionCreatorFactory implements DetectionCreatorFactory
	{

		private final DoublePropertyMap< Spot > pm;

		private final ModelGraph graph;

		private final SpatioTemporalIndex< Spot > sti;

		public DontAddDetectionCreatorFactory( final ModelGraph graph, final DoublePropertyMap< Spot > pm, final SpatioTemporalIndex< Spot > sti )
		{
			this.graph = graph;
			this.pm = pm;
			this.sti = sti;
		}

		@Override
		public DetectionCreator create( final int timepoint )
		{
			return new DontAddDetectionCreator( graph, pm, sti.getSpatialIndex( timepoint ).getNearestNeighborSearch(), timepoint );
		}
	}

	private static class DontAddDetectionCreator extends AddDetectionCreator
	{

		private final NearestNeighborSearch< Spot > search;

		private final RealPoint point;

		private DontAddDetectionCreator( final ModelGraph graph, final DoublePropertyMap< Spot > pm, final NearestNeighborSearch< Spot > search, final int timepoint )
		{
			super( graph, pm, timepoint );
			this.search = search;
			this.point = new RealPoint( 3 );
		}

		@Override
		public void createDetection( final double[] pos, final double radius, final double quality )
		{
			// Check if there is a close existing spot.
			point.setPosition( pos );
			search.search( point );
			// If yes, don't add.
			if ( search.getSquareDistance() < radius * radius )
				return;

			/*
			 * TODO This is imperfect. There might be large existing spots that are further
			 * than the radius and still encompasses the new spot. We do not act on these.
			 * 
			 * To fix this, we need to know the max radius in a time-point, and therefore to
			 * access the BoundingSphereRadiusStatistics. But the instance is part of the
			 * app model and creating a new one might be costly. Maybe pre-compute the max
			 * radius of this time point "by hand" in the #preAddition method?
			 */

			final Spot spot = graph.addVertex( ref ).init( timepoint, pos, radius );
			pm.set( spot, quality );
		}
	}
}
