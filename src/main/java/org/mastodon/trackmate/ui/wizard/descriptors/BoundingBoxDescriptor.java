package org.mastodon.trackmate.ui.wizard.descriptors;

import static org.mastodon.detection.DetectorKeys.KEY_SETUP_ID;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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
import net.imglib2.Interval;
import net.imglib2.util.Intervals;

public class BoundingBoxDescriptor extends WizardPanelDescriptor
{

	public static final String IDENTIFIER = "Setup bounding-box";

	private final Settings settings;

	private final WindowManager wm;

	private BoundingBoxOverlay boxOverlay;

	private ViewerPanel viewer;

	public BoundingBoxDescriptor( final Settings settings, final WindowManager wm )
	{
		this.settings = settings;
		this.wm = wm;
		this.panelIdentifier = IDENTIFIER;
		this.targetPanel = new BoundingBoxPanel();
	}

	@Override
	public void displayingPanel()
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

	@Override
	public void aboutToDisplayPanel()
	{
		final int setupID = ( int ) settings.values.getDetectorSettings().get( KEY_SETUP_ID );
		final BoundingBoxPanel panel = ( BoundingBoxPanel ) targetPanel;
		panel.boxSelectionPanel.setBoundsInterval( getRangeInterval( setupID ) );
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

		private BoundingBoxPanel()
		{
			super();

			final int setupID = ( int ) settings.values.getDetectorSettings().get( KEY_SETUP_ID );

			final GridLayout layout = new GridLayout( 0, 1 );
			layout.setHgap( 5 );
			layout.setVgap( 5 );
			setLayout( layout );

			final JLabel lblTitle = new JLabel( "Region of interest" );
			lblTitle.setFont( getFont().deriveFont( Font.BOLD ) );
			add( lblTitle );

			final JCheckBox useRoi = new JCheckBox( "Process only a ROI.", false );
			add( useRoi );

			final ModifiableInterval roi = new ModifiableInterval( 3 );
			final Interval rangeInterval = getRangeInterval( setupID );
			boxSelectionPanel = new BoxSelectionPanel( roi, rangeInterval );
			boxSelectionPanel.setEnabled( useRoi.isSelected() );
			add( boxSelectionPanel );

			this.boxModePanel = new BoxModePanel();
			boxModePanel.setEnabled( useRoi.isSelected() );
			add( boxModePanel );
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

		@Override
		public void setEnabled( final boolean b )
		{
			super.setEnabled( b );
			for ( final Component c : getComponents() )
				c.setEnabled( b );
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
}
