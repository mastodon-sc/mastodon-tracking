package org.mastodon.trackmate.ui.wizard.descriptors;

import static org.mastodon.linking.LinkerKeys.DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR;
import static org.mastodon.linking.LinkerKeys.DEFAULT_BLOCKING_VALUE;
import static org.mastodon.linking.LinkerKeys.DEFAULT_CUTOFF_PERCENTILE;
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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusListener;
import java.awt.event.MouseWheelListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.function.BinaryOperator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import org.mastodon.linking.FeatureKey;
import org.mastodon.linking.LinkingUtils;
import org.mastodon.revised.model.feature.Feature;
import org.mastodon.revised.model.feature.FeatureModel;
import org.mastodon.trackmate.ui.wizard.Wizard;
import org.mastodon.trackmate.ui.wizard.util.EverythingDisablerAndReenabler;
import org.mastodon.trackmate.ui.wizard.util.ScrollToFocusListener;
import org.mastodon.trackmate.ui.wizard.util.SelectOnFocusListener;

public class LAPLinkerConfigPanel extends JPanel
{

	private static final long serialVersionUID = -1L;

	private static final ImageIcon ADD_ICON = new ImageIcon( Wizard.class.getResource( "add.png" ) );

	private static final ImageIcon REMOVE_ICON = new ImageIcon( Wizard.class.getResource( "remove.png" ) );

	private static final NumberFormat FORMAT = new DecimalFormat( "0.0" );

	private static final NumberFormat INTEGER_FORMAT = new DecimalFormat( "0" );

	/*
	 * Frame to frame linking fields.
	 */

	private final JFormattedTextField jTextFieldLinkingMaxDistance;

	private final JPanelFeatureSelectionGui jPanelLinkingFeatures;

	/*
	 * Gap-closing fields.
	 */

	private final JCheckBox jCheckBoxAllowGapClosing;

	private final JPanelFeatureSelectionGui jPanelGapClosing;

	private final JFormattedTextField jTextFieldGapClosingMaxFrameInterval;

	private final JFormattedTextField jTextFieldGapClosingMaxDistance;

	private final EverythingDisablerAndReenabler enablerGapClosing;

	/*
	 * Splitting fields.
	 */

	private final JCheckBox jCheckBoxAllowSplitting;

	private final JFormattedTextField jTextFieldSplittingMaxDistance;

	private final JPanelFeatureSelectionGui jPanelSplittingFeatures;

	private final EverythingDisablerAndReenabler enablerSplitting;

	/*
	 * Merging fields.
	 */

	private final JCheckBox jCheckBoxAllowMerging;

	private final JFormattedTextField jTextFieldMergingMaxDistance;

	private final JPanelFeatureSelectionGui jPanelMergingFeatures;

	private final EverythingDisablerAndReenabler enablerMerging;

	/*
	 * Other fields.
	 */

	private final ScrollToFocusListener scrollToFocusListener;

	private final FeatureModel featureModel;

	private final Class< ? > vertexClass;

