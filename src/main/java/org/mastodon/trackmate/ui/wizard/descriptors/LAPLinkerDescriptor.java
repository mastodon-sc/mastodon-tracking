package org.mastodon.trackmate.ui.wizard.descriptors;

import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.linking.LinkerKeys.KEY_DO_LINK_SELECTION;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
import org.scijava.log.LogLevel;
import org.scijava.plugin.Plugin;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;

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
		final Map< String, Object > ls = panel.configPanel.getSettings( settings.values.getLinkerSettings() );
		settings.linkerSettings( ls );

		final Integer setupID = ( Integer ) settings.values.getDetectorSettings().get( DetectorKeys.KEY_SETUP_ID );
		final List< SourceAndConverter< ? > > sources = settings.values.getSources();
		final Source< ? > source = sources.get( setupID.intValue() ).getSpimSource();
		final String units = source.getVoxelDimensions().unit();
		logger.log( LogLevel.INFO, "Configured LAP linker with the following parameters:\n" );
		logger.log( LogLevel.INFO, LAPLinkerConfigPanel.echoSettingsMap( ls, units ) );
		logger.log( LogLevel.INFO, String.format( "  - target: %s\n", ( boolean ) ls.get( KEY_DO_LINK_SELECTION ) ? "selection only." : "all detections." ) );
		logger.log( LogLevel.INFO, String.format( "  - min time-point: %d\n", ( int ) ls.get( KEY_MIN_TIMEPOINT ) ) );
		logger.log( LogLevel.INFO, String.format( "  - max time-point: %d\n", ( int ) ls.get( KEY_MAX_TIMEPOINT ) ) );
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
			final List< SourceAndConverter< ? > > sources = settings.values.getSources();
			final Source< ? > source = sources.get( setupID.intValue() ).getSpimSource();
			final String units = source.getVoxelDimensions().unit();
			this.configPanel = new LAPLinkerConfigPanel( "LAP linker.", units, model.getFeatureModel(), Spot.class );
			jScrollPaneMain.setViewportView( configPanel );
		}
	}
}
