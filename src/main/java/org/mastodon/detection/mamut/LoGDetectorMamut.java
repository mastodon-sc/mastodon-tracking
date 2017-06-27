package org.mastodon.detection.mamut;

import org.mastodon.detection.LoGDetectorOp;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.scijava.plugin.Plugin;

import bdv.spimdata.SpimDataMinimal;
import net.imglib2.algorithm.Benchmark;

/**
 * Laplacian of Gaussian detector for Mamut model.
 *
 * @author Jean-Yves Tinevez
 */
@Plugin( type = SpotDetectorOp.class )
public class LoGDetectorMamut extends AbstractSpotDetectorOp implements SpotDetectorOp, Benchmark
{
	@Override
	public void compute( final SpimDataMinimal spimData, final ModelGraph graph )
	{
		exec( spimData, graph, LoGDetectorOp.class );
	}
}
