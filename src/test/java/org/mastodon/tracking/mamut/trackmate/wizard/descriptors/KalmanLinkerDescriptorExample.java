package org.mastodon.tracking.mamut.trackmate.wizard.descriptors;

import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.UIManager;

import org.mastodon.mamut.model.Model;
import org.mastodon.model.DefaultSelectionModel;
import org.mastodon.tracking.mamut.linking.KalmanLinkerMamut;
import org.mastodon.tracking.mamut.trackmate.Settings;
import org.mastodon.tracking.mamut.trackmate.TrackMate;
import org.scijava.Context;

public class KalmanLinkerDescriptorExample
{

	public static void main( final String[] args ) throws Exception
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		Locale.setDefault( Locale.ROOT );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		try (Context context = new Context())
		{
			final Model model = new Model();
			final Settings settings = new Settings();
			settings.linkerSettings( KalmanLinkerMamut.getDefaultSettingsMap() );

			final TrackMate trackmate = new TrackMate( settings, model, new DefaultSelectionModel<>( model.getGraph(), model.getGraphIdBimap() ) );
			context.inject( trackmate );

			final JFrame frame = new JFrame( "Kalman linker config panel test" );
			frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
			frame.setSize( 300, 600 );
			final KalmanLinkerDescriptor descriptor = new KalmanLinkerDescriptor();
			context.inject( descriptor );
			descriptor.setTrackMate( trackmate );
			frame.getContentPane().add( descriptor.getPanelComponent() );
			descriptor.aboutToDisplayPanel();
			frame.setVisible( true );
			descriptor.displayingPanel();
		}
	}
}
