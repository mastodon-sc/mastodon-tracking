package org.mastodon.trackmate.ui.wizard.descriptors;

import static org.mastodon.linking.LinkerKeys.KEY_LINKING_FEATURE_PENALTIES;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mastodon.detection.DetectorKeys;
import org.mastodon.linking.LinkingUtils;
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

	@Override
	public void aboutToDisplayPanel()
	{
		final LAPLinkerPanel panel = ( LAPLinkerPanel ) targetPanel;
		panel.configPanel.echoSettings( settings.values.getLinkerSettings() );
	}

	@Override
	public void aboutToHidePanel()
	{
		final LAPLinkerPanel panel = ( LAPLinkerPanel ) targetPanel;
		settings.linkerSettings( panel.configPanel.getSettings() );
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
	public Map< String, Object > getDefaultSettings()
	{
		return LinkingUtils.getDefaultLAPSettingsMap();
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

		private final LAPLinkerConfigPanel configPanel;

		public LAPLinkerPanel()
		{
			setLayout( new BorderLayout() );
			setPreferredSize( new Dimension( 300, 500 ) );

			final JScrollPane jScrollPaneMain = new JScrollPane();
			this.add( jScrollPaneMain, BorderLayout.CENTER );
			jScrollPaneMain.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
			jScrollPaneMain.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
			jScrollPaneMain.getVerticalScrollBar().setUnitIncrement( 24 );

			final Integer setupID = ( Integer ) settings.values.getDetectorSettings().get( DetectorKeys.KEY_SETUP_ID );
			final String units = ( null != setupID )
					? settings.values.getSpimData().getSequenceDescription()
							.getViewSetups().get( setupID ).getVoxelSize().unit()
					: "pixels";
			this.configPanel = new LAPLinkerConfigPanel( "LAP linker.", units, model.getGraphFeatureModel() );
			jScrollPaneMain.setViewportView( configPanel );
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
		@SuppressWarnings( "unchecked" )
		final Map< String, Double > linkingPenalties = ( Map< String, Double > ) settings.values.getLinkerSettings().get( KEY_LINKING_FEATURE_PENALTIES );
		linkingPenalties.put( "Spot N links", 36.9 );

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
		descriptor.aboutToDisplayPanel();
		frame.setVisible( true );
		descriptor.displayingPanel();
	}
}
