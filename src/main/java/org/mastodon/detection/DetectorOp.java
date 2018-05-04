package org.mastodon.detection;

import java.util.List;

import org.mastodon.HasErrorMessage;
import org.scijava.Cancelable;
import org.scijava.app.StatusService;
import org.scijava.log.Logger;

import bdv.viewer.SourceAndConverter;
import net.imagej.ops.special.inplace.BinaryInplace1OnlyOp;

public interface DetectorOp extends BinaryInplace1OnlyOp< DetectionCreatorFactory, List< SourceAndConverter< ? > > >, Cancelable, HasErrorMessage
{

	public void setLogger( Logger logger );

	public void setStatusService( StatusService statusService );
}
