package org.mastodon.trackmate.semiauto.ui;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import org.mastodon.trackmate.ui.wizard.util.SetupIDComboBox;

import bdv.spimdata.SpimDataMinimal;
import bdv.tools.brightness.SliderPanel;
import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.BoundedValue;
import bdv.util.BoundedValueDouble;

public class SemiAutomaticTrackerConfigPanel extends JPanel
{

	private static final String QUALITY_FACTOR_TOOLTIP = "<html><p width=\"500\">"
			+ "Parameter that specifies the tolerance on quality for "
			+ "discovered spots. A target spot may be linked to the source spot only if "
			+ "the target spot has a quality higher than the source spot quality times "
			+ "this factor."
			+ "</p></html>";

	private static final String DISTANCE_FACTOR_TOOLTIP = "<html><p width=\"500\">"
			+ "Parameter that specifies the tolerance on distance for "
			+ "discovered spots. A target spot may be linked to the source spot only if "
			+ " they are not father than the source spot radius times this factor."
			+ "</p></html>";

	private static final String N_TIMEPOINTS_TOOLTIP = "<html><p width=\"500\">"
			+ "Parameter specifying how many time-points are processed at "
			+ "most, from the input source spot. "
			+ "</p></html>";

	private static final String ALLOW_LINKING_TO_EXISTING_TOOLTIP = "<html><p width=\"500\">"
			+ "Parameter specifying whether we allow linking to a spot "
			+ "already existing in the model. "
			+ "If the best candidate spot is found near a spot already existing in the "
			+ "model (within radius), semi-automatic tracking will stop, unless this "
			+ "parameter is checked. In that case the source spot might "
			+ "be linked to the pre-existing spot, depending on whether it has incoming "
			+ "or outgoing links already. "
			+ "</p></html>";

	private static final String ALLOW_LINKING_IF_INCOMING_TOOLTIP = "<html><p width=\"500\"> "
			+ "Parameter specifying whether we allow linking to spot already "
			+ "existing in the model if it has already incoming links. "
			+ "If this parameter is set to <code>true</code>, we allow linking to "
			+ "existing spots that already have incoming links (more than 0)."
			+ "</p></html>";

	private static final String ALLOW_LINKING_IF_OUTGOING_TOOLTIP = "<html><p width=\"500\"> "
			+ "Parameter specifying whether we allow linking to spot already "
			+ "existing in the model if it has already outgoing links. "
			+ "If this parameter is set to <code>true</code>, we allow linking to "
			+ "existing spots that already have outgoing links (more than 0)."
			+ "</p></html>";

	private static final String CONTINUE_LINKING_TOOLTIP = "<html><p width=\"500\"> "
			+ "Parameter that specifies whether we continue semi-automatic "
			+ "tracking even if a link already exists in the model between the source and the "
			+ "target spots. "
			+ "</p></html>";

	public SemiAutomaticTrackerConfigPanel( final SpimDataMinimal spimData )
	{
		final int nTimePoints = ( null == spimData )
				? 100
				: spimData.getSequenceDescription().getTimePoints().size();

		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, Double.MIN_VALUE };
		setLayout( gridBagLayout );

		final JLabel lblTitle = new JLabel( "Configure semi-automatic tracker." );
		lblTitle.setFont( lblTitle.getFont().deriveFont( lblTitle.getFont().getSize() + 5f ) );
		final GridBagConstraints gbc_lblTitle = new GridBagConstraints();
		gbc_lblTitle.gridwidth = 2;
		gbc_lblTitle.insets = new Insets( 5, 5, 5, 0 );
		gbc_lblTitle.gridx = 0;
		gbc_lblTitle.gridy = 0;
		add( lblTitle, gbc_lblTitle );

		final JLabel lblDetection = new JLabel( "Detection." );
		lblDetection.setFont( lblDetection.getFont().deriveFont( lblDetection.getFont().getStyle() | Font.BOLD ) );
		final GridBagConstraints gbc_lblDetection = new GridBagConstraints();
		gbc_lblDetection.gridwidth = 2;
		gbc_lblDetection.insets = new Insets( 5, 5, 5, 0 );
		gbc_lblDetection.gridx = 0;
		gbc_lblDetection.gridy = 1;
		add( lblDetection, gbc_lblDetection );

