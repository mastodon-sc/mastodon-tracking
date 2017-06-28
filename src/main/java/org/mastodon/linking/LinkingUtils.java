package org.mastodon.linking;

import static org.mastodon.linking.LinkerKeys.DEFAULT_ALLOW_GAP_CLOSING;
import static org.mastodon.linking.LinkerKeys.DEFAULT_ALLOW_TRACK_MERGING;
import static org.mastodon.linking.LinkerKeys.DEFAULT_ALLOW_TRACK_SPLITTING;
import static org.mastodon.linking.LinkerKeys.DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR;
import static org.mastodon.linking.LinkerKeys.DEFAULT_BLOCKING_VALUE;
import static org.mastodon.linking.LinkerKeys.DEFAULT_CUTOFF_PERCENTILE;
import static org.mastodon.linking.LinkerKeys.DEFAULT_GAP_CLOSING_FEATURE_PENALTIES;
import static org.mastodon.linking.LinkerKeys.DEFAULT_GAP_CLOSING_MAX_DISTANCE;
import static org.mastodon.linking.LinkerKeys.DEFAULT_GAP_CLOSING_MAX_FRAME_GAP;
import static org.mastodon.linking.LinkerKeys.DEFAULT_LINKING_FEATURE_PENALTIES;
import static org.mastodon.linking.LinkerKeys.DEFAULT_LINKING_MAX_DISTANCE;
import static org.mastodon.linking.LinkerKeys.DEFAULT_MERGING_FEATURE_PENALTIES;
import static org.mastodon.linking.LinkerKeys.DEFAULT_MERGING_MAX_DISTANCE;
import static org.mastodon.linking.LinkerKeys.DEFAULT_SPLITTING_FEATURE_PENALTIES;
import static org.mastodon.linking.LinkerKeys.DEFAULT_SPLITTING_MAX_DISTANCE;
import static org.mastodon.linking.LinkerKeys.KEY_ALLOW_GAP_CLOSING;
import static org.mastodon.linking.LinkerKeys.KEY_ALLOW_TRACK_MERGING;
import static org.mastodon.linking.LinkerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static org.mastodon.linking.LinkerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static org.mastodon.linking.LinkerKeys.KEY_BLOCKING_VALUE;
import static org.mastodon.linking.LinkerKeys.KEY_CUTOFF_PERCENTILE;
import static org.mastodon.linking.LinkerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static org.mastodon.linking.LinkerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static org.mastodon.linking.LinkerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static org.mastodon.linking.LinkerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static org.mastodon.linking.LinkerKeys.KEY_LINKING_MAX_DISTANCE;
import static org.mastodon.linking.LinkerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static org.mastodon.linking.LinkerKeys.KEY_MERGING_MAX_DISTANCE;
import static org.mastodon.linking.LinkerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static org.mastodon.linking.LinkerKeys.KEY_SPLITTING_MAX_DISTANCE;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.mastodon.graph.Vertex;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.feature.Feature;
import org.mastodon.revised.model.feature.FeatureModel;
import org.mastodon.revised.model.feature.FeatureProjection;
import org.mastodon.revised.model.feature.FeatureProjectors;
import org.mastodon.revised.model.feature.FeatureTarget;

import net.imglib2.RealLocalizable;

public class LinkingUtils
{

	private static final Border RED_BORDER = new LineBorder( Color.RED );

	/**
	 * The key of the edge linking cost feature and projection returned by
	 * {@link ParticleLinkerOp#getLinkCostFeature()}.
	 */
	public static final String LINK_COST_FEATURE_NAME = "Link cost";

	/*
	 * STATIC METHODS - UTILS
	 */

	/**
	 * Returns a new feature wrapping the specified property map, that serves as
	 * an Edge linking cost feature for the linkers of Mastodon. This feature is
	 * expected to be common to all particle linkers.
	 *
	 * @param linkCosts
	 *            the property map containing the link cost values of all edges
	 *            in the model.
	 * @return the link cost feature.
	 */
	public static final < E > Feature< E, Double, DoublePropertyMap< E > > getLinkCostFeature( final DoublePropertyMap< E > linkCosts )
	{
		return new Feature< E, Double, DoublePropertyMap< E > >(
				LINK_COST_FEATURE_NAME, FeatureTarget.EDGE, linkCosts,
				Collections.singletonMap( LINK_COST_FEATURE_NAME, FeatureProjectors.project( linkCosts ) ) );
	}

