/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2024 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.tracking.linking.sequential.lap.costfunction;

import java.util.Map;

import org.mastodon.feature.FeatureModel;
import org.mastodon.feature.FeatureProjection;
import org.mastodon.feature.FeatureProjectionKey;
import org.mastodon.tracking.linking.LinkingUtils;

import net.imglib2.RealLocalizable;

/**
 * A cost function that tempers a square distance cost by difference in feature
 * values. Feature penalties are used to penalize link costs when the difference
 * in feature value from the candidate source vertex and the candidate target
 * vertex are very different. A penalty is expressed as a user-specified weight
 * times a measure of the normalized difference of the feature values. The
 * resulting value is then used to alter the initial value of the link cost.
 * <p>
 * This cost is calculated as follow:
 * <ul>
 * <li>The distance between the two vertices <code>D</code> is calculated
 * <li>For each feature in the map, a penalty <code>p</code> is calculated as
 * <code>p = 3 × α × |f1-f2| / |f1+f2|</code>, where <code>α</code> is the
 * factor associated to the feature in the map. This expression is such that:
 * <ul>
 * <li>there is no penalty if the 2 feature values <code>f1</code> and
 * <code>f2</code> are the same;
 * <li>that, with a factor of 1, the penalty if 1 is one value is the double of
 * the other;
 * <li>the penalty is 2 if one is 5 times the other one.
 * </ul>
 * <li>All penalties are summed, to form <code>P = (1 + ∑ p )</code>
 * <li>The cost is set to the square of the product: <code>C = ( D × P )²</code>
 * </ul>
 * For instance: if 2 vertices differ by twice the value in a feature which is
 * in the penalty map with a factor of 1, they will <i>look</i> as if they were
 * twice as far.
 *
 * @author Jean-Yves Tinevez
 * @param <V>
 *            the type of the vertices to compute cost for.
 */
public class FeaturePenaltiesCostFunction< V extends RealLocalizable > implements CostFunction< V, V >
{
	private final Map< FeatureProjection< V >, Double > projections;

	public FeaturePenaltiesCostFunction( final Map< FeatureProjectionKey, Double > featurePenalties, final FeatureModel featureModel )
	{
		this.projections = LinkingUtils.penaltyToProjectionMap( featurePenalties, featureModel );
	}

	@Override
	public double linkingCost( final V source, final V target )
	{
		final double d2 = LinkingUtils.squareDistance( source, target );
		double penalty = 1.;
		for ( final FeatureProjection< V > projection : projections.keySet() )
		{
			final double ndiff = LinkingUtils.normalizeDiffCost( source, target, projection );
			if ( Double.isNaN( ndiff ) )
				continue;
			final double weight = projections.get( projection ).doubleValue();
			penalty += weight * 1.5 * ndiff;
		}
		return d2 * penalty * penalty;
	}
}
