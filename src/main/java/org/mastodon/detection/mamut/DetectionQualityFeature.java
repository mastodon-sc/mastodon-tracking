package org.mastodon.detection.mamut;

import org.mastodon.RefPool;
import org.mastodon.feature.Dimension;
import org.mastodon.feature.DoubleScalarFeature;
import org.mastodon.feature.FeatureModel;
import org.mastodon.revised.model.mamut.Spot;

public class DetectionQualityFeature extends DoubleScalarFeature< Spot >
{

	public static final String KEY = "Detection quality";

	private static final String HELP_STRING = "Report the detection quality.";

	public DetectionQualityFeature( final RefPool< Spot > pool )
	{
		super( KEY, HELP_STRING, Dimension.QUALITY, Dimension.QUALITY_UNITS, pool );
	}

	/**
	 * Retrieves an instance of {@link DetectionQualityFeature} in the specified
	 * feature model. If the feature model does not contain such a feature,
	 * creates one based on the specified {@link RefPool}, declares it in the
	 * feature model and returns it.
	 *
	 * @param featureModel
	 *            the feature model to query.
	 * @param pool
	 *            the pool to base the new feature on.
	 * @return a {@link DetectionQualityFeature} instance.
	 */
	public static final DetectionQualityFeature getOrRegister( final FeatureModel featureModel, final RefPool< Spot > pool )
	{
		final DetectionQualityFeature feature = new DetectionQualityFeature( pool );
		final DetectionQualityFeature retrieved = ( DetectionQualityFeature ) featureModel.getFeature( feature.getSpec() );
		if ( null == retrieved )
		{
			featureModel.declareFeature( feature );
			return feature;
		}
		return retrieved;
	}
}
