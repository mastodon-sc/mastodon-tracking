package org.mastodon.trackmate.ui.wizard.descriptors;

import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.linking.LinkerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static org.mastodon.linking.LinkerKeys.KEY_KALMAN_SEARCH_RADIUS;
import static org.mastodon.linking.LinkerKeys.KEY_LINKING_MAX_DISTANCE;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mastodon.detection.DetectorKeys;
import org.mastodon.linking.mamut.KalmanLinkerMamut;
import org.mastodon.linking.mamut.SpotLinkerOp;
import org.mastodon.model.DefaultSelectionModel;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.TrackMate;
import org.mastodon.trackmate.ui.wizard.util.SelectOnFocusListener;
import org.scijava.Context;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.plugin.SciJavaPlugin;

@Plugin( type = SpotLinkerDescriptor.class, name = "Kalman linker configuration descriptor" )
public class KalmanLinkerDescriptor extends SpotLinkerDescriptor
{

	public static final String IDENTIFIER = "Canfigure Kalman linker";

	private static final Format FORMAT = new DecimalFormat( "0.0" );

	private static final Format INTEGER_FORMAT = new DecimalFormat( "0" );

	@Parameter
	private PluginService pluginService;

	private Settings settings;

	public KalmanLinkerDescriptor()
	{
		this.panelIdentifier = IDENTIFIER;
	}

	@Override
	public void aboutToDisplayPanel()
	{
		final Map< String, Object > ls = settings.values.getLinkerSettings();
		final KalmanLinkerPanel panel = ( KalmanLinkerPanel ) targetPanel;
		panel.searchRadius.setValue( ls.get( KEY_KALMAN_SEARCH_RADIUS ) );
		panel.initialSearchRadius.setValue( ls.get( KEY_LINKING_MAX_DISTANCE ) );
		panel.maxFrameGap.setValue( ls.get( KEY_GAP_CLOSING_MAX_FRAME_GAP ) );
	}

	@Override
	public void aboutToHidePanel()
	{
		// Panel settings.
		final Map< String, Object > ls = new HashMap<>();
		final KalmanLinkerPanel panel = ( KalmanLinkerPanel ) targetPanel;
		ls.put( KEY_KALMAN_SEARCH_RADIUS, ( ( Number ) panel.searchRadius.getValue() ).doubleValue() );
		ls.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, ( ( Number ) panel.maxFrameGap.getValue() ).intValue() );
		ls.put( KEY_LINKING_MAX_DISTANCE, ( ( Number ) panel.initialSearchRadius.getValue() ).doubleValue() );

		// Timepoints - copy from detection step.
		final Map< String, Object > ds = settings.values.getDetectorSettings();
		ls.put( KEY_MIN_TIMEPOINT, ds.get( KEY_MIN_TIMEPOINT ) );
		ls.put( KEY_MAX_TIMEPOINT, ds.get( KEY_MAX_TIMEPOINT ) );

		settings.linkerSettings( ls );

		final Integer setupID = ( Integer ) settings.values.getDetectorSettings().get( DetectorKeys.KEY_SETUP_ID );
		final String units = ( null != setupID && null != settings.values.getSpimData() )
				? settings.values.getSpimData().getSequenceDescription()
						.getViewSetups().get( setupID ).getVoxelSize().unit()
				: "pixels";
		log.log( "Configured Kalman linker with the following parameters:\n" );
		log.log( String.format( "  - initial search radius: %.1f %s\n", ( double ) ls.get( KEY_LINKING_MAX_DISTANCE ), units ) );
		log.log( String.format( "  - search radius: %.1f %s\n", ( double ) ls.get( KEY_KALMAN_SEARCH_RADIUS ), units ) );
		log.log( String.format( "  - max frame gap: %d\n", ( int ) ls.get( KEY_GAP_CLOSING_MAX_FRAME_GAP ) ) );
		log.log( String.format( "  - min time-point: %d\n", ( int ) ls.get( KEY_MIN_TIMEPOINT ) ) );
		log.log( String.format( "  - max time-point: %d\n", ( int ) ls.get( KEY_MAX_TIMEPOINT ) ) );
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	@Override
	public Collection< Class< ? extends SpotLinkerOp > > getTargetClasses()
	{
		final Collection b = Collections.unmodifiableCollection( Arrays.asList( new Class[] {
				KalmanLinkerMamut.class
		} ) );
		final Collection< Class< ? extends SpotLinkerOp > > a = b;
		return a;
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		return KalmanLinkerMamut.getDefaultSettingsMap();
	}

	@Override
	public void setTrackMate( final TrackMate trackmate )
	{
		this.settings = trackmate.getSettings();
		this.targetPanel = new KalmanLinkerPanel();
	}

