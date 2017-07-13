package org.mastodon.trackmate.ui.wizard.descriptors;

import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_RADIUS;
import static org.mastodon.detection.DetectorKeys.KEY_SETUP_ID;
import static org.mastodon.detection.DetectorKeys.KEY_THRESHOLD;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.detection.DetectorKeys;
import org.mastodon.detection.mamut.DoGDetectorMamut;
import org.mastodon.detection.mamut.LoGDetectorMamut;
import org.mastodon.detection.mamut.SpotDetectorOp;
import org.mastodon.revised.mamut.BdvManager.BdvWindow;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.TrackMate;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import bdv.spimdata.SpimDataMinimal;
import bdv.viewer.ViewerFrame;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imagej.ops.OpService;
import net.imagej.ops.special.hybrid.Hybrids;

@Plugin( type = SpotDetectorDescriptor.class )
public class DogDetectorDescriptor extends SpotDetectorDescriptor
{

	private static final NumberFormat FORMAT = new DecimalFormat( "0.0" );

	private static final String IDENTIFIER = "Configure DoG detector";

	private static final Icon PREVIEW_ICON = new ImageIcon( DogDetectorDescriptor.class.getResource( "../led-icon-eye-green.png" ) );

	@Parameter
	private LogService log;

	@Parameter
	private OpService ops;

	private Settings settings;

	private Model model;

	private WindowManager windowManager;

	private ViewerFrame viewerFrame;

	public DogDetectorDescriptor()
	{
		this.panelIdentifier = IDENTIFIER;
		this.targetPanel = new DogDetectorPanel();
	}

	private void preview()
	{
		grabSettings();
		final ModelGraph graph = model.getGraph();
		final SpimDataMinimal spimData = settings.values.getSpimData();
		if ( null == spimData )
		{
			log.error( "Cannot start detection: SpimData obect is null." );
			return;
		}

		/*
		 * Get or show a viewer.
		 */

		final int currentTimepoint;
		if ( null != windowManager )
		{
			if ( viewerFrame == null || !viewerFrame.isShowing() )
			{
				final List< BdvWindow > bdvWindows = windowManager.getMamutWindowModel().getBdvWindows();
				if ( bdvWindows == null || bdvWindows.isEmpty() )
					viewerFrame = windowManager.createBigDataViewer();
				else
					viewerFrame = bdvWindows.get( 0 ).getViewerFrame();
				viewerFrame.toFront();
			}
			currentTimepoint = viewerFrame.getViewerPanel().getState().getCurrentTimepoint();
		}
		else
		{
			currentTimepoint = spimData.getSequenceDescription().getTimePoints().getTimePointsOrdered().get( 0 ).getId();
		}

		final DogDetectorPanel panel = ( DogDetectorPanel ) targetPanel;
		panel.preview.setEnabled( false );
		new Thread( "DogDetectorPanel preview thread" )
		{
			@Override
			public void run()
			{
				try
				{
					/*
					 * Delete spots from current time-point if we have some.
					 */

					final SpatialIndex< Spot > spatialIndex = model.getSpatioTemporalIndex().getSpatialIndex( currentTimepoint );
					final RefList< Spot > toRemove = RefCollections.createRefList( graph.vertices() );
					for ( final Spot spot : spatialIndex )
						toRemove.add( spot );

					for ( final Spot spot : toRemove )
						graph.remove( spot );

					/*
					 * Tune settings for preview.
					 */

					final Class< ? extends SpotDetectorOp > cl = settings.values.getDetector();
					final Map< String, Object > detectorSettings = new HashMap<>( settings.values.getDetectorSettings() );
					detectorSettings.put( KEY_MIN_TIMEPOINT, currentTimepoint );
					detectorSettings.put( KEY_MAX_TIMEPOINT, currentTimepoint );

					/*
					 * Execute preview.
					 */

					final SpotDetectorOp detector = ( SpotDetectorOp ) Hybrids.unaryCF( ops, cl,
							graph, spimData,
							detectorSettings );
					log.info( "Detection with " + detector );
					detector.compute( spimData, graph );

					if ( !detector.wasSuccessful() )
					{
						log.error( "Detection failed:\n" + detector.getErrorMessage() );
						return;
					}

					model.getGraphFeatureModel().declareFeature( detector.getQualityFeature() );
					graph.notifyGraphChanged();

					int nSpots = 0;
					for ( @SuppressWarnings( "unused" ) final Spot spot : spatialIndex )
						nSpots++;

					log.info( "Found " + nSpots + " spots in time-point " + currentTimepoint ); // TODO
				}
				finally
				{
					panel.preview.setEnabled( true );
				}
			};
		}.start();

	}

