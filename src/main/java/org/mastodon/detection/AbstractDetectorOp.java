package org.mastodon.detection;

import java.util.Map;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;

import bdv.spimdata.SpimDataMinimal;
import net.imagej.ops.special.inplace.AbstractBinaryInplace1Op;

public abstract class AbstractDetectorOp
		extends AbstractBinaryInplace1Op< DetectionCreatorFactory, SpimDataMinimal >
		implements DetectorOp
{

	@Parameter( type = ItemIO.INPUT )
	protected Map< String, Object > settings;

	@Parameter( type = ItemIO.OUTPUT )
	protected String errorMessage;

	@Parameter( type = ItemIO.OUTPUT )
	protected boolean ok;

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
