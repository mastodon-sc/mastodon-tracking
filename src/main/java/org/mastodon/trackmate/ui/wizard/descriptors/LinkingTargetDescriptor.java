package org.mastodon.trackmate.ui.wizard.descriptors;

import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;
import static org.mastodon.linking.LinkerKeys.KEY_DO_LINK_SELECTION;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mastodon.model.SelectionListener;
import org.mastodon.model.SelectionModel;
import org.mastodon.revised.mamut.MainWindow;
import org.mastodon.revised.mamut.MamutProject;
import org.mastodon.revised.mamut.MamutProjectIO;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.TrackMate;
import org.mastodon.trackmate.ui.wizard.WizardPanelDescriptor;
import org.scijava.Context;

import bdv.tools.brightness.SliderPanel;
import bdv.util.BoundedValue;
import mpicbg.spim.data.SpimDataException;

public class LinkingTargetDescriptor extends WizardPanelDescriptor
{

	private final TrackMate trackmate;

	private final SelectionModel< Spot, Link > selectionModel;

	public LinkingTargetDescriptor( final TrackMate trackmate, final WindowManager windowManager )
	{
		this.trackmate = trackmate;
		this.selectionModel = windowManager.getAppModel().getSelectionModel();
		final int nTimepoints = windowManager.getAppModel().getMaxTimepoint() - windowManager.getAppModel().getMinTimepoint();
		this.targetPanel = new LinkingTargetPanel( nTimepoints );
	}

	@Override
	public void aboutToDisplayPanel()
	{
		final LinkingTargetPanel panel = ( ( LinkingTargetPanel ) targetPanel );
		selectionModel.listeners().add( panel );

		final Settings settings = trackmate.getSettings();
		final Map< String, Object > linkerSettings = settings.values.getLinkerSettings();
		final boolean doLinkSelection = ( boolean ) linkerSettings.get( KEY_DO_LINK_SELECTION );
		final int minT = ( int ) linkerSettings.get( KEY_MIN_TIMEPOINT );
		final int maxT = ( int ) linkerSettings.get( KEY_MAX_TIMEPOINT );

		panel.btnAllSpots.setSelected( !doLinkSelection );
		panel.minT.setCurrentValue( minT );
		panel.maxT.setCurrentValue( maxT );
	}

	@Override
	public void aboutToHidePanel()
	{
		final LinkingTargetPanel panel = ( ( LinkingTargetPanel ) targetPanel );
		selectionModel.listeners().remove( panel );

		final Settings settings = trackmate.getSettings();
		final Map< String, Object > linkerSettings = settings.values.getLinkerSettings();
		linkerSettings.put( KEY_DO_LINK_SELECTION, !panel.btnAllSpots.isSelected() );
		linkerSettings.put( KEY_MIN_TIMEPOINT, panel.minT.getCurrentValue() );
		linkerSettings.put( KEY_MAX_TIMEPOINT, panel.maxT.getCurrentValue() );
	}

	private class LinkingTargetPanel extends JPanel implements SelectionListener
	{

		private static final long serialVersionUID = 1L;

		private final BoundedValue minT;

		private final BoundedValue maxT;

		private final JRadioButton btnAllSpots;

		private final JLabel lblInfo;

