package org.mastodon.detection.mamut;

import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.collection.RefSet;
import org.mastodon.detection.DetectionCreatorFactory;
import org.mastodon.detection.DetectionCreatorFactory.DetectionCreator;
import org.mastodon.kdtree.IncrementalNearestNeighborSearch;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.spatial.SpatioTemporalIndex;

import net.imglib2.RealPoint;
import net.imglib2.util.LinAlgHelpers;

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

		public DetectionCreatorFactory getFactory( final ModelGraph graph, final DoublePropertyMap< Spot > pm, final SpatioTemporalIndex< Spot > sti )
		{
			switch ( this )
			{
			case ADD:
				return getAddDetectionCreatorFactory( graph, pm );
			case DONTADD:
				return getDontAddDetectionCreatorFactory( graph, pm, sti );
			case REMOVEALL:
				return getRemoveAllDetectionCreatorFactory( graph, pm, sti );
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

	public static final DetectionCreatorFactory getRemoveAllDetectionCreatorFactory( final ModelGraph graph, final DoublePropertyMap< Spot > pm, final SpatioTemporalIndex< Spot > sti )
	{
		return new RemoveAllDetectionCreatorFactory( graph, pm, sti );
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

		public RemoveAllDetectionCreatorFactory( final ModelGraph graph, final DoublePropertyMap< Spot > pm, final SpatioTemporalIndex< Spot > sti )
		{
			this.graph = graph;
			this.pm = pm;
			this.sti = sti;
		}

		@Override
		public DetectionCreator create( final int timepoint )
		{
			return new RemoveAllDetectionCreator( graph, pm, sti.getSpatialIndex( timepoint ), timepoint );
		}
	}

	private static class RemoveAllDetectionCreator implements DetectionCreator
	{

		private final ModelGraph graph;

		private final DoublePropertyMap< Spot > pm;

		private final SpatialIndex< Spot > spatialIndex;

		private final int timepoint;

		private final Spot ref;

		public RemoveAllDetectionCreator( final ModelGraph graph, final DoublePropertyMap< Spot > pm, final SpatialIndex< Spot > spatialIndex, final int timepoint )
		{
			this.graph = graph;
			this.pm = pm;
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
			return new ReplaceDetectionCreator( graph, pm, sti.getSpatialIndex( timepoint ), timepoint );
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

		private final DoublePropertyMap< Spot > pm;

		private final int timepoint;

		private final Spot ref;

		private ReplaceDetectionCreator( final ModelGraph graph, final DoublePropertyMap< Spot > pm, final SpatialIndex< Spot > si, final int timepoint )
		{
			this.graph = graph;
			this.pm = pm;
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
			pm.set( spot, quality );
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
			return new DontAddDetectionCreator( graph, pm, sti.getSpatialIndex( timepoint ), timepoint );
		}
	}

	private static class DontAddDetectionCreator implements DetectionCreator
	{

		private final IncrementalNearestNeighborSearch< Spot > search;

		private double r2max;

		private final ModelGraph graph;

		private final DoublePropertyMap< Spot > pm;

		private final int timepoint;

		private final SpatialIndex< Spot > si;

		private final EllipsoidInsideTest test;

		private final Spot ref;

		private final RealPoint center;

		private DontAddDetectionCreator( final ModelGraph graph, final DoublePropertyMap< Spot > pm, final SpatialIndex< Spot > si, final int timepoint )
		{
			this.graph = graph;
			this.pm = pm;
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
			pm.set( spot, quality );
		}

		@Override
		public void postAddition()
		{
			graph.getLock().writeLock().unlock();
		}
	}

	/**
	 * Adapted from ScreenVertexMath.
	 *
	 */
	private static class EllipsoidInsideTest
	{

		/** Holder for the first spot position. */
		private final double[] pos1 = new double[ 3 ];

		/** Holder for the second spot position. */
		private final double[] pos2 = new double[ 3 ];

		/** Holder for the covariance position. */
		private final double[][] cov = new double[ 3 ][ 3 ];

		/** Used to determines whether a spot contains a position. */
		private final double[] diff = new double[ 3 ];

		/** Used to determines whether a spot contains a position. */
		private final double[] vn = new double[ 3 ];

		/** Precision. */
		private final double[][] P = new double[ 3 ][ 3 ];

		/**
		 * Returns <code>true</code> if the first spot contains the center of the second
		 * one, or if the second spot contains the center of the first one.
		 * 
		 * @param s1
		 *            the first spot.
		 * @param s2
		 *            the second spot.
		 * @return <code>true</code> if a center of spot is included in the other spot.
		 */
		public boolean areCentersInside( final Spot s1, final Spot s2 )
		{
			s2.localize( pos2 );
			if ( isPointInside( pos2, s1 ) )
				return true;

			s1.localize( pos2 );
			return isPointInside( pos2, s2 );
		}

		/**
		 * Returns <code>true</code> is the specified position lies inside the spot
		 * ellipsoid.
		 * 
		 * @param pos
		 *            the position to test.
		 * @param spot
		 *            the spot.
		 * @return <code>true</code> if the position is inside the spot.
		 */
		public boolean isPointInside( final double[] pos, final Spot spot )
		{
			spot.localize( pos1 );
			LinAlgHelpers.subtract( pos1, pos, diff );
			spot.getCovariance( cov );
			LinAlgHelpers.invertSymmetric3x3( cov, P );
			LinAlgHelpers.mult( P, diff, vn );
			final double d2 = LinAlgHelpers.dot( diff, vn );
			return d2 < 1.;
		}

		/**
		 * Returns <code>true</code> if the spot center is within the specified radius
		 * around the specified position.
		 * 
		 * @param spot
		 *            the spot to test.
		 * @param pos
		 *            the position.
		 * @param radius
		 *            the radius.
		 * @return <code>true</code> if the spot center is within <code>radius</code> of
		 *         <code>pos</code>.
		 */
		public boolean isCenterWithin( final Spot spot, final double[] pos, final double radius )
		{
			spot.localize( pos1 );
			return LinAlgHelpers.squareDistance( pos1, pos ) < radius * radius;
		}
	}

	private MamutDetectionCreatorFactories()
	{}
}
