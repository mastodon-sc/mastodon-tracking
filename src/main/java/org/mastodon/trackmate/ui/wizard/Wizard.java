package org.mastodon.trackmate.ui.wizard;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mastodon.revised.mamut.MainWindow;
import org.mastodon.revised.mamut.MamutProject;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.TrackMate;
import org.mastodon.trackmate.ui.wizard.descriptors.LogDescriptor;
import org.scijava.Context;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimDataException;

public class Wizard
{
	private final JFrame frame;

	private final WizardLogService logService;

	public Wizard( final Context context )
	{
		this.frame = new JFrame( "Mastodon Trackmate" );
		this.logService = context.getService( WizardLogService.class );
	}

	public WizardLogService getLogService()
	{
		return logService;
	}

	public void show( final WizardSequence sequence )
	{
		final WizardController controller = new WizardController( sequence, new LogDescriptor( logService.getPanel() ) );
		frame.getContentPane().removeAll();
		frame.getContentPane().add( controller.getWizardPanel() );
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		frame.setSize( 300, 600 );
		controller.init();
		frame.setVisible( true );
	}

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException, SpimDataException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		Locale.setDefault( Locale.ROOT );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		final Context context = new Context();

		/*
		 * Load SpimData
		 */
//		final String bdvFile = "../TrackMate3/samples/datasethdf5.xml";
//		final String bdvFile = "/Users/Jean-Yves/Desktop/MaMuT_demo_dataset/MaMuT_Parhyale_demo.xml";
//		final String bdvFile = "/Users/pietzsch/Desktop/data/MAMUT/MaMuT_demo_dataset/MaMuT_Parhyale_demo.xml";
		final String bdvFile = "/Users/tinevez/Projects/JYTinevez/MaMuT/MaMuT_demo_dataset/MaMuT_Parhyale_demo.xml";
//		final String bdvFile = "/Users/tinevez/Projects/JYTinevez/MaMuT/MaMuT_demo_dataset/MaMuT_Parhyale_demo.xml";

		SpimDataMinimal sd = null;
		try
		{
			sd = new XmlIoSpimDataMinimal().load( bdvFile );
		}
		catch ( final SpimDataException e )
		{
			e.printStackTrace();
			return;
		}
		final SpimDataMinimal spimData = sd;
		final Settings settings = new Settings()
				.spimData( spimData );

		final WindowManager windowManager = new WindowManager( context );
		final MainWindow mw = new MainWindow( windowManager );

		final MamutProject project = new MamutProject( null, new File( bdvFile ) );
		windowManager.getProjectManager().open( project );


		mw.setVisible( true );

		final Wizard wizard = new Wizard( windowManager.getContext() );
		final TrackMate trackmate = new TrackMate( settings, windowManager.getAppModel().getModel() );
		context.inject( trackmate );
		final DetectionSequence sequence = new DetectionSequence( trackmate, windowManager, wizard.getLogService().getPanel() );
		wizard.show( sequence );
	}

}
