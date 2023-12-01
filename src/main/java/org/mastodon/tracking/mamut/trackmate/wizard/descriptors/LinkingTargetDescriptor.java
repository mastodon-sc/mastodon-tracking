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
package org.mastodon.tracking.mamut.trackmate.wizard.descriptors;

import static org.mastodon.tracking.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.tracking.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.tracking.linking.LinkerKeys.KEY_DO_LINK_SELECTION;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.SelectionListener;
import org.mastodon.model.SelectionModel;
import org.mastodon.tracking.mamut.trackmate.Settings;
import org.mastodon.tracking.mamut.trackmate.TrackMate;
import org.mastodon.tracking.mamut.trackmate.wizard.WizardLogService;
import org.mastodon.tracking.mamut.trackmate.wizard.WizardPanelDescriptor;

import bdv.tools.brightness.SliderPanel;
import bdv.util.BoundedValue;

public class LinkingTargetDescriptor extends WizardPanelDescriptor
{

	private static final String IDENTIFIER = "Linking target";

	private final TrackMate trackmate;

	private final SelectionModel< Spot, Link > selectionModel;

	private final WizardLogService log;

	public LinkingTargetDescriptor( final TrackMate trackmate, final ProjectModel appModel, final WizardLogService logService )
	{
		this.trackmate = trackmate;
		this.log = logService;
		this.selectionModel = appModel.getSelectionModel();
		final int nTimepoints = appModel.getMaxTimepoint() - appModel.getMinTimepoint();
		this.targetPanel = new LinkingTargetPanel( nTimepoints );
		this.panelIdentifier = IDENTIFIER;
	}

	@Override
	public void aboutToDisplayPanel()
	{
		final LinkingTargetPanel panel = ( ( LinkingTargetPanel ) targetPanel );
		selectionModel.listeners().add( panel );

		final Settings settings = trackmate.getSettings();
		final Map< String, Object > linkerSettings = settings.values.getLinkerSettings();
		final boolean doLinkSelection = ( boolean ) linkerSettings.get( KEY_DO_LINK_SELECTION );
		final int minT = ( int ) linkerSettings.get( KEY_MIN_TIMEPOINT );
		final int maxT = ( int ) linkerSettings.get( KEY_MAX_TIMEPOINT );

		panel.btnAllSpots.setSelected( !doLinkSelection );
		panel.minT.setCurrentValue( minT );
		panel.maxT.setCurrentValue( maxT );
	}

	@Override
	public void aboutToHidePanel()
	{
		final LinkingTargetPanel panel = ( ( LinkingTargetPanel ) targetPanel );
		selectionModel.listeners().remove( panel );

		final Settings settings = trackmate.getSettings();
		final Map< String, Object > linkerSettings = settings.values.getLinkerSettings();
		linkerSettings.put( KEY_DO_LINK_SELECTION, !panel.btnAllSpots.isSelected() );
		if ( !panel.btnAllSpots.isSelected() )
		{
			final int[] tminmax = getSelectionTMinMax( selectionModel );
			linkerSettings.put( KEY_MIN_TIMEPOINT, tminmax[ 0 ] );
			linkerSettings.put( KEY_MAX_TIMEPOINT, tminmax[ 1 ] );
		}
		else
		{
			linkerSettings.put( KEY_MIN_TIMEPOINT, panel.minT.getCurrentValue() );
			linkerSettings.put( KEY_MAX_TIMEPOINT, panel.maxT.getCurrentValue() );
		}
		log.log( "Configured linking target as follow:\n" );
		log.log( String.format( "  - target: %s\n", ( boolean ) linkerSettings.get( KEY_DO_LINK_SELECTION ) ? "selection only." : "all detections." ) );
		log.log( String.format( "  - min time-point: %d\n", ( int ) linkerSettings.get( KEY_MIN_TIMEPOINT ) ) );
		log.log( String.format( "  - max time-point: %d\n", ( int ) linkerSettings.get( KEY_MAX_TIMEPOINT ) ) );
	}

	private class LinkingTargetPanel extends JPanel implements SelectionListener
	{

		private static final long serialVersionUID = 1L;

		private final BoundedValue minT;

		private final BoundedValue maxT;

		private final JRadioButton btnAllSpots;

