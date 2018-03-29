package org.mastodon.trackmate.ui.wizard.util;

import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;

import java.awt.Font;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.NumberTickUnitSource;
import org.jfree.chart.axis.TickUnitSource;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.mastodon.adapter.FocusModelAdapter;
import org.mastodon.adapter.HighlightModelAdapter;
import org.mastodon.adapter.RefBimap;
import org.mastodon.adapter.SelectionModelAdapter;
import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.detection.DetectionUtil;
import org.mastodon.detection.DetectorKeys;
import org.mastodon.detection.mamut.DoGDetectorMamut;
import org.mastodon.detection.mamut.SpotDetectorOp;
import org.mastodon.graph.GraphIdBimap;
import org.mastodon.grouping.GroupManager;
import org.mastodon.model.DefaultFocusModel;
import org.mastodon.model.DefaultHighlightModel;
import org.mastodon.model.DefaultSelectionModel;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.bdv.BigDataViewerMamut;
import org.mastodon.revised.bdv.NavigationActionsMamut;
import org.mastodon.revised.bdv.SharedBigDataViewerData;
import org.mastodon.revised.bdv.ViewerFrameMamut;
import org.mastodon.revised.bdv.ViewerPanelMamut;
import org.mastodon.revised.bdv.overlay.OverlayGraphRenderer;
import org.mastodon.revised.bdv.overlay.wrap.OverlayEdgeWrapper;
import org.mastodon.revised.bdv.overlay.wrap.OverlayGraphWrapper;
import org.mastodon.revised.bdv.overlay.wrap.OverlayVertexWrapper;
import org.mastodon.revised.mamut.KeyConfigContexts;
import org.mastodon.revised.mamut.MamutProject;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.revised.model.feature.Feature;
import org.mastodon.revised.model.mamut.BoundingSphereRadiusStatistics;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.ModelOverlayProperties;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.revised.ui.keymap.Keymap;
import org.mastodon.revised.ui.keymap.KeymapManager;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.trackmate.Settings;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.Behaviours;

import bdv.spimdata.SpimDataMinimal;
import bdv.tools.InitializeViewerState;
import mpicbg.spim.data.SpimDataException;
import net.imagej.ops.OpService;
import net.imagej.ops.special.hybrid.Hybrids;

/**
 * A collection of static utilities related to running Msatodon with the wizard
 * framework in this package.
 *
 * @author Jean-Yves Tinevez
 */
public class WizardUtils
{

	/**
	 * The name used as frame title for the detection preview frame created by
	 * {@link #preview(WindowManager, ViewerFrameMamut, Model, LogService)}
	 * method.
	 */
	public static String PREVIEW_DETECTION_FRAME_NAME = "Preview detection";

	private static final NumberFormat FORMAT = new DecimalFormat( "0.0" );

	public static final int countSpotsIn(final Model model, final int timepoint)
	{
		int nSpots = 0;
		final SpatialIndex< Spot > spatialIndex = model.getSpatioTemporalIndex().getSpatialIndex( timepoint );
		for ( @SuppressWarnings( "unused" )
		final Spot spot : spatialIndex )
			nSpots++;

		return nSpots;
	}

	public static final ChartPanel createQualityHistogram(final Model model)
	{
		@SuppressWarnings( "unchecked" )
		final Feature< Spot, DoublePropertyMap< Spot > > qFeature =
				( Feature< Spot, DoublePropertyMap< Spot > > ) model.getFeatureModel().getFeature( DetectionUtil.QUALITY_FEATURE_NAME );
		final DoublePropertyMap< Spot > pm = qFeature.getPropertyMap();
		final double[] values = pm.getMap().values();
		if ( values.length == 0 )
			return null;

		final ChartPanel chartPanel = HistogramUtil.createHistogramPlot( values, false );
		chartPanel.getChart().setTitle( new TextTitle( "Quality histogram", chartPanel.getFont() ) );

		// Disable zoom.
		for ( final MouseListener ml : chartPanel.getMouseListeners() )
			chartPanel.removeMouseListener( ml );

		// Re enable the X axis.
		final XYPlot plot = chartPanel.getChart().getXYPlot();
		plot.getDomainAxis().setVisible( true );
		final TickUnitSource source = new NumberTickUnitSource( false, FORMAT );
		plot.getDomainAxis().setStandardTickUnits( source );
		final Font smallFont = chartPanel.getFont().deriveFont( chartPanel.getFont().getSize2D() - 2f );
		plot.getDomainAxis().setTickLabelFont( smallFont );

		// Transparent background.
		chartPanel.getChart().setBackgroundPaint( null );

		return chartPanel;
	}

