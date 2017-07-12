package org.mastodon.trackmate.ui.wizard.descriptors;

import static org.mastodon.detection.DetectorKeys.KEY_SETUP_ID;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.mastodon.revised.bdv.SharedBigDataViewerData;
import org.mastodon.revised.mamut.BdvManager.BdvWindow;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.ui.boundingbox.BoundingBoxOverlay;
import org.mastodon.trackmate.ui.boundingbox.BoundingBoxOverlay.DisplayMode;
import org.mastodon.trackmate.ui.wizard.WizardPanelDescriptor;

import bdv.tools.boundingbox.BoxSelectionPanel;
import bdv.util.ModifiableInterval;
import bdv.viewer.Source;
import bdv.viewer.ViewerFrame;
import bdv.viewer.ViewerPanel;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.util.Intervals;

public class BoundingBoxDescriptor extends WizardPanelDescriptor
{

	public static final String IDENTIFIER = "Setup bounding-box";

	private final Settings settings;

	private final WindowManager wm;

	private BoundingBoxOverlay boxOverlay;

	private ViewerPanel viewer;

	private int previousSetupID = -1;

	final ModifiableInterval roi;

	public BoundingBoxDescriptor( final Settings settings, final WindowManager wm )
	{
		this.settings = settings;
		this.wm = wm;
		this.panelIdentifier = IDENTIFIER;
		this.roi = new ModifiableInterval( 3 );
		final long[] a = new long[] { 1000l, 1000l, 1000l };
		roi.set( new FinalInterval( a, a ) );
		this.targetPanel = new BoundingBoxPanel();
	}

	@Override
	public void aboutToDisplayPanel()
	{
		final BoundingBoxPanel panel = ( BoundingBoxPanel ) targetPanel;
		toggleBoundingBox( panel.useRoi.isSelected() );

		final int setupID = ( int ) settings.values.getDetectorSettings().get( KEY_SETUP_ID );
		if ( setupID != previousSetupID )
		{
			final Interval rangeInterval = getRangeInterval( setupID );
			roi.set( rangeInterval );
			panel.boxSelectionPanel.setBoundsInterval( rangeInterval );
			panel.boxSelectionPanel.updateSliders( rangeInterval );
			previousSetupID = setupID;
		}
	}

	@Override
	public String getBackPanelDescriptorIdentifier()
	{
		return SetupIdDecriptor.IDENTIFIER;
	}

	@Override
	public String getNextPanelDescriptorIdentifier()
	{
		return Descriptor1.ID;
	}

	private class BoundingBoxPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		private final BoxSelectionPanel boxSelectionPanel;

		private final BoxModePanel boxModePanel;

		private final JCheckBox useRoi;

		private BoundingBoxPanel()
		{
			final GridBagLayout layout = new GridBagLayout();
			layout.columnWidths = new int[] { 80 };
			layout.columnWeights = new double[] { 1. };
			setLayout( layout );

			final GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weighty = 1.;
			gbc.insets = new Insets( 5, 5, 5, 5 );

			final JLabel lblTitle = new JLabel( "Region of interest." );
			lblTitle.setFont( getFont().deriveFont( Font.BOLD ) );
			add( lblTitle, gbc );

			gbc.gridy++;
			gbc.anchor = GridBagConstraints.CENTER;
			this.useRoi = new JCheckBox( "Process only a ROI.", false );
			useRoi.addActionListener( ( e ) -> toggleBoundingBox( useRoi.isSelected() ) );
			add( useRoi, gbc );

			gbc.gridy++;
			this.boxSelectionPanel = new BoxSelectionPanel( roi, roi );
			add( boxSelectionPanel, gbc );

			gbc.gridy++;
			this.boxModePanel = new BoxModePanel();
			add( boxModePanel, gbc );
		}
	}

	private class BoxModePanel extends JPanel
	{
		private static final long serialVersionUID = 1L;

		final JRadioButton full;

		final JLabel modeLabel;

		public BoxModePanel()
		{
			final GridBagLayout layout = new GridBagLayout();
			layout.columnWidths = new int[] { 80, 80 };
			layout.columnWeights = new double[] { 0.5, 0.5 };
			setLayout( layout );
			final GridBagConstraints gbc = new GridBagConstraints();

			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.gridwidth = 2;
			gbc.anchor = GridBagConstraints.BASELINE_LEADING;
			gbc.insets = new Insets( 5, 5, 5, 5 );

			final JLabel overlayLabel = new JLabel( "Overlay:", JLabel.LEFT );
			overlayLabel.setFont( getFont().deriveFont( Font.BOLD ) );
			add( overlayLabel, gbc );

			gbc.gridy++;
			gbc.gridwidth = 1;
			this.full = new JRadioButton( "Full" );
			final JRadioButton section = new JRadioButton( "Section" );
			final ActionListener l = new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					if ( null != boxOverlay && null == viewer )
						return;

					boxOverlay.setDisplayMode( full.isSelected() ? DisplayMode.FULL : DisplayMode.SECTION );
					viewer.requestRepaint();
				}
			};
			full.addActionListener( l );
			section.addActionListener( l );
			final ButtonGroup group = new ButtonGroup();
			group.add( full );
			group.add( section );
			full.setSelected( true );
			add( full, gbc );
			gbc.gridx++;
			add( section, gbc );

			gbc.gridy++;
			gbc.gridx = 0;
			this.modeLabel = new JLabel( "Navigation mode", JLabel.LEFT );
			add( modeLabel, gbc );
		}
	}

	private void setPanelEnabled( final JPanel panel, final boolean isEnabled )
	{
		panel.setEnabled( isEnabled );
		final Component[] components = panel.getComponents();
		for ( int i = 0; i < components.length; i++ )
		{
			if ( components[ i ] instanceof JPanel )
				setPanelEnabled( ( JPanel ) components[ i ], isEnabled );

			components[ i ].setEnabled( isEnabled );
		}
	}

	private Interval getRangeInterval( final int setupID )
	{
		final SharedBigDataViewerData data = wm.getSharedBigDataViewerData();
		final Source< ? > source = data.getSources().get( setupID ).getSpimSource();
		final int numTimepoints = data.getNumTimepoints();
		int tp = 0;
		Interval interval = null;
		while ( tp++ < numTimepoints )
		{
			if ( source.isPresent( tp ) )
			{
				interval = source.getSource( tp, 0 );
				break;
			}
		}
		if ( null == interval )
			interval = Intervals.createMinMax( 0, 0, 0, 1, 1, 1 );

		return interval;
	}

	private void toggleBoundingBox( final boolean useRoi )
	{
		final BoundingBoxPanel panel = ( BoundingBoxPanel ) targetPanel;
		setPanelEnabled( panel.boxSelectionPanel, useRoi );
		setPanelEnabled( panel.boxModePanel, useRoi );
		if ( useRoi )
			showViewer();
	}

	private void showViewer()
	{
		final List< BdvWindow > bdvWindows = wm.getMamutWindowModel().getBdvWindows();
		final ViewerFrame viewerFrame;
		if ( bdvWindows == null || bdvWindows.isEmpty() )
			viewerFrame = wm.createBigDataViewer();
		else
			viewerFrame = bdvWindows.get( 0 ).getViewerFrame();

		viewer = viewerFrame.getViewerPanel();
		viewerFrame.toFront();
	}
}
