package org.mastodon.trackmate.ui.boundingbox;

import static org.mastodon.trackmate.ui.boundingbox.BoundingBoxOverlay.DisplayMode.FULL;
import static org.mastodon.trackmate.ui.boundingbox.BoundingBoxOverlay.DisplayMode.SECTION;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import org.mastodon.trackmate.ui.boundingbox.BoundingBoxOverlay.CornerHighlighter;

import bdv.tools.boundingbox.BoxSelectionPanel;
import bdv.tools.brightness.SetupAssignments;
import bdv.viewer.DisplayMode;
import bdv.viewer.ViewerPanel;
import bdv.viewer.VisibilityAndGrouping;
import net.imglib2.Interval;

class BoundingBoxDialog extends JDialog
{

	private static final long serialVersionUID = 1L;

	final BoxSelectionPanel boxSelectionPanel;

	final BoxModePanel boxModePanel;

	final BoundingBoxOverlay boxOverlay;

	private boolean contentCreated = false;

	private final ViewerPanel viewer;

	public BoundingBoxDialog( final Frame owner, final String title, final BoundingBoxModel model, final ViewerPanel viewer, final SetupAssignments setupAssignments, final Interval rangeInterval )
	{
		this( owner, title, model, viewer, setupAssignments, rangeInterval, true, true );
	}

	public BoundingBoxDialog( final Frame owner, final String title, final BoundingBoxModel model, final ViewerPanel viewer, final SetupAssignments setupAssignments, final Interval rangeInterval, final boolean showBoxSource, final boolean showBoxOverlay )
	{
		super( owner, title, false );
		this.viewer = viewer;

		// create an Overlay to show 3D wireframe box
		boxOverlay = new BoundingBoxOverlay( model );
		boxOverlay.setPerspective( 0 );

		// create a JPanel with sliders to modify the bounding box interval
		// (boxRealRandomAccessible.getInterval())
		boxSelectionPanel = new BoxSelectionPanel( model.getInterval(), rangeInterval );
		// listen for updates on the bbox to trigger repainting
		boxSelectionPanel.addSelectionUpdateListener( new BoxSelectionPanel.SelectionUpdateListener()
		{
			@Override
			public void selectionUpdated()
			{
				viewer.requestRepaint();
			}
		} );

		this.boxModePanel = new BoxModePanel();

		// when dialog is made visible, add bbox source
		// when dialog is hidden, remove bbox source
		final CornerHighlighter cornerHighlighter = boxOverlay.new CornerHighlighter();
		addComponentListener( new ComponentAdapter()
		{

			@Override
			public void componentShown( final ComponentEvent e )
			{
				if ( showBoxSource )
				{
					viewer.addSource( model.getBoxSourceAndConverter() );
					setupAssignments.addSetup( model.getBoxConverterSetup() );
					model.getBoxConverterSetup().setViewer( viewer );

					final int bbSourceIndex = viewer.getState().numSources() - 1;
					final VisibilityAndGrouping vg = viewer.getVisibilityAndGrouping();
					if ( vg.getDisplayMode() != DisplayMode.FUSED )
					{
						for ( int i = 0; i < bbSourceIndex; ++i )
							vg.setSourceActive( i, vg.isSourceVisible( i ) );
						vg.setDisplayMode( DisplayMode.FUSED );
					}
					vg.setSourceActive( bbSourceIndex, true );
					vg.setCurrentSource( bbSourceIndex );
				}
				if ( showBoxOverlay )
				{
					viewer.getDisplay().addOverlayRenderer( boxOverlay );
					viewer.addRenderTransformListener( boxOverlay );
				}
				viewer.getDisplay().addHandler( cornerHighlighter );
			}

			@Override
			public void componentHidden( final ComponentEvent e )
			{
				if ( showBoxSource )
				{
					viewer.removeSource( model.getBoxSourceAndConverter().getSpimSource() );
					setupAssignments.removeSetup( model.getBoxConverterSetup() );
				}
				if ( showBoxOverlay )
				{
					viewer.getDisplay().removeOverlayRenderer( boxOverlay );
					viewer.removeTransformListener( boxOverlay );
				}
				viewer.getDisplay().removeHandler( cornerHighlighter );
			}
		} );

		// make 'V' key hide dialog
		final ActionMap am = getRootPane().getActionMap();
		final InputMap im = getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
		final Object hideKey = new Object();
		final Action hideAction = new AbstractAction()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				setVisible( false );
			}
		};
		im.put( KeyStroke.getKeyStroke( BoundingBoxMamut.TOGGLE_BOUNDING_BOX_KEYS ), hideKey );
		am.put( hideKey, hideAction );

		setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
	}

	@Override
	public void setVisible( final boolean b )
	{
		if ( b && !contentCreated )
		{
			createContent();
			contentCreated = true;
		}
		super.setVisible( b );
	}

	// Override in subclasses
	public void createContent()
	{
		getContentPane().add( boxSelectionPanel, BorderLayout.NORTH );
		getContentPane().add( boxModePanel, BorderLayout.SOUTH );
		pack();
	}

	class BoxModePanel extends JPanel
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
					boxOverlay.setDisplayMode( full.isSelected() ? FULL : SECTION );
					viewer.requestRepaint();
				}
			};
			full.addActionListener( l );
			section.addActionListener( l );
			final ButtonGroup group = new ButtonGroup();
			group.add( full );
			group.add( section );
			full.setSelected( true );
			boxOverlay.setDisplayMode( FULL );
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

}