		final JLabel lblSetupId = new JLabel( "Setup ID:" );
		final GridBagConstraints gbc_lblSetupId = new GridBagConstraints();
		gbc_lblSetupId.anchor = GridBagConstraints.EAST;
		gbc_lblSetupId.insets = new Insets( 0, 5, 5, 5 );
		gbc_lblSetupId.gridx = 0;
		gbc_lblSetupId.gridy = 2;
		add( lblSetupId, gbc_lblSetupId );

		final SetupIDComboBox comboBox = new SetupIDComboBox( spimData );
		final GridBagConstraints gbc_comboBox = new GridBagConstraints();
		gbc_comboBox.insets = new Insets( 5, 5, 5, 5 );
		gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBox.gridx = 1;
		gbc_comboBox.gridy = 2;
		add( comboBox, gbc_comboBox );

		final JLabel lblQualityFactor = new JLabel( "Quality factor:" );
		lblQualityFactor.setToolTipText( QUALITY_FACTOR_TOOLTIP );
		final GridBagConstraints gbc_lblQualityFactor = new GridBagConstraints();
		gbc_lblQualityFactor.anchor = GridBagConstraints.EAST;
		gbc_lblQualityFactor.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblQualityFactor.gridx = 0;
		gbc_lblQualityFactor.gridy = 3;
		add( lblQualityFactor, gbc_lblQualityFactor );

		final BoundedValueDouble qualityFactor = new BoundedValueDouble( 0., 5., 1.2 );
		final SliderPanelDouble qualityFactorSlider = new SliderPanelDouble( "", qualityFactor, 0.2 );
		qualityFactorSlider.setToolTipText( QUALITY_FACTOR_TOOLTIP );
		qualityFactorSlider.setNumColummns( 3 );
		final GridBagConstraints gbc_lblQualityFactorSlider = new GridBagConstraints();
		gbc_lblQualityFactorSlider.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblQualityFactorSlider.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblQualityFactorSlider.gridx = 1;
		gbc_lblQualityFactorSlider.gridy = 3;
		add( qualityFactorSlider, gbc_lblQualityFactorSlider );

		final JLabel lblDistanceFactor = new JLabel( "Distance factor:" );
		lblDistanceFactor.setToolTipText( DISTANCE_FACTOR_TOOLTIP );
		final GridBagConstraints gbc_lblDistanceFactor = new GridBagConstraints();
		gbc_lblDistanceFactor.anchor = GridBagConstraints.EAST;
		gbc_lblDistanceFactor.insets = new Insets( 0, 5, 5, 5 );
		gbc_lblDistanceFactor.gridx = 0;
		gbc_lblDistanceFactor.gridy = 4;
		add( lblDistanceFactor, gbc_lblDistanceFactor );

		final BoundedValueDouble distanceFactor = new BoundedValueDouble( 0., 5., 1.2 );
		final SliderPanelDouble distanceFactorSlider = new SliderPanelDouble( "", distanceFactor, 0.2 );
		distanceFactorSlider.setNumColummns( 3 );
		distanceFactorSlider.setToolTipText( DISTANCE_FACTOR_TOOLTIP );
		final GridBagConstraints gbc_lblDistanceFactorSlider = new GridBagConstraints();
		gbc_lblDistanceFactorSlider.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblDistanceFactorSlider.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblDistanceFactorSlider.gridx = 1;
		gbc_lblDistanceFactorSlider.gridy = 4;
		add( distanceFactorSlider, gbc_lblDistanceFactorSlider );

