package org.mastodon.tracking.linking.sequential.lap.costfunction;

import org.mastodon.tracking.linking.LinkingUtils;

import net.imglib2.RealLocalizable;

/**
 * A cost function that returns cost equal to the square distance. Suited to
 * Brownian motion.
 *
 * @author Jean-Yves Tinevez - 2014
 * @param <K> the type of objects to compute cost for.
 *
 */
public class SquareDistCostFunction< K extends RealLocalizable > implements CostFunction< K, K >
{

	@Override
	public double linkingCost( final K source, final K target )
	{
		return LinkingUtils.squareDistance( source, target );
	}

}