	/**
	 * Returns a new settings map filled with default values suitable for the
	 * LAP trackers.
	 *
	 * @return a new map.
	 */
	public static final Map< String, Object > getDefaultLAPSettingsMap()
	{
		final Map< String, Object > settings = new HashMap< String, Object >();
		// Linking
		settings.put( KEY_LINKING_MAX_DISTANCE, DEFAULT_LINKING_MAX_DISTANCE );
		settings.put( KEY_LINKING_FEATURE_PENALTIES, new HashMap< String, Double >( DEFAULT_LINKING_FEATURE_PENALTIES ) );
		// Gap closing
		settings.put( KEY_ALLOW_GAP_CLOSING, DEFAULT_ALLOW_GAP_CLOSING );
		settings.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, DEFAULT_GAP_CLOSING_MAX_FRAME_GAP );
		settings.put( KEY_GAP_CLOSING_MAX_DISTANCE, DEFAULT_GAP_CLOSING_MAX_DISTANCE );
		settings.put( KEY_GAP_CLOSING_FEATURE_PENALTIES, new HashMap< String, Double >( DEFAULT_GAP_CLOSING_FEATURE_PENALTIES ) );
		// Track splitting
		settings.put( KEY_ALLOW_TRACK_SPLITTING, DEFAULT_ALLOW_TRACK_SPLITTING );
		settings.put( KEY_SPLITTING_MAX_DISTANCE, DEFAULT_SPLITTING_MAX_DISTANCE );
		settings.put( KEY_SPLITTING_FEATURE_PENALTIES, new HashMap< String, Double >( DEFAULT_SPLITTING_FEATURE_PENALTIES ) );
		// Track merging
		settings.put( KEY_ALLOW_TRACK_MERGING, DEFAULT_ALLOW_TRACK_MERGING );
		settings.put( KEY_MERGING_MAX_DISTANCE, DEFAULT_MERGING_MAX_DISTANCE );
		settings.put( KEY_MERGING_FEATURE_PENALTIES, new HashMap< String, Double >( DEFAULT_MERGING_FEATURE_PENALTIES ) );
		// Others
		settings.put( KEY_BLOCKING_VALUE, DEFAULT_BLOCKING_VALUE );
		settings.put( KEY_ALTERNATIVE_LINKING_COST_FACTOR, DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR );
		settings.put( KEY_CUTOFF_PERCENTILE, DEFAULT_CUTOFF_PERCENTILE );
		// return
		return settings;
	}

	/**
	 * Prints a feature penalty map.
	 *
	 * @param featurePenalties
	 *            the feature penalty map.
	 * @return a string representation of the map.
	 */
	public static String echoFeaturePenalties( final Map< String, Double > featurePenalties )
	{
		String str = "";
		if ( featurePenalties.isEmpty() )
			str += "    - no feature penalties\n";
		else
		{
			str += "    - with feature penalties:\n";
			for ( final String feature : featurePenalties.keySet() )
			{
				str += "      - " + feature.toString() + ": weight = " + String.format( "%.1f", featurePenalties.get( feature ) ) + '\n';
			}
		}
		return str;

	}

	/**
	 * Check that the given map has all some keys. Two String collection allows
	 * specifying that some keys are mandatory, other are optional.
	 *
	 * @param map
	 *            the map to inspect.
	 * @param mandatoryKeys
	 *            the collection of keys that are expected to be in the map. Can
	 *            be <code>null</code>.
	 * @param optionalKeys
	 *            the collection of keys that can be - or not - in the map. Can
	 *            be <code>null</code>.
	 * @param errorHolder
	 *            will be appended with an error message.
	 * @return if all mandatory keys are found in the map, and possibly some
	 *         optional ones, but no others.
	 */
	public static final < T > boolean checkMapKeys( final Map< T, ? > map, Collection< T > mandatoryKeys, Collection< T > optionalKeys, final StringBuilder errorHolder )
	{
		if ( null == optionalKeys )
		{
			optionalKeys = new ArrayList< T >();
		}
		if ( null == mandatoryKeys )
		{
			mandatoryKeys = new ArrayList< T >();
		}
		boolean ok = true;
		final Set< T > keySet = map.keySet();
		for ( final T key : keySet )
		{
			if ( !( mandatoryKeys.contains( key ) || optionalKeys.contains( key ) ) )
			{
				ok = false;
				errorHolder.append( "Map contains unexpected key: " + key + ".\n" );
			}
		}

		for ( final T key : mandatoryKeys )
		{
			if ( !keySet.contains( key ) )
			{
				ok = false;
				errorHolder.append( "Mandatory key " + key + " was not found in the map.\n" );
			}
		}
		return ok;
	}

	/**
	 * Check the presence and the validity of a key in a map, and test it is of
	 * the desired class.
	 *
	 * @param map
	 *            the map to inspect.
	 * @param key
	 *            the key to find.
	 * @param expectedClass
	 *            the expected class of the target value .
	 * @param errorHolder
	 *            will be appended with an error message.
	 * @return true if the key is found in the map, and map a value of the
	 *         desired class.
	 */
	public static final boolean checkParameter( final Map< String, Object > map, final String key, final Class< ? > expectedClass, final StringBuilder errorHolder )
	{
		final Object obj = map.get( key );
		if ( null == obj )
		{
			errorHolder.append( "Parameter " + key + " could not be found in settings map, or is null.\n" );
			return false;
		}
		if ( !expectedClass.isInstance( obj ) )
		{
			errorHolder.append( "Value for parameter " + key + " is not of the right class. Expected " + expectedClass.getName() + ", got " + obj.getClass().getName() + ".\n" );
			return false;
		}
		return true;
	}

	/**
	 * Check the validity of a feature penalty map in a settings map.
	 * <p>
	 * A feature penalty setting is valid if it is either <code>null</code> (not
	 * here, that is) or an actual <code>Map&lt;String, Double&gt;</code>. Then,
	 * all its keys must be Strings and all its values as well.
	 *
	 * @param map
	 *            the map to inspect.
	 * @param featurePenaltiesKey
	 *            the key that should map to a feature penalty map.
	 * @param errorHolder
	 *            will be appended with an error message.
	 * @return true if the feature penalty map is valid.
	 */
	@SuppressWarnings( "rawtypes" )
	public static final boolean checkFeatureMap( final Map< String, Object > map, final String featurePenaltiesKey, final StringBuilder errorHolder )
	{
		final Object obj = map.get( featurePenaltiesKey );
		if ( null == obj ) { return true; // Not here is acceptable
		}
		if ( !( obj instanceof Map ) )
		{
			errorHolder.append( "Feature penalty map is not of the right class. Expected a Map, got a " + obj.getClass().getName() + ".\n" );
			return false;
		}
		boolean ok = true;
		final Map fpMap = ( Map ) obj;
		final Set fpKeys = fpMap.keySet();
		for ( final Object fpKey : fpKeys )
		{
			if ( !( fpKey instanceof String ) )
			{
				ok = false;
				errorHolder.append( "One key (" + fpKey.toString() + ") in the map is not of the right class.\n" +
						"Expected String, got " + fpKey.getClass().getName() + ".\n" );
			}
			final Object fpVal = fpMap.get( fpKey );
			if ( !( fpVal instanceof Double ) )
			{
				ok = false;
				errorHolder.append( "The value for key " + fpVal.toString() + " in the map is not of the right class.\n" +
						"Expected Double, got " + fpVal.getClass().getName() + ".\n" );
			}
		}
		return ok;
	}

	/**
	 * Prints a string representation of a double matrix/
	 *
	 * @param m
	 *            the double matrix to print.
	 */
	public static final void echoMatrix( final double[][] m )
	{
		final int nlines = m.length;
		if ( nlines == 0 )
		{
			System.out.println( "0x0 empty matrix" );
			return;
		}
		final int nrows = m[ 0 ].length;
		double val;
		System.out.print( "L\\C\t" );
		for ( int j = 0; j < nrows; j++ )
		{
			System.out.print( String.format( "%7d: ", j ) );
		}
		System.out.println();
		for ( int i = 0; i < nlines; i++ )
		{
			System.out.print( i + ":\t" );
			for ( int j = 0; j < nrows; j++ )
			{
				val = m[ i ][ j ];
				if ( val > Double.MAX_VALUE / 2 )
					System.out.print( "     B   " );
				else
					System.out.print( String.format( "%7.1f  ", val ) );
			}
			System.out.println();
		}
	}

	/**
	 * Display the cost matrix solved by the Hungarian algorithm in the LAP
	 * approach.
	 *
	 * @param costs
	 *            the cost matrix
	 * @param nSegments
	 *            the number of track segments found in the first step of the
	 *            LAP tracking
	 * @param nSpots
	 *            the number of middle spots to consider
	 * @param blockingValue
	 *            the blocking value for cost
	 * @param solutions
	 *            the Hungarian assignment couple
	 */
	public static final void displayCostMatrix( final double[][] costs, final int nSegments, final int nSpots, final double blockingValue, final int[][] solutions )
	{
		final int width = costs.length;
		final int height = costs[ 0 ].length;
		double val;
		String txt;
		System.out.println( String.format( "Displaying table with: Width = %d, Height = %d", width, height ) );

		// Set column header
		final TableModel model = new DefaultTableModel( height, width )
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getColumnName( final int i )
			{
				if ( i < nSegments )
					return "Ts " + i;
				else if ( i < nSegments + nSpots )
					return "Sp " + ( i - nSegments );
				else
					return "ø";
			}
		};

		// Create table with specific coloring
		final JTable debugTable = new JTable( model )
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Component prepareRenderer( final TableCellRenderer renderer, final int row, final int col )
			{

				final JLabel label = ( JLabel ) super.prepareRenderer( renderer, row, col );
				// Change font color according to matrix parts
				if ( col < nSegments )
				{

					if ( row < nSegments )
						label.setForeground( Color.BLUE ); // Gap closing
					else if ( row < nSegments + nSpots )
						label.setForeground( Color.GREEN.darker() ); // Splitting
					else
						label.setForeground( Color.BLACK ); // Initiating

				}
				else if ( col < nSegments + nSpots )
				{

					if ( row < nSegments )
						label.setForeground( Color.CYAN.darker() ); // Merging
					else if ( row < nSegments + nSpots )
						label.setForeground( Color.RED.darker() ); // Middle
					// block
					else
						label.setForeground( Color.BLACK ); // Initiating

				}
				else
				{
					if ( row < nSegments + nSpots )
						label.setForeground( Color.BLACK ); // Terminating
					else
						label.setForeground( Color.GRAY ); // Bottom right block
				}
				label.setHorizontalAlignment( SwingConstants.CENTER );

				// Change border color according to Hungarian solution
				label.setBorder( null );
				for ( int i = 0; i < solutions.length; i++ )
				{
					final int srow = solutions[ i ][ 0 ];
					final int scol = solutions[ i ][ 1 ];
					if ( row == srow && col == scol )
						label.setBorder( RED_BORDER );
				}

				return label;
			}
		};

		// Set values
		for ( int row = 0; row < height; row++ )
		{
			for ( int col = 0; col < width; col++ )
			{
				val = costs[ row ][ col ];
				if ( val == blockingValue )
					txt = "B";
				else
					txt = String.format( "%.1f", val );
				model.setValueAt( txt, row, col );
			}
		}

		// Row headers
		final TableModel rhm = new AbstractTableModel()
		{
			private static final long serialVersionUID = 1L;

			String headers[] = new String[ 2 * ( nSegments + nSpots ) ];
			{
				for ( int i = 0; i < nSegments; i++ )
					headers[ i ] = "Te " + i;
				for ( int i = nSegments; i < nSegments + nSpots; i++ )
					headers[ i ] = "Sp " + ( i - nSegments );
				for ( int i = nSegments + nSpots; i < headers.length; i++ )
					headers[ i ] = "ø";
			}

			@Override
			public int getColumnCount()
			{
				return 1;
			}

			@Override
			public int getRowCount()
			{
				return headers.length;
			}

			@Override
			public Object getValueAt( final int rowIndex, final int columnIndex )
			{
				return headers[ rowIndex ];
			}
		};
		final JTable rowHeader = new JTable( rhm );
		final Dimension d = rowHeader.getPreferredScrollableViewportSize();
		d.width = rowHeader.getPreferredSize().width;
		rowHeader.setPreferredScrollableViewportSize( d );

		// Set column width
		debugTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		for ( int i = 0; i < debugTable.getColumnCount(); i++ )
		{
			debugTable.getColumnModel().getColumn( i ).setPreferredWidth( 50 );
		}

		// Embed table in scroll pane
		final JScrollPane scrollPane = new JScrollPane( debugTable );
		debugTable.setFillsViewportHeight( true );
		scrollPane.setRowHeaderView( rowHeader );
		final JFrame frame = new JFrame( "Segment cost matrix" );
		frame.getContentPane().add( scrollPane );
		frame.setSize( 800, 600 );
		frame.setVisible( true );
	}

	public static void echoSolutions( final int[][] solutions )
	{
		for ( int i = 0; i < solutions.length; i++ )
			System.out.println( String.format( "%3d: %3d -> %3d", i, solutions[ i ][ 0 ], solutions[ i ][ 1 ] ) );
	}

	public static void displayLAPresults( final int[][] array )
	{
		final Object[][] data = new Object[ array.length ][ array[ 0 ].length ];
		final Object[] headers = new Object[ array[ 0 ].length ];
		for ( int i = 0; i < data.length; i++ )
		{
			for ( int j = 0; j < data[ 0 ].length; j++ )
			{
				data[ i ][ j ] = "" + array[ i ][ j ];
			}
		}
		for ( int i = 0; i < headers.length; i++ )
		{
			headers[ i ] = "" + i;
		}
		final JTable table = new JTable( data, headers );

		final JScrollPane scrollPane = new JScrollPane( table );
		table.setFillsViewportHeight( true );
		final JFrame frame = new JFrame( "Hungarian solution" );
		frame.getContentPane().add( scrollPane );
		frame.setVisible( true );
	}

	/**
	 * Returns the square distance between two {@link RealLocalizable}s.
	 *
	 * @param source
	 *            the source position.
	 * @param target
	 *            the target position.
	 * @return the square distance.
	 */
	public static final < K extends RealLocalizable > double squareDistance( final K source, final K target )
	{

		double d2 = 0.;
		for ( int d = 0; d < source.numDimensions(); d++ )
		{
			final double dx = target.getDoublePosition( d ) - source.getDoublePosition( d );
			d2 += ( dx * dx );
		}
		return d2;
	}

	/**
	 * Returns the normalized difference of the feature value between two
	 * vertices. This value is equal to
	 *
	 * <pre>
	 * | a - b | / ( ( a + b )/2 )
	 * </pre>
	 *
	 * where <code>a</code> and <code>b</code> are the feature value for the
	 * source and target vertex.
	 *
	 * If one of the feature value cannot be found, this method returns
	 * {@link Double#NaN}.
	 *
	 * @param source
	 *            the source vertex.
	 * @param target
	 *            the target vertex.
	 * @param feature
	 *            the feature key.
	 * @param featureModel
	 *            the feature model to read feature values from.
	 * @return the normalized difference.
	 */
	public static < V extends Vertex< ? > > double normalizeDiffCost( final V source, final V target, final String feature, final FeatureModel< V, ? > featureModel )
	{
		final FeatureProjection< V > vertexProjection = featureModel.getVertexProjection( feature );
		if ( !vertexProjection.isSet( source ) || !vertexProjection.isSet( target ) )
			return Double.NaN;
		final double a = vertexProjection.value( source );
		final double b = vertexProjection.value( target );
		if ( a == -b )
			return 0d;
		else
			return Math.abs( a - b ) / ( ( a + b ) / 2 );
	}

	private LinkingUtils()
	{}
}
