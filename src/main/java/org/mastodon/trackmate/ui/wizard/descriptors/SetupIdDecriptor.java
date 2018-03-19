package org.mastodon.trackmate.ui.wizard.descriptors;

import static org.mastodon.detection.DetectorKeys.KEY_SETUP_ID;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.ui.wizard.WizardLogService;
import org.mastodon.trackmate.ui.wizard.WizardPanelDescriptor;
import org.scijava.Context;
import org.scijava.Contextual;
import org.scijava.NullContextException;
import org.scijava.plugin.Parameter;

import bdv.spimdata.SpimDataMinimal;
import bdv.util.Affine3DHelpers;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.base.NamedEntity;
import mpicbg.spim.data.generic.sequence.BasicMultiResolutionImgLoader;
import mpicbg.spim.data.generic.sequence.BasicMultiResolutionSetupImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Dimensions;
import net.imglib2.realtransform.AffineTransform3D;

public class SetupIdDecriptor extends WizardPanelDescriptor implements ActionListener, Contextual
{

	public static final String IDENTIFIER = "Setup ID config";

	private final WizardLogService log;

	private final Map< String, Integer > idMap;

	private final Settings settings;

	public SetupIdDecriptor( final Settings settings, final WizardLogService logService )
	{
		this.settings = settings;
		this.log = logService;
		this.panelIdentifier = IDENTIFIER;
		this.targetPanel = new SetupIdConfigPanel();
		this.idMap = new HashMap<>();
	}

	@Override
	public void aboutToDisplayPanel()
	{
		final SpimDataMinimal spimData = settings.values.getSpimData();
		final SetupIdConfigPanel panel = ( SetupIdConfigPanel ) targetPanel;

		final String dataName = spimData.getBasePath().getAbsolutePath();
		panel.lblDataName.setText(
				"<html>"
						+ dataName.replaceAll( Pattern.quote( File.separator ), " / " )
						+ "</html>" );

		final List< BasicViewSetup > setups = spimData.getSequenceDescription().getViewSetupsOrdered();
		final int nSetups = setups.size();
		panel.lblNSetups.setText( nSetups == 1 ? "1 setup" : "" + nSetups + " setups" );

		final Map< Integer, String > strMap = new HashMap<>( nSetups );
		final String[] items = new String[ nSetups ];
		int i = 0;
		for ( final BasicViewSetup setup : setups )
		{
			final int setupID = setup.getId();
			final String name = setup.getName();
			final Dimensions size = setup.getSize();

			final String str = ( null == name )
					? setupID + "  -  " + size.dimension( 0 ) + " x " + size.dimension( 1 ) + " x " + size.dimension( 2 )
					: setupID + "  -  " + name;
			items[ i++ ] = str;
			idMap.put( str, setupID );
			strMap.put( setupID, str );
		}

		final ComboBoxModel< String > aModel = new DefaultComboBoxModel<>( items );
		panel.comboBox.setModel( aModel );

		final Integer setupID = ( Integer ) settings.values.getDetectorSettings().get( KEY_SETUP_ID );
		if ( null != setupID )
			panel.comboBox.setSelectedItem( strMap.get( setupID ) );
	}

	@Override
	public void aboutToHidePanel()
	{
		final SetupIdConfigPanel panel = ( SetupIdConfigPanel ) targetPanel;
		final Object obj = panel.comboBox.getSelectedItem();
		final Integer setupID = idMap.get( obj );
		final Map< String, Object > detectorSettings = settings.values.getDetectorSettings();
		detectorSettings.put( KEY_SETUP_ID, setupID );

		log.log( String.format( "Selected setup ID %d for detection:\n", ( int ) setupID ) );
		log.log( echoSetupIDInfo( setupID ) );
	}

	private String echoSetupIDInfo( final int setupID )
	{
		final BasicViewSetup setup = settings.values.getSpimData().getSequenceDescription().getViewSetups().get( setupID );
		if ( null == setup )
			return "";

		final StringBuilder str = new StringBuilder();

		final Dimensions size = setup.getSize();
		str.append( "  - size: " + size.dimension( 0 ) + " x " + size.dimension( 1 ) + " x " + size.dimension( 2 ) + "\n" );

		final VoxelDimensions voxelSize = setup.getVoxelSize();
		str.append( "  - voxel size: " + voxelSize.dimension( 0 ) + " x " + voxelSize.dimension( 1 ) + " x "
				+ voxelSize.dimension( 2 ) + " " + voxelSize.unit() + "\n" );

		final Map< String, Entity > attributes = setup.getAttributes();
		for ( final String key : attributes.keySet() )
		{
			final Entity entity = attributes.get( key );
			if ( entity instanceof NamedEntity )
			{
				final NamedEntity ne = ( NamedEntity ) entity;
				str.append( "  - " + key + ": " + ne.getName() + "\n" );
			}
		}

		final SpimDataMinimal spimData = settings.values.getSpimData();
		if ( spimData.getSequenceDescription().getImgLoader() instanceof BasicMultiResolutionImgLoader )
		{
			final BasicMultiResolutionSetupImgLoader< ? > loader =
					( ( BasicMultiResolutionImgLoader ) spimData.getSequenceDescription().getImgLoader() )
							.getSetupImgLoader( setupID );

			final int numMipmapLevels = loader.numMipmapLevels();

			if ( numMipmapLevels > 1 )
			{
				final AffineTransform3D[] mipmapTransforms = loader.getMipmapTransforms();
				str.append( String.format( "  - multi-resolution image with %d levels:\n", numMipmapLevels ) );
				for ( int level = 0; level < mipmapTransforms.length; level++ )
				{
					final double sx = Affine3DHelpers.extractScale( mipmapTransforms[ level ], 0 );
					final double sy = Affine3DHelpers.extractScale( mipmapTransforms[ level ], 1 );
					final double sz = Affine3DHelpers.extractScale( mipmapTransforms[ level ], 2 );
					str.append( String.format( "     - level %d: %.0f x %.0f x %.0f\n", level, sx, sy, sz ) );
				}
			}
			else
			{
				str.append( " - single-resolution image.\n" );
			}
		}
		else
		{
			str.append( " - single-resolution image.\n" );
		}

		return str.toString();
	}

	@Override
	public void actionPerformed( final ActionEvent e )
	{
		final SetupIdConfigPanel panel = ( SetupIdConfigPanel ) targetPanel;
		final Object obj = panel.comboBox.getSelectedItem();
		final Integer setupID = idMap.get( obj );

		String info = echoSetupIDInfo( setupID );
		// HTMLize the info
		info = "<html>" + info + "</html>";
		info = info.replace( "\n", "<br>" );
		info = info.replace( "    ", "&#160&#160&#160&#160" );
		panel.lblFill.setText( info );
	}

	private class SetupIdConfigPanel extends JPanel
	{
		private static final long serialVersionUID = 1L;

		private JComboBox< String > comboBox;

		private JLabel lblDataName;

		private JLabel lblNSetups;

		private JLabel lblFill;

		public SetupIdConfigPanel()
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

			this.comboBox = new JComboBox<>();
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
