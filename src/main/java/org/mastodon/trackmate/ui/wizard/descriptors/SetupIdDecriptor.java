package org.mastodon.trackmate.ui.wizard.descriptors;

import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_SETUP_ID;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.ui.wizard.WizardLogService;
import org.mastodon.trackmate.ui.wizard.WizardPanelDescriptor;
import org.mastodon.trackmate.ui.wizard.util.SetupIDComboBox;
import org.scijava.Context;
import org.scijava.Contextual;
import org.scijava.NullContextException;
import org.scijava.plugin.Parameter;

import bdv.spimdata.SpimDataMinimal;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;

public class SetupIdDecriptor extends WizardPanelDescriptor implements ActionListener, Contextual
{

	public static final String IDENTIFIER = "Setup ID config";

	private final WizardLogService log;

	private final Settings settings;

	public SetupIdDecriptor( final Settings settings, final WizardLogService logService )
	{
		this.settings = settings;
		this.log = logService;
		this.panelIdentifier = IDENTIFIER;
		final SpimDataMinimal spimData = settings.values.getSpimData();
		this.targetPanel = new SetupIdConfigPanel( spimData );
	}

	@Override
	public void aboutToDisplayPanel()
	{
		final SpimDataMinimal spimData = settings.values.getSpimData();
		final SetupIdConfigPanel panel = ( SetupIdConfigPanel ) targetPanel;

		final String dataName = spimData.getBasePath().getAbsolutePath();
		panel.lblDataName.setText( "<html>"
						+ dataName.replaceAll( Pattern.quote( File.separator ), " / " )
						+ "</html>" );

		final List< BasicViewSetup > setups = spimData.getSequenceDescription().getViewSetupsOrdered();
		final int nSetups = setups.size();
		panel.lblNSetups.setText( nSetups == 1 ? "1 setup" : "" + nSetups + " setups" );

		final Integer setupID = ( Integer ) settings.values.getDetectorSettings().get( KEY_SETUP_ID );
		if ( null != setupID )
			panel.comboBox.setSelectedSetupID( setupID );
	}

	@Override
	public void aboutToHidePanel()
	{
		final SetupIdConfigPanel panel = ( SetupIdConfigPanel ) targetPanel;
		final Map< String, Object > detectorSettings = settings.values.getDetectorSettings();
		final int setupID = panel.comboBox.getSelectedSetupID();
		detectorSettings.put( KEY_SETUP_ID, Integer.valueOf( setupID ) );
		final SpimDataMinimal spimData = settings.values.getSpimData();
		final int nTimePoints = spimData.getSequenceDescription().getTimePoints().getTimePoints().size();
		detectorSettings.put( KEY_MIN_TIMEPOINT, Integer.valueOf( 0 ) );
		detectorSettings.put( KEY_MAX_TIMEPOINT, Integer.valueOf( nTimePoints - 1 ) );

		log.log( String.format( "Selected setup ID %d for detection:\n", setupID ) );
		log.log( panel.comboBox.echoSetupIDInfo() );
	}

	@Override
	public void actionPerformed( final ActionEvent e )
	{
		final SetupIdConfigPanel panel = ( SetupIdConfigPanel ) targetPanel;
		String info = panel.comboBox.echoSetupIDInfo();
		// HTMLize the info
		info = "<html>" + info + "</html>";
		info = info.replace( "\n", "<br>" );
		info = info.replace( "    ", "&#160&#160&#160&#160" );
		panel.lblFill.setText( info );
	}

	private class SetupIdConfigPanel extends JPanel
	{
		private static final long serialVersionUID = 1L;

		private final SetupIDComboBox comboBox;

		private final JLabel lblDataName;

		private final JLabel lblNSetups;

		private final JLabel lblFill;

		public SetupIdConfigPanel( final SpimDataMinimal spimData )
		{

			final GridBagLayout layout = new GridBagLayout();
			layout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 126 };
			layout.rowWeights = new double[] { 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 };
			layout.columnWidths = new int[] { 83, 80 };
			layout.columnWeights = new double[] { 0.0, 0.5 };
			setLayout( layout );

