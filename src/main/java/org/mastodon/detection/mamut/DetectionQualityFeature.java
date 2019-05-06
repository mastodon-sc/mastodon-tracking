package org.mastodon.detection.mamut;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.mastodon.RefPool;
import org.mastodon.collection.RefCollection;
import org.mastodon.feature.Dimension;
import org.mastodon.feature.DoubleScalarFeature;
import org.mastodon.feature.DoubleScalarFeatureSerializer;
import org.mastodon.feature.FeatureModel;
import org.mastodon.feature.FeatureProjectionSpec;
import org.mastodon.feature.FeatureSpec;
import org.mastodon.feature.Multiplicity;
import org.mastodon.feature.io.FeatureSerializer;
import org.mastodon.io.FileIdToObjectMap;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.mamut.Spot;
import org.scijava.plugin.Plugin;

public class DetectionQualityFeature extends DoubleScalarFeature< Spot >
{

	public static final String KEY = "Detection quality";

	private static final String HELP_STRING = "Report the detection quality.";

	public static final Spec SPEC = new Spec();

	@Plugin(type = FeatureSpec.class)
	public static class Spec extends FeatureSpec< DetectionQualityFeature, Spot >
	{
		public Spec()
		{
			super(
					KEY,
					HELP_STRING,
					DetectionQualityFeature.class,
					Spot.class,
					Multiplicity.SINGLE,
					new FeatureProjectionSpec( KEY, Dimension.QUALITY) );
		}
	}

	public DetectionQualityFeature( final RefPool< Spot > pool )
	{
		super( KEY, Dimension.QUALITY, Dimension.QUALITY_UNITS, pool );
	}

	private DetectionQualityFeature( final DoublePropertyMap< Spot > map )
	{
		super( KEY, Dimension.QUALITY, Dimension.QUALITY_UNITS, map );
	}

	@Override
	public FeatureSpec< DetectionQualityFeature, Spot > getSpec()
	{
		return SPEC;
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

	@Plugin( type = FeatureSerializer.class )
	public static class DetectionQualityFeatureSerializer extends DoubleScalarFeatureSerializer< DetectionQualityFeature, Spot >
	{

		@Override
		public DetectionQualityFeature deserialize( final FileIdToObjectMap< Spot > idmap, final RefCollection< Spot > pool, final ObjectInputStream ois ) throws IOException, ClassNotFoundException
		{
			final DeserializedStruct struct = read( idmap, pool, ois );
			return new DetectionQualityFeature( struct.map );
		}

		@Override
		public FeatureSpec< DetectionQualityFeature, Spot > getFeatureSpec()
		{
			return SPEC;
		}
	}
}