	public LAPLinkerConfigPanel( final String trackerName, final String spaceUnits, final FeatureModel featureModel, final Class< ? > vertexClass )
	{
		this.featureModel = featureModel;
		this.vertexClass = vertexClass;
		this.scrollToFocusListener = new ScrollToFocusListener( this );

		/*
		 * GUI.
		 */

		final GridBagLayout layout = new GridBagLayout();
		layout.columnWeights = new double[] { 0.1, 0.8, 0.1 };
		setLayout( layout );

		final Font smallFont = getFont().deriveFont( getFont().getSize2D() - 2f );
		final SelectOnFocusListener fl = new SelectOnFocusListener();

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 3;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets( 5, 5, 5, 5 );

		final JLabel jLabelTitle = new JLabel( "Settings for " + trackerName );
		jLabelTitle.setFont( getFont().deriveFont( Font.BOLD ) );
		add( jLabelTitle, gbc );

		gbc.gridy++;
		add( new JSeparator( JSeparator.HORIZONTAL ), gbc );

		/*
		 * Frame to frame linking.
		 */

		final JLabel jLabel2 = new JLabel( "Frame to frame linking:" );
		gbc.gridy = 3;
		jLabel2.setFont( getFont().deriveFont( Font.ITALIC ) );
		add( jLabel2, gbc );

		final JLabel jLabel3 = new JLabel( "Max distance:" );
		jLabel3.setFont( smallFont );
		gbc.gridy++;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		add( jLabel3, gbc );

		jTextFieldLinkingMaxDistance = new JFormattedTextField( FORMAT );
		jTextFieldLinkingMaxDistance.setHorizontalAlignment( JLabel.RIGHT );
		jTextFieldLinkingMaxDistance.addFocusListener( fl );
		jTextFieldLinkingMaxDistance.setFont( smallFont );
		gbc.gridx++;
		add( jTextFieldLinkingMaxDistance, gbc );

		final JLabel jLabelLinkingMaxDistanceUnits = new JLabel( spaceUnits );
		jLabelLinkingMaxDistanceUnits.setFont( smallFont );
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx++;
		add( jLabelLinkingMaxDistanceUnits, gbc );

		final JLabel jLabel4 = new JLabel( "Feature penalties:" );
		jLabel4.setFont( smallFont );
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.gridx = 0;
		gbc.gridy++;
		gbc.gridwidth = 3;
		add( jLabel4, gbc );

		final JScrollPane jScrollPaneLinkingFeatures = new JScrollPane();
		final MouseWheelListener[] l0 = jScrollPaneLinkingFeatures.getMouseWheelListeners();
		jScrollPaneLinkingFeatures.removeMouseWheelListener( l0[ 0 ] );
		jScrollPaneLinkingFeatures.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		jScrollPaneLinkingFeatures.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		jScrollPaneLinkingFeatures.setBorder( null );
		jScrollPaneLinkingFeatures.setViewportBorder( null );
		jPanelLinkingFeatures = new JPanelFeatureSelectionGui( featureModel );
		jScrollPaneLinkingFeatures.setViewportView( jPanelLinkingFeatures );
		gbc.gridy++;
		gbc.fill = GridBagConstraints.BOTH;
		add( jScrollPaneLinkingFeatures, gbc );

		gbc.gridy++;
		add( new JSeparator( JSeparator.HORIZONTAL ), gbc );

		/*
		 * Gap closing
		 */

		final JLabel jLabel5 = new JLabel( "Gap closing:" );
		jLabel5.setFont( getFont().deriveFont( Font.ITALIC ) );
		gbc.gridy++;
		gbc.weighty = 0.;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add( jLabel5, gbc );

		jCheckBoxAllowGapClosing = new JCheckBox( "Allow gap-closing" );
		jCheckBoxAllowGapClosing.setFont( smallFont );
		gbc.gridy++;
		add( jCheckBoxAllowGapClosing, gbc );

		final JPanel panelGapClosing = new JPanel();
		final GridBagLayout layout1 = new GridBagLayout();
		layout1.columnWidths = layout.columnWidths;
		layout1.columnWeights = layout.columnWeights;
		panelGapClosing.setLayout( layout1 );

		final GridBagConstraints gbc1 = new GridBagConstraints();
		gbc1.gridx = 0;
		gbc1.gridy = 0;
		gbc1.gridwidth = 3;
		gbc1.anchor = GridBagConstraints.CENTER;
		gbc1.fill = GridBagConstraints.HORIZONTAL;
		gbc1.insets = new Insets( 5, 5, 5, 5 );

		final JLabel jLabel6 = new JLabel( "Max distance:" );
		jLabel6.setText( "Max distance:" );
		jLabel6.setFont( smallFont );
		gbc1.gridy++;
		gbc1.gridwidth = 1;
		panelGapClosing.add( jLabel6, gbc1 );

		jTextFieldGapClosingMaxDistance = new JFormattedTextField( FORMAT );
		jTextFieldGapClosingMaxDistance.setHorizontalAlignment( JLabel.RIGHT );
		jTextFieldGapClosingMaxDistance.setFont( smallFont );
		jTextFieldGapClosingMaxDistance.addFocusListener( fl );
		gbc1.gridx++;
		panelGapClosing.add( jTextFieldGapClosingMaxDistance, gbc1 );

		final JLabel jLabelGapClosingMaxDistanceUnit = new JLabel( spaceUnits );
		jLabelGapClosingMaxDistanceUnit.setFont( smallFont );
		gbc1.gridx++;
		gbc1.anchor = GridBagConstraints.WEST;
		panelGapClosing.add( jLabelGapClosingMaxDistanceUnit, gbc1 );

		final JLabel jLabel7 = new JLabel( "Max frame gap:" );
		jLabel7.setFont( smallFont );
		gbc1.gridx = 0;
		gbc1.gridy++;
		panelGapClosing.add( jLabel7, gbc1 );

		jTextFieldGapClosingMaxFrameInterval = new JFormattedTextField( INTEGER_FORMAT );
		jTextFieldGapClosingMaxFrameInterval.setHorizontalAlignment( JLabel.RIGHT );
		jTextFieldGapClosingMaxFrameInterval.setFont( smallFont );
		jTextFieldGapClosingMaxFrameInterval.addFocusListener( fl );
		gbc1.gridx++;
		gbc1.anchor = GridBagConstraints.CENTER;
		panelGapClosing.add( jTextFieldGapClosingMaxFrameInterval, gbc1 );

		final JLabel jLabel8 = new JLabel( "Feature penalties:" );
		jLabel8.setFont( smallFont );
		gbc1.gridx = 0;
		gbc1.gridy++;
		gbc1.anchor = GridBagConstraints.WEST;
		panelGapClosing.add( jLabel8, gbc1 );

		final JScrollPane jScrollPaneGapClosingFeatures = new JScrollPane();
		final MouseWheelListener[] l1 = jScrollPaneGapClosingFeatures.getMouseWheelListeners();
		jScrollPaneGapClosingFeatures.removeMouseWheelListener( l1[ 0 ] );
		jScrollPaneGapClosingFeatures.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		jScrollPaneGapClosingFeatures.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		jScrollPaneGapClosingFeatures.setBorder( null );
		jScrollPaneGapClosingFeatures.setViewportBorder( null );
		jPanelGapClosing = new JPanelFeatureSelectionGui( featureModel );
		jScrollPaneGapClosingFeatures.setViewportView( jPanelGapClosing );
		gbc1.gridy++;
		gbc1.gridwidth = 3;
		gbc1.fill = GridBagConstraints.BOTH;
		gbc1.anchor = GridBagConstraints.CENTER;
		panelGapClosing.add( jScrollPaneGapClosingFeatures, gbc1 );

		this.enablerGapClosing = new EverythingDisablerAndReenabler( panelGapClosing, new Class[] { JLabel.class } );
		jCheckBoxAllowGapClosing.addActionListener( ( e ) -> enablerGapClosing.setEnabled( jCheckBoxAllowGapClosing.isSelected() ) );

		gbc.gridy++;
		gbc.gridwidth = 3;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.BOTH;
		add( panelGapClosing, gbc );

		gbc.gridy++;
		add( new JSeparator( JSeparator.HORIZONTAL ), gbc );

		/*
		 * Splitting
		 */

		final JLabel jLabel9 = new JLabel( "Track division:" );
		jLabel9.setFont( getFont().deriveFont( Font.ITALIC ) );
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridy++;
		add( jLabel9, gbc );

		jCheckBoxAllowSplitting = new JCheckBox( "Allow track division" );
		jCheckBoxAllowSplitting.setFont( smallFont );
		gbc.gridy++;
		add( jCheckBoxAllowSplitting, gbc );

		final JPanel panelSplitting = new JPanel();
		final GridBagLayout layout2 = new GridBagLayout();
		layout2.columnWidths = layout.columnWidths;
		layout2.columnWeights = layout.columnWeights;
		panelSplitting.setLayout( layout2 );

		final GridBagConstraints gbc2 = new GridBagConstraints();
		gbc2.gridx = 0;
		gbc2.gridy = 0;
		gbc2.gridwidth = 1;
		gbc2.anchor = GridBagConstraints.CENTER;
		gbc2.fill = GridBagConstraints.HORIZONTAL;
		gbc2.insets = new Insets( 5, 5, 5, 5 );

		final JLabel jLabel10 = new JLabel( "Max distance:" );
		jLabel10.setFont( smallFont );
		panelSplitting.add( jLabel10, gbc2 );

		jTextFieldSplittingMaxDistance = new JFormattedTextField( FORMAT );
		jTextFieldSplittingMaxDistance.setHorizontalAlignment( JLabel.RIGHT );
		jTextFieldSplittingMaxDistance.setFont( smallFont );
		jTextFieldSplittingMaxDistance.addFocusListener( fl );
		gbc2.gridx++;
		panelSplitting.add( jTextFieldSplittingMaxDistance, gbc2 );

		final JLabel jLabelSplittingMaxDistanceUnit = new JLabel( spaceUnits );
		jLabelSplittingMaxDistanceUnit.setFont( smallFont );
		gbc2.gridx++;
		gbc2.anchor = GridBagConstraints.WEST;
		panelSplitting.add( jLabelSplittingMaxDistanceUnit, gbc2 );

		final JLabel jLabel15 = new JLabel( "Feature penalties:" );
		jLabel15.setFont( smallFont );
		gbc2.gridy++;
		gbc2.gridx = 0;
		gbc2.gridwidth = 3;
		gbc2.anchor = GridBagConstraints.CENTER;
		gbc2.fill = GridBagConstraints.HORIZONTAL;
		panelSplitting.add( jLabel15, gbc2 );

		final JScrollPane jScrollPaneSplittingFeatures = new JScrollPane();
		final MouseWheelListener[] l2 = jScrollPaneSplittingFeatures.getMouseWheelListeners();
		jScrollPaneSplittingFeatures.removeMouseWheelListener( l2[ 0 ] );
		jScrollPaneSplittingFeatures.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		jScrollPaneSplittingFeatures.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		jScrollPaneSplittingFeatures.setBorder( null );
		jScrollPaneSplittingFeatures.setViewportBorder( null );
		jPanelSplittingFeatures = new JPanelFeatureSelectionGui( featureModel );
		jScrollPaneSplittingFeatures.setViewportView( jPanelSplittingFeatures );
		gbc2.gridy++;
		gbc2.fill = GridBagConstraints.BOTH;
		panelSplitting.add( jScrollPaneSplittingFeatures, gbc2 );

		this.enablerSplitting = new EverythingDisablerAndReenabler( panelSplitting, new Class[] { JLabel.class } );
		jCheckBoxAllowSplitting.addActionListener( ( e ) -> enablerSplitting.setEnabled( jCheckBoxAllowSplitting.isSelected() ) );

		gbc.gridy++;
		gbc.gridx = 0;
		gbc.gridwidth = 3;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.BOTH;
		add( panelSplitting, gbc );

		gbc.gridy++;
		add( new JSeparator( JSeparator.HORIZONTAL ), gbc );

		/*
		 * Merging
		 */

		final JLabel jLabel12 = new JLabel( "Track fusion:" );
		jLabel12.setFont( getFont().deriveFont( Font.ITALIC ) );
		gbc.gridy++;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add( jLabel12, gbc );

		jCheckBoxAllowMerging = new JCheckBox( "Allow track fusion" );
		jCheckBoxAllowMerging.setFont( smallFont );
		gbc.gridy++;
		add( jCheckBoxAllowMerging, gbc );

		final JPanel panelMerging = new JPanel();
		final GridBagLayout layout3 = new GridBagLayout();
		layout3.columnWidths = layout.columnWidths;
		layout3.columnWeights = layout.columnWeights;
		panelMerging.setLayout( layout3 );

		final GridBagConstraints gbc3 = new GridBagConstraints();
		gbc3.gridx = 0;
		gbc3.gridy = 0;
		gbc3.gridwidth = 1;
		gbc3.anchor = GridBagConstraints.CENTER;
		gbc3.fill = GridBagConstraints.HORIZONTAL;
		gbc3.insets = new Insets( 5, 5, 5, 5 );

		final JLabel jLabel13 = new JLabel( "Max distance:" );
		jLabel13.setFont( smallFont );
		panelMerging.add( jLabel13, gbc3 );

		jTextFieldMergingMaxDistance = new JFormattedTextField( FORMAT );
		jTextFieldMergingMaxDistance.setHorizontalAlignment( JLabel.RIGHT );
		jTextFieldMergingMaxDistance.setFont( smallFont );
		jTextFieldMergingMaxDistance.addFocusListener( fl );
		gbc3.gridx++;
		panelMerging.add( jTextFieldMergingMaxDistance, gbc3 );

		final JLabel jLabelMergingMaxDistanceUnit = new JLabel( spaceUnits );
		jLabelMergingMaxDistanceUnit.setFont( smallFont );
		gbc3.gridx++;
		gbc3.anchor = GridBagConstraints.WEST;
		panelMerging.add( jLabelMergingMaxDistanceUnit, gbc3 );

		final JLabel jLabel16 = new JLabel( "Feature penalties:" );
		jLabel16.setFont( smallFont );
		gbc3.gridx = 0;
		gbc3.gridy++;
		gbc3.gridwidth = 3;
		gbc3.anchor = GridBagConstraints.CENTER;
		panelMerging.add( jLabel16, gbc3 );

		final JScrollPane jScrollPaneMergingFeatures = new JScrollPane();
		final MouseWheelListener[] l3 = jScrollPaneMergingFeatures.getMouseWheelListeners();
		jScrollPaneMergingFeatures.removeMouseWheelListener( l3[ 0 ] );
		jScrollPaneMergingFeatures.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		jScrollPaneMergingFeatures.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		jScrollPaneMergingFeatures.setBorder( null );
		jScrollPaneMergingFeatures.setViewportBorder( null );
		jPanelMergingFeatures = new JPanelFeatureSelectionGui( featureModel );
		jScrollPaneMergingFeatures.setViewportView( jPanelMergingFeatures );
		gbc3.gridy++;
		gbc3.fill = GridBagConstraints.BOTH;
		panelMerging.add( jScrollPaneMergingFeatures, gbc3 );

		this.enablerMerging = new EverythingDisablerAndReenabler( panelMerging, new Class[] { JLabel.class } );
		jCheckBoxAllowMerging.addActionListener( ( e ) -> enablerMerging.setEnabled( jCheckBoxAllowMerging.isSelected() ) );

		gbc.gridy++;
		gbc.gridwidth = 3;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.BOTH;
		add( panelMerging, gbc );

		gbc.gridy++;
		add( new JSeparator( JSeparator.HORIZONTAL ), gbc );

		/*
		 * Add a focus listener to scroll the JScrollPanel to the focused
		 * components to all children.
		 */

		installFocusListener( scrollToFocusListener, this );
	}

