package org.mastodon.trackmate.ui.wizard.descriptors;

import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.linking.LinkerKeys.DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR;
import static org.mastodon.linking.LinkerKeys.DEFAULT_BLOCKING_VALUE;
import static org.mastodon.linking.LinkerKeys.DEFAULT_CUTOFF_PERCENTILE;
import static org.mastodon.linking.LinkerKeys.DEFAULT_MERGING_MAX_DISTANCE;
import static org.mastodon.linking.LinkerKeys.DEFAULT_SPLITTING_MAX_DISTANCE;
import static org.mastodon.linking.LinkerKeys.KEY_ALLOW_GAP_CLOSING;
import static org.mastodon.linking.LinkerKeys.KEY_ALLOW_TRACK_MERGING;
import static org.mastodon.linking.LinkerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static org.mastodon.linking.LinkerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static org.mastodon.linking.LinkerKeys.KEY_BLOCKING_VALUE;
import static org.mastodon.linking.LinkerKeys.KEY_CUTOFF_PERCENTILE;
import static org.mastodon.linking.LinkerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static org.mastodon.linking.LinkerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static org.mastodon.linking.LinkerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static org.mastodon.linking.LinkerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static org.mastodon.linking.LinkerKeys.KEY_LINKING_MAX_DISTANCE;
import static org.mastodon.linking.LinkerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static org.mastodon.linking.LinkerKeys.KEY_MERGING_MAX_DISTANCE;
import static org.mastodon.linking.LinkerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static org.mastodon.linking.LinkerKeys.KEY_SPLITTING_MAX_DISTANCE;

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
import java.util.Map;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mastodon.detection.DetectorKeys;
import org.mastodon.linking.LinkingUtils;
import org.mastodon.linking.mamut.SimpleSparseLAPLinkerMamut;
import org.mastodon.linking.mamut.SpotLinkerOp;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.TrackMate;
import org.mastodon.trackmate.ui.wizard.util.SelectOnFocusListener;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.plugin.SciJavaPlugin;

@Plugin( type = SpotLinkerDescriptor.class, name = "Simple LAP linker configuration descriptor" )
public class SimpleLAPLinkerDescriptor extends SpotLinkerDescriptor
{

	public static final String IDENTIFIER = "Configure Simple LAP linker";

	private static final Format FORMAT = new DecimalFormat( "0.0" );

	private static final Format INTEGER_FORMAT = new DecimalFormat( "0" );

	@Parameter
	private PluginService pluginService;

	private Settings settings;

	public SimpleLAPLinkerDescriptor()
	{
		this.panelIdentifier = IDENTIFIER;
	}

	@Override
	public void aboutToDisplayPanel()
	{
		final SimpleLAPLinkerPanel panel = ( SimpleLAPLinkerPanel ) targetPanel;
		panel.echoSettings( settings.values.getLinkerSettings() );
	}

