package org.mastodon.trackmate.ui.wizard.descriptors;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mastodon.detection.mamut.SpotDetectorOp;
import org.mastodon.trackmate.PluginProvider;
import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.ui.wizard.WizardPanelDescriptor;
import org.scijava.Context;
import org.scijava.Contextual;
import org.scijava.NullContextException;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

public class ChooseDetectorDescriptor extends WizardPanelDescriptor implements Contextual
{

	public static final String IDENTIFIER = "Detector selection";

	@Parameter
	private Context context;

	@Parameter
	private LogService log;

	private final Settings settings;

	private final DefaultComboBoxModel< String > model;

	private List< String > names;

	private Map< String, String > descriptions;

	private List< Class< ? extends SpotDetectorOp > > classes;

	public ChooseDetectorDescriptor( final Settings settings )
	{
		this.model = new DefaultComboBoxModel<>();
		this.settings = settings;
		this.targetPanel = new ChooseDetectorPanel();
		this.panelIdentifier = IDENTIFIER;
	}

	@Override
	public void aboutToDisplayPanel()
	{
		final PluginProvider< SpotDetectorOp > provider = new PluginProvider<>( SpotDetectorOp.class );
		context.inject( provider );

		this.names = provider.getVisibleNames();
		this.descriptions = provider.getDescriptions();
		this.classes = provider.getClasses();

		int indexOf = 0;
		final Class< ? extends SpotDetectorOp > detectorClass = settings.values.getDetector();
		if ( null != detectorClass )
		{
			indexOf = classes.indexOf( detectorClass );
			if ( indexOf < -1 )
				log.error( "Unkown detector class: " + detectorClass );
		}

		model.removeAllElements();
		for ( final String name : names )
			model.addElement( name );

		model.setSelectedItem( names.get( indexOf ) );
	}

	@Override
	public void aboutToHidePanel()
	{
		final String name = ( String ) model.getSelectedItem();
		settings.detector( classes.get( names.indexOf( name ) ) );
	}

	@Override
	public String getBackPanelDescriptorIdentifier()
	{
		return BoundingBoxDescriptor.IDENTIFIER;
	}

	@Override
	public String getNextPanelDescriptorIdentifier()
	{
		return Descriptor1.ID;
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
			lblInfo.setFont( getFont().deriveFont( getFont().getSize2D() - 2f ) );
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridy = 3;
			gbc.weighty = 1.;
			gbc.anchor = GridBagConstraints.EAST;
			add( lblInfo, gbc );

			comboBox.addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					lblInfo.setText( descriptions.get( model.getSelectedItem() ) );
				}
			} );
		}
	}

	// -- Contextual methods --

	@Override
	public Context context()
	{
		if ( context == null )
			throw new NullContextException();
		return context;
	}

	@Override
	public Context getContext()
	{
		return context;
	}
}
