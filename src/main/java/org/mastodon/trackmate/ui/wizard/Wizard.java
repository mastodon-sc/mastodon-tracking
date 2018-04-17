package org.mastodon.trackmate.ui.wizard;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;

import org.mastodon.revised.mamut.MainWindow;
import org.mastodon.revised.mamut.MamutProject;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.TrackMate;
import org.scijava.Context;
import org.scijava.plugin.Parameter;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimDataException;

public class Wizard
{

	@Parameter
	private Context context;

	private final JFrame frame;

	private final WizardLogService wizardLogService;

	public Wizard( final StyledDocument log )
	{
		this.frame = new JFrame();
		this.wizardLogService = new WizardLogService( log );
	}

	public Wizard()
	{
		this( new DefaultStyledDocument() );
	}

	public WizardLogService getLogService()
	{
		return wizardLogService;
	}

	public void show( final WizardSequence sequence, final String title )
	{
		context.inject( wizardLogService );
		final WizardController controller = new WizardController( sequence, wizardLogService );
		frame.getContentPane().removeAll();
		frame.getContentPane().add( controller.getWizardPanel() );
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		frame.setSize( 300, 600 );
		frame.setTitle( title );
		controller.init();
		frame.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
		frame.addWindowListener( controller );
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

		final Wizard wizard = new Wizard();
		windowManager.getContext().inject( wizard );
		final TrackMate trackmate = new TrackMate( settings, windowManager.getAppModel().getModel(), windowManager.getAppModel().getSelectionModel() );
		context.inject( trackmate );
		final DetectionSequence sequence = new DetectionSequence( trackmate, windowManager, wizard.getLogService() );
		wizard.show( sequence, "TrackMate detection" );
	}

}
