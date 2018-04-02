package org.mastodon.trackmate.ui.wizard.descriptors;

import static org.mastodon.detection.DetectorKeys.KEY_ADD_BEHAVIOR;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_RADIUS;
import static org.mastodon.detection.DetectorKeys.KEY_SETUP_ID;
import static org.mastodon.detection.DetectorKeys.KEY_THRESHOLD;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartPanel;
import org.mastodon.detection.DetectionUtil;
import org.mastodon.detection.DetectorKeys;
import org.mastodon.detection.DoGDetectorOp;
import org.mastodon.detection.mamut.AdvancedDoGDetectorMamut;
import org.mastodon.detection.mamut.MamutDetectionCreatorFactories;
import org.mastodon.detection.mamut.MamutDetectionCreatorFactories.DetectionBehavior;
import org.mastodon.detection.mamut.SpotDetectorOp;
import org.mastodon.revised.bdv.SharedBigDataViewerData;
import org.mastodon.revised.bdv.ViewerFrameMamut;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.TrackMate;
import org.mastodon.trackmate.ui.wizard.Wizard;
import org.mastodon.trackmate.ui.wizard.util.WizardUtils;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.util.Affine3DHelpers;
import mpicbg.spim.data.generic.sequence.BasicMultiResolutionImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imagej.ops.OpService;
import net.imglib2.realtransform.AffineTransform3D;

@Plugin( type = SpotDetectorDescriptor.class, name = "Advanced DoG detector configuration descriptor" )
public class AdvancedDoGDetectorDescriptor extends SpotDetectorDescriptor
{

	public static final String IDENTIFIER = "Configure Advanced DoG detector";

	private static final Icon PREVIEW_ICON = new ImageIcon( Wizard.class.getResource( "led-icon-eye-green.png" ) );

	private static final NumberFormat FORMAT = new DecimalFormat( "0.0" );

	@Parameter
	private LogService log;

	@Parameter
	private OpService ops;

	private Settings settings;

	private WindowManager windowManager;

	private ChartPanel chartPanel;

	private ViewerFrameMamut viewFrame;

	private final Model localModel;

	public AdvancedDoGDetectorDescriptor()
	{
		this.panelIdentifier = IDENTIFIER;
		this.targetPanel = new AdvancedDoGDetectorPanel();
		/*
		 * Use a separate model for the preview. We do not want to touch the
		 * existing model.
		 */
		this.localModel = new Model();
	}

