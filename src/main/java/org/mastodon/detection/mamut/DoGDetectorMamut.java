package org.mastodon.detection.mamut;

import org.mastodon.detection.DogDetectorOp;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import bdv.spimdata.SpimDataMinimal;
import net.imglib2.algorithm.Benchmark;

/**
 * Difference of Gaussian detector.
 *
 * @author Tobias Pietzsch
 * @author Jean-Yves Tinevez
 */
@Plugin( type = SpotDetectorOp.class, priority = Priority.HIGH_PRIORITY )
public class DoGDetectorMamut extends AbstractSpotDetectorOp implements SpotDetectorOp, Benchmark
{

	@Override
	public void compute( final SpimDataMinimal spimData, final ModelGraph graph )
	{
		exec( spimData, graph, DogDetectorOp.class );
	}

}
