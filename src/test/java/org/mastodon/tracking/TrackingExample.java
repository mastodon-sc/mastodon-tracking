/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2021 Tobias Pietzsch, Jean-Yves Tinevez
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
package org.mastodon.tracking;

import static org.mastodon.tracking.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_RADIUS;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_THRESHOLD;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.mastodon.mamut.WindowManager;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.project.MamutProject;
import org.mastodon.model.SelectionModel;
import org.mastodon.tracking.detection.DetectionUtil;
import org.mastodon.tracking.linking.LinkingUtils;
import org.mastodon.tracking.mamut.detection.DoGDetectorMamut;
import org.mastodon.tracking.mamut.detection.SpotDetectorOp;
import org.mastodon.tracking.mamut.linking.SimpleSparseLAPLinkerMamut;
import org.mastodon.tracking.mamut.linking.SpotLinkerOp;
import org.mastodon.tracking.mamut.trackmate.Settings;
import org.mastodon.tracking.mamut.trackmate.TrackMate;
import org.scijava.Context;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.SpimDataException;

public class TrackingExample
{
	public static void main( final String[] args ) throws SpimDataException, IOException
	{

		/*
		 * Open image data.
		 */

		final String bdvFile = "../TrackMate3/samples/mamutproject/datasethdf5.xml";

		/*
		 * Create empty model and window manager.
		 */

		final WindowManager windowManager = new WindowManager( new Context() );
		windowManager.getProjectManager().open( new MamutProject( null, new File( bdvFile ) ) );
		final Model model = windowManager.getAppModel().getModel();
		final SelectionModel< Spot, Link > selectionModel = windowManager.getAppModel().getSelectionModel();

		/*
		 * Detection parameters.
		 */

		final List< SourceAndConverter< ? > > sources = windowManager.getAppModel().getSharedBdvData().getSources();
		final Class< ? extends SpotDetectorOp > detector = DoGDetectorMamut.class;
		final Map< String, Object > detectorSettings = DetectionUtil.getDefaultDetectorSettingsMap();
		detectorSettings.put( KEY_RADIUS, Double.valueOf( 7. ) );
		detectorSettings.put( KEY_THRESHOLD, Double.valueOf( 100. ) );
		detectorSettings.put( KEY_MIN_TIMEPOINT, 0 );
		detectorSettings.put( KEY_MAX_TIMEPOINT, 30 );

		/*
		 * Linking parameters.
		 */

		final Class< ? extends SpotLinkerOp > linker = SimpleSparseLAPLinkerMamut.class;
		final Map< String, Object > linkerSettings = LinkingUtils.getDefaultLAPSettingsMap();
		linkerSettings.put( KEY_MIN_TIMEPOINT, 0 );
		linkerSettings.put( KEY_MAX_TIMEPOINT, 30 );

		/*
		 * Tracking settings.
		 */

		final Settings settings = new Settings()
				.sources( sources )
				.detector( detector )
				.detectorSettings( detectorSettings )
				.linker( linker )
				.linkerSettings( linkerSettings );

		final long start = System.currentTimeMillis();
		final TrackMate trackmate = new TrackMate( settings, model, selectionModel );
		trackmate.setContext( windowManager.getContext() );
		if ( !trackmate.execDetection() || !trackmate.execParticleLinking() )
		{
			System.out.println( "Tracking failed: " + trackmate.getErrorMessage() );
			return;
		}
		final long end = System.currentTimeMillis();
		System.out.println( String.format( "Tracking successful. Done in %.1f s.", ( end - start ) / 1000. ) );

		/*
		 * Show results.
		 */

		windowManager.createBigDataViewer();

	}
}
