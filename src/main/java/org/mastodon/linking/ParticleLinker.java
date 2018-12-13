package org.mastodon.linking;

import org.mastodon.HasErrorMessage;
import org.scijava.Cancelable;
import org.scijava.app.StatusService;
import org.scijava.log.Logger;

public interface ParticleLinker extends HasErrorMessage, Cancelable
{
	public void setLogger( Logger logger );

	public void setStatusService( StatusService statusService );
}
