package org.mastodon.trackmate.ui.wizard;

import java.util.Locale;

import javax.swing.UIManager;

import org.mastodon.project.MamutProject;
import org.mastodon.project.MamutProjectIO;
import org.mastodon.revised.mamut.Mastodon;
import org.scijava.Context;

public class WizardPluginExample
{

	public static void main( final String[] args ) throws Exception
	{
		Locale.setDefault( Locale.US );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );

		final Mastodon mastodon = new Mastodon();
		new Context().inject( mastodon );
		mastodon.run();

//		final MamutProject project = new MamutProjectIO().load( "/Users/pietzsch/Desktop/Mastodon/testdata/MaMut_Parhyale_demo" );
		final MamutProject project = new MamutProjectIO().load( "../TrackMate3/samples/mamutproject/" );
		mastodon.openProject( project );
	}
}
