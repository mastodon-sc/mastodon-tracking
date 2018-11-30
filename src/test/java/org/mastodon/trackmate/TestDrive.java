package org.mastodon.trackmate;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mastodon.project.MamutProject;
import org.mastodon.revised.mamut.MainWindow;
import org.mastodon.revised.mamut.WindowManager;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;

public class TestDrive
{

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException, SpimDataException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		Locale.setDefault( Locale.ROOT );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		final Context context = new Context();

		final String bdvFile = "../TrackMate3/samples/mamutproject/datasethdf5.xml";
//		final String bdvFile = "/Users/Jean-Yves/Desktop/MaMuT_demo_dataset/MaMuT_Parhyale_demo.xml";
//		final String bdvFile = "/Users/pietzsch/Desktop/data/MAMUT/MaMuT_demo_dataset/MaMuT_Parhyale_demo.xml";
//		final String bdvFile = "/Users/tinevez/Projects/JYTinevez/MaMuT/MaMuT_demo_dataset/MaMuT_Parhyale_demo.xml";
//		final String bdvFile = "/Users/tinevez/Projects/JYTinevez/MaMuT/MaMuT_demo_dataset/MaMuT_Parhyale_demo.xml";
//		final String projectFile = "../TrackMate3/samples/mamutproject";

		final WindowManager windowManager = new WindowManager( context );
		final MainWindow mw = new MainWindow( windowManager );

//		final MamutProject project = new MamutProjectIO().load( projectFile );
		final MamutProject project = new MamutProject( null, new File( bdvFile ) );
		windowManager.getProjectManager().open( project );

		mw.setVisible( true );
	}
}