	/**
	 * Returns a settings map with the values displayed in this panel.
	 * @param oldSettings
	 *
	 * @return a new map.
	 */
	Map< String, Object > getSettings(final Map< String, Object > oldSettings)
	{
		final Map< String, Object > settings = new HashMap<>( oldSettings );

		// Frame to frame linking
		settings.put( KEY_LINKING_MAX_DISTANCE, ( ( Number ) jTextFieldLinkingMaxDistance.getValue() ).doubleValue() );
		settings.put( KEY_LINKING_FEATURE_PENALTIES, toMap( jPanelLinkingFeatures.getFeaturePenalties() ) );

		// Gap-closing.
		settings.put( KEY_ALLOW_GAP_CLOSING, jCheckBoxAllowGapClosing.isSelected() );
		settings.put( KEY_GAP_CLOSING_MAX_DISTANCE, ( ( Number ) jTextFieldGapClosingMaxDistance.getValue() ).doubleValue() );
		settings.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, ( ( Number ) jTextFieldGapClosingMaxFrameInterval.getValue() ).intValue() );
		settings.put( KEY_GAP_CLOSING_FEATURE_PENALTIES, toMap( jPanelGapClosing.getFeaturePenalties() ) );

		// Track splitting.
		settings.put( KEY_ALLOW_TRACK_SPLITTING, jCheckBoxAllowSplitting.isSelected() );
		settings.put( KEY_SPLITTING_MAX_DISTANCE, ( ( Number ) jTextFieldSplittingMaxDistance.getValue() ).doubleValue() );
		settings.put( KEY_SPLITTING_FEATURE_PENALTIES, toMap( jPanelSplittingFeatures.getFeaturePenalties() ) );

