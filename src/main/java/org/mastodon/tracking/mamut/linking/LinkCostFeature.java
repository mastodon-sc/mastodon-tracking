/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2023 Tobias Pietzsch, Jean-Yves Tinevez
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
package org.mastodon.tracking.mamut.linking;

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
import org.mastodon.mamut.model.Link;
import org.mastodon.properties.DoublePropertyMap;
import org.scijava.plugin.Plugin;

public class LinkCostFeature extends DoubleScalarFeature< Link >
{
	public static final String KEY = "Link cost";

	private static final String HELP_STRING = "Report the link cost.";

	public static final Spec SPEC = new Spec();

	@Plugin(type = FeatureSpec.class)
	public static class Spec extends FeatureSpec< LinkCostFeature, Link >
	{
		public Spec()
		{
			super(
					KEY,
					HELP_STRING,
					LinkCostFeature.class,
					Link.class,
					Multiplicity.SINGLE,
					new FeatureProjectionSpec( KEY, Dimension.COST) );
		}
	}

	public LinkCostFeature( final RefPool< Link > pool )
	{
		super( KEY, Dimension.COST, Dimension.COST_UNITS, pool );
	}

	private LinkCostFeature( final DoublePropertyMap< Link > map )
	{
		super( KEY, Dimension.COST, Dimension.COST_UNITS, map );
	}

	@Override
	public FeatureSpec< LinkCostFeature, Link > getSpec()
	{
		return SPEC;
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

	@Plugin( type = FeatureSerializer.class )
	public static class LinkCostFeatureFeatureSerializer extends DoubleScalarFeatureSerializer< LinkCostFeature, Link >
	{


		@Override
		public LinkCostFeature deserialize( final FileIdToObjectMap< Link > idmap, final RefCollection< Link > pool, final ObjectInputStream ois ) throws IOException, ClassNotFoundException
		{
			final DeserializedStruct struct = read( idmap, pool, ois );
			return new LinkCostFeature( struct.map );
		}

		@Override
		public FeatureSpec< LinkCostFeature, Link > getFeatureSpec()
		{
			return SPEC;
		}
	}
}
