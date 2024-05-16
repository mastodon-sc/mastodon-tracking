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