	/**
	 * Executes a detection preview with the detector set and configured in the
	 * specified {@link Settings}, on the images it points to. The results are
	 * added to the specified {@link Model}.
	 * <p>
	 * Only the specified time-point is processed, using the ROI specified in
	 * the settings as well.
	 *
	 * @param model
	 *            the model to add preview results to.
	 * @param settings
	 *            the settings in which the detector is set and configured, and
	 *            the image data is specified.
	 * @param ops
	 *            the OpService.
	 * @param currentTimepoint
	 *            the time-point in the data to run the preview on.
	 * @return <code>true</code> if the preview ran successfully.
	 */
	public static final boolean executeDetectionPreview( final Model model, final Settings settings, final OpService ops, final int currentTimepoint )
	{
		/*
		 * Remove spots from current time point.
		 */
		final ModelGraph graph = model.getGraph();
		final RefList< Spot > toRemove = RefCollections.createRefList( graph.vertices() );
		model.getSpatioTemporalIndex().readLock().lock();
		try
		{
			final SpatialIndex< Spot > spatialIndex = model.getSpatioTemporalIndex().getSpatialIndex( currentTimepoint );
			for ( final Spot spot : spatialIndex )
				toRemove.add( spot );
		}
		finally
		{
			model.getSpatioTemporalIndex().readLock().unlock();
		}

		graph.getLock().writeLock().lock();
		try
		{
			for ( final Spot spot : toRemove )
				graph.remove( spot );
		}
		finally
		{
			graph.getLock().writeLock().unlock();
		}

		/*
		 * Tune settings for preview.
		 */
		final Class< ? extends SpotDetectorOp > cl = settings.values.getDetector();
		// Copy settings.
		final Map< String, Object > detectorSettings = new HashMap<>( settings.values.getDetectorSettings() );
		detectorSettings.put( KEY_MIN_TIMEPOINT, currentTimepoint );
		detectorSettings.put( KEY_MAX_TIMEPOINT, currentTimepoint );

		/*
		 * Execute preview.
		 */
		final SpimDataMinimal spimData = settings.values.getSpimData();
		final SpotDetectorOp detector = ( SpotDetectorOp ) Hybrids.unaryCF( ops, cl,
				graph, spimData,
				detectorSettings );
		detector.compute( spimData, graph );

		if ( !detector.isSuccessful() )
		{
			final LogService log = ops.getContext().getService( LogService.class );
			log.error( "Detection failed:\n" + detector.getErrorMessage() );
			return false;
		}

		model.getFeatureModel().declareFeature( detector.getQualityFeature() );
		graph.notifyGraphChanged();
		return true;
	}