		public LinkingTargetPanel( final int nTimepoints )
		{
			final GridBagLayout layout = new GridBagLayout();
			layout.columnWidths = new int[] { 80, 80 };
			layout.columnWeights = new double[] { 0.5, 0.5 };
			layout.rowHeights = new int[] { 0, 0, 0, 0, 0, 30 };
			layout.rowWeights = new double[] { 0., 1., 0., 0., 0., 0., 1.0 };
			setLayout( layout );

			final GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.gridwidth = 2;
			gbc.anchor = GridBagConstraints.BASELINE_LEADING;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets( 5, 5, 5, 5 );

			final JLabel title = new JLabel( "Linker target." );
			title.setFont( getFont().deriveFont( Font.BOLD ) );
			add( title, gbc );

			final JLabel lblPick = new JLabel( "Perform linking on:" );
			gbc.gridy = 1;
			gbc.anchor = GridBagConstraints.SOUTHWEST;
			add( lblPick, gbc );

			gbc.gridy = 2;
			this.btnAllSpots = new JRadioButton( "All spots, between time-points:" );
			add( btnAllSpots, gbc );

			gbc.gridy = 3;
			this.minT = new BoundedValue( 0, nTimepoints, 0 );
			final SliderPanel tMinPanel = new SliderPanel( "t min", minT, 1 );
			add( tMinPanel, gbc );

			gbc.gridy = 4;
			this.maxT = new BoundedValue( 0, nTimepoints, nTimepoints );
			final SliderPanel tMaxPanel = new SliderPanel( "t max", maxT, 1 );
			add( tMaxPanel, gbc );

			gbc.gridy = 5;
			final JRadioButton btnSelection = new JRadioButton( "Spots in selection." );
			add( btnSelection, gbc );

			gbc.gridy = 6;
			gbc.anchor = GridBagConstraints.NORTHWEST;
			lblInfo = new JLabel( " " );
			lblInfo.setFont( getFont().deriveFont( getFont().getSize2D() - 2f ) );
			lblInfo.setVerticalAlignment( JLabel.TOP );
			add( lblInfo, gbc );

			final ButtonGroup buttonGroup = new ButtonGroup();
			buttonGroup.add( btnAllSpots );
			buttonGroup.add( btnSelection );

			btnAllSpots.addItemListener( ( e ) -> {
				LinkingTargetDescriptor.setEnabled( tMinPanel, btnAllSpots.isSelected() );
				LinkingTargetDescriptor.setEnabled( tMaxPanel, btnAllSpots.isSelected() );
				selectionChanged();
			} );
		}

		@Override
		public void selectionChanged()
		{
			if ( btnAllSpots.isSelected() )
			{
				lblInfo.setText( " " );
			}
			else
			{
				new Thread( "Investigate selection thread" )
				{
					@Override
					public void run()
					{
						final int nSpots = selectionModel.getSelectedVertices().size();
						if ( nSpots == 0 )
						{
							SwingUtilities.invokeLater( () -> lblInfo.setText( "Selection is empty." ) );
							return;
						}
						int tmin = Integer.MAX_VALUE;
						int tmax = Integer.MIN_VALUE;
						for ( final Spot spot : selectionModel.getSelectedVertices() )
						{
							if ( spot.getTimepoint() > tmax )
								tmax = spot.getTimepoint();
							if ( spot.getTimepoint() < tmin )
								tmin = spot.getTimepoint();
						}
						final int tmin2 = tmin;
						final int tmax2 = tmax;
						SwingUtilities.invokeLater( () -> lblInfo.setText( "Selection has " + nSpots
								+ " spots, from t = " + tmin2 + " to t = " + tmax2 + "." ) );
					}
				}.start();
			}
		}
	}

	private static void setEnabled( final Component component, final boolean enabled )
	{
		component.setEnabled( enabled );
		if ( component instanceof Container )
		{
			for ( final Component child : ( ( Container ) component ).getComponents() )
			{
				setEnabled( child, enabled );
			}
		}
	}

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException, SpimDataException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		Locale.setDefault( Locale.ROOT );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		final Context context = new Context();
		final String projectFile = "../TrackMate3/samples/mamutproject";

		final WindowManager windowManager = new WindowManager( context );
		final MainWindow mw = new MainWindow( windowManager );

		final MamutProject project = new MamutProjectIO().load( projectFile );
		windowManager.getProjectManager().open( project );

		mw.setVisible( true );

		final Settings settings = new Settings();
		final TrackMate trackmate = new TrackMate( settings, windowManager.getAppModel().getModel(), windowManager.getAppModel().getSelectionModel() );
		final LinkingTargetDescriptor desc = new LinkingTargetDescriptor( trackmate, windowManager );

		final JFrame frame = new JFrame( "Test" );
		frame.getContentPane().add( desc.targetPanel );
		frame.setSize( 300, 600 );
		desc.aboutToDisplayPanel();
		frame.setVisible( true );
	}
}
