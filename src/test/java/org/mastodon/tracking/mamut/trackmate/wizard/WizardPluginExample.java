package org.mastodon.tracking.mamut.trackmate.wizard;

import java.util.Locale;

import javax.swing.UIManager;

import org.mastodon.mamut.Mastodon;
import org.mastodon.mamut.project.MamutProject;
import org.mastodon.mamut.project.MamutProjectIO;
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
		final MamutProject project = new MamutProjectIO().load( "../mastodon/samples/drosophila_crop.mastodon" );
		mastodon.openProject( project );
	}
}
