package org.mastodon.linking.mamut;

import org.mastodon.RefPool;
import org.mastodon.feature.Dimension;
import org.mastodon.feature.DoubleScalarFeature;
import org.mastodon.feature.FeatureModel;
import org.mastodon.revised.model.mamut.Link;

public class LinkCostFeature extends DoubleScalarFeature< Link >
{
	public static final String KEY = "Link cost";

	private static final String HELP_STRING = "Report the link cost.";

	public LinkCostFeature( final RefPool< Link > pool )
	{
		super( KEY, HELP_STRING, Dimension.COST, Dimension.COST_UNITS, pool );
	}

	/**
	 * Retrieves an instance of {@link LinkCostFeature} in the specified
	 * feature model. If the feature model does not contain such a feature,
	 * creates one based on the specified {@link RefPool}, declares it in the
	 * feature model and returns it.
	 *
	 * @param featureModel
	 *            the feature model to query.
	 * @param pool
	 *            the pool to base the new feature on.
	 * @return a {@link LinkCostFeature} instance.
	 */
	public static final LinkCostFeature getOrRegister( final FeatureModel featureModel, final RefPool< Link > pool )
	{
		final LinkCostFeature feature = new LinkCostFeature( pool );
		final LinkCostFeature retrieved = ( LinkCostFeature ) featureModel.getFeature( feature.getSpec() );
		if ( null == retrieved )
		{
			featureModel.declareFeature( feature );
			return feature;
		}
		return retrieved;
	}
}
