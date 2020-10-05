package org.mastodon.tracking.mamut.detection;

import java.util.List;

import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.tracking.detection.LoGDetectorOp;
import org.scijava.plugin.Plugin;

import bdv.viewer.SourceAndConverter;
import net.imglib2.algorithm.Benchmark;

/**
 * Laplacian of Gaussian detector for Mamut model.
 *
 * @author Jean-Yves Tinevez
 */
@Plugin( type = SpotDetectorOp.class, name = "LoG detector",
		description = "<html>"
				+ "This detector applies a LoG (Laplacian of Gaussian) filter "
				+ "to the image, with a sigma suited to the blob estimated size. "
				+ "<p>"
				+ "This detector exploits multi-resolution images to speed-up detection. "
				+ "Calculations are made in the Fourier space. The maxima in the "
				+ "filtered image are searched for, and a quadratic fitting scheme allows to do "
				+ "sub-pixel localization. "
				+ "</html>" )
public class LoGDetectorMamut extends AbstractSpotDetectorOp implements SpotDetectorOp, Benchmark
{
	@Override
	public void compute( final List< SourceAndConverter< ? > > sources, final ModelGraph graph )
	{
		exec( sources, graph, LoGDetectorOp.class );
	}
}
