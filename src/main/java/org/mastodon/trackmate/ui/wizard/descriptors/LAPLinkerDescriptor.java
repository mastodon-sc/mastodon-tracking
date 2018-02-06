package org.mastodon.trackmate.ui.wizard.descriptors;

import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.mastodon.detection.DetectorKeys;
import org.mastodon.linking.LinkingUtils;
import org.mastodon.linking.mamut.SparseLAPLinkerMamut;
import org.mastodon.linking.mamut.SpotLinkerOp;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.TrackMate;
import org.mastodon.trackmate.ui.wizard.WizardLogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin( type = SpotLinkerDescriptor.class, name = "LAP linker configuration descriptor" )
public class LAPLinkerDescriptor extends SpotLinkerDescriptor
{

	public static final String IDENTIFIER = "Configure LAP linker";

	@Parameter
	private WizardLogService log;

	private Settings settings;

	private Model model;

	public LAPLinkerDescriptor()
	{
		this.panelIdentifier = IDENTIFIER;
	}

	@Override
	public String getNextPanelDescriptorIdentifier()
	{
		return ExecuteLinkingDescriptor.IDENTIFIER;
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
		// Panel settings.
		final LAPLinkerPanel panel = ( LAPLinkerPanel ) targetPanel;
		final Map< String, Object > ls = panel.configPanel.getSettings();

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
		log.info( "Configured LAP linker with the following parameters:\n" );
		log.info( LAPLinkerConfigPanel.echoSettingsMap( ls, units ) );
		log.info( String.format( "  - min time-point: %d\n", ( int ) ls.get( KEY_MIN_TIMEPOINT ) ) );
		log.info( String.format( "  - max time-point: %d\n", ( int ) ls.get( KEY_MAX_TIMEPOINT ) ) );
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
			final String units = ( null != setupID && null != settings.values.getSpimData() )
					? settings.values.getSpimData().getSequenceDescription()
							.getViewSetups().get( setupID ).getVoxelSize().unit()
					: "pixels";
			this.configPanel = new LAPLinkerConfigPanel( "LAP linker.", units, model.getFeatureModel(), Spot.class );
			jScrollPaneMain.setViewportView( configPanel );
		}
	}
}