	/**
	 * Update the settings field of this descriptor with the values set on the
	 * GUI.
	 */
	private void grabSettings()
	{
		if ( null == settings )
			return;

		final AdvancedDoGDetectorPanel panel = ( AdvancedDoGDetectorPanel ) targetPanel;
		final Map< String, Object > detectorSettings = settings.values.getDetectorSettings();
		detectorSettings.put( KEY_RADIUS, ( ( Number ) panel.diameter.getValue() ).doubleValue() / 2. );
		detectorSettings.put( KEY_THRESHOLD, ( ( Number ) panel.threshold.getValue() ).doubleValue() );
		detectorSettings.put( KEY_ADD_BEHAVIOR, ( ( DetectionBehavior ) panel.behaviorCB.getSelectedItem() ).name() );
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	@Override
	public Collection< Class< ? extends SpotDetectorOp > > getTargetClasses()
	{
		final Collection b = Collections.unmodifiableCollection( Arrays.asList( new Class[] {
				AdvancedDoGDetectorMamut.class
		} ) );
		final Collection< Class< ? extends SpotDetectorOp > > a = b;
		return a;
	}

	@Override
	public void aboutToHidePanel()
	{
		if ( null != viewFrame )
			viewFrame.dispose();
		viewFrame = null;

		grabSettings();
		final Integer setupID = ( Integer ) settings.values.getDetectorSettings().get( DetectorKeys.KEY_SETUP_ID );
		final String units = ( null != setupID && null != settings.values.getSpimData() )
				? settings.values.getSpimData().getSequenceDescription()
						.getViewSetups().get( setupID ).getVoxelSize().unit()
				: "pixels";

		final SpimDataMinimal spimData = settings.values.getSpimData();
		final double radius = ( double ) settings.values.getDetectorSettings().get( KEY_RADIUS );
		final double minSizePixel = DoGDetectorOp.MIN_SPOT_PIXEL_SIZE / 2.;
		final int timepoint = ( int ) settings.values.getDetectorSettings().get( KEY_MIN_TIMEPOINT );
		final int level = DetectionUtil.determineOptimalResolutionLevel( spimData, radius, minSizePixel, timepoint, setupID );
		final AffineTransform3D mipmapTransform = DetectionUtil.getMipmapTransform( spimData, timepoint, setupID, level );
		final AffineTransform3D transform = DetectionUtil.getTransform( spimData, timepoint, setupID, level );

		final double sx = Affine3DHelpers.extractScale( mipmapTransform, 0 );
		final double sy = Affine3DHelpers.extractScale( mipmapTransform, 1 );
		final double sz = Affine3DHelpers.extractScale( mipmapTransform, 2 );

		final double px = Affine3DHelpers.extractScale( transform, 0 );
		final double py = Affine3DHelpers.extractScale( transform, 1 );
		final double pz = Affine3DHelpers.extractScale( transform, 2 );

		final double rx = radius / px;
		final double ry = radius / py;
		final double rz = radius / pz;

		log.info( "Configured detector with parameters:\n" );
		log.info( String.format( "  - spot radius: %.1f %s\n", radius, units ) );
		log.info( String.format( "  - quality threshold: %.1f\n", ( double ) settings.values.getDetectorSettings().get( KEY_THRESHOLD ) ) );
		final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
		if ( seq.getImgLoader() instanceof BasicMultiResolutionImgLoader )
		{
			log.info( String.format( "  - will operate on resolution level %d (%.0f x %.0f x %.0f)\n", level, sx, sy, sz ) );
			log.info( String.format( "  - at this level, radius = %.1f %s corresponds to:\n", radius, units ) );
		}
		else
		{
			log.info( String.format( "  - equivalent radius = %.1f %s in pixels:\n", radius, units ) );
		}
		log.info( String.format( "      - %.1f pixels in X.\n", rx ) );
		log.info( String.format( "      - %.1f pixels in Y.\n", ry ) );
		log.info( String.format( "      - %.1f pixels in Z.\n", rz ) );

	}

	private void preview()
	{
		if ( null == windowManager )
			return;

		final SharedBigDataViewerData shared = windowManager.getAppModel().getSharedBdvData();
		viewFrame = WizardUtils.previewFrame( viewFrame, shared, localModel );
		final int currentTimepoint = viewFrame.getViewerPanel().getState().getCurrentTimepoint();

		final AdvancedDoGDetectorPanel panel = ( AdvancedDoGDetectorPanel ) targetPanel;
		panel.preview.setEnabled( false );
		new Thread( "DogDetectorPanel preview thread" )
		{
			@Override
			public void run()
			{
				try
				{
					grabSettings();
					final boolean ok = WizardUtils.executeDetectionPreview( localModel, settings, ops, currentTimepoint );
					if ( !ok )
						return;

					final int nSpots = WizardUtils.countSpotsIn( localModel, currentTimepoint );
					panel.lblInfo.setText( "Found " + nSpots + " spots in time-point " + currentTimepoint );
					plotQualityHistogram();
				}
				finally
				{
					panel.preview.setEnabled( true );
				}
			};
		}.start();

	}

	private void plotQualityHistogram()
	{
		final AdvancedDoGDetectorPanel panel = ( AdvancedDoGDetectorPanel ) targetPanel;
		if ( null != chartPanel )
		{
			panel.remove( chartPanel );
			panel.repaint();
		}
		this.chartPanel = WizardUtils.createQualityHistogram( localModel );
		if ( null == chartPanel )
			return;

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridy = 7;
		gbc.gridx = 0;
		gbc.gridwidth = 3;
		gbc.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets( 5, 5, 5, 5 );
		panel.add( chartPanel, gbc );
		panel.revalidate();
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		return DetectionUtil.getDefaultDetectorSettingsMap();
	}

	@Override
	public void setTrackMate( final TrackMate trackmate )
	{
		final AdvancedDoGDetectorPanel panel = ( AdvancedDoGDetectorPanel ) targetPanel;

		this.settings = trackmate.getSettings();
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

		DetectionBehavior detectionBehavior = DetectionBehavior.ADD;
		final String addBehavior = ( String ) detectorSettings.get( KEY_ADD_BEHAVIOR );
		if ( null != addBehavior )
		{
			try
			{
				detectionBehavior = MamutDetectionCreatorFactories.DetectionBehavior.valueOf( addBehavior );
			}
			catch ( final IllegalArgumentException e )
			{}
		}
		
		final int setupID = ( int ) settings.values.getDetectorSettings().get( KEY_SETUP_ID );
		final BasicViewSetup setup = settings.values.getSpimData().getSequenceDescription().getViewSetups().get( setupID );

		panel.diameter.setValue( diameter );
		panel.threshold.setValue( threshold );
		panel.lblDiameterUnit.setText( setup.getVoxelSize().unit() );
		panel.behaviorCB.setSelectedItem( detectionBehavior );
	}

	@Override
	public void setWindowManager( final WindowManager windowManager )
	{
		this.windowManager = windowManager;
	}

	private class AdvancedDoGDetectorPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		private final JFormattedTextField diameter;

		private final JFormattedTextField threshold;

		private final JLabel lblDiameterUnit;

		private final JButton preview;

		private final JLabel lblInfo;

		private final JComboBox< DetectionBehavior > behaviorCB;

		public AdvancedDoGDetectorPanel()
		{
			final GridBagLayout layout = new GridBagLayout();
			layout.columnWidths = new int[] { 80, 80, 40 };
			layout.columnWeights = new double[] { 0.2, 0.7, 0.1 };
			layout.rowHeights = new int[] { 26, 0, 0, 0, 0, 50, 26, 26 };
			layout.rowWeights = new double[] { 1., 0., 0., 0., 0., 0., 0., 1. };
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
			diameter.addFocusListener( new SelectAllOnFocus( diameter ) );
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
			threshold.addFocusListener( new SelectAllOnFocus( threshold ) );
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.CENTER;
			add( threshold, gbc );

			// Behavior.
			final JLabel lblAddBehavior = new JLabel( "Behavior:", JLabel.RIGHT );
			gbc.gridy++;
			gbc.gridx = 0;
			gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
			add( lblAddBehavior, gbc );

			this.behaviorCB = new JComboBox<>( MamutDetectionCreatorFactories.DetectionBehavior.values() );
			gbc.gridx++;
			gbc.anchor = GridBagConstraints.CENTER;
			add( behaviorCB, gbc );

			// Behavior info.
			final JLabel lblAddBehaviorInfo = new JLabel( "<html>" + ( ( DetectionBehavior ) behaviorCB.getSelectedItem() ).info() + "</html>" );
			gbc.gridy++;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.FIRST_LINE_START;
			gbc.gridx = 0;
			gbc.gridwidth = 3;
			add( lblAddBehaviorInfo, gbc );
			
			// Hook it to changes in the CB.
			behaviorCB.addItemListener( ( e ) -> lblAddBehaviorInfo.setText( "<html>" + ( ( DetectionBehavior ) behaviorCB.getSelectedItem() ).info() + "</html>" ) );

			// Preview button.
			preview = new JButton( "Preview", PREVIEW_ICON );
			preview.addActionListener( ( e ) -> preview() );
			gbc.gridy++;
			gbc.gridx = 0;
			gbc.gridwidth = 3;
			gbc.anchor = GridBagConstraints.EAST;
			gbc.fill = GridBagConstraints.NONE;
			add( preview, gbc );

			// Info text.
			this.lblInfo = new JLabel( "", JLabel.RIGHT );
			lblInfo.setFont( getFont().deriveFont( getFont().getSize2D() - 2f ) );
			gbc.gridwidth = 3;
			gbc.gridy++;
			gbc.gridx = 0;
			add( lblInfo, gbc );

			// Quality histogram place holder.
		}
	}

	private static class SelectAllOnFocus extends FocusAdapter
	{
		private final JFormattedTextField textField;

		public SelectAllOnFocus( final JFormattedTextField textField )
		{
			this.textField = textField;
		}

		@Override
		public void focusGained( final FocusEvent e )
		{
			SwingUtilities.invokeLater( () -> textField.selectAll() );
		}
	}
}
