package org.mastodon.tracking.kalman;

import org.mastodon.pool.ByteMappedElement;
import org.mastodon.pool.ByteMappedElementArray;
import org.mastodon.pool.Pool;
import org.mastodon.pool.PoolObjectLayout;
import org.mastodon.pool.SingleArrayMemPool;
import org.mastodon.pool.attributes.RealPointAttribute;

class PredictionPool extends Pool< Prediction, ByteMappedElement >
{
	static class PredictionLayout extends PoolObjectLayout
	{
		final DoubleArrayField position = doubleArrayField( 3 );
	}

	static final PredictionLayout layout = new PredictionLayout();

	final RealPointAttribute< Prediction > position;

	public PredictionPool( final int initialCapacity )
	{
		super(
				initialCapacity,
				layout,
				Prediction.class,
				SingleArrayMemPool.factory( ByteMappedElementArray.factory ) );
		position = new RealPointAttribute<>( layout.position, this );
	}

	@Override
	public Prediction create( final Prediction obj )
	{
		return super.create( obj );
	}

	public Prediction create()
	{
		return super.create( createRef() );
	}

	@Override
	public void delete( final Prediction obj )
	{
		super.delete( obj );
	}

	@Override
	protected Prediction createEmptyRef()
	{
		return new Prediction( this );
	};
}
