/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2023 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.tracking.mamut.trackmate.semiauto.ui;

import static org.mastodon.app.ui.StyleElements.doubleElement;
import static org.mastodon.app.ui.StyleElements.intElement;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.mastodon.app.ui.GroupLocksPanel;
import org.mastodon.grouping.GroupHandle;
import org.mastodon.tracking.mamut.trackmate.semiauto.ui.SemiAutomaticTrackerSettings.UpdateListener;
import org.mastodon.ui.util.SetupIDComboBox;
import org.mastodon.views.bdv.SharedBigDataViewerData;

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
			+ " they are not farther than the source spot radius times this factor."
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

	private static final String DETECT_SPOT_TOOLTIP = "<html><p width=\"500\"> "
			+ "Parameter that specifies whether we will perform spot "
			+ "detection. If <code>false</code>, the semi-automatic tracker will stop if "
			+ "no existing spots cannot be found as target for linking. If "
			+ "<code>true</code>, the detection process will be run on the neighborhood "
			+ "to create spots to link to from the image data. "
			+ "</p></html>";

	/**
	 * The cancel button.
	 */
	final JButton btnCancel;

	/**
	 * The track button.
	 */
	final JButton btnTrack;

	public SemiAutomaticTrackerConfigPanel(
			final SharedBigDataViewerData data,
			final SemiAutomaticTrackerSettings editedSettings,
			final GroupHandle groupHandle )
	{
		final int nTimePoints = ( null == data )
				? 100
				: data.getSpimData().getSequenceDescription().getTimePoints().size();

		final GridBagLayout gbl = new GridBagLayout();
		gbl.columnWidths = new int[] { 0, 0, 0 };
		gbl.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gbl.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
		setLayout( gbl );

		final JLabel lblDetection = new JLabel( "Detection." );
		lblDetection.setFont( lblDetection.getFont().deriveFont( lblDetection.getFont().getStyle() | Font.BOLD ) );
		final GridBagConstraints gbcLblDetection = new GridBagConstraints();
		gbcLblDetection.anchor = GridBagConstraints.WEST;
		gbcLblDetection.gridwidth = 2;
		gbcLblDetection.insets = new Insets( 5, 5, 5, 0 );
		gbcLblDetection.gridx = 0;
		gbcLblDetection.gridy = 0;
		add( lblDetection, gbcLblDetection );

		final JLabel lblSetupId = new JLabel( "Setup ID:" );
		final GridBagConstraints gbcLblSetupId = new GridBagConstraints();
		gbcLblSetupId.anchor = GridBagConstraints.EAST;
		gbcLblSetupId.insets = new Insets( 0, 5, 5, 5 );
		gbcLblSetupId.gridx = 0;
		gbcLblSetupId.gridy = 1;
		add( lblSetupId, gbcLblSetupId );

		final SetupIDComboBox comboBox = new SetupIDComboBox( data.getSources() );
		comboBox.addItemListener( ( e ) -> editedSettings.setSetupID( comboBox.getSelectedSetupID() ) );
		final GridBagConstraints gbcComboBox = new GridBagConstraints();
		gbcComboBox.insets = new Insets( 5, 5, 5, 0 );
		gbcComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbcComboBox.gridx = 1;
		gbcComboBox.gridy = 1;
		add( comboBox, gbcComboBox );

		final JLabel lblQualityFactor = new JLabel( "Quality factor:" );
		lblQualityFactor.setToolTipText( QUALITY_FACTOR_TOOLTIP );
		final GridBagConstraints gbcLblQualityFactor = new GridBagConstraints();
		gbcLblQualityFactor.anchor = GridBagConstraints.EAST;
		gbcLblQualityFactor.insets = new Insets( 0, 0, 5, 5 );
		gbcLblQualityFactor.gridx = 0;
		gbcLblQualityFactor.gridy = 2;
		add( lblQualityFactor, gbcLblQualityFactor );

		final BoundedValueDouble qualityFactor = doubleElement( "Quality factor", 0., 5., editedSettings::getQualityFactor, editedSettings::setQualityFactor )
				.getValue();
		final SliderPanelDouble qualityFactorSlider = new SliderPanelDouble( "", qualityFactor, 0.2 );
		qualityFactorSlider.setToolTipText( QUALITY_FACTOR_TOOLTIP );
		qualityFactorSlider.setNumColummns( 3 );
		final GridBagConstraints gbcLblQualityFactorSlider = new GridBagConstraints();
		gbcLblQualityFactorSlider.fill = GridBagConstraints.HORIZONTAL;
		gbcLblQualityFactorSlider.insets = new Insets( 0, 0, 5, 0 );
		gbcLblQualityFactorSlider.gridx = 1;
		gbcLblQualityFactorSlider.gridy = 2;
		add( qualityFactorSlider, gbcLblQualityFactorSlider );

		final JLabel lblDistanceFactor = new JLabel( "Distance factor:" );
		lblDistanceFactor.setToolTipText( DISTANCE_FACTOR_TOOLTIP );
		final GridBagConstraints gbcLblDistanceFactor = new GridBagConstraints();
		gbcLblDistanceFactor.anchor = GridBagConstraints.EAST;
		gbcLblDistanceFactor.insets = new Insets( 0, 5, 5, 5 );
		gbcLblDistanceFactor.gridx = 0;
		gbcLblDistanceFactor.gridy = 3;
		add( lblDistanceFactor, gbcLblDistanceFactor );

		final BoundedValueDouble distanceFactor = doubleElement( "Distance factor", 0., 5., editedSettings::getDistanceFactor, editedSettings::setDistanceFactor )
				.getValue();
		final SliderPanelDouble distanceFactorSlider = new SliderPanelDouble( "", distanceFactor, 0.2 );
		distanceFactorSlider.setNumColummns( 3 );
		distanceFactorSlider.setToolTipText( DISTANCE_FACTOR_TOOLTIP );
		final GridBagConstraints gbcLblDistanceFactorSlider = new GridBagConstraints();
		gbcLblDistanceFactorSlider.fill = GridBagConstraints.HORIZONTAL;
		gbcLblDistanceFactorSlider.insets = new Insets( 0, 0, 5, 0 );
		gbcLblDistanceFactorSlider.gridx = 1;
		gbcLblDistanceFactorSlider.gridy = 3;
		add( distanceFactorSlider, gbcLblDistanceFactorSlider );

		final JLabel lblNTimepoints = new JLabel( "N time-points:" );
		lblNTimepoints.setToolTipText( N_TIMEPOINTS_TOOLTIP );
		final GridBagConstraints gbcLblNTimepoints = new GridBagConstraints();
		gbcLblNTimepoints.anchor = GridBagConstraints.EAST;
		gbcLblNTimepoints.insets = new Insets( 0, 0, 5, 5 );
		gbcLblNTimepoints.gridx = 0;
		gbcLblNTimepoints.gridy = 4;
		add( lblNTimepoints, gbcLblNTimepoints );

		final BoundedValue nTimepoints = intElement( "N time-points", 1, nTimePoints, editedSettings::getnTimepoints, editedSettings::setNTimepoints )
				.getValue();
		final SliderPanel nTimepointsSlider = new SliderPanel( "", nTimepoints, 1 );
		nTimepointsSlider.setNumColummns( 3 );
		nTimepointsSlider.setToolTipText( N_TIMEPOINTS_TOOLTIP );
		final GridBagConstraints gbcLblNTimepointsSlider = new GridBagConstraints();
		gbcLblNTimepointsSlider.fill = GridBagConstraints.HORIZONTAL;
		gbcLblNTimepointsSlider.insets = new Insets( 0, 0, 5, 0 );
		gbcLblNTimepointsSlider.gridx = 1;
		gbcLblNTimepointsSlider.gridy = 4;
		add( nTimepointsSlider, gbcLblNTimepointsSlider );

		final JSeparator sep1 = new JSeparator();
		sep1.setMinimumSize( new Dimension( 5, 10 ) );
		final GridBagConstraints gbcSeparator1 = new GridBagConstraints();
		gbcSeparator1.fill = GridBagConstraints.HORIZONTAL;
		gbcSeparator1.gridwidth = 2;
		gbcSeparator1.insets = new Insets( 0, 0, 5, 0 );
		gbcSeparator1.gridx = 0;
		gbcSeparator1.gridy = 5;
		gbcSeparator1.anchor = GridBagConstraints.SOUTH;
		add( sep1, gbcSeparator1 );

		final JLabel lblTrackingDirection = new JLabel( "Tracking direction." );
		lblTrackingDirection.setFont( lblTrackingDirection.getFont().deriveFont( lblTrackingDirection.getFont().getStyle() | Font.BOLD ) );
		final GridBagConstraints gbcLblTrackingDirection = new GridBagConstraints();
		gbcLblTrackingDirection.anchor = GridBagConstraints.WEST;
		gbcLblTrackingDirection.gridwidth = 2;
		gbcLblTrackingDirection.insets = new Insets( 5, 5, 5, 0 );
		gbcLblTrackingDirection.gridx = 0;
		gbcLblTrackingDirection.gridy = 6;
		add( lblTrackingDirection, gbcLblTrackingDirection );

		final JPanel panelTrackingDirection = new JPanel();
		final GridBagConstraints gbcPanelTrackingDirection = new GridBagConstraints();
		gbcPanelTrackingDirection.insets = new Insets( 5, 5, 5, 0 );
		gbcPanelTrackingDirection.gridwidth = 2;
		gbcPanelTrackingDirection.fill = GridBagConstraints.VERTICAL;
		gbcPanelTrackingDirection.gridx = 0;
		gbcPanelTrackingDirection.gridy = 7;
		add( panelTrackingDirection, gbcPanelTrackingDirection );

		final JRadioButton rdbtnForward = new JRadioButton( "Forward in time." );
		rdbtnForward.addItemListener( ( e ) -> editedSettings.setForwardInTime( rdbtnForward.isSelected() ) );
		panelTrackingDirection.add( rdbtnForward );

		final JRadioButton rdbtnBackward = new JRadioButton( "Backward in time." );
		rdbtnBackward.addItemListener( ( e ) -> editedSettings.setForwardInTime( rdbtnForward.isSelected() ) );
		panelTrackingDirection.add( rdbtnBackward );

		final ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add( rdbtnForward );
		buttonGroup.add( rdbtnBackward );

		final JSeparator sep2 = new JSeparator();
		sep2.setMinimumSize( new Dimension( 5, 10 ) );
		final GridBagConstraints gbcSeparator2 = new GridBagConstraints();
		gbcSeparator2.anchor = GridBagConstraints.SOUTH;
		gbcSeparator2.fill = GridBagConstraints.HORIZONTAL;
		gbcSeparator2.gridwidth = 2;
		gbcSeparator2.insets = new Insets( 0, 0, 5, 0 );
		gbcSeparator2.gridx = 0;
		gbcSeparator2.gridy = 8;
		add( sep2, gbcSeparator2 );

		final JLabel lblDealWithExisting = new JLabel( "Existing spots." );
		lblDealWithExisting.setFont( lblDealWithExisting.getFont().deriveFont( lblDealWithExisting.getFont().getStyle() | Font.BOLD ) );
		final GridBagConstraints gbcLblDealWithExisting = new GridBagConstraints();
		gbcLblDealWithExisting.anchor = GridBagConstraints.WEST;
		gbcLblDealWithExisting.gridwidth = 2;
		gbcLblDealWithExisting.insets = new Insets( 5, 5, 5, 0 );
		gbcLblDealWithExisting.gridx = 0;
		gbcLblDealWithExisting.gridy = 9;
		add( lblDealWithExisting, gbcLblDealWithExisting );

		final JCheckBox chckbxAllowLinkingIf = new JCheckBox( "Allow linking to an existing spot?" );
		chckbxAllowLinkingIf.setToolTipText( ALLOW_LINKING_TO_EXISTING_TOOLTIP );
		chckbxAllowLinkingIf.addItemListener( ( e ) -> editedSettings.setAllowLinkingToExisting( chckbxAllowLinkingIf.isSelected() ) );
		final GridBagConstraints gbcLblAllowLinkingIf = new GridBagConstraints();
		gbcLblAllowLinkingIf.gridwidth = 2;
		gbcLblAllowLinkingIf.anchor = GridBagConstraints.WEST;
		gbcLblAllowLinkingIf.fill = GridBagConstraints.VERTICAL;
		gbcLblAllowLinkingIf.insets = new Insets( 5, 5, 5, 0 );
		gbcLblAllowLinkingIf.gridx = 0;
		gbcLblAllowLinkingIf.gridy = 10;
		add( chckbxAllowLinkingIf, gbcLblAllowLinkingIf );

		final JCheckBox chckbxLinkIncoming = new JCheckBox( "Link even if target has incoming links." );
		chckbxLinkIncoming.setToolTipText( ALLOW_LINKING_IF_INCOMING_TOOLTIP );
		chckbxLinkIncoming.addItemListener( ( e ) -> editedSettings.setAllowIfIncomingLinks( chckbxLinkIncoming.isSelected() ) );
		final GridBagConstraints gbcChckbxLinkIncoming = new GridBagConstraints();
		gbcChckbxLinkIncoming.insets = new Insets( 5, 5, 5, 0 );
		gbcChckbxLinkIncoming.anchor = GridBagConstraints.WEST;
		gbcChckbxLinkIncoming.gridx = 1;
		gbcChckbxLinkIncoming.gridy = 11;
		add( chckbxLinkIncoming, gbcChckbxLinkIncoming );

		final JCheckBox chckbxLinkOutgoing = new JCheckBox( "Link even if target has outgoing links." );
		chckbxLinkOutgoing.setToolTipText( ALLOW_LINKING_IF_OUTGOING_TOOLTIP );
		chckbxLinkOutgoing.addItemListener( ( e ) -> editedSettings.setAllowIfOutgoingLinks( chckbxLinkOutgoing.isSelected() ) );
		final GridBagConstraints gbcChckbxLinkOutgoing = new GridBagConstraints();
		gbcChckbxLinkOutgoing.insets = new Insets( 5, 5, 5, 0 );
		gbcChckbxLinkOutgoing.anchor = GridBagConstraints.WEST;
		gbcChckbxLinkOutgoing.gridx = 1;
		gbcChckbxLinkOutgoing.gridy = 12;
		add( chckbxLinkOutgoing, gbcChckbxLinkOutgoing );

		final JCheckBox chckbxContinueTracking = new JCheckBox( "<html>"
				+ "Continue tracking if source and target spots are already linked.</html>" );
		chckbxContinueTracking.setVerticalTextPosition( SwingConstants.TOP );
		chckbxContinueTracking.setVerticalAlignment( SwingConstants.TOP );
		chckbxContinueTracking.setToolTipText( CONTINUE_LINKING_TOOLTIP );
		chckbxContinueTracking.addItemListener( ( e ) -> editedSettings.setContinueIfLinked( chckbxContinueTracking.isSelected() ) );
		final GridBagConstraints gbcChckbxContinueTracking = new GridBagConstraints();
		gbcChckbxContinueTracking.anchor = GridBagConstraints.NORTH;
		gbcChckbxContinueTracking.fill = GridBagConstraints.HORIZONTAL;
		gbcChckbxContinueTracking.insets = new Insets( 5, 5, 5, 0 );
		gbcChckbxContinueTracking.gridx = 1;
		gbcChckbxContinueTracking.gridy = 13;
		add( chckbxContinueTracking, gbcChckbxContinueTracking );

		final JCheckBox chckbxDetectSpot = new JCheckBox("Run detection if existing spot cannot be found?");
		chckbxDetectSpot.setToolTipText( DETECT_SPOT_TOOLTIP );
		chckbxDetectSpot.addItemListener( ( e ) -> editedSettings.setDetectSpot( chckbxDetectSpot.isSelected() ) );
		final GridBagConstraints gbcChckbxDetectSpot = new GridBagConstraints();
		gbcChckbxDetectSpot.anchor = GridBagConstraints.WEST;
		gbcChckbxDetectSpot.gridwidth = 2;
		gbcChckbxDetectSpot.insets = new Insets( 5, 5, 5, 0 );
		gbcChckbxDetectSpot.gridx = 0;
		gbcChckbxDetectSpot.gridy = 14;
		add( chckbxDetectSpot, gbcChckbxDetectSpot );

		final JSeparator sep3 = new JSeparator();
		sep3.setMinimumSize( new Dimension( 5, 10 ) );
		final GridBagConstraints gbcSeparator3 = new GridBagConstraints();
		gbcSeparator3.anchor = GridBagConstraints.SOUTH;
		gbcSeparator3.fill = GridBagConstraints.HORIZONTAL;
		gbcSeparator3.gridwidth = 2;
		gbcSeparator3.insets = new Insets( 0, 0, 5, 0 );
		gbcSeparator3.gridx = 0;
		gbcSeparator3.gridy = 15;
		add( sep3, gbcSeparator3 );

		final JLabel lblNavigation = new JLabel( "Navigation." );
		lblNavigation.setFont( lblNavigation.getFont().deriveFont( lblNavigation.getFont().getStyle() | Font.BOLD ) );
		final GridBagConstraints gbcLblNavigation = new GridBagConstraints();
		gbcLblNavigation.anchor = GridBagConstraints.WEST;
		gbcLblNavigation.gridwidth = 2;
		gbcLblNavigation.insets = new Insets( 5, 5, 5, 0 );
		gbcLblNavigation.gridx = 0;
		gbcLblNavigation.gridy = 16;
		add( lblNavigation, gbcLblNavigation );

		if ( null != groupHandle )
		{
			final GroupLocksPanel groupLocksPanel = new GroupLocksPanel( groupHandle );
			final GridBagConstraints gbcGroupLocksPanel = new GridBagConstraints();
			gbcGroupLocksPanel.gridwidth = 2;
			gbcGroupLocksPanel.insets = new Insets( 0, 0, 5, 0 );
			gbcGroupLocksPanel.fill = GridBagConstraints.BOTH;
			gbcGroupLocksPanel.gridx = 0;
			gbcGroupLocksPanel.gridy = 17;
			add( groupLocksPanel, gbcGroupLocksPanel );
		}

		final JPanel panelButtons = new JPanel();
		final GridBagConstraints gbcPanelButtons = new GridBagConstraints();
		gbcPanelButtons.anchor = GridBagConstraints.SOUTH;
		gbcPanelButtons.gridwidth = 2;
		gbcPanelButtons.insets = new Insets( 5, 5, 0, 0 );
		gbcPanelButtons.fill = GridBagConstraints.HORIZONTAL;
		gbcPanelButtons.gridx = 0;
		gbcPanelButtons.gridy = 19;
		add( panelButtons, gbcPanelButtons );
		panelButtons.setLayout( new BoxLayout( panelButtons, BoxLayout.X_AXIS ) );

		panelButtons.add( Box.createHorizontalGlue() );
		this.btnCancel = new JButton( "Cancel" );
		panelButtons.add( btnCancel );
		this.btnTrack = new JButton("Track");
		panelButtons.add( btnTrack );

		final UpdateListener refresher = () -> {
			comboBox.setSelectedSetupID( editedSettings.getSetupID() );
			qualityFactor.setCurrentValue( editedSettings.getQualityFactor() );
			distanceFactor.setCurrentValue( editedSettings.getDistanceFactor() );
			nTimepoints.setCurrentValue( editedSettings.getnTimepoints() );
			rdbtnForward.setSelected( editedSettings.isForwardInTime() );
			rdbtnBackward.setSelected( !editedSettings.isForwardInTime() );
			chckbxAllowLinkingIf.setSelected( editedSettings.allowLinkingToExisting() );
			chckbxLinkIncoming.setSelected( editedSettings.allowIfIncomingLinks() );
			chckbxLinkOutgoing.setSelected( editedSettings.allowIfOutgoingLinks() );
			chckbxContinueTracking.setSelected( editedSettings.continueIfLinked() );
			chckbxDetectSpot.setSelected( editedSettings.detectSpot() );
			repaint();
		};
		final ItemListener disabler = ( e ) -> {
			chckbxLinkIncoming.setEnabled( chckbxAllowLinkingIf.isSelected() );
			chckbxLinkOutgoing.setEnabled( chckbxAllowLinkingIf.isSelected() );
			chckbxContinueTracking.setEnabled( chckbxAllowLinkingIf.isSelected() );
		};

		refresher.settingsChanged();
		disabler.itemStateChanged( null );
		editedSettings.updateListeners().add( refresher );
		chckbxAllowLinkingIf.addItemListener( disabler );
	}

	private static final long serialVersionUID = 1L;

}
