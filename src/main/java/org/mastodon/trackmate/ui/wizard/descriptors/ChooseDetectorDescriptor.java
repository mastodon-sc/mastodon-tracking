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

import org.mastodon.detection.mamut.SpotDetectorOp;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.trackmate.PluginProvider;
import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.TrackMate;
import org.mastodon.trackmate.ui.wizard.WizardController;
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

	private final DefaultComboBoxModel< String > model;

	private List< String > names;

	private Map< String, String > descriptions;

	private List< Class< ? extends SpotDetectorOp > > classes;

	private final WizardController controller;

	private String nextDescriptorIdentifier = "Not null"; // FIXME

	private final TrackMate trackmate;

	private final WindowManager windowManager;

	public ChooseDetectorDescriptor( final TrackMate trackmate, final WizardController controller, final WindowManager windowManager )
	{
		this.trackmate = trackmate;
		this.controller = controller;
		this.windowManager = windowManager;
		this.model = new DefaultComboBoxModel<>();
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
		final Settings settings = trackmate.getSettings();
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
		final Class< ? extends SpotDetectorOp > detectorClass = classes.get( names.indexOf( name ) );
		final Settings settings = trackmate.getSettings();
		settings.detector( detectorClass );

		/*
		 * Determine and register the next descriptor.
		 */

		final PluginProvider< SpotDetectorDescriptor > provider = new PluginProvider<>( SpotDetectorDescriptor.class );
		provider.setContext( context() );
		final List< String > detectorPanelNames = provider.getNames();
		for ( final String key : detectorPanelNames )
		{
			final SpotDetectorDescriptor detectorPanel = provider.getInstance( key );
			if ( detectorPanel.getTargetClasses().contains( detectorClass ) )
			{
				controller.registerWizardPanel( detectorPanel );
				detectorPanel.getPanelComponent().setSize( targetPanel.getSize() );
				detectorPanel.setTrackMate( trackmate );
				detectorPanel.setWindowManager( windowManager );
				context().inject( detectorPanel );
				nextDescriptorIdentifier = detectorPanel.getPanelDescriptorIdentifier();
			}
		}
	}

	@Override
	public String getBackPanelDescriptorIdentifier()
	{
		return BoundingBoxDescriptor.IDENTIFIER;
	}

	@Override
	public String getNextPanelDescriptorIdentifier()
	{
		return nextDescriptorIdentifier;
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

			comboBox.addActionListener( ( e ) -> lblInfo.setText( descriptions.get( model.getSelectedItem() ) ) );
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
