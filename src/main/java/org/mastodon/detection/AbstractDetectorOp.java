package org.mastodon.detection;

import java.util.Map;

import org.mastodon.graph.Graph;
import org.mastodon.graph.Vertex;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.feature.Feature;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;

import bdv.spimdata.SpimDataMinimal;
import net.imagej.ops.special.inplace.AbstractBinaryInplace1Op;

public abstract class AbstractDetectorOp< V extends Vertex< ? > >
		extends AbstractBinaryInplace1Op< Graph< V, ? >, SpimDataMinimal >
		implements DetectorOp< V >
{

	@Parameter( type = ItemIO.INPUT )
	protected Map< String, Object > settings;

	/**
	 * The {@link VertexCreator} that will be used to create and add vertices to
	 * the graph.
	 */
	@Parameter( type = ItemIO.INPUT )
	protected VertexCreator< V > vertexCreator;

	@Parameter( type = ItemIO.OUTPUT )
	protected String errorMessage;

	@Parameter( type = ItemIO.OUTPUT )
	protected boolean ok;

	/**
	 * The quality feature provided by this detector.
	 */
	@Parameter( type = ItemIO.OUTPUT )
	protected Feature< V, DoublePropertyMap< V > > qualityFeature;

	@Override
	public Feature< V, DoublePropertyMap< V > > getQualityFeature()
	{
		return qualityFeature;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public boolean isSuccessful()
	{
		return ok;
	}

	// -- Cancelable methods --

	/** Reason for cancelation, or null if not canceled. */
	private String cancelReason;

	@Override
	public boolean isCanceled()
	{
		return cancelReason != null;
	}

	/** Cancels the command execution, with the given reason for doing so. */
	@Override
	public void cancel( final String reason )
	{
		cancelReason = reason == null ? "" : reason;
	}

	@Override
	public String getCancelReason()
	{
		return cancelReason;
	}

}
