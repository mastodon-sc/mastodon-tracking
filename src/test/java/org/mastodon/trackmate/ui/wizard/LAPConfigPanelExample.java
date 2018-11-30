package org.mastodon.trackmate.ui.wizard;

import static org.mastodon.linking.LinkerKeys.KEY_LINKING_FEATURE_PENALTIES;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mastodon.feature.Feature;
import org.mastodon.feature.FeatureProjectionKey;
import org.mastodon.feature.FeatureProjectionSpec;
import org.mastodon.feature.FeatureSpec;
import org.mastodon.feature.FeatureSpecsService;
import org.mastodon.mamut.feature.MamutFeatureComputerService;
import org.mastodon.model.DefaultSelectionModel;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.Spot;
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

		final Context context = new Context( OpService.class, MamutFeatureComputerService.class, FeatureSpecsService.class );

		final Model model = new Model();
		final Settings settings = new Settings();
		@SuppressWarnings( "unchecked" )
		final Map< FeatureProjectionKey, Double > linkingPenalties = ( Map< FeatureProjectionKey, Double > ) settings.values.getLinkerSettings().get( KEY_LINKING_FEATURE_PENALTIES );
		linkingPenalties.put( FeatureProjectionKey.key( new FeatureProjectionSpec( "Spot N links" ) ), 36.9 );

		final TrackMate trackmate = new TrackMate( settings, model, new DefaultSelectionModel<>( model.getGraph(), model.getGraphIdBimap() ) );
		context.inject( trackmate );

		final MamutFeatureComputerService computerService = context.getService( MamutFeatureComputerService.class );
		final FeatureSpecsService featureSpecsService = context.getService( FeatureSpecsService.class );
		final List< FeatureSpec< ?, Spot > > specs = featureSpecsService.getSpecs( Spot.class );

		System.out.println( "Found the following computers: " + specs );
		@SuppressWarnings( { "unchecked", "rawtypes" } )
		final Map< FeatureSpec< ?, ? >, Feature< ? > > map = computerService.compute( (List) specs );
		for ( final Feature< ? > feature : map.values() )
			model.getFeatureModel().declareFeature( feature );

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