		// Track merging.
		settings.put( KEY_ALLOW_TRACK_MERGING, jCheckBoxAllowMerging.isSelected() );
		settings.put( KEY_MERGING_MAX_DISTANCE, ( ( Number ) jTextFieldMergingMaxDistance.getValue() ).doubleValue() );
		settings.put( KEY_MERGING_FEATURE_PENALTIES, toMap( jPanelMergingFeatures.getFeaturePenalties() ) );

		// Other - use defaults.
		settings.put( KEY_BLOCKING_VALUE, DEFAULT_BLOCKING_VALUE );
		settings.put( KEY_ALTERNATIVE_LINKING_COST_FACTOR, DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR );
		settings.put( KEY_CUTOFF_PERCENTILE, DEFAULT_CUTOFF_PERCENTILE );

		return settings;
	}

	private static final Map<FeatureKey, Double> toMap( final List< FeaturePenalty > featurePenalties )
	{
		final Map<FeatureKey, Double> fp = new HashMap<>();
		for ( final FeaturePenalty featurePenalty : featurePenalties )
			fp.put( featurePenalty.key, Double.valueOf( featurePenalty.weight ) );

		return fp;
	}

	/**
	 * Displays the content of the specified settings map onto this panel. This
	 * method will throw an error if some expected keys are missing in the
	 * specified map.
	 *
	 * @param settings
	 *            the map of settings.
	 */
	void echoSettings( final Map< String, Object > settings )
	{
		// Frame to frame linking
		jTextFieldLinkingMaxDistance.setValue( settings.get( KEY_LINKING_MAX_DISTANCE ) );
		@SuppressWarnings( "unchecked" )
		final Map< FeatureKey, Double > linkingPenalties = ( Map< FeatureKey, Double > ) settings.get( KEY_LINKING_FEATURE_PENALTIES );
		jPanelLinkingFeatures.removeAllPanels();
		for ( final FeatureKey fk : linkingPenalties.keySet() )
			if (LinkingUtils.isFeatureKeyValid( fk, featureModel, vertexClass ))
				jPanelLinkingFeatures.addPanel( new FeaturePenalty( fk, linkingPenalties.get( fk ) ) );

		// Gap-closing.
		jCheckBoxAllowGapClosing.setSelected( ( boolean ) settings.get( KEY_ALLOW_GAP_CLOSING ) );
		enablerGapClosing.setEnabled( jCheckBoxAllowGapClosing.isSelected() );
		jTextFieldGapClosingMaxDistance.setValue( settings.get( KEY_GAP_CLOSING_MAX_DISTANCE ) );
		jTextFieldGapClosingMaxFrameInterval.setValue( settings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP ) );
		@SuppressWarnings( "unchecked" )
		final Map<FeatureKey, Double> gapClosingPenalties = ( Map<FeatureKey, Double> ) settings.get( KEY_GAP_CLOSING_FEATURE_PENALTIES );
		jPanelGapClosing.removeAllPanels();
		for ( final FeatureKey fk : gapClosingPenalties.keySet() )
			if (LinkingUtils.isFeatureKeyValid( fk, featureModel, vertexClass ))
				jPanelGapClosing.addPanel( new FeaturePenalty( fk, gapClosingPenalties.get( fk ) ) );

		// Track splitting.
		jCheckBoxAllowSplitting.setSelected( ( boolean ) settings.get( KEY_ALLOW_TRACK_SPLITTING ) );
		enablerSplitting.setEnabled( jCheckBoxAllowSplitting.isSelected() );
		jTextFieldSplittingMaxDistance.setValue( settings.get( KEY_SPLITTING_MAX_DISTANCE ) );
		@SuppressWarnings( "unchecked" )
		final Map<FeatureKey, Double> splittingPenalties = ( Map<FeatureKey, Double> ) settings.get( KEY_SPLITTING_FEATURE_PENALTIES );
		jPanelSplittingFeatures.removeAllPanels();
		for ( final FeatureKey fk : splittingPenalties.keySet() )
			if (LinkingUtils.isFeatureKeyValid( fk, featureModel, vertexClass ))
				jPanelSplittingFeatures.addPanel( new FeaturePenalty( fk, splittingPenalties.get( fk ) ) );

		// Track merging.
		jCheckBoxAllowMerging.setSelected( ( boolean ) settings.get( KEY_ALLOW_TRACK_MERGING ) );
		enablerMerging.setEnabled( jCheckBoxAllowMerging.isSelected() );
		jTextFieldMergingMaxDistance.setValue( settings.get( KEY_MERGING_MAX_DISTANCE ) );
		@SuppressWarnings( "unchecked" )
		final Map<FeatureKey, Double> mergingPenalties = ( Map<FeatureKey, Double> ) settings.get( KEY_MERGING_FEATURE_PENALTIES );
		jPanelMergingFeatures.removeAllPanels();
		for ( final FeatureKey fk : mergingPenalties.keySet() )
			if (LinkingUtils.isFeatureKeyValid( fk, featureModel, vertexClass ))
				jPanelMergingFeatures.addPanel( new FeaturePenalty( fk, mergingPenalties.get( fk ) ) );
	}

	/**
	 * Installs the specified focus listener on all the children of the
	 * specified container, recursively.
	 *
	 * @param focusListener
	 *            the focus listener to install.
	 * @param container
	 *            the container to install it on.
	 */
	private void installFocusListener( final FocusListener focusListener, final Container container )
	{
		final Component[] components = container.getComponents();
		for ( final Component component : components )
		{
			component.addFocusListener( focusListener );
			if ( component instanceof Container )
				installFocusListener( focusListener, ( Container ) component );
		}
	}

	class JPanelFeatureSelectionGui extends JPanel
	{

		private static final long serialVersionUID = 1l;

		private final JPanel jPanelButtons;

		private final JButton jButtonRemove;

		private final JButton jButtonAdd;

		private final Stack< JPanelFeaturePenalty > featurePanels = new Stack< JPanelFeaturePenalty >();

		private final FeatureModel featureModel;

		private int index;

		public JPanelFeatureSelectionGui( final FeatureModel featureModel )
		{
			this.featureModel = featureModel;
			setLayout( new BoxLayout( this, BoxLayout.PAGE_AXIS ) );

			jPanelButtons = new JPanel();
			add( jPanelButtons );
			jPanelButtons.setLayout( new BoxLayout( jPanelButtons, BoxLayout.LINE_AXIS ) );
			jPanelButtons.add( Box.createHorizontalGlue() );

			jButtonRemove = new JButton( REMOVE_ICON );
			jPanelButtons.add( jButtonRemove );
			jButtonRemove.addActionListener( ( e ) -> removeButtonPushed() );

			jButtonAdd = new JButton( ADD_ICON );
			jPanelButtons.add( jButtonAdd );
			jButtonAdd.addActionListener( ( e ) -> addButtonPushed() );

			index = -1;
		}

		private List< FeaturePenalty > getFeaturePenalties()
		{
			final List< FeaturePenalty > featurePenalties = new ArrayList<>();
			for ( final JPanelFeaturePenalty panel : featurePanels )
				featurePenalties.add( panel.getFeaturePenalty() );

			return featurePenalties;
		}

		private void addPanel( final FeaturePenalty featurePenalty )
		{
			if ( !LinkingUtils.isFeatureKeyValid( featurePenalty.key, featureModel, vertexClass ) )
				return;

			final JPanelFeaturePenalty panel = new JPanelFeaturePenalty( getAvailableFeatureKeys(), featurePenalty.key, featurePenalty.weight );
			panel.setMaximumSize( new Dimension( 5000, 30 ) );
			installFocusListener( scrollToFocusListener, panel );

			featurePanels.push( panel );
			remove( jPanelButtons );
			add( panel );
			add( Box.createVerticalStrut( 5 ) );
			add( jPanelButtons );
			LAPLinkerConfigPanel.this.revalidate();
			jButtonAdd.requestFocusInWindow();
		}

		private void addButtonPushed()
		{
			final List< FeatureKey > featureKeys = getAvailableFeatureKeys();
			if ( featureKeys.isEmpty() )
				return;

			index = index + 1;
			if ( index >= featureKeys.size() )
				index = 0;

			final FeatureKey featureKey = featureKeys.get( index );
			final FeaturePenalty featurePenalty = new FeaturePenalty( featureKey, 1.0 );
			addPanel( featurePenalty );
		}

		private void removeButtonPushed()
		{
			if ( featurePanels.isEmpty() )
				return;

			final JPanelFeaturePenalty panel = featurePanels.pop();
			remove( panel );
			LAPLinkerConfigPanel.this.revalidate();
			jButtonRemove.requestFocusInWindow();
		}

		private void removeAllPanels()
		{
			while ( !featurePanels.isEmpty() )
			{
				final JPanelFeaturePenalty panel = featurePanels.pop();
				remove( panel );
			}
			LAPLinkerConfigPanel.this.revalidate();
		}
	}

	private List< FeatureKey > getAvailableFeatureKeys()
	{
		final List< FeatureKey > featureKeys = new ArrayList<>();
		final Set< Feature< ?, ? > > featureSet = Optional.ofNullable( featureModel.getFeatureSet( vertexClass ) ).orElse( Collections.emptySet() );
		for ( final Feature< ?, ? > feature : featureSet )
			for ( final String projectionKey : feature.getProjections().keySet() )
				featureKeys.add( new FeatureKey( feature.getKey(), projectionKey ) );

		featureKeys.sort( ( a, b ) -> a.toString().compareTo( b.toString() ) );
		return featureKeys;
	}

	private class JPanelFeaturePenalty extends javax.swing.JPanel
	{

		private static final long serialVersionUID = 1l;

		private final JComboBox< FeatureKey > jComboBoxFeatureKeys;

		private final JFormattedTextField jTextFieldFeatureWeight;

		public JPanelFeaturePenalty( final List< FeatureKey > featureKeys, final FeatureKey selectedFeatureKey, final double selectedWeight )
		{
			setLayout( new BoxLayout( this, BoxLayout.LINE_AXIS ) );
			final Font smallFont = getFont().deriveFont( getFont().getSize2D() - 2f );

			final BinaryOperator< FeatureKey > longest = ( t, u ) -> t.toString().length() > u.toString().length() ? t : u;
			final FeatureKey prototypeDisplayValue = featureKeys.stream().reduce( new FeatureKey( "", "" ), longest ) ;

			jComboBoxFeatureKeys = new JComboBox<>( featureKeys.toArray( new FeatureKey[] {} ) );
			jComboBoxFeatureKeys.setFont( smallFont );
			jComboBoxFeatureKeys.setSelectedIndex( featureKeys.indexOf( selectedFeatureKey ) );
			jComboBoxFeatureKeys.setPrototypeDisplayValue( prototypeDisplayValue );
			add( jComboBoxFeatureKeys );

			add( Box.createHorizontalStrut( 5 ) );

			jTextFieldFeatureWeight = new JFormattedTextField( FORMAT );
			jTextFieldFeatureWeight.setValue( selectedWeight );
			jTextFieldFeatureWeight.setFont( smallFont );
			jTextFieldFeatureWeight.setHorizontalAlignment( JFormattedTextField.RIGHT );
			jTextFieldFeatureWeight.addFocusListener( new SelectOnFocusListener() );
			add( jTextFieldFeatureWeight );
		}

		private FeaturePenalty getFeaturePenalty()
		{
			final FeaturePenalty featurePenalty = new FeaturePenalty(
					( FeatureKey ) jComboBoxFeatureKeys.getSelectedItem(),
					( ( Number ) jTextFieldFeatureWeight.getValue() ).doubleValue() );
			return featurePenalty;
		}
	}

	private static class FeaturePenalty
	{

		public final double weight;

		public final FeatureKey key;

		public FeaturePenalty( final FeatureKey key, final double weight )
		{
			this.key = key;
			this.weight = weight;
		}
	}

	@SuppressWarnings( "unchecked" )
	public static final String echoSettingsMap( final Map< String, Object > sm, final String units )
	{
		final StringBuilder str = new StringBuilder();

		str.append( "  - linking conditions:\n" );
		str.append( String.format( "      - max distance: %.1f %s\n", ( double ) sm.get( KEY_LINKING_MAX_DISTANCE ), units ) );
		str.append( echoFeaturePenalties( ( Map< FeatureKey, Double > ) sm.get( KEY_LINKING_FEATURE_PENALTIES ) ) );

		if ( ( Boolean ) sm.get( KEY_ALLOW_GAP_CLOSING ) )
		{
			str.append( "  - gap-closing conditions:\n" );
			str.append( String.format( "      - max distance: %.1f %s\n", ( double ) sm.get( KEY_GAP_CLOSING_MAX_DISTANCE ), units ) );
			str.append( String.format( "      - max frame gap: %d\n", ( int ) sm.get( KEY_GAP_CLOSING_MAX_FRAME_GAP ) ) );
			str.append( echoFeaturePenalties( ( Map< FeatureKey, Double > ) sm.get( KEY_GAP_CLOSING_FEATURE_PENALTIES ) ) );
		}
		else
		{
			str.append( "  - gap-closing not allowed.\n" );
		}

		if ( ( Boolean ) sm.get( KEY_ALLOW_TRACK_SPLITTING ) )
		{
			str.append( "  - track division conditions:\n" );
			str.append( String.format( "      - max distance: %.1f %s\n", ( double ) sm.get( KEY_SPLITTING_MAX_DISTANCE ), units ) );
			str.append( echoFeaturePenalties( ( Map< FeatureKey, Double > ) sm.get( KEY_SPLITTING_FEATURE_PENALTIES ) ) );
		}
		else
		{
			str.append( "  - track division not allowed.\n" );
		}

		if ( ( Boolean ) sm.get( KEY_ALLOW_TRACK_MERGING ) )
		{
			str.append( "  - track fusion conditions:\n" );
			str.append( String.format( "      - max distance: %.1f  %s\n", ( double ) sm.get( KEY_MERGING_MAX_DISTANCE ), units ) );
			str.append( echoFeaturePenalties( ( Map< FeatureKey, Double > ) sm.get( KEY_MERGING_FEATURE_PENALTIES ) ) );
		}
		else
		{
			str.append( "  - track fusion not allowed.\n" );
		}

		return str.toString();
	}

	private static final String echoFeaturePenalties( final Map< FeatureKey, Double > featurePenalties )
	{
		String str = "";
		if ( featurePenalties.isEmpty() )
			str += "      - no feature penalties\n";
		else
		{
			str += "      - with feature penalties:\n";
			for ( final FeatureKey feature : featurePenalties.keySet() )
			{
				str += "          - " + feature.toString() + ": weight = " + String.format( "%.1f", featurePenalties.get( feature ) ) + '\n';
			}
		}
		return str;

	}
}
