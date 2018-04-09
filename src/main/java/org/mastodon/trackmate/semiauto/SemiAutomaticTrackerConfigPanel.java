package org.mastodon.trackmate.semiauto;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class SemiAutomaticTrackerConfigPanel extends JPanel
{
	public SemiAutomaticTrackerConfigPanel()
	{
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 240, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 0.0, 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE };
		setLayout( gridBagLayout );

		final JLabel lblTitle = new JLabel( "Configure semi-automatic tracker." );
		lblTitle.setFont( lblTitle.getFont().deriveFont( lblTitle.getFont().getSize() + 5f ) );
		final GridBagConstraints gbc_lblTitle = new GridBagConstraints();
		gbc_lblTitle.gridwidth = 3;
		gbc_lblTitle.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblTitle.gridx = 0;
		gbc_lblTitle.gridy = 0;
		add( lblTitle, gbc_lblTitle );

		final JLabel lblDetection = new JLabel( "Detection." );
		lblDetection.setFont( lblDetection.getFont().deriveFont( lblDetection.getFont().getStyle() | Font.BOLD ) );
		final GridBagConstraints gbc_lblDetection = new GridBagConstraints();
		gbc_lblDetection.gridwidth = 3;
		gbc_lblDetection.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblDetection.gridx = 0;
		gbc_lblDetection.gridy = 1;
		add( lblDetection, gbc_lblDetection );

		final JLabel lblSetupId = new JLabel( "Setup ID:" );
		final GridBagConstraints gbc_lblSetupId = new GridBagConstraints();
		gbc_lblSetupId.anchor = GridBagConstraints.EAST;
		gbc_lblSetupId.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblSetupId.gridx = 0;
		gbc_lblSetupId.gridy = 2;
		add( lblSetupId, gbc_lblSetupId );

		final JComboBox comboBox = new JComboBox();
		final GridBagConstraints gbc_comboBox = new GridBagConstraints();
		gbc_comboBox.gridwidth = 2;
		gbc_comboBox.insets = new Insets( 5, 5, 5, 5 );
		gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBox.gridx = 1;
		gbc_comboBox.gridy = 2;
		add( comboBox, gbc_comboBox );

		final JLabel lblQualityFactor = new JLabel( "Quality factor:" );
		final GridBagConstraints gbc_lblQualityFactor = new GridBagConstraints();
		gbc_lblQualityFactor.anchor = GridBagConstraints.EAST;
		gbc_lblQualityFactor.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblQualityFactor.gridx = 0;
		gbc_lblQualityFactor.gridy = 3;
		add( lblQualityFactor, gbc_lblQualityFactor );

		final JLabel lblDistanceFactor = new JLabel( "Distance factor:" );
		final GridBagConstraints gbc_lblDistanceFactor = new GridBagConstraints();
		gbc_lblDistanceFactor.anchor = GridBagConstraints.EAST;
		gbc_lblDistanceFactor.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblDistanceFactor.gridx = 0;
		gbc_lblDistanceFactor.gridy = 4;
		add( lblDistanceFactor, gbc_lblDistanceFactor );

		final JLabel lblNTimepoints = new JLabel( "N time-points:" );
		final GridBagConstraints gbc_lblNTimepoints = new GridBagConstraints();
		gbc_lblNTimepoints.anchor = GridBagConstraints.EAST;
		gbc_lblNTimepoints.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblNTimepoints.gridx = 0;
		gbc_lblNTimepoints.gridy = 5;
		add( lblNTimepoints, gbc_lblNTimepoints );

		final JLabel lblTrackingDirection = new JLabel( "Tracking direction." );
		lblTrackingDirection.setFont( lblTrackingDirection.getFont().deriveFont( lblTrackingDirection.getFont().getStyle() | Font.BOLD ) );
		final GridBagConstraints gbc_lblTrackingDirection = new GridBagConstraints();
		gbc_lblTrackingDirection.gridwidth = 3;
		gbc_lblTrackingDirection.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblTrackingDirection.gridx = 0;
		gbc_lblTrackingDirection.gridy = 6;
		add( lblTrackingDirection, gbc_lblTrackingDirection );

		final JPanel panelTrackingDirection = new JPanel();
		final GridBagConstraints gbc_panelTrackingDirection = new GridBagConstraints();
		gbc_panelTrackingDirection.insets = new Insets( 5, 5, 5, 5 );
		gbc_panelTrackingDirection.gridwidth = 3;
		gbc_panelTrackingDirection.fill = GridBagConstraints.VERTICAL;
		gbc_panelTrackingDirection.gridx = 0;
		gbc_panelTrackingDirection.gridy = 7;
		add( panelTrackingDirection, gbc_panelTrackingDirection );

		final JRadioButton rdbtnForward = new JRadioButton( "Forward in time." );
		panelTrackingDirection.add( rdbtnForward );

		final JRadioButton rdbtnBackward = new JRadioButton( "Backward in time." );
		panelTrackingDirection.add( rdbtnBackward );

		final JLabel lblDealWithExisting = new JLabel( "Existing links." );
		lblDealWithExisting.setFont( lblDealWithExisting.getFont().deriveFont( lblDealWithExisting.getFont().getStyle() | Font.BOLD ) );
		final GridBagConstraints gbc_lblDealWithExisting = new GridBagConstraints();
		gbc_lblDealWithExisting.gridwidth = 3;
		gbc_lblDealWithExisting.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblDealWithExisting.gridx = 0;
		gbc_lblDealWithExisting.gridy = 8;
		add( lblDealWithExisting, gbc_lblDealWithExisting );

		final JLabel lblAllowLinkingIf = new JLabel( "Allow linking if target spot has links?" );
		final GridBagConstraints gbc_lblAllowLinkingIf = new GridBagConstraints();
		gbc_lblAllowLinkingIf.gridwidth = 3;
		gbc_lblAllowLinkingIf.anchor = GridBagConstraints.WEST;
		gbc_lblAllowLinkingIf.fill = GridBagConstraints.VERTICAL;
		gbc_lblAllowLinkingIf.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblAllowLinkingIf.gridx = 0;
		gbc_lblAllowLinkingIf.gridy = 9;
		add( lblAllowLinkingIf, gbc_lblAllowLinkingIf );

		final JCheckBox chckbxLinkIncoming = new JCheckBox( "Link even if target has incoming links." );
		final GridBagConstraints gbc_chckbxLinkIncoming = new GridBagConstraints();
		gbc_chckbxLinkIncoming.insets = new Insets( 5, 5, 5, 5 );
		gbc_chckbxLinkIncoming.anchor = GridBagConstraints.WEST;
		gbc_chckbxLinkIncoming.gridwidth = 2;
		gbc_chckbxLinkIncoming.gridx = 1;
		gbc_chckbxLinkIncoming.gridy = 10;
		add( chckbxLinkIncoming, gbc_chckbxLinkIncoming );

		final JCheckBox chckbxLinkOutgoing = new JCheckBox( "Link even if target has outgoing links." );
		final GridBagConstraints gbc_chckbxLinkOutgoing = new GridBagConstraints();
		gbc_chckbxLinkOutgoing.insets = new Insets( 5, 5, 5, 5 );
		gbc_chckbxLinkOutgoing.anchor = GridBagConstraints.WEST;
		gbc_chckbxLinkOutgoing.gridwidth = 2;
		gbc_chckbxLinkOutgoing.gridx = 1;
		gbc_chckbxLinkOutgoing.gridy = 11;
		add( chckbxLinkOutgoing, gbc_chckbxLinkOutgoing );

		final JLabel lblContinueTrackingIf = new JLabel( "Continue tracking if source and target spots are already linked?" );
		final GridBagConstraints gbc_lblContinueTrackingIf = new GridBagConstraints();
		gbc_lblContinueTrackingIf.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblContinueTrackingIf.anchor = GridBagConstraints.WEST;
		gbc_lblContinueTrackingIf.gridwidth = 3;
		gbc_lblContinueTrackingIf.gridx = 0;
		gbc_lblContinueTrackingIf.gridy = 12;
		add( lblContinueTrackingIf, gbc_lblContinueTrackingIf );

		final JCheckBox chckbxContinueTracking = new JCheckBox( "Continue tracking." );
		final GridBagConstraints gbc_chckbxContinueTracking = new GridBagConstraints();
		gbc_chckbxContinueTracking.insets = new Insets( 5, 5, 5, 5 );
		gbc_chckbxContinueTracking.anchor = GridBagConstraints.WEST;
		gbc_chckbxContinueTracking.gridwidth = 2;
		gbc_chckbxContinueTracking.gridx = 1;
		gbc_chckbxContinueTracking.gridy = 13;
		add( chckbxContinueTracking, gbc_chckbxContinueTracking );

		final JPanel panelButtons = new JPanel();
		final GridBagConstraints gbc_panelButtons = new GridBagConstraints();
		gbc_panelButtons.anchor = GridBagConstraints.SOUTH;
		gbc_panelButtons.gridwidth = 3;
		gbc_panelButtons.insets = new Insets( 5, 5, 5, 5 );
		gbc_panelButtons.fill = GridBagConstraints.HORIZONTAL;
		gbc_panelButtons.gridx = 0;
		gbc_panelButtons.gridy = 14;
		add( panelButtons, gbc_panelButtons );
		panelButtons.setLayout( new BoxLayout( panelButtons, BoxLayout.X_AXIS ) );

		final JButton btnDefaults = new JButton( "Defaults" );
		panelButtons.add( btnDefaults );

		final Component horizontalGlue = Box.createHorizontalGlue();
		panelButtons.add( horizontalGlue );

		final JButton btnCancel = new JButton( "Cancel" );
		panelButtons.add( btnCancel );

		final JButton btnApply = new JButton( "Apply" );
		panelButtons.add( btnApply );
	}

	private static final long serialVersionUID = 1L;

}
