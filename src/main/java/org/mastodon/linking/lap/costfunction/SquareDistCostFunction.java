package org.mastodon.linking.lap.costfunction;

import org.mastodon.linking.lap.LAPUtils;

import net.imglib2.RealLocalizable;

/**
 * A cost function that returns cost equal to the square distance. Suited to
 * Brownian motion.
 *
 * @author Jean-Yves Tinevez - 2014
 *
 */
public class SquareDistCostFunction< K extends RealLocalizable > implements CostFunction< K, K >
{

	@Override
	public double linkingCost( final K source, final K target )
	{
		return LAPUtils.squareDistance( source, target );
	}

}
