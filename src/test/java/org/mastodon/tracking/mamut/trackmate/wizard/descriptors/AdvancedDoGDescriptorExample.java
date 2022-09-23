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