	@Override
	public void setWindowManager( final WindowManager windowManager )
	{}

	private class KalmanLinkerPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		private final JFormattedTextField searchRadius;

		private final JFormattedTextField maxFrameGap;

		private final JFormattedTextField initialSearchRadius;

		public KalmanLinkerPanel()
		{
			final SelectOnFocusListener onFocusListener = new SelectOnFocusListener();
			final PluginInfo< SciJavaPlugin > pluginInfo = pluginService.getPlugin( KalmanLinkerMamut.class );

			final Integer setupID = ( Integer ) settings.values.getDetectorSettings().get( DetectorKeys.KEY_SETUP_ID );
			final String units = ( null != setupID && null != settings.values.getSpimData() )
					? settings.values.getSpimData().getSequenceDescription()
							.getViewSetups().get( setupID ).getVoxelSize().unit()
					: "pixels";

			final GridBagLayout layout = new GridBagLayout();
			layout.columnWidths = new int[] { 80, 80, 40 };
			layout.columnWeights = new double[] { 0.2, 0.7, 0.1 };
			layout.rowHeights = new int[] { 26, 0, 0, 0, 26, 26 };
			layout.rowWeights = new double[] { 1., 0., 0., 0., 0., 1. };
			setLayout( layout );

			final GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.gridwidth = 3;
			gbc.anchor = GridBagConstraints.BASELINE_LEADING;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets( 5, 5, 5, 5 );

			final JLabel title = new JLabel( "Configure " + pluginInfo.getName() + "." );
			title.setFont( getFont().deriveFont( Font.BOLD ) );
			add( title, gbc );

			// Search radius

			final JLabel lblSearchRadius = new JLabel( "Search radius:", JLabel.RIGHT );
			gbc.gridy++;
			gbc.gridwidth = 1;
			gbc.anchor = GridBagConstraints.LINE_END;
			add( lblSearchRadius, gbc );

			this.searchRadius = new JFormattedTextField( FORMAT );
			searchRadius.setHorizontalAlignment( JLabel.RIGHT );
			searchRadius.addFocusListener( onFocusListener );
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.CENTER;
			add( searchRadius, gbc );

			final JLabel lblSearchRadiusUnit = new JLabel( units );
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.LINE_END;
			add( lblSearchRadiusUnit, gbc );

			// Max frame gap.

			final JLabel lblMaxFrameGap = new JLabel( "Max frame gap:", JLabel.RIGHT );
			gbc.gridy++;
			gbc.gridx = 0;
			gbc.anchor = GridBagConstraints.LINE_END;
			add( lblMaxFrameGap, gbc );

			this.maxFrameGap = new JFormattedTextField( INTEGER_FORMAT );
			maxFrameGap.setHorizontalAlignment( JLabel.RIGHT );
			maxFrameGap.addFocusListener( onFocusListener );
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.CENTER;
			add( maxFrameGap, gbc );

			// Initial search radius

			final JLabel lblInitialSearchRadius = new JLabel( "Initial search radius:", JLabel.RIGHT );
			gbc.gridx = 0;
			gbc.gridy++;
			gbc.gridwidth = 1;
			gbc.anchor = GridBagConstraints.LINE_END;
			add( lblInitialSearchRadius, gbc );

			this.initialSearchRadius = new JFormattedTextField( FORMAT );
			initialSearchRadius.setHorizontalAlignment( JLabel.RIGHT );
			initialSearchRadius.addFocusListener( onFocusListener );
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.CENTER;
			add( initialSearchRadius, gbc );

			final JLabel lblInitialSearchRadiusUnit = new JLabel( units );
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.LINE_END;
			add( lblInitialSearchRadiusUnit, gbc );

			// Info text.
			final JLabel lblInfo = new JLabel( pluginInfo.getDescription(), JLabel.RIGHT );
			lblInfo.setFont( getFont().deriveFont( getFont().getSize2D() - 2f ) );
			gbc.gridwidth = 3;
			gbc.gridy++;
			gbc.gridx = 0;
			add( lblInfo, gbc );
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
		settings.linkerSettings( KalmanLinkerMamut.getDefaultSettingsMap() );

		final TrackMate trackmate = new TrackMate( settings, model, new DefaultSelectionModel<>( model.getGraph(), model.getGraphIdBimap() ) );
		context.inject( trackmate );

		final JFrame frame = new JFrame( "Kalman linker config panel test" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.setSize( 300, 600 );
		final KalmanLinkerDescriptor descriptor = new KalmanLinkerDescriptor();
		context.inject( descriptor );
		descriptor.setTrackMate( trackmate );
		frame.getContentPane().add( descriptor.targetPanel );
		descriptor.aboutToDisplayPanel();
		frame.setVisible( true );
		descriptor.displayingPanel();
	}
}
