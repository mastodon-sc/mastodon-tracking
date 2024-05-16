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
package org.mastodon.tracking.detection;

import java.util.List;
import java.util.Map;

import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;

import bdv.viewer.SourceAndConverter;
import net.imagej.ops.special.inplace.AbstractBinaryInplace1Op;

public abstract class AbstractDetectorOp
		extends AbstractBinaryInplace1Op< DetectionCreatorFactory, List< SourceAndConverter< ? > > >
		implements DetectorOp
{

	@Parameter( type = ItemIO.INPUT )
	protected Map< String, Object > settings;

	@Parameter( type = ItemIO.OUTPUT )
	protected String errorMessage;

	@Parameter( type = ItemIO.OUTPUT )
	protected boolean ok;

	@Parameter( required = false )
	protected Logger logger;

	@Parameter( required = false )
	protected StatusService statusService;

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
