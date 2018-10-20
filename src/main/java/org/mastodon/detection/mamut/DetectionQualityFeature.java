package org.mastodon.detection.mamut;

import org.mastodon.RefPool;
import org.mastodon.feature.Dimension;
import org.mastodon.feature.DoubleScalarFeature;
import org.mastodon.feature.FeatureSpec;
import org.mastodon.revised.model.mamut.Spot;

public class DetectionQualityFeature extends DoubleScalarFeature< Spot >
{

	public static final String KEY = "Detection quality";

	private static final String HELP_STRING = "Report the detection quality.";

	public DetectionQualityFeature( final RefPool< Spot > pool )
	{
		super( KEY, Dimension.QUALITY_UNITS, pool );
	}

	public FeatureSpec< DoubleScalarFeature< Spot >, Spot > featureSpec()
	{
		return createFeatureSpec( HELP_STRING, Dimension.QUALITY );
	}
}
