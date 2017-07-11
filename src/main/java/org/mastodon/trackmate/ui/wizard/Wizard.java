package org.mastodon.trackmate.ui.wizard;

import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mastodon.revised.mamut.MainWindow;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.TrackMate;
import org.mastodon.trackmate.ui.wizard.descriptors.BoundingBoxDescriptor;
import org.mastodon.trackmate.ui.wizard.descriptors.Descriptor1;
import org.mastodon.trackmate.ui.wizard.descriptors.Descriptor2;
import org.mastodon.trackmate.ui.wizard.descriptors.Descriptor3;
import org.mastodon.trackmate.ui.wizard.descriptors.SetupIdDecriptor;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimDataException;
import net.imagej.ImageJ;

public class Wizard
{
	private final TrackMate trackmate;

	private final JFrame frame;

	private final WizardController controller;

	private final WindowManager windowManager;

	public Wizard( final TrackMate trackmate, final WindowManager windowManager )
	{
		this.trackmate = trackmate;
		this.windowManager = windowManager;
		this.frame = new JFrame( "Mastodon Trackmate" );
		final WizardModel model = new WizardModel();
		this.controller = new WizardController( model );

		createDescriptors();

		frame.getContentPane().add( controller.getWizardPanel() );
	}

	private void createDescriptors()
	{
		final SetupIdDecriptor setupIdDecriptor = new SetupIdDecriptor( trackmate.getSettings() );
		controller.registerWizardPanel( setupIdDecriptor );

		final BoundingBoxDescriptor boundingBoxDescriptor = new BoundingBoxDescriptor( trackmate.getSettings(), windowManager );
		controller.registerWizardPanel( boundingBoxDescriptor );

		controller.registerWizardPanel( new Descriptor1() );
		controller.registerWizardPanel( new Descriptor2() );
		controller.registerWizardPanel( new Descriptor3() );
		controller.init( setupIdDecriptor );
	}

	public void show()
	{
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.setSize( 300, 500 );
		frame.setVisible( true );
	}

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		Locale.setDefault( Locale.ROOT );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		final ImageJ ij = new ImageJ();
		ij.launch( args );


		/*
		 * Load SpimData
		 */
//		final String bdvFile = "samples/datasethdf5.xml";
		final String bdvFile = "/Users/tinevez/Projects/JYTinevez/MaMuT/MaMuT_demo_dataset/MaMuT_Parhyale_demo.xml";

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

		final Model model = new Model();
		final TrackMate trackmate = new TrackMate( settings, model );
		trackmate.setContext( ij.context() );

		final MainWindow mw = new MainWindow( model, spimData, bdvFile, MainWindow.getInputTriggerConfig() );
		mw.setVisible( true );
		new Wizard( new TrackMate( settings, model ), mw.getWindowManager() ).show();
	}

}
