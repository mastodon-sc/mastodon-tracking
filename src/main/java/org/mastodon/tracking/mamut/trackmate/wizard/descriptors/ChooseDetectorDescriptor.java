/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2021 Tobias Pietzsch, Jean-Yves Tinevez
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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mastodon.mamut.WindowManager;
import org.mastodon.tracking.mamut.detection.SpotDetectorOp;
import org.mastodon.tracking.mamut.trackmate.PluginProvider;
import org.mastodon.tracking.mamut.trackmate.Settings;
import org.mastodon.tracking.mamut.trackmate.TrackMate;
import org.mastodon.tracking.mamut.trackmate.wizard.WizardPanelDescriptor;
import org.scijava.Context;

public class ChooseDetectorDescriptor extends WizardPanelDescriptor
{

	public static final String IDENTIFIER = "Detector selection";

	private final PluginProvider< SpotDetectorDescriptor > descriptorProvider;

	private final DefaultComboBoxModel< String > model;

	private final List< String > names;

	private final Map< String, String > descriptions;

	private final List< Class< ? extends SpotDetectorOp > > classes;

	private final TrackMate trackmate;

	public ChooseDetectorDescriptor( final TrackMate trackmate, final WindowManager windowManager )
	{
		this.trackmate = trackmate;
		this.model = new DefaultComboBoxModel<>();
		this.targetPanel = new ChooseDetectorPanel();
		this.panelIdentifier = IDENTIFIER;

		final PluginProvider< SpotDetectorOp > detectorprovider = new PluginProvider<>( SpotDetectorOp.class );
		final Context context = windowManager.getContext();
		context .inject( detectorprovider );
		this.names = detectorprovider.getVisibleNames();
		this.descriptions = detectorprovider.getDescriptions();
		this.classes = detectorprovider.getClasses();

		this.descriptorProvider = new PluginProvider<>( SpotDetectorDescriptor.class );
		context.inject( descriptorProvider );
	}

	@Override
	public void aboutToDisplayPanel()
	{
		int indexOf = 0;
		final Settings settings = trackmate.getSettings();
		final Class< ? extends SpotDetectorOp > detectorClass = settings.values.getDetector();
		if ( null != detectorClass )
			indexOf = classes.indexOf( detectorClass );

		model.removeAllElements();
		for ( final String name : names )
			model.addElement( name );

		model.setSelectedItem( names.get( indexOf ) );
	}

	@Override
	public void aboutToHidePanel()
	{
		final String name = ( String ) model.getSelectedItem();
		final Class< ? extends SpotDetectorOp > detectorClass = classes.get( names.indexOf( name ) );
		final Settings settings = trackmate.getSettings();
		settings.detector( detectorClass );
	}

	private class ChooseDetectorPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		public ChooseDetectorPanel()
		{
			final GridBagLayout layout = new GridBagLayout();
			layout.columnWidths = new int[] { 80, 80 };
			layout.columnWeights = new double[] { 0.5, 0.5 };
			layout.rowHeights = new int[] { 0, 0, 0, 26 };
			layout.rowWeights = new double[] { 0., 0., 0., 1.0 };
			setLayout( layout );

			final GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.gridwidth = 2;
			gbc.anchor = GridBagConstraints.BASELINE_LEADING;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets( 5, 5, 5, 5 );

			final JLabel title = new JLabel( "Detector selection." );
			title.setFont( getFont().deriveFont( Font.BOLD ) );
			add( title, gbc );

			final JLabel lblPick = new JLabel( "Pick a detector:" );
			gbc.gridy = 1;
			gbc.anchor = GridBagConstraints.SOUTHWEST;
			add( lblPick, gbc );

			gbc.gridy = 2;
			gbc.gridx = 0;
			gbc.gridwidth = 2;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.NORTHWEST;
			gbc.insets = new Insets( 5, 5, 5, 5 );
			final JComboBox< String > comboBox = new JComboBox<>( model );
			add( comboBox, gbc );

			final JLabel lblInfo = new JLabel();
			lblInfo.setVerticalAlignment( JLabel.TOP );
			lblInfo.setFont( getFont().deriveFont( getFont().getSize2D() - 2f ) );
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridy = 3;
			gbc.weighty = 1.;
			gbc.anchor = GridBagConstraints.EAST;
			add( lblInfo, gbc );

			comboBox.addActionListener( ( e ) -> lblInfo.setText( descriptions.get( model.getSelectedItem() ) ) );
		}
	}

}