		final JLabel lblNTimepoints = new JLabel( "N time-points:" );
		lblNTimepoints.setToolTipText( N_TIMEPOINTS_TOOLTIP );
		final GridBagConstraints gbc_lblNTimepoints = new GridBagConstraints();
		gbc_lblNTimepoints.anchor = GridBagConstraints.EAST;
		gbc_lblNTimepoints.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblNTimepoints.gridx = 0;
		gbc_lblNTimepoints.gridy = 5;
		add( lblNTimepoints, gbc_lblNTimepoints );

		final BoundedValue nTimepoints = new BoundedValue( 1, nTimePoints, 20 );
		final SliderPanel nTimepointsSlider = new SliderPanel( "", nTimepoints, 1 );
		nTimepointsSlider.setNumColummns( 3 );
		nTimepointsSlider.setToolTipText( N_TIMEPOINTS_TOOLTIP );
		final GridBagConstraints gbc_lblNTimepointsSlider = new GridBagConstraints();
		gbc_lblNTimepointsSlider.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNTimepointsSlider.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblNTimepointsSlider.gridx = 1;
		gbc_lblNTimepointsSlider.gridy = 5;
		add( nTimepointsSlider, gbc_lblNTimepointsSlider );

		final JLabel lblTrackingDirection = new JLabel( "Tracking direction." );
		lblTrackingDirection.setFont( lblTrackingDirection.getFont().deriveFont( lblTrackingDirection.getFont().getStyle() | Font.BOLD ) );
		final GridBagConstraints gbc_lblTrackingDirection = new GridBagConstraints();
		gbc_lblTrackingDirection.gridwidth = 2;
		gbc_lblTrackingDirection.insets = new Insets( 5, 5, 5, 0 );
		gbc_lblTrackingDirection.gridx = 0;
		gbc_lblTrackingDirection.gridy = 6;
		add( lblTrackingDirection, gbc_lblTrackingDirection );

		final JPanel panelTrackingDirection = new JPanel();
		final GridBagConstraints gbc_panelTrackingDirection = new GridBagConstraints();
		gbc_panelTrackingDirection.insets = new Insets( 5, 5, 5, 0 );
		gbc_panelTrackingDirection.gridwidth = 2;
		gbc_panelTrackingDirection.fill = GridBagConstraints.VERTICAL;
		gbc_panelTrackingDirection.gridx = 0;
		gbc_panelTrackingDirection.gridy = 7;
		add( panelTrackingDirection, gbc_panelTrackingDirection );

		final JRadioButton rdbtnForward = new JRadioButton( "Forward in time." );
		panelTrackingDirection.add( rdbtnForward );

		final JRadioButton rdbtnBackward = new JRadioButton( "Backward in time." );
		panelTrackingDirection.add( rdbtnBackward );

		final JLabel lblDealWithExisting = new JLabel( "Existing spots." );
		lblDealWithExisting.setFont( lblDealWithExisting.getFont().deriveFont( lblDealWithExisting.getFont().getStyle() | Font.BOLD ) );
		final GridBagConstraints gbc_lblDealWithExisting = new GridBagConstraints();
		gbc_lblDealWithExisting.gridwidth = 2;
		gbc_lblDealWithExisting.insets = new Insets( 5, 5, 5, 0 );
		gbc_lblDealWithExisting.gridx = 0;
		gbc_lblDealWithExisting.gridy = 8;
		add( lblDealWithExisting, gbc_lblDealWithExisting );

		final JCheckBox chckbxAllowLinkingIf = new JCheckBox( "Allow linking to an existing spot?" );
		chckbxAllowLinkingIf.setToolTipText( ALLOW_LINKING_TO_EXISTING_TOOLTIP );
		final GridBagConstraints gbc_lblAllowLinkingIf = new GridBagConstraints();
		gbc_lblAllowLinkingIf.gridwidth = 2;
		gbc_lblAllowLinkingIf.anchor = GridBagConstraints.WEST;
		gbc_lblAllowLinkingIf.fill = GridBagConstraints.VERTICAL;
		gbc_lblAllowLinkingIf.insets = new Insets( 5, 5, 5, 0 );
		gbc_lblAllowLinkingIf.gridx = 0;
		gbc_lblAllowLinkingIf.gridy = 9;
		add( chckbxAllowLinkingIf, gbc_lblAllowLinkingIf );

