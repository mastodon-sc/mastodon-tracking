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
package org.mastodon.tracking.mamut.trackmate.wizard;

import static org.mastodon.tracking.linking.LinkerKeys.KEY_LINKING_FEATURE_PENALTIES;

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
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.DefaultSelectionModel;
import org.mastodon.tracking.mamut.trackmate.Settings;
import org.mastodon.tracking.mamut.trackmate.TrackMate;
import org.mastodon.tracking.mamut.trackmate.wizard.descriptors.LAPLinkerDescriptor;
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
