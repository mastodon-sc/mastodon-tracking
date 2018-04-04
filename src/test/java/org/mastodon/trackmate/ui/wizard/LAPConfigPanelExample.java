package org.mastodon.trackmate.ui.wizard;

import static org.mastodon.linking.LinkerKeys.KEY_LINKING_FEATURE_PENALTIES;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mastodon.linking.FeatureKey;
import org.mastodon.linking.ProgressListeners;
import org.mastodon.model.DefaultSelectionModel;
import org.mastodon.revised.mamut.feature.DefaultMamutFeatureComputerService;
import org.mastodon.revised.model.feature.FeatureComputer;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.TrackMate;
import org.mastodon.trackmate.ui.wizard.descriptors.LAPLinkerDescriptor;
import org.scijava.Context;

import net.imagej.ops.OpService;

public class LAPConfigPanelExample
{


	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		Locale.setDefault( Locale.ROOT );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		final Context context = new Context( OpService.class );

		final Model model = new Model();
		final Settings settings = new Settings();
		@SuppressWarnings( "unchecked" )
		final Map< FeatureKey, Double > linkingPenalties = ( Map< FeatureKey, Double > ) settings.values.getLinkerSettings().get( KEY_LINKING_FEATURE_PENALTIES );
		linkingPenalties.put( new FeatureKey( "Spot N links" ), 36.9 );

		final TrackMate trackmate = new TrackMate( settings, model, new DefaultSelectionModel<>( model.getGraph(), model.getGraphIdBimap() ) );
		context.inject( trackmate );

		final DefaultMamutFeatureComputerService featureComputerService = new DefaultMamutFeatureComputerService();
		context.inject( featureComputerService );
		featureComputerService.initialize();

		final Set< FeatureComputer< Model > > featureComputers = new HashSet<>( featureComputerService.getFeatureComputers() );
		System.out.println( "Found the following computers: " + featureComputers );
		featureComputerService.compute( model, model.getFeatureModel(), featureComputers, ProgressListeners.defaultLogger() );

		final JFrame frame = new JFrame( "LAP config panel test" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.setSize( 400, 600 );
		final LAPLinkerDescriptor descriptor = new LAPLinkerDescriptor();
		context.inject( descriptor );
		descriptor.setTrackMate( trackmate );
		frame.getContentPane().add( descriptor.targetPanel );
		descriptor.aboutToDisplayPanel();
		frame.setVisible( true );
		descriptor.displayingPanel();
	}

}
