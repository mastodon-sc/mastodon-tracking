package org.mastodon.trackmate.ui.wizard.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import bdv.spimdata.SpimDataMinimal;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imglib2.Dimensions;

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

	public SetupIDComboBox( final SpimDataMinimal spimData )
	{
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
}
