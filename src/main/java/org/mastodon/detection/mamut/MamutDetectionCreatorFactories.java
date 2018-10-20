package org.mastodon.detection.mamut;

import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.collection.RefSet;
import org.mastodon.detection.DetectionCreatorFactory;
import org.mastodon.detection.DetectionCreatorFactory.DetectionCreator;
import org.mastodon.kdtree.IncrementalNearestNeighborSearch;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.util.EllipsoidInsideTest;

import net.imglib2.RealPoint;

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
	 * {@link DetectionCreatorFactory}s deal with how spots are added to an existing
	 * model, depending on whether there are existing spots in the vicinity.
	 *
	 * @author Jean-Yves Tinevez
	 */
	public static enum DetectionBehavior
	{
		/**
		 * Add a new spot even if an existing one is found within its radius.
		 */
		ADD( "Add", "Add a new spot even if an existing one is found within its radius." ),
		/**
		 * Replace existing spots found within radius of the new one.
		 */
		REPLACE( "Replace", "Replace existing spots found within radius of the new one." ),
		/**
		 * Do not add a new spot if an existing spot is found within radius.
		 */
		DONTADD( "Don't add", "Do not add a new spot if an existing spot is found within radius." ),
		/**
		 * Remove all spots in time-point prior to detection.
		 */
		REMOVEALL( "Remove all", "Remove all spots in time-point prior to detection." );

		private final String str;

		private final String info;

		private DetectionBehavior( final String str, final String info )
		{
			this.str = str;
			this.info = info;
		}

		@Override
		public String toString()
		{
			return str;
		}

		public String info()
		{
			return info;
		}

		public DetectionCreatorFactory getFactory( final ModelGraph graph, final DetectionQualityFeature qualityFeature, final SpatioTemporalIndex< Spot > sti )
		{
			switch ( this )
			{
			case ADD:
				return getAddDetectionCreatorFactory( graph, qualityFeature );
			case DONTADD:
				return getDontAddDetectionCreatorFactory( graph, qualityFeature, sti );
			case REMOVEALL:
				return getRemoveAllDetectionCreatorFactory( graph, qualityFeature, sti );
			case REPLACE:
				return getReplaceDetectionCreatorFactory( graph, qualityFeature, sti );
			default:
				throw new IllegalArgumentException( "Unknown detection bahaviour: " + this );
			}
		}
	}

	public static final DetectionCreatorFactory getAddDetectionCreatorFactory( final ModelGraph graph, final DetectionQualityFeature qualityFeature )
	{
		return new AddDetectionCreatorFactory( graph, qualityFeature );
	}

	public static final DetectionCreatorFactory getDontAddDetectionCreatorFactory( final ModelGraph graph, final DetectionQualityFeature qualityFeature, final SpatioTemporalIndex< Spot > sti )
	{
		return new DontAddDetectionCreatorFactory( graph, qualityFeature, sti );
	}

	public static final DetectionCreatorFactory getReplaceDetectionCreatorFactory( final ModelGraph graph, final DetectionQualityFeature qualityFeature, final SpatioTemporalIndex< Spot > sti )
	{
		return new ReplaceDetectionCreatorFactory( graph, qualityFeature, sti );
	}

	public static final DetectionCreatorFactory getRemoveAllDetectionCreatorFactory( final ModelGraph graph, final DetectionQualityFeature qualityFeature, final SpatioTemporalIndex< Spot > sti )
	{
		return new RemoveAllDetectionCreatorFactory( graph, qualityFeature, sti );
	}

	/**
	 * Default detection creator suitable to create {@link Spot} vertices in a
	 * {@link ModelGraph} from the detection returned by the detector. Takes care of
	 * acquiring the writing lock before adding all the detections of a time-point
	 * and returning it after. Also resets and feeds the quality value to a quality
	 * feature.
	 * <p>
	 * Add spots to the model, regardless of whether there is an existing one
	 * nearby.
	 *
	 * @author Jean-Yves Tinevez
	 *
	 */
	private static class AddDetectionCreatorFactory implements DetectionCreatorFactory
	{

		private final DetectionQualityFeature qualityFeature;

		private final ModelGraph graph;

		public AddDetectionCreatorFactory( final ModelGraph graph, final DetectionQualityFeature qualityFeature )
		{
			this.graph = graph;
			this.qualityFeature = qualityFeature;
		}

		@Override
		public DetectionCreator create( final int timepoint )
		{
			return new AddDetectionCreator( graph, qualityFeature, timepoint );
		}
	}

	private static class AddDetectionCreator implements DetectionCreator
	{

		protected final Spot ref;

		protected final int timepoint;

		protected final DetectionQualityFeature qualityFeature;

		protected final ModelGraph graph;

		private AddDetectionCreator( final ModelGraph graph, final DetectionQualityFeature qualityFeature, final int timepoint )
		{
			this.graph = graph;
			this.qualityFeature = qualityFeature;
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
			qualityFeature.set( spot, quality );
		}
	}

	private static final class RemoveAllDetectionCreatorFactory implements DetectionCreatorFactory
	{

		private final ModelGraph graph;

		private final DetectionQualityFeature qualityFeature;

		private final SpatioTemporalIndex< Spot > sti;

		public RemoveAllDetectionCreatorFactory( final ModelGraph graph, final DetectionQualityFeature qualityFeature, final SpatioTemporalIndex< Spot > sti )
		{
			this.graph = graph;
			this.qualityFeature = qualityFeature;
			this.sti = sti;
		}

		@Override
		public DetectionCreator create( final int timepoint )
		{
			return new RemoveAllDetectionCreator( graph, qualityFeature, sti.getSpatialIndex( timepoint ), timepoint );
		}
	}

	private static class RemoveAllDetectionCreator implements DetectionCreator
	{

		private final ModelGraph graph;

		private final DetectionQualityFeature qualityFeature;

		private final SpatialIndex< Spot > spatialIndex;

		private final int timepoint;

		private final Spot ref;

		public RemoveAllDetectionCreator( final ModelGraph graph, final DetectionQualityFeature qualityFeature, final SpatialIndex< Spot > spatialIndex, final int timepoint )
		{
			this.graph = graph;
			this.qualityFeature = qualityFeature;
			this.spatialIndex = spatialIndex;
			this.timepoint = timepoint;
			this.ref = graph.vertexRef();
		}

		@Override
		public void preAddition()
		{
			graph.getLock().readLock().lock();
			final RefList< Spot > toRemove = RefCollections.createRefList( graph.vertices() );
			// Remove all in time-point.
			for ( final Spot spot : spatialIndex )
				toRemove.add( spot );

			graph.getLock().readLock().unlock();
			graph.getLock().writeLock().lock();
			for ( final Spot spot : toRemove )
				graph.remove( spot );
		}

		@Override
		public void createDetection( final double[] pos, final double radius, final double quality )
		{
			final Spot spot = graph.addVertex( ref ).init( timepoint, pos, radius );
			qualityFeature.set( spot, quality );
		}

		@Override
		public void postAddition()
		{
			graph.getLock().writeLock().unlock();
		}
	}

	private static class ReplaceDetectionCreatorFactory implements DetectionCreatorFactory
	{

		private final DetectionQualityFeature qualityFeature;

		private final ModelGraph graph;

		private final SpatioTemporalIndex< Spot > sti;

		public ReplaceDetectionCreatorFactory( final ModelGraph graph, final DetectionQualityFeature qualityFeature, final SpatioTemporalIndex< Spot > sti )
		{
			this.graph = graph;
			this.qualityFeature = qualityFeature;
			this.sti = sti;
		}

		@Override
		public DetectionCreator create( final int timepoint )
		{
			return new ReplaceDetectionCreator( graph, qualityFeature, sti.getSpatialIndex( timepoint ), timepoint );
		}
	}

	private static class ReplaceDetectionCreator implements DetectionCreator
	{

		private final IncrementalNearestNeighborSearch< Spot > search;

		private double r2max = 0.;

		private final SpatialIndex< Spot > si;

		private final EllipsoidInsideTest test;

		private final RefSet< Spot > toRemove;

		private final ModelGraph graph;

		private final DetectionQualityFeature qualityFeature;

		private final int timepoint;

		private final Spot ref;

		private ReplaceDetectionCreator( final ModelGraph graph, final DetectionQualityFeature qualityFeature, final SpatialIndex< Spot > si, final int timepoint )
		{
			this.graph = graph;
			this.qualityFeature = qualityFeature;
			this.si = si;
			this.timepoint = timepoint;
			this.ref = graph.vertexRef();
			this.search = si.getIncrementalNearestNeighborSearch();
			this.test = new EllipsoidInsideTest();
			this.toRemove = RefCollections.createRefSet( graph.vertices() );
		}

		@Override
		public void preAddition()
		{
			toRemove.clear();
			// Measure current max bounding sphere radius squared.
			graph.getLock().readLock().lock();
			this.r2max = 0.;
			for ( final Spot spot : si )
				if ( spot.getBoundingSphereRadiusSquared() > r2max )
					r2max = spot.getBoundingSphereRadiusSquared();

			graph.getLock().readLock().unlock();
			graph.getLock().writeLock().lock();
		}

		@Override
		public void createDetection( final double[] pos, final double radius, final double quality )
		{
			final Spot spot = graph.addVertex( ref ).init( timepoint, pos, radius );
			qualityFeature.set( spot, quality );
			search.search( spot );
			final double lr2 = Math.max( r2max, radius * radius );
			while ( search.hasNext() )
			{
				final Spot neigbhor = search.next();
				if ( neigbhor.equals( spot ) )
					continue;
				if ( search.getSquareDistance() > lr2 )
					break;
				if ( test.areCentersInside( spot, neigbhor ) )
					toRemove.add( neigbhor );
			}
		}

		@Override
		public void postAddition()
		{
			for ( final Spot spot : toRemove )
				graph.remove( spot );

			graph.getLock().writeLock().unlock();
		}
	}

	private static class DontAddDetectionCreatorFactory implements DetectionCreatorFactory
	{

		private final DetectionQualityFeature qualityFeature;

		private final ModelGraph graph;

		private final SpatioTemporalIndex< Spot > sti;

		public DontAddDetectionCreatorFactory( final ModelGraph graph, final DetectionQualityFeature qualityFeature, final SpatioTemporalIndex< Spot > sti )
		{
			this.graph = graph;
			this.qualityFeature = qualityFeature;
			this.sti = sti;
		}

		@Override
		public DetectionCreator create( final int timepoint )
		{
			return new DontAddDetectionCreator( graph, qualityFeature, sti.getSpatialIndex( timepoint ), timepoint );
		}
	}

	private static class DontAddDetectionCreator implements DetectionCreator
	{

		private final IncrementalNearestNeighborSearch< Spot > search;

		private double r2max;

		private final ModelGraph graph;

		private final DetectionQualityFeature qualityFeature;

		private final int timepoint;

		private final SpatialIndex< Spot > si;

		private final EllipsoidInsideTest test;

		private final Spot ref;

		private final RealPoint center;

		private DontAddDetectionCreator( final ModelGraph graph, final DetectionQualityFeature qualityFeature, final SpatialIndex< Spot > si, final int timepoint )
		{
			this.graph = graph;
			this.qualityFeature = qualityFeature;
			this.si = si;
			this.search = si.getIncrementalNearestNeighborSearch();
			this.timepoint = timepoint;
			this.ref = graph.vertexRef();
			this.test = new EllipsoidInsideTest();
			this.center = new RealPoint( 3 );
		}

		@Override
		public void preAddition()
		{
			graph.getLock().readLock().lock();
			this.r2max = 0.;
			for ( final Spot spot : si )
				if ( spot.getBoundingSphereRadiusSquared() > r2max )
					r2max = spot.getBoundingSphereRadiusSquared();

			graph.getLock().readLock().unlock();
			graph.getLock().writeLock().lock();
		}

		@Override
		public void createDetection( final double[] pos, final double radius, final double quality )
		{
			center.setPosition( pos );
			search.search( center );
			final double lr2 = Math.max( r2max, radius * radius );
			while ( search.hasNext() )
			{
				final Spot neigbhor = search.next();
				if ( search.getSquareDistance() > lr2 )
					break;
				if ( test.isCenterWithin( neigbhor, pos, radius ) || test.isPointInside( pos, neigbhor ) )
					return;
			}

			final Spot spot = graph.addVertex( ref ).init( timepoint, pos, radius );
			qualityFeature.set( spot, quality );
		}

		@Override
		public void postAddition()
		{
			graph.getLock().writeLock().unlock();
		}
	}

	private MamutDetectionCreatorFactories()
	{}
}
