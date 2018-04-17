package org.mastodon.detection;

import org.mastodon.HasErrorMessage;
import org.scijava.Cancelable;
import org.scijava.app.StatusService;
import org.scijava.log.Logger;

import bdv.spimdata.SpimDataMinimal;
import net.imagej.ops.special.inplace.BinaryInplace1OnlyOp;

public interface DetectorOp extends BinaryInplace1OnlyOp< DetectionCreatorFactory, SpimDataMinimal >, Cancelable, HasErrorMessage
{

	public void setLogger( Logger logger );

	public void setStatusService( StatusService statusService );
}