	@Override
	public void aboutToHidePanel()
	{
		final SimpleLAPLinkerPanel panel = ( SimpleLAPLinkerPanel ) targetPanel;
		final Map< String, Object > ls = panel.getSettings();
		settings.linkerSettings( ls );

		final Integer setupID = ( Integer ) settings.values.getDetectorSettings().get( DetectorKeys.KEY_SETUP_ID );
		final String units = ( null != setupID && null != settings.values.getSpimData() )
				? settings.values.getSpimData().getSequenceDescription()
						.getViewSetups().get( setupID ).getVoxelSize().unit()
				: "pixels";
		log.log( "Configured Simple LAP linker with the following parameters:\n" );
		log.log( String.format( "  - max linking distance: %.1f %s\n", ( double ) ls.get( KEY_LINKING_MAX_DISTANCE ), units ) );
		log.log( String.format( "  - max gap-closing distance: %.1f %s\n", ( double ) ls.get( KEY_GAP_CLOSING_MAX_DISTANCE ), units ) );
		log.log( String.format( "  - max frame gap: %d\n", ( int ) ls.get( KEY_GAP_CLOSING_MAX_FRAME_GAP ) ) );
		log.log( String.format( "  - min time-point: %d\n", ( int ) ls.get( KEY_MIN_TIMEPOINT ) ) );
		log.log( String.format( "  - max time-point: %d\n", ( int ) ls.get( KEY_MAX_TIMEPOINT ) ) );
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	@Override
	public Collection< Class< ? extends SpotLinkerOp > > getTargetClasses()
	{
		final Collection b = Collections.unmodifiableCollection( Arrays.asList( new Class[] {
				SimpleSparseLAPLinkerMamut.class
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
		this.targetPanel = new SimpleLAPLinkerPanel();
	}

	@Override
	public void setWindowManager( final WindowManager windowManager )
	{}

	private class SimpleLAPLinkerPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		private JFormattedTextField maxLinkingDistance;

		private JFormattedTextField maxGapClosingDistance;

		private JFormattedTextField maxFrameGap;

		public SimpleLAPLinkerPanel()
		{
			final SelectOnFocusListener onFocusListener = new SelectOnFocusListener();
			final PluginInfo< SciJavaPlugin > pluginInfo = pluginService.getPlugin( SimpleSparseLAPLinkerMamut.class );

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

			// Max linking distance.

			final JLabel lblMaxLinkingDistance = new JLabel( "Max linking distance:", JLabel.RIGHT );
			gbc.gridy++;
			gbc.gridwidth = 1;
			gbc.anchor = GridBagConstraints.LINE_END;
			add( lblMaxLinkingDistance, gbc );

			this.maxLinkingDistance = new JFormattedTextField( FORMAT );
			maxLinkingDistance.setHorizontalAlignment( JLabel.RIGHT );
			maxLinkingDistance.addFocusListener( onFocusListener );
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.CENTER;
			add( maxLinkingDistance, gbc );

			final JLabel lblMaxLinkingDistanceUnit = new JLabel( units );
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.LINE_END;
			add( lblMaxLinkingDistanceUnit, gbc );

			// Max gap-closing distance.

			final JLabel lblMaxGapClosingDistance = new JLabel( "Max gap-closing distance:", JLabel.RIGHT );
			gbc.gridx = 0;
			gbc.gridy++;
			gbc.gridwidth = 1;
			gbc.anchor = GridBagConstraints.LINE_END;
			add( lblMaxGapClosingDistance, gbc );

			this.maxGapClosingDistance = new JFormattedTextField( FORMAT );
			maxGapClosingDistance.setHorizontalAlignment( JLabel.RIGHT );
			maxGapClosingDistance.addFocusListener( onFocusListener );
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.CENTER;
			add( maxGapClosingDistance, gbc );

			final JLabel lblMaxGapClosingDistanceUnit = new JLabel( units );
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.LINE_END;
			add( lblMaxGapClosingDistanceUnit, gbc );

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

			// Info text.
			final JLabel lblInfo = new JLabel( pluginInfo.getDescription(), JLabel.RIGHT );
			lblInfo.setFont( getFont().deriveFont( getFont().getSize2D() - 2f ) );
			gbc.gridwidth = 3;
			gbc.gridy++;
			gbc.gridx = 0;
			add( lblInfo, gbc );
		}

		private Map< String, Object > getSettings()
		{
			final Map< String, Object > ls = new HashMap<>();

			// Panel settings.
			// Frame to frame linking
			ls.put( KEY_LINKING_MAX_DISTANCE, ( ( Number ) maxLinkingDistance.getValue() ).doubleValue() );
			ls.put( KEY_LINKING_FEATURE_PENALTIES, null );

			// Gap-closing.
			ls.put( KEY_ALLOW_GAP_CLOSING, true );
			ls.put( KEY_GAP_CLOSING_MAX_DISTANCE, ( ( Number ) maxGapClosingDistance.getValue() ).doubleValue() );
			ls.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, ( ( Number ) maxFrameGap.getValue() ).intValue() );
			ls.put( KEY_GAP_CLOSING_FEATURE_PENALTIES, null );

			// Track splitting.
			ls.put( KEY_ALLOW_TRACK_SPLITTING, false );
			ls.put( KEY_SPLITTING_MAX_DISTANCE, DEFAULT_SPLITTING_MAX_DISTANCE );
			ls.put( KEY_SPLITTING_FEATURE_PENALTIES, null );

			// Track merging.
			ls.put( KEY_ALLOW_TRACK_MERGING, false );
			ls.put( KEY_MERGING_MAX_DISTANCE, DEFAULT_MERGING_MAX_DISTANCE );
			ls.put( KEY_MERGING_FEATURE_PENALTIES, null );

			// Other - use defaults.
			ls.put( KEY_BLOCKING_VALUE, DEFAULT_BLOCKING_VALUE );
			ls.put( KEY_ALTERNATIVE_LINKING_COST_FACTOR, DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR );
			ls.put( KEY_CUTOFF_PERCENTILE, DEFAULT_CUTOFF_PERCENTILE );

			// Timepoints - copy from detection step.
			final Map< String, Object > ds = settings.values.getDetectorSettings();
			ls.put( KEY_MIN_TIMEPOINT, ds.get( KEY_MIN_TIMEPOINT ) );
			ls.put( KEY_MAX_TIMEPOINT, ds.get( KEY_MAX_TIMEPOINT ) );

			return ls;
		}

		private void echoSettings( final Map< String, Object > linkerSettings )
		{
			maxLinkingDistance.setValue( linkerSettings.get( KEY_LINKING_MAX_DISTANCE ) );
			maxFrameGap.setValue( linkerSettings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP ) );
			maxGapClosingDistance.setValue( linkerSettings.get( KEY_GAP_CLOSING_MAX_DISTANCE ) );
		}
	}
}