		private final JLabel lblInfo;

		public LinkingTargetPanel( final int nTimepoints )
		{
			final GridBagLayout layout = new GridBagLayout();
			layout.columnWidths = new int[] { 80, 80 };
			layout.columnWeights = new double[] { 0.5, 0.5 };
			layout.rowHeights = new int[] { 0, 0, 0, 0, 0, 30 };
			layout.rowWeights = new double[] { 0., 1., 0., 0., 0., 0., 1.0 };
			setLayout( layout );

			final GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.gridwidth = 2;
			gbc.anchor = GridBagConstraints.BASELINE_LEADING;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets( 5, 5, 5, 5 );

			final JLabel title = new JLabel( "Linker target." );
			title.setFont( getFont().deriveFont( Font.BOLD ) );
			add( title, gbc );

			final JLabel lblPick = new JLabel( "Perform linking on:" );
			gbc.gridy = 1;
			gbc.anchor = GridBagConstraints.SOUTHWEST;
			add( lblPick, gbc );

			gbc.gridy = 2;
			this.btnAllSpots = new JRadioButton( "All spots, between time-points:" );
			add( btnAllSpots, gbc );

			gbc.gridy = 3;
			this.minT = new BoundedValue( 0, nTimepoints, 0 );
			final SliderPanel tMinPanel = new SliderPanel( "t min", minT, 1 );
			add( tMinPanel, gbc );

			gbc.gridy = 4;
			this.maxT = new BoundedValue( 0, nTimepoints, nTimepoints );
			final SliderPanel tMaxPanel = new SliderPanel( "t max", maxT, 1 );
			add( tMaxPanel, gbc );

			gbc.gridy = 5;
			final JRadioButton btnSelection = new JRadioButton( "Spots in selection." );
			add( btnSelection, gbc );

			gbc.gridy = 6;
			gbc.anchor = GridBagConstraints.NORTHWEST;
			lblInfo = new JLabel( " " );
			lblInfo.setFont( getFont().deriveFont( getFont().getSize2D() - 2f ) );
			lblInfo.setVerticalAlignment( JLabel.TOP );
			add( lblInfo, gbc );

			final ButtonGroup buttonGroup = new ButtonGroup();
			buttonGroup.add( btnAllSpots );
			buttonGroup.add( btnSelection );

			btnAllSpots.addItemListener( ( e ) -> {
				LinkingTargetDescriptor.setEnabled( tMinPanel, btnAllSpots.isSelected() );
				LinkingTargetDescriptor.setEnabled( tMaxPanel, btnAllSpots.isSelected() );
				selectionChanged();
			} );
		}

		@Override
		public void selectionChanged()
		{
			if ( btnAllSpots.isSelected() )
			{
				lblInfo.setText( " " );
			}
			else
			{
				new Thread( "Investigate selection thread" )
				{
					@Override
					public void run()
					{
						final int nSpots = selectionModel.getSelectedVertices().size();
						if ( nSpots == 0 )
						{
							SwingUtilities.invokeLater( () -> lblInfo.setText( "Selection is empty." ) );
							return;
						}
						final int[] tminmax = getSelectionTMinMax( selectionModel );
						SwingUtilities.invokeLater( () -> lblInfo.setText( "Selection has " + nSpots
								+ " spots, from t = " + tminmax[ 0 ] + " to t = " + tminmax[ 1 ] + "." ) );
					}
				}.start();
			}
		}
	}

	private static int[] getSelectionTMinMax(final SelectionModel< Spot, Link > selectionModel)
	{
		int tmin = Integer.MAX_VALUE;
		int tmax = Integer.MIN_VALUE;
		for ( final Spot spot : selectionModel.getSelectedVertices() )
		{
			if ( spot.getTimepoint() > tmax )
				tmax = spot.getTimepoint();
			if ( spot.getTimepoint() < tmin )
				tmin = spot.getTimepoint();
		}
		return new int[] { tmin, tmax };
	}

	private static void setEnabled( final Component component, final boolean enabled )
	{
		component.setEnabled( enabled );
		if ( component instanceof Container )
		{
			for ( final Component child : ( ( Container ) component ).getComponents() )
			{
				setEnabled( child, enabled );
			}
		}
	}
}
