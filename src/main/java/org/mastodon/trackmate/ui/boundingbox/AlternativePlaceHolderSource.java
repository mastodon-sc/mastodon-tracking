package org.mastodon.trackmate.ui.boundingbox;

import org.mastodon.trackmate.ui.boundingbox.BoundingBoxOverlay.BoundingBoxOverlaySource;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Sampler;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

final class AlternativePlaceHolderSource implements Source< UnsignedShortType >
{
	private final UnsignedShortType type = new UnsignedShortType();

	private final String name;

	private final BoundingBoxOverlaySource bbSource;

	public AlternativePlaceHolderSource( final String name, final BoundingBoxOverlaySource bbSource )
	{
		this.name = name;
		this.bbSource = bbSource;
	}

	@Override
	public UnsignedShortType getType()
	{
		return type;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return null;
	}

	@Override
	public int getNumMipmapLevels()
	{
		return 1;
	}

	@Override
	public boolean isPresent( final int t )
	{
		return true;
	}

	@Override
	public RandomAccessibleInterval< UnsignedShortType > getSource( final int t, final int level )
	{
		return Views.interval( Views.raster( rra ), Intervals.smallestContainingInterval( bbSource.getInterval() ) );
	}

	@Override
	public RealRandomAccessible< UnsignedShortType > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		return rra;
	}

	@Override
	public void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		bbSource.getTransform( transform );
	}

	private final RealRandomAccessible< UnsignedShortType > rra = new RealRandomAccessible< UnsignedShortType >()
	{
		@Override
		public int numDimensions()
		{
			return 3;
		}

		@Override
		public RealRandomAccess< UnsignedShortType > realRandomAccess()
		{
			return access;
		}

		@Override
		public RealRandomAccess< UnsignedShortType > realRandomAccess( final RealInterval interval )
		{
			return access;
		}
	};

	private final RealRandomAccess< UnsignedShortType > access = new RealRandomAccess< UnsignedShortType >()
	{
		@Override
		public RealRandomAccess< UnsignedShortType > copyRealRandomAccess()
		{
			return this;
		}

		@Override
		public void localize( final float[] position )
		{}

		@Override
		public void move( final float distance, final int d )
		{}

		@Override
		public void move( final double distance, final int d )
		{}

		@Override
		public void move( final RealLocalizable distance )
		{}

		@Override
		public void move( final float[] distance )
		{}

		@Override
		public void move( final double[] distance )
		{}

		@Override
		public void setPosition( final RealLocalizable position )
		{}

		@Override
		public void setPosition( final float[] position )
		{}

		@Override
		public void setPosition( final double[] position )
		{}

		@Override
		public void setPosition( final float position, final int d )
		{}

		@Override
		public void setPosition( final double position, final int d )
		{}

		@Override
		public void fwd( final int d )
		{}

		@Override
		public void bck( final int d )
		{}

		@Override
		public void move( final int distance, final int d )
		{}

		@Override
		public void move( final long distance, final int d )
		{}

		@Override
		public void move( final Localizable localizable )
		{}

		@Override
		public void move( final int[] distance )
		{}

		@Override
		public void move( final long[] distance )
		{}

		@Override
		public void setPosition( final Localizable localizable )
		{}

		@Override
		public void setPosition( final int[] position )
		{}

		@Override
		public void setPosition( final long[] position )
		{}

		@Override
		public void setPosition( final int position, final int d )
		{}

		@Override
		public void setPosition( final long position, final int d )
		{}

		@Override
		public void localize( final double[] position )
		{}

		@Override
		public float getFloatPosition( final int d )
		{
			return 0;
		}

		@Override
		public double getDoublePosition( final int d )
		{
			return 0;
		}

		@Override
		public int numDimensions()
		{
			return 3;
		}

		@Override
		public UnsignedShortType get()
		{
			return type;
		}

		@Override
		public Sampler< UnsignedShortType > copy()
		{
			return this;
		}
	};
}
