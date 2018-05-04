package org.mastodon.detection.mamut;

import java.util.List;

import org.mastodon.detection.DoGDetectorOp;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import bdv.viewer.SourceAndConverter;
import net.imglib2.algorithm.Benchmark;

/**
 * Difference of Gaussian detector.
 *
 * @author Tobias Pietzsch
 * @author Jean-Yves Tinevez
 */
@Plugin( type = SpotDetectorOp.class, priority = Priority.HIGH, name = "DoG detector",
		description = "<html>"
				+ "This detector relies on an approximation of the LoG operator  "
				+ "by differences of gaussian (DoG). Computations are made in direct space."
				+ "<p>"
				+ "This detector exploits multi-resolution images to speed-up detection. "
				+ "It can do sub-pixel localization of spots using a quadratic fitting scheme."
				+ "</html>" )
public class DoGDetectorMamut extends AbstractSpotDetectorOp implements SpotDetectorOp, Benchmark
{

	@Override
	public void compute( final List< SourceAndConverter< ? > > sources, final ModelGraph graph )
	{
		exec( sources, graph, DoGDetectorOp.class );
	}

}
