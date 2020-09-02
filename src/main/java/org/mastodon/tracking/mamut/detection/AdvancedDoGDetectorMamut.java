package org.mastodon.tracking.mamut.detection;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;

/**
 * Difference of Gaussian detector with advanced configuration.
 *
 * @author Tobias Pietzsch
 * @author Jean-Yves Tinevez
 */
@Plugin( type = SpotDetectorOp.class, priority = Priority.NORMAL, name = "Advanced DoG detector",
		description = "<html>"
				+ "This detector is identifical to the DoG detector, but offers "
				+ "more configuration options.</html>" )
public class AdvancedDoGDetectorMamut extends DoGDetectorMamut
{}