			final GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.gridwidth = 2;
			gbc.anchor = GridBagConstraints.BASELINE_LEADING;
			gbc.insets = new Insets( 5, 5, 5, 0 );

			final JLabel title = new JLabel( "Pick a setup ID for detection." );
			title.setFont( getFont().deriveFont( Font.BOLD ) );
			add( title, gbc );

			final JLabel lblForDatal = new JLabel( "For data in:" );
			final GridBagConstraints gbc_lblForDatal = new GridBagConstraints();
			gbc_lblForDatal.anchor = GridBagConstraints.EAST;
			gbc_lblForDatal.insets = new Insets( 5, 5, 5, 5 );
			gbc_lblForDatal.gridx = 0;
			gbc_lblForDatal.gridy = 2;
			add( lblForDatal, gbc_lblForDatal );

			this.lblDataName = new JLabel();
			lblDataName.setFont( getFont().deriveFont( getFont().getSize2D() - 2f ) );
			final GridBagConstraints gbc_lblDataName = new GridBagConstraints();
			gbc_lblDataName.insets = new Insets( 5, 5, 5, 0 );
			gbc_lblDataName.fill = GridBagConstraints.HORIZONTAL;
			gbc_lblDataName.gridx = 1;
			gbc_lblDataName.gridy = 2;
			add( lblDataName, gbc_lblDataName );

			final JLabel lblFound = new JLabel( "Found:" );
			final GridBagConstraints gbc_lblFound = new GridBagConstraints();
			gbc_lblFound.anchor = GridBagConstraints.EAST;
			gbc_lblFound.insets = new Insets( 5, 5, 5, 5 );
			gbc_lblFound.gridx = 0;
			gbc_lblFound.gridy = 3;
			add( lblFound, gbc_lblFound );

			this.lblNSetups = new JLabel();
			final GridBagConstraints gbc_lblNSetups = new GridBagConstraints();
			gbc_lblNSetups.insets = new Insets( 5, 5, 5, 0 );
			gbc_lblNSetups.gridx = 1;
			gbc_lblNSetups.gridy = 3;
			add( lblNSetups, gbc_lblNSetups );

			final JLabel lblSetup = new JLabel( "Setups:" );
			final GridBagConstraints gbc_lblSetup = new GridBagConstraints();
			gbc_lblSetup.anchor = GridBagConstraints.EAST;
			gbc_lblSetup.insets = new Insets( 5, 5, 5, 5 );
			gbc_lblSetup.gridx = 0;
			gbc_lblSetup.gridy = 4;
			add( lblSetup, gbc_lblSetup );

			this.comboBox = new SetupIDComboBox( spimData );
			comboBox.addActionListener( SetupIdDecriptor.this );
			final GridBagConstraints gbc_comboBox = new GridBagConstraints();
			gbc_comboBox.gridwidth = 2;
			gbc_comboBox.insets = new Insets( 5, 5, 5, 5 );
			gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
			gbc_comboBox.gridx = 0;
			gbc_comboBox.gridy = 5;
			add( comboBox, gbc_comboBox );

			this.lblFill = new JLabel();
			lblFill.setFont( getFont().deriveFont( getFont().getSize2D() - 2f ) );
			lblFill.setVerticalAlignment( JLabel.TOP );
			final GridBagConstraints gbc_lblFill = new GridBagConstraints();
			gbc_lblFill.gridwidth = 2;
			gbc_lblFill.insets = new Insets( 5, 5, 5, 5 );
			gbc_lblFill.fill = GridBagConstraints.BOTH;
			gbc_lblFill.weighty = 1.;
			gbc_lblFill.anchor = GridBagConstraints.EAST;
			gbc_lblFill.gridx = 0;
			gbc_lblFill.gridy = 7;
			add( lblFill, gbc_lblFill );

		}
	}

	// -- Contextual methods --

	@Parameter
	private Context context;

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
