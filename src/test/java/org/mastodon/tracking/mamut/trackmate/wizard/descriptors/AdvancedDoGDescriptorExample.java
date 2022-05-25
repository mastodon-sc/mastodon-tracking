package org.mastodon.tracking.mamut.trackmate.wizard.descriptors;

import java.util.Locale;
import java.util.Map;

import javax.swing.UIManager;

import org.mastodon.mamut.MainWindow;
import org.mastodon.mamut.WindowManager;
import org.mastodon.mamut.project.MamutProject;
import org.mastodon.mamut.project.MamutProjectIO;
import org.mastodon.tracking.detection.DetectionUtil;
import org.mastodon.tracking.detection.DetectorKeys;
import org.mastodon.tracking.mamut.detection.AdvancedDoGDetectorMamut;
import org.mastodon.tracking.mamut.trackmate.wizard.WizardDetectionPlugin;
import org.scijava.Context;

public class AdvancedDoGDescriptorExample
{

	public static void main( final String[] args ) throws Exception
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		Locale.setDefault( Locale.ROOT );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		try (Context context = new Context())
		{
			final MamutProject project = new MamutProjectIO().load( "../mastodon/samples/Celegans.mastodon" );

			final WindowManager wm = new WindowManager( context );
			final MainWindow mw = new MainWindow( wm );
			wm.getProjectManager().open( project );
			mw.setVisible( true );

			// Edit default detection settings.
			WizardDetectionPlugin.settings.detector( AdvancedDoGDetectorMamut.class );
			final Map< String, Object > ds = DetectionUtil.getDefaultDetectorSettingsMap();
			ds.put( DetectorKeys.KEY_THRESHOLD, 10. );
			WizardDetectionPlugin.settings.detectorSettings( ds );

			// Execute detection plugin.
			wm.getAppModel().getPlugins().getPluginActions().getActionMap()
					.get( "run spot detection wizard" ).actionPerformed( null );
		}
	}
}
