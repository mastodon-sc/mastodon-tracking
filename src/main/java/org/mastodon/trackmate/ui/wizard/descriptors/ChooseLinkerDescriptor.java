package org.mastodon.trackmate.ui.wizard.descriptors;

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

import org.mastodon.linking.mamut.SpotLinkerOp;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.trackmate.PluginProvider;
import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.TrackMate;
import org.mastodon.trackmate.ui.wizard.WizardPanelDescriptor;
import org.scijava.Context;

public class ChooseLinkerDescriptor extends WizardPanelDescriptor
{

	public static final String IDENTIFIER = "Linker selection";

	private PluginProvider< SpotLinkerDescriptor > descriptorProvider;

	private final DefaultComboBoxModel< String > model;

	private List< String > names;

	private Map< String, String > descriptions;

	private List< Class< ? extends SpotLinkerOp > > classes;

	private final TrackMate trackmate;

	public ChooseLinkerDescriptor( final TrackMate trackmate, final WindowManager windowManager )
	{
		this.trackmate = trackmate;
		this.model = new DefaultComboBoxModel<>();
		this.targetPanel = new ChooseDetectorPanel();
		this.panelIdentifier = IDENTIFIER;

		final PluginProvider< SpotLinkerOp > linkerProvider = new PluginProvider<>( SpotLinkerOp.class );
		final Context context = windowManager.getContext();
		context.inject( linkerProvider );
		this.names = linkerProvider.getVisibleNames();
		this.descriptions = linkerProvider.getDescriptions();
		this.classes = linkerProvider.getClasses();

		this.descriptorProvider = new PluginProvider<>( SpotLinkerDescriptor.class );
		context.inject( descriptorProvider );
	}

	@Override
	public void aboutToDisplayPanel()
	{
		int indexOf = 0;
		final Settings settings = trackmate.getSettings();
		final Class< ? extends SpotLinkerOp > linkerClass = settings.values.getLinker();
		if ( null != linkerClass )
			indexOf = classes.indexOf( linkerClass );

		model.removeAllElements();
		for ( final String name : names )
			model.addElement( name );

		model.setSelectedItem( names.get( indexOf ) );
	}

	@Override
	public void aboutToHidePanel()
	{
		final String name = ( String ) model.getSelectedItem();
		final Class< ? extends SpotLinkerOp > linkerClass = classes.get( names.indexOf( name ) );
		final Settings settings = trackmate.getSettings();
		settings.linker( linkerClass );
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

			final JLabel title = new JLabel( "Linker selection." );
			title.setFont( getFont().deriveFont( Font.BOLD ) );
			add( title, gbc );

			final JLabel lblPick = new JLabel( "Pick a spot linker:" );
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
			lblInfo.setVerticalAlignment( JLabel.TOP );
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridy = 3;
			gbc.weighty = 1.;
			gbc.anchor = GridBagConstraints.EAST;
			add( lblInfo, gbc );

			comboBox.addActionListener( ( e ) -> lblInfo.setText( descriptions.get( model.getSelectedItem() ) ) );
		}
	}
}
