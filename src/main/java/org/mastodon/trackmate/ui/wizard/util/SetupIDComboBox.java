package org.mastodon.trackmate.ui.wizard.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

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

/**
 * A combo-box that lets the user select one of the setup ids present in a spim
 * data.
 * 
 * @author Jean-Yves Tinevez
 */
public class SetupIDComboBox extends JComboBox< String >
{

	private static final long serialVersionUID = 1L;

	private final Map< String, Integer > idMap;

	private final Map< Integer, String > strMap;

	private final SpimDataMinimal spimData;

	public SetupIDComboBox( final SpimDataMinimal spimData )
	{
		this.spimData = spimData;
		if ( null == spimData )
		{
			this.idMap = new HashMap<>( 1 );
			this.strMap = new HashMap<>( 1 );
			final String str = "No spim data";
			final Integer id = Integer.valueOf( -1 );
			idMap.put( str, id );
			strMap.put( id, str );
			final ComboBoxModel< String > aModel = new DefaultComboBoxModel<>( new String[] { str } );
			setModel( aModel );
		}
		else
		{
			final List< BasicViewSetup > setups = spimData.getSequenceDescription().getViewSetupsOrdered();
			final int nSetups = setups.size();
			this.idMap = new HashMap<>( nSetups );
			this.strMap = new HashMap<>( nSetups );

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
			setModel( aModel );
		}
	}

	/**
	 * Returns the id of the setup id currently selected.
	 * 
	 * @return the setup id.
	 */
	public int getSelectedSetupID()
	{
		return idMap.get( getSelectedItem() );
	}

	/**
	 * Sets the specified setup id as selection.
	 * 
	 * @param setupID
	 *            the setup id to select.
	 */
	public void setSelectedSetupID( final int setupID )
	{
		setSelectedItem( strMap.get( Integer.valueOf( setupID ) ) );
	}

	/**
	 * Returns an explanatory string that states what resolutions are available
	 * in the {@link SpimDataMinimal} for the currently selected setup id.
	 * 
	 * @return a string.
	 */
	public String echoSetupIDInfo()
	{
		if ( spimData == null )
			return "No spim data.";
		final BasicViewSetup setup = spimData.getSequenceDescription().getViewSetups().get( getSelectedSetupID() );

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

		if ( spimData.getSequenceDescription().getImgLoader() instanceof BasicMultiResolutionImgLoader )
		{
			final BasicMultiResolutionSetupImgLoader< ? > loader =
					( ( BasicMultiResolutionImgLoader ) spimData.getSequenceDescription().getImgLoader() )
							.getSetupImgLoader( getSelectedSetupID() );

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
}
