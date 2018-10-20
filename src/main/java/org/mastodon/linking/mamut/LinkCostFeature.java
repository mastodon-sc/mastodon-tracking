package org.mastodon.linking.mamut;

import org.mastodon.RefPool;
import org.mastodon.feature.Dimension;
import org.mastodon.feature.DoubleScalarFeature;
import org.mastodon.feature.FeatureSpec;
import org.mastodon.revised.model.mamut.Link;

public class LinkCostFeature extends DoubleScalarFeature< Link >
{
	public static final String KEY = "Link cost";

	private static final String HELP_STRING = "Report the link cost.";

	public LinkCostFeature( final RefPool< Link > pool )
	{
		super( KEY, Dimension.COST_UNITS, pool );
	}

	public FeatureSpec< DoubleScalarFeature< Link >, Link > featureSpec()
	{
		return createFeatureSpec( HELP_STRING, Dimension.COST );
	}
}