	private void grabSettings()
	{
		if ( null == settings )
			return;

		final DogDetectorPanel panel = ( DogDetectorPanel ) targetPanel;
		final Map< String, Object > detectorSettings = settings.values.getDetectorSettings();
		detectorSettings.put( KEY_RADIUS, ( ( Number ) panel.diameter.getValue() ).doubleValue() / 2. );
		detectorSettings.put( KEY_THRESHOLD, ( ( Number ) panel.threshold.getValue() ).doubleValue() );
	}

	@Override
	public String getBackPanelDescriptorIdentifier()
	{
		return ChooseDetectorDescriptor.IDENTIFIER;
	}

	@Override
	public String getNextPanelDescriptorIdentifier()
	{
		return Descriptor1.ID;
	}

	@Override
	public Collection< Class< ? extends SpotDetectorOp > > getTargetClasses()
	{
		return Collections.unmodifiableCollection( Arrays.asList( new Class[] {
				DoGDetectorMamut.class,
				LoGDetectorMamut.class
		} ) );
	}

	@Override
	public void setTrackMate( final TrackMate trackmate )
	{
		final DogDetectorPanel panel = ( DogDetectorPanel ) targetPanel;

		this.settings = trackmate.getSettings();
		this.model = trackmate.getModel();
		panel.preview.setEnabled( model != null );
		if ( null == settings )
			return;

		final Map< String, Object > detectorSettings = settings.values.getDetectorSettings();

		final double diameter;
		final Object objRadius = detectorSettings.get( KEY_RADIUS );
		if ( null == objRadius )
			diameter = 2. * DetectorKeys.DEFAULT_RADIUS;
		else
			diameter = 2. * ( double ) objRadius;

		final double threshold;
		final Object objThreshold = detectorSettings.get( KEY_THRESHOLD );
		if ( null == objThreshold )
			threshold = DetectorKeys.DEFAULT_THRESHOLD;
		else
			threshold = ( double ) objThreshold;

		final int setupID = ( int ) settings.values.getDetectorSettings().get( KEY_SETUP_ID );
		final BasicViewSetup setup = settings.values.getSpimData().getSequenceDescription().getViewSetups().get( setupID );

		panel.diameter.setValue( diameter );
		panel.threshold.setValue( threshold );
		panel.lblDiameterUnit.setText( setup.getVoxelSize().unit() );
	}

	@Override
	public void setWindowManager( final WindowManager windowManager )
	{
		this.windowManager = windowManager;
	}

	private class DogDetectorPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		private JFormattedTextField diameter;

		private JFormattedTextField threshold;

		private JLabel lblDiameterUnit;

		private JButton preview;

		public DogDetectorPanel()
		{
			final GridBagLayout layout = new GridBagLayout();
			layout.columnWidths = new int[] { 80, 80, 40 };
			layout.columnWeights = new double[] { 0.2, 0.7, 0.1 };
			layout.rowHeights = new int[] { 26, 0, 0, 0, 26 };
			layout.rowWeights = new double[] { 1., 0., 0., 0., 1. };
			setLayout( layout );

			final GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.gridwidth = 3;
			gbc.anchor = GridBagConstraints.BASELINE_LEADING;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets( 5, 5, 5, 5 );

			final JLabel title = new JLabel( "Configure detector." );
			title.setFont( getFont().deriveFont( Font.BOLD ) );
			add( title, gbc );

			// Diameter.

			final JLabel lblDiameter = new JLabel( "Estimated diameter:", JLabel.RIGHT );
			gbc.gridy++;
			gbc.gridwidth = 1;
			gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
			add( lblDiameter, gbc );

			this.diameter = new JFormattedTextField( FORMAT );
			diameter.setHorizontalAlignment( JLabel.RIGHT );
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.CENTER;
			add( diameter, gbc );

			lblDiameterUnit = new JLabel();
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.LINE_END;
			add( lblDiameterUnit, gbc );

			// Threshold.

			final JLabel lblThreshold = new JLabel( "Quality threshold:", JLabel.RIGHT );
			gbc.gridy++;
			gbc.gridx = 0;
			gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
			add( lblThreshold, gbc );

			this.threshold = new JFormattedTextField( FORMAT );
			threshold.setHorizontalAlignment( JLabel.RIGHT );
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.CENTER;
			add( threshold, gbc );

			// Preview button.
			preview = new JButton( "Preview", PREVIEW_ICON );
			preview.addActionListener( ( e ) -> preview() );
			gbc.gridy++;
			gbc.gridx = 2;
			add( preview, gbc );
		}

	}
}
