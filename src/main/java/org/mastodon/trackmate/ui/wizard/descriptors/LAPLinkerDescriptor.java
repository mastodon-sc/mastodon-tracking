package org.mastodon.trackmate.ui.wizard.descriptors;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mastodon.linking.ProgressListeners;
import org.mastodon.linking.mamut.SparseLAPLinkerMamut;
import org.mastodon.linking.mamut.SpotLinkerOp;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.revised.mamut.feature.DefaultMamutFeatureComputerService;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.TrackMate;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

@Plugin( type = SpotLinkerDescriptor.class, name = "LAP linker configuration descriptor" )
public class LAPLinkerDescriptor extends SpotLinkerDescriptor
{

	public static final String IDENTIFIER = "Configure LAP linker";

	private Settings settings;

	private Model model;

	public LAPLinkerDescriptor()
	{
		this.panelIdentifier = IDENTIFIER;
	}

	@Override
	public String getNextPanelDescriptorIdentifier()
	{
		return Descriptor1.ID;
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	@Override
	public Collection< Class< ? extends SpotLinkerOp > > getTargetClasses()
	{
		final Collection b = Collections.unmodifiableCollection( Arrays.asList( new Class[] {
				SparseLAPLinkerMamut.class
		} ) );
		final Collection< Class< ? extends SpotLinkerOp > > a = b;
		return a;
	}

	@Override
	public void setTrackMate( final TrackMate trackmate )
	{
		this.settings = trackmate.getSettings();
		this.model = trackmate.getModel();
		this.targetPanel = new LAPLinkerPanel();
	}

	@Override
	public void setWindowManager( final WindowManager windowManager )
	{}

	private class LAPLinkerPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		public LAPLinkerPanel()
		{
			setLayout( new BorderLayout() );
			setPreferredSize( new Dimension( 300, 500 ) );

			final JScrollPane jScrollPaneMain = new JScrollPane();
			this.add( jScrollPaneMain, BorderLayout.CENTER );
			jScrollPaneMain.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
			jScrollPaneMain.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
			jScrollPaneMain.getVerticalScrollBar().setUnitIncrement( 24 );
			final LAPLinkerConfigPanel jPanelMain = new LAPLinkerConfigPanel( "LAP linker.", "TODO", model.getGraphFeatureModel() );
			jScrollPaneMain.setViewportView( jPanelMain );
		}
	}

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		Locale.setDefault( Locale.ROOT );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		final Context context = new org.scijava.Context();

		final Model model = new Model();
		final Settings settings = new Settings();
		final TrackMate trackmate = new TrackMate( settings, model );
		context.inject( trackmate );

		final DefaultMamutFeatureComputerService featureComputerService = new DefaultMamutFeatureComputerService();
		context.inject( featureComputerService );
		featureComputerService.initialize();

		System.out.println( "Found the following computers: " + featureComputerService.getAvailableVertexFeatureComputers() );
		featureComputerService.compute( model, featureComputerService.getAvailableVertexFeatureComputers(), ProgressListeners.defaultLogger() );

		final JFrame frame = new JFrame( "LAP config panel test" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.setSize( 300, 600 );
		final LAPLinkerDescriptor descriptor = new LAPLinkerDescriptor();
		context.inject( descriptor );
		descriptor.setTrackMate( trackmate );
		frame.getContentPane().add( descriptor.targetPanel );
		frame.setVisible( true );
	}
}
