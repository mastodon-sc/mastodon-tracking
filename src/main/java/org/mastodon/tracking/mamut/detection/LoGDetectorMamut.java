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
package org.mastodon.tracking.mamut.detection;

import java.util.List;
import java.util.Map;

import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.tracking.detection.DetectionUtil;
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

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		return DetectionUtil.getDefaultDetectorSettingsMap();
	}
}