	/**
	 * Creates or shows a BDV window suitable to be used as a preview detection
	 * frame.
	 *
	 * @param viewFrame
	 *            the viewer frame, potentially created from a previous call to
	 *            this method. If <code>null</code> a new one will be created
	 *            and returned. If not <code>null</code>, the window will be
	 *            brought up front.
	 * @param shared
	 *            the {@link SharedBigDataViewerData} from which the image data
	 *            to show is taken.
	 * @param model
	 *            the model to display in the preview frame. The preview window
	 *            will be notified of changes in this model, and display them.
	 * @return a new {@link ViewerFrameMamut} or the non-<code>null</code> one
	 *         passed in argument.
	 */
	public static final ViewerFrameMamut previewFrame( ViewerFrameMamut viewFrame, final SharedBigDataViewerData shared, final Model model )
	{
		if ( null == viewFrame || !viewFrame.isShowing() )
		{
			final ModelGraph graph = model.getGraph();
			final GraphIdBimap< Spot, Link > graphIdBimap = model.getGraphIdBimap();
			final String[] keyConfigContexts = new String[] { KeyConfigContexts.BIGDATAVIEWER };
			final Keymap keymap = new KeymapManager().getForwardDefaultKeymap();

			final BigDataViewerMamut bdv = new BigDataViewerMamut( shared, "Preview detection", new GroupManager( 0 ).createGroupHandle() );
			final ViewerPanelMamut viewer = bdv.getViewer();
			InitializeViewerState.initTransform( viewer );
			viewFrame = bdv.getViewerFrame();

			final BoundingSphereRadiusStatistics radiusStats = new BoundingSphereRadiusStatistics( model );
			final OverlayGraphWrapper< Spot, Link > viewGraph = new OverlayGraphWrapper< Spot, Link >(
					graph,
					graphIdBimap,
					model.getSpatioTemporalIndex(),
					graph.getLock(),
					new ModelOverlayProperties( graph, radiusStats ) );
			final RefBimap< Spot, OverlayVertexWrapper< Spot, Link > > vertexMap = viewGraph.getVertexMap();
			final RefBimap< Link, OverlayEdgeWrapper< Spot, Link > > edgeMap = viewGraph.getEdgeMap();

			final DefaultHighlightModel< Spot, Link > highlightModel = new DefaultHighlightModel<>( graphIdBimap );
			final HighlightModelAdapter< Spot, Link, OverlayVertexWrapper< Spot, Link >, OverlayEdgeWrapper< Spot, Link > > highlightModelAdapter =
					new HighlightModelAdapter<>( highlightModel, vertexMap, edgeMap );

			final DefaultFocusModel< Spot, Link > focusModel = new DefaultFocusModel<>( graphIdBimap );
			final FocusModelAdapter< Spot, Link, OverlayVertexWrapper< Spot, Link >, OverlayEdgeWrapper< Spot, Link > > focusModelAdapter =
					new FocusModelAdapter<>( focusModel, vertexMap, edgeMap );

			final DefaultSelectionModel< Spot, Link > selectionModel = new DefaultSelectionModel<>( graph, graphIdBimap );
			final SelectionModelAdapter< Spot, Link, OverlayVertexWrapper< Spot, Link >, OverlayEdgeWrapper< Spot, Link > > selectionModelAdapter =
					new SelectionModelAdapter<>( selectionModel, vertexMap, edgeMap );

			final OverlayGraphRenderer< OverlayVertexWrapper< Spot, Link >, OverlayEdgeWrapper< Spot, Link > > tracksOverlay = new OverlayGraphRenderer<>(
					viewGraph,
					highlightModelAdapter,
					focusModelAdapter,
					selectionModelAdapter );
			viewer.getDisplay().addOverlayRenderer( tracksOverlay );
			viewer.addRenderTransformListener( tracksOverlay );
			viewer.addTimePointListener( tracksOverlay );
			graph.addGraphChangeListener( () -> viewer.getDisplay().repaint() );

			final Actions viewActions = new Actions( keymap.getConfig(), keyConfigContexts );
			viewActions.install( viewFrame.getKeybindings(), "view" );
			final Behaviours viewBehaviours = new Behaviours( keymap.getConfig(), keyConfigContexts );
			viewBehaviours.install( viewFrame.getTriggerbindings(), "view" );

			NavigationActionsMamut.install( viewActions, viewer );
			viewer.getTransformEventHandler().install( viewBehaviours );

			viewFrame.setVisible( true );
		}
		viewFrame.toFront();
		return viewFrame;
	}

	private WizardUtils()
	{}

	public static void main( final String[] args ) throws IOException, SpimDataException
	{
		final Context context = new Context();
		final WindowManager windowManager = new WindowManager( context );

		final String bdvFile = "../TrackMate3/samples/mamutproject/datasethdf5.xml";
		final MamutProject project = new MamutProject( null, new File( bdvFile ) );
		windowManager.getProjectManager().open( project );

		final Model model = new Model();
		model.getGraph().addVertex().init( 1, new double[] { 50., 50., 50., }, 20. );

		previewFrame( null, windowManager.getAppModel().getSharedBdvData(), model );
		final Map< String, Object > detectorSettings = DetectionUtil.getDefaultDetectorSettingsMap();
		detectorSettings.put( DetectorKeys.KEY_THRESHOLD, 50. );
		final Settings settings = new Settings()
				.spimData( ( SpimDataMinimal ) windowManager.getAppModel().getSharedBdvData().getSpimData() )
				.detector( DoGDetectorMamut.class )
				.detectorSettings( detectorSettings );
		final int currentTimepoint = 0;
		executeDetectionPreview( model, settings, context.getService( OpService.class ), currentTimepoint );
	}
}
