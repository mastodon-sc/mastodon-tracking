package org.mastodon.trackmate;

import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_RADIUS;
import static org.mastodon.detection.DetectorKeys.KEY_THRESHOLD;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.mastodon.detection.DetectionUtil;
import org.mastodon.detection.mamut.DoGDetectorMamut;
import org.mastodon.detection.mamut.SpotDetectorOp;
import org.mastodon.linking.LinkingUtils;
import org.mastodon.linking.mamut.SimpleSparseLAPLinkerMamut;
import org.mastodon.linking.mamut.SpotLinkerOp;
import org.mastodon.model.SelectionModel;
import org.mastodon.project.MamutProject;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.Spot;
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
