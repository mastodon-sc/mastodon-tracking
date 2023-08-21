/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2022 Tobias Pietzsch, Jean-Yves Tinevez
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
package org.mastodon.tracking.detection;

import static org.mastodon.tracking.detection.DetectorKeys.KEY_ADD_BEHAVIOR;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_RADIUS;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_THRESHOLD;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.io.ProjectCreator;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.views.bdv.MamutViewBdv;
import org.mastodon.tracking.mamut.detection.AdvancedDoGDetectorMamut;
import org.mastodon.tracking.mamut.detection.DoGDetectorMamut;
import org.mastodon.tracking.mamut.detection.MamutDetectionCreatorFactories.DetectionBehavior;
import org.mastodon.tracking.mamut.trackmate.Settings;
import org.mastodon.tracking.mamut.trackmate.TrackMate;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;

public class TestDriveAddBehaviorFactories
{

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException, SpimDataException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		Locale.setDefault( Locale.ROOT );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		final String bdvFile = "samples/mamutproject/datasethdf5.xml";

		final Context context = new Context();
		final ProjectModel appModel = ProjectCreator.createProjectFromBdvFile( new File( bdvFile ), context );

		final Map< String, Object > ds = DetectionUtil.getDefaultDetectorSettingsMap();
		ds.put( KEY_THRESHOLD, 20. );
		ds.put( KEY_RADIUS, 5. );

		final Settings settings = new Settings()
				.sources( appModel.getSharedBdvData().getSources() )
				.detector( DoGDetectorMamut.class )
				.detectorSettings( ds );

		final Model model = appModel.getModel();
		final TrackMate trackmate = new TrackMate( settings, model, appModel.getSelectionModel() );
		trackmate.setContext( appModel.getContext() );

		if ( !trackmate.execDetection() )
		{
			System.err.println( "Detection failed:\n" + trackmate.getErrorMessage() );
			return;
		}
		System.out.println( "First detection completed. Found " + model.getGraph().vertices().size() + " spots." );
		appModel.getWindowManager().createView( MamutViewBdv.class );

		ds.put( KEY_ADD_BEHAVIOR, DetectionBehavior.DONTADD.name() );
		ds.put( KEY_RADIUS, 12. );
		ds.put( KEY_THRESHOLD, 300. );
		settings.detector( AdvancedDoGDetectorMamut.class );
		System.out.println( "Second detection round." );

		if ( !trackmate.execDetection() )
		{
			System.err.println( "Detection failed:\n" + trackmate.getErrorMessage() );
			return;
		}
		System.out.println( "Second detection completed. Found " + model.getGraph().vertices().size() + " spots." );
	}
}
