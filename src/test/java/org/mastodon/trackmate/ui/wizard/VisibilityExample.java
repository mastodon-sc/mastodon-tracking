package org.mastodon.trackmate.ui.wizard;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.mastodon.revised.mamut.MainWindow;
import org.mastodon.revised.mamut.MamutProject;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.revised.mamut.feature.SpotPositionFeatureComputer;
import org.mastodon.revised.model.feature.Feature;
import org.mastodon.revised.model.feature.FeatureProjection;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.trackmate.ui.wizard.util.FilterPanel;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;
import net.imglib2.RealLocalizable;
import net.imglib2.ui.PainterThread;
import net.imglib2.ui.PainterThread.Paintable;

public class VisibilityExample extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final JLabel lblFilteringCompletedIn;

	private final JLabel lblVisibleSpots;

	private final JPanel panelFilter;

	public VisibilityExample()
	{
		final GridBagLayout layout = new GridBagLayout();
		layout.rowWeights = new double[] { 0.0, 0.0, 1.0, 0.0, 0.0 };
		layout.columnWeights = new double[] { 1.0 };
		setLayout( layout );

		final JLabel lblVisibilityTestingThrough = new JLabel( "Visibility testing through filtering." );
		final GridBagConstraints gbc_lblVisibilityTestingThrough = new GridBagConstraints();
		gbc_lblVisibilityTestingThrough.insets = new Insets( 0, 0, 5, 0 );
		gbc_lblVisibilityTestingThrough.gridx = 0;
		gbc_lblVisibilityTestingThrough.gridy = 0;
		add( lblVisibilityTestingThrough, gbc_lblVisibilityTestingThrough );

		final JLabel lblFilterOnX = new JLabel( "Filter on X value:" );
		final GridBagConstraints gbc_lblFilterOnX = new GridBagConstraints();
		gbc_lblFilterOnX.insets = new Insets( 0, 0, 5, 0 );
		gbc_lblFilterOnX.anchor = GridBagConstraints.WEST;
		gbc_lblFilterOnX.gridx = 0;
		gbc_lblFilterOnX.gridy = 1;
		add( lblFilterOnX, gbc_lblFilterOnX );

		panelFilter = new JPanel( new GridLayout() );
		final GridBagConstraints gbc_panelFilter = new GridBagConstraints();
		gbc_panelFilter.insets = new Insets( 0, 0, 5, 0 );
		gbc_panelFilter.fill = GridBagConstraints.BOTH;
		gbc_panelFilter.gridx = 0;
		gbc_panelFilter.gridy = 2;
		add( panelFilter, gbc_panelFilter );

		lblVisibleSpots = new JLabel( "# visible spots." );
		final GridBagConstraints gbc_lblVisibleSpots = new GridBagConstraints();
		gbc_lblVisibleSpots.insets = new Insets( 0, 0, 5, 0 );
		gbc_lblVisibleSpots.anchor = GridBagConstraints.EAST;
		gbc_lblVisibleSpots.gridx = 0;
		gbc_lblVisibleSpots.gridy = 3;
		add( lblVisibleSpots, gbc_lblVisibleSpots );

		lblFilteringCompletedIn = new JLabel( "Filtering completed in # ms." );
		final GridBagConstraints gbc_lblFilteringCompletedIn = new GridBagConstraints();
		gbc_lblFilteringCompletedIn.anchor = GridBagConstraints.EAST;
		gbc_lblFilteringCompletedIn.gridx = 0;
		gbc_lblFilteringCompletedIn.gridy = 4;
		add( lblFilteringCompletedIn, gbc_lblFilteringCompletedIn );

	}

	public static void main( final String[] args ) throws IOException, SpimDataException
	{
		final String bdvFile = "samples/datasethdf5.xml";

//		final String bdvFile = "/Users/tinevez/Projects/JYTinevez/MaMuT/MaMuT_demo_dataset/MaMuT_Parhyale_demo.xml";
//		final String modelFile = "/Users/tinevez/Projects/JYTinevez/MaMuT/MaMuT_demo_dataset/MaMuT_Parhyale_demo-mastodon.raw";

		final WindowManager wm = new WindowManager( new Context() );
		final MamutProject project = new MamutProject( new File("samples"), new File( bdvFile ) );
		wm.getProjectManager().open( project );
		final Model model = wm.getAppModel().getModel();


		final MainWindow mw = new MainWindow( wm );
		mw.setVisible( true );

		System.out.println( "Loaded a model with " + model.getGraph().vertices().size() + " vertices." );

		final SpotPositionFeatureComputer computer = new SpotPositionFeatureComputer();
		final Feature< Spot, RealLocalizable > feature = computer.compute( model );
		model.getFeatureModel().declareFeature( feature );
		System.out.println( "Calculated Spot position feature." );

		final String key = "Spot X position";
		final FeatureProjection< Spot > featureProjection = feature.getProjections().get( key );

		final double[] values = new double[ model.getGraph().vertices().size() ];
		int i = 0;
		for ( final Spot spot : model.getGraph().vertices() )
			values[ i++ ] = featureProjection.value( spot );

		final FilterPanel filterPanel = new FilterPanel( values );
		final VisibilityExample example = new VisibilityExample();
		example.panelFilter.add( filterPanel );

		final Paintable paintable = new Paintable()
		{

			@Override
			public void paint()
			{
				example.lblFilteringCompletedIn.setText( "Starting filtering..." );
				final long start = System.currentTimeMillis();
//				final FeatureFilter filter = new FeatureFilter( key, filterPanel.getThreshold(), filterPanel.isAboveThreshold() );
//				mgw.filter( Collections.singleton( filter ), model.getFeatureModel() );
				final long end = System.currentTimeMillis();
				example.lblFilteringCompletedIn.setText( "Filtering completed in " + ( end - start ) + " ms." );
//				example.lblVisibleSpots.setText( "Retained " + mgw.vertices().size() + " out of " + mgw.allVertices().size() );
			}
		};
		final PainterThread filterThread = new PainterThread( paintable );
		filterThread.start();

		filterPanel.addChangeListener( new ChangeListener()
		{

			@Override
			public void stateChanged( final ChangeEvent e )
			{
				filterThread.requestRepaint();
			}
		} );

		final JFrame frame = new JFrame( "Filtering example" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.getContentPane().add( example );
		frame.setSize( 400, 400 );
		frame.setVisible( true );
	}

}
