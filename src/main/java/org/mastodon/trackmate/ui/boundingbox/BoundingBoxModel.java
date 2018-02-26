package org.mastodon.trackmate.ui.boundingbox;

import org.mastodon.trackmate.ui.boundingbox.BoundingBoxOverlay.BoundingBoxOverlaySource;

import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;

public interface BoundingBoxModel extends BoundingBoxOverlaySource
{
	@Override
	public RealInterval getInterval();

	@Override
	public void getTransform( final AffineTransform3D transform );

	public void setInterval( RealInterval interval );
}