		final JCheckBox chckbxLinkIncoming = new JCheckBox( "Link even if target has incoming links." );
		chckbxLinkIncoming.setToolTipText( ALLOW_LINKING_IF_INCOMING_TOOLTIP );
		final GridBagConstraints gbc_chckbxLinkIncoming = new GridBagConstraints();
		gbc_chckbxLinkIncoming.insets = new Insets( 5, 5, 5, 0 );
		gbc_chckbxLinkIncoming.anchor = GridBagConstraints.WEST;
		gbc_chckbxLinkIncoming.gridx = 1;
		gbc_chckbxLinkIncoming.gridy = 10;
		add( chckbxLinkIncoming, gbc_chckbxLinkIncoming );

		final JCheckBox chckbxLinkOutgoing = new JCheckBox( "Link even if target has outgoing links." );
		chckbxLinkOutgoing.setToolTipText( ALLOW_LINKING_IF_OUTGOING_TOOLTIP );
		final GridBagConstraints gbc_chckbxLinkOutgoing = new GridBagConstraints();
		gbc_chckbxLinkOutgoing.insets = new Insets( 5, 5, 5, 0 );
		gbc_chckbxLinkOutgoing.anchor = GridBagConstraints.WEST;
		gbc_chckbxLinkOutgoing.gridx = 1;
		gbc_chckbxLinkOutgoing.gridy = 11;
		add( chckbxLinkOutgoing, gbc_chckbxLinkOutgoing );

		final JCheckBox chckbxContinueTracking = new JCheckBox( "<html>"
				+ "Continue tracking if source and target spots are already linked.</html>" );
		chckbxContinueTracking.setVerticalTextPosition( SwingConstants.TOP );
		chckbxContinueTracking.setVerticalAlignment( SwingConstants.TOP );
		chckbxContinueTracking.setToolTipText( CONTINUE_LINKING_TOOLTIP );
		final GridBagConstraints gbc_chckbxContinueTracking = new GridBagConstraints();
		gbc_chckbxContinueTracking.anchor = GridBagConstraints.NORTHWEST;
		gbc_chckbxContinueTracking.fill = GridBagConstraints.BOTH;
		gbc_chckbxContinueTracking.insets = new Insets( 5, 5, 5, 0 );
		gbc_chckbxContinueTracking.gridx = 1;
		gbc_chckbxContinueTracking.gridy = 12;
		add( chckbxContinueTracking, gbc_chckbxContinueTracking );

		chckbxAllowLinkingIf.addItemListener( ( e ) -> {
			chckbxLinkIncoming.setEnabled( chckbxAllowLinkingIf.isSelected() );
			chckbxLinkOutgoing.setEnabled( chckbxAllowLinkingIf.isSelected() );
			chckbxContinueTracking.setEnabled( chckbxAllowLinkingIf.isSelected() );
		} );

		final JPanel panelButtons = new JPanel();
		final GridBagConstraints gbc_panelButtons = new GridBagConstraints();
		gbc_panelButtons.anchor = GridBagConstraints.SOUTH;
		gbc_panelButtons.gridwidth = 2;
		gbc_panelButtons.insets = new Insets( 5, 5, 0, 0 );
		gbc_panelButtons.fill = GridBagConstraints.HORIZONTAL;
		gbc_panelButtons.gridx = 0;
		gbc_panelButtons.gridy = 13;
		add( panelButtons, gbc_panelButtons );
		panelButtons.setLayout( new BoxLayout( panelButtons, BoxLayout.X_AXIS ) );

		final JButton btnDefaults = new JButton( "Defaults" );
		panelButtons.add( btnDefaults );

		final Component horizontalGlue = Box.createHorizontalGlue();
		panelButtons.add( horizontalGlue );

		final JButton btnTrack = new JButton( "Track" );
		panelButtons.add( btnTrack );
	}

	private static final long serialVersionUID = 1L;

}
