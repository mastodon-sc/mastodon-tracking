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

import org.mastodon.mamut.WindowManager;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.project.MamutProject;
import org.mastodon.tracking.detection.DetectionUtil;
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
		final Context context = new Context();
		final WindowManager windowManager = new WindowManager( context );

		final String bdvFile = "../TrackMate3/samples/mamutproject/datasethdf5.xml";
		final MamutProject project = new MamutProject( null, new File( bdvFile ) );
		windowManager.getProjectManager().open( project );

		final Map< String, Object > ds = DetectionUtil.getDefaultDetectorSettingsMap();
		ds.put( KEY_THRESHOLD, 20. );
		ds.put( KEY_RADIUS, 5. );

		final Settings settings = new Settings()
				.sources( windowManager.getAppModel().getSharedBdvData().getSources() )
				.detector( DoGDetectorMamut.class )
				.detectorSettings( ds );

		final Model model = windowManager.getAppModel().getModel();
		final TrackMate trackmate = new TrackMate( settings, model, windowManager.getAppModel().getSelectionModel() );
		trackmate.setContext( windowManager.getContext() );

		if ( !trackmate.execDetection() )
		{
			System.err.println( "Detection failed:\n" + trackmate.getErrorMessage() );
			return;
		}
		System.out.println( "First detection completed. Found " + model.getGraph().vertices().size() + " spots." );
		windowManager.createBigDataViewer();

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
