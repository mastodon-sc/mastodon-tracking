package org.mastodon.trackmate.ui.wizard.descriptors;

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
import java.util.List;
import java.util.Stack;

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

import org.mastodon.revised.model.feature.FeatureModel;
import org.mastodon.revised.model.feature.FeatureTarget;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.trackmate.ui.wizard.util.EverythingDisablerAndReenabler;
import org.mastodon.trackmate.ui.wizard.util.ScrollToFocusListener;
import org.mastodon.trackmate.ui.wizard.util.SelectOnFocusListener;

public class LAPLinkerConfigPanel extends JPanel
{

	private static final long serialVersionUID = -1L;

	private static final ImageIcon ADD_ICON = new ImageIcon( JPanelFeatureSelectionGui.class.getResource( "../add.png" ) );

	private static final ImageIcon REMOVE_ICON = new ImageIcon( JPanelFeatureSelectionGui.class.getResource( "../remove.png" ) );

	private static final NumberFormat FORMAT = new DecimalFormat( "0.0" );

	private JFormattedTextField jTextFieldSplittingMaxDistance;

	private JCheckBox jCheckBoxAllowSplitting;

	private JPanelFeatureSelectionGui jPanelGapClosing;

	private JPanelFeatureSelectionGui jPanelMergingFeatures;

	private JPanelFeatureSelectionGui jPanelLinkingFeatures;

	private JPanelFeatureSelectionGui jPanelSplittingFeatures;

	private JScrollPane jScrollPaneMergingFeatures;

	private JFormattedTextField jTextFieldMergingMaxDistance;

	private JCheckBox jCheckBoxAllowMerging;

	private JScrollPane jScrollPaneSplittingFeatures;

	private JScrollPane jScrollPaneGapClosingFeatures;

	private JFormattedTextField jTextFieldGapClosingMaxFrameInterval;

	private JFormattedTextField jTextFieldGapClosingMaxDistance;

	private JCheckBox jCheckBoxAllowGapClosing;

	private JFormattedTextField jTextFieldLinkingMaxDistance;

	private final ScrollToFocusListener scrollToFocusListener;

	public LAPLinkerConfigPanel( final String trackerName, final String spaceUnits, final FeatureModel< Spot, Link > featureModel )
	{
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

		jTextFieldGapClosingMaxFrameInterval = new JFormattedTextField( FORMAT );
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

		jScrollPaneGapClosingFeatures = new JScrollPane();
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

		final EverythingDisablerAndReenabler enabler1 = new EverythingDisablerAndReenabler( panelGapClosing, new Class[] { JLabel.class } );
		jCheckBoxAllowGapClosing.addActionListener( ( e ) -> enabler1.setEnabled( jCheckBoxAllowGapClosing.isSelected() ) );

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

		jScrollPaneSplittingFeatures = new JScrollPane();
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

		final EverythingDisablerAndReenabler enabler2 = new EverythingDisablerAndReenabler( panelSplitting, new Class[] { JLabel.class } );
		jCheckBoxAllowSplitting.addActionListener( ( e ) -> enabler2.setEnabled( jCheckBoxAllowSplitting.isSelected() ) );

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

		jScrollPaneMergingFeatures = new JScrollPane();
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

		final EverythingDisablerAndReenabler enabler3 = new EverythingDisablerAndReenabler( panelMerging, new Class[] { JLabel.class } );
		jCheckBoxAllowMerging.addActionListener( ( e ) -> enabler3.setEnabled( jCheckBoxAllowMerging.isSelected() ) );

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

	private class JPanelFeatureSelectionGui extends JPanel
	{

		private static final long serialVersionUID = 1l;

		private final JPanel jPanelButtons;

		private final JButton jButtonRemove;

		private final JButton jButtonAdd;

		private final Stack< JPanelFeaturePenalty > featurePanels = new Stack< JPanelFeaturePenalty >();

		private final FeatureModel< Spot, Link > featureModel;

		private int index;

		public JPanelFeatureSelectionGui( final FeatureModel< Spot, Link > featureModel )
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

		private void addButtonPushed()
		{
			final List< String > projectionKeys = new ArrayList<>( featureModel.getProjectionKeys( FeatureTarget.VERTEX ) );
			if ( projectionKeys.isEmpty() )
				return;

			index = index + 1;
			if ( index >= projectionKeys.size() )
				index = 0;

			final String projectionKey = projectionKeys.get( index );
			final JPanelFeaturePenalty panel = new JPanelFeaturePenalty( projectionKeys, projectionKey, 1.0 );
			panel.setMaximumSize( new Dimension( 500, 30 ) );
			installFocusListener( scrollToFocusListener, panel );

			featurePanels.push( panel );
			remove( jPanelButtons );
			add( panel );
			add( jPanelButtons );
			LAPLinkerConfigPanel.this.revalidate();
			jButtonAdd.requestFocusInWindow();
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
	}

	private class JPanelFeaturePenalty extends javax.swing.JPanel
	{

		private static final long serialVersionUID = 1l;

		private final JComboBox< String > jComboBoxFeature;

		private final JFormattedTextField jTextFieldFeatureWeight;

		public JPanelFeaturePenalty( final List< String > features, final String selectedFeature, final double selectedPenalty )
		{
			setLayout( new BoxLayout( this, BoxLayout.LINE_AXIS ) );
			final Font smallFont = getFont().deriveFont( getFont().getSize2D() - 2f );

			jComboBoxFeature = new JComboBox< String >( features.toArray( new String[] {} ) );
			jComboBoxFeature.setFont( smallFont );
			jComboBoxFeature.setSelectedIndex( features.indexOf( selectedFeature ) );
			add( jComboBoxFeature );

			jTextFieldFeatureWeight = new JFormattedTextField( FORMAT );
			jTextFieldFeatureWeight.setValue( selectedPenalty );
			jTextFieldFeatureWeight.setFont( smallFont );
			jTextFieldFeatureWeight.setHorizontalAlignment( JFormattedTextField.RIGHT );
			jTextFieldFeatureWeight.addFocusListener( new SelectOnFocusListener() );
			add( jTextFieldFeatureWeight );
		}

		private FeaturePenalty getFeaturePenalty()
		{
			final FeaturePenalty featurePenalty = new FeaturePenalty();
			featurePenalty.feature = ( String ) jComboBoxFeature.getSelectedItem();
			featurePenalty.penalty = ( ( Number ) jTextFieldFeatureWeight.getValue() ).doubleValue();
			return featurePenalty;
		}
	}

	private static class FeaturePenalty
	{
		public String feature;

		public double penalty;
	}
}
