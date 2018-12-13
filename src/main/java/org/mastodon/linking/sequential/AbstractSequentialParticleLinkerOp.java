package org.mastodon.linking.sequential;

import java.util.Comparator;
import java.util.Map;

import org.mastodon.collection.RefCollection;
import org.mastodon.feature.FeatureModel;
import org.mastodon.linking.EdgeCreator;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;

import net.imagej.ops.special.inplace.AbstractBinaryInplace1Op;
import net.imglib2.RealLocalizable;

public abstract class AbstractSequentialParticleLinkerOp< V extends  RealLocalizable >
	extends AbstractBinaryInplace1Op< EdgeCreator< V >, SpatioTemporalIndex< V > >
	implements SequentialParticleLinkerOp< V >
{

	@Parameter
	protected StatusService statusService;

	@Parameter(type = ItemIO.INPUT )
	protected Map< String, Object > settings;

	@Parameter(type = ItemIO.INPUT )
	protected FeatureModel featureModel;

	@Parameter(type = ItemIO.INPUT )
	protected Comparator< V > spotComparator;

	@Parameter( type = ItemIO.INPUT )
	protected RefCollection< V > refcol;

	@Parameter( required = false )
	protected Logger logger;

	@Parameter( type = ItemIO.OUTPUT )
	protected String errorMessage;

	@Parameter( type = ItemIO.OUTPUT )
	protected boolean ok;

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

	@Override
	public void setLogger( final Logger logger )
	{
		this.logger = logger;
	}

	@Override
	public void setStatusService( final StatusService statusService )
	{
		this.statusService = statusService;
	}
}
