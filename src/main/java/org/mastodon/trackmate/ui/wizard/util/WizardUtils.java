package org.mastodon.trackmate.ui.wizard.util;

import static org.mastodon.detection.DetectorKeys.KEY_ADD_BEHAVIOR;
import static org.mastodon.detection.DetectorKeys.KEY_MAX_TIMEPOINT;
import static org.mastodon.detection.DetectorKeys.KEY_MIN_TIMEPOINT;

import java.awt.Font;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
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
import org.mastodon.detection.mamut.DetectionQualityFeature;
import org.mastodon.detection.mamut.MamutDetectionCreatorFactories.DetectionBehavior;
import org.mastodon.graph.GraphIdBimap;
import org.mastodon.grouping.GroupManager;
import org.mastodon.model.DefaultFocusModel;
import org.mastodon.model.DefaultHighlightModel;
import org.mastodon.model.DefaultSelectionModel;
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
import org.mastodon.trackmate.TrackMate;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.Behaviours;

import bdv.spimdata.SpimDataMinimal;
import bdv.tools.InitializeViewerState;
import bdv.util.Affine3DHelpers;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;

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
	 * {@link #previewFrame(ViewerFrameMamut, SharedBigDataViewerData, Model)}
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

	public static final ChartPanel createQualityHistogram( final Model model )
	{
		final DetectionQualityFeature qFeature =
				DetectionQualityFeature.getOrRegister( model.getFeatureModel(), model.getGraph().vertices().getRefPool() );
		final double[] values = qFeature.values();
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
	 * @param logger
	 *            the log service to report possible errors.
	 * @param statusService
	 *            the status service to follow progress.
	 * @return <code>true</code> if the preview ran successfully.
	 */
	public static final boolean executeDetectionPreview( final Model model, final Settings settings, final OpService ops, final int currentTimepoint, final Logger logger, final StatusService statusService )
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

		// Copy settings.
		final Settings localSettings = settings.copy();
		final Map< String, Object > detectorSettings = localSettings.values.getDetectorSettings();
		detectorSettings.put( KEY_MIN_TIMEPOINT, currentTimepoint );
		detectorSettings.put( KEY_MAX_TIMEPOINT, currentTimepoint );
		detectorSettings.put( KEY_ADD_BEHAVIOR, DetectionBehavior.REMOVEALL.name() );

		/*
		 * Execute preview.
		 */

		final TrackMate trackmate = new TrackMate( localSettings, model, new DefaultSelectionModel<>( model.getGraph(), model.getGraphIdBimap() ) );
		ops.context().inject( trackmate );
		trackmate.setStatusService( statusService );
		trackmate.setLogger( logger );
		final boolean ok = trackmate.execDetection();

		if ( !ok )
		{
			final LogService log = ops.getContext().getService( LogService.class );
			log.error( "Detection failed:\n" + trackmate.getErrorMessage() );
			return false;
		}
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
			final OverlayGraphWrapper< Spot, Link > viewGraph = new OverlayGraphWrapper< >(
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

	/**
	 * Returns an explanatory string that states what resolutions are available
	 * in the {@link SpimDataMinimal} for the specified setup id.
	 *
	 * @param sources
	 *            the sources.
	 * @param setupID
	 *            the setup id to query.
	 * @return a string.
	 */
	public static final String echoSetupIDInfo( final List< SourceAndConverter< ? > > sources, final int setupID )
	{
		if ( sources == null )
			return "No data.";

		final Source< ? > source = sources.get( setupID ).getSpimSource();
		final RandomAccessibleInterval< ? > ra0 = source.getSource( 0, 0 );
		final long[] size = new long[ ra0.numDimensions() ];
		ra0.dimensions( size );

		final StringBuilder str = new StringBuilder();

		str.append( "  - name: " + source.getName() + "\n" );
		str.append( "  - size: " + size[ 0 ] + " x " + size[ 1 ] + " x " + size[ 2 ] + "\n" );
		final VoxelDimensions voxelSize = source.getVoxelDimensions();
		str.append( "  - voxel size: " + voxelSize.dimension( 0 ) + " x " + voxelSize.dimension( 1 ) + " x "
				+ voxelSize.dimension( 2 ) + " " + voxelSize.unit() + "\n" );

		final int numMipmapLevels = source.getNumMipmapLevels();
		if ( numMipmapLevels > 1 )
		{
			str.append( String.format( "  - multi-resolution image with %d levels:\n", numMipmapLevels ) );
			for ( int level = 0; level < numMipmapLevels; level++ )
			{
				final AffineTransform3D mipmapTransform = DetectionUtil.getMipmapTransform( sources, 0, setupID, level );
				final double sx = Affine3DHelpers.extractScale( mipmapTransform, 0 );
				final double sy = Affine3DHelpers.extractScale( mipmapTransform, 1 );
				final double sz = Affine3DHelpers.extractScale( mipmapTransform, 2 );
				str.append( String.format( "     - level %d: %.0f x %.0f x %.0f\n", level, sx, sy, sz ) );
			}
		}
		else
		{
			str.append( " - single-resolution image.\n" );
		}
		return str.toString();
	}

	/**
	 * Returns the physical units in which spatial coordinates are stored.
	 *
	 * @param sources
	 *            the image data.
	 * @param setupID
	 *            the id of the setup to query.
	 * @return the spatial units.
	 */
	public static final String getSpatialUnits( final List< SourceAndConverter< ? > > sources, final int setupID )
	{
		final String units = ( null != sources )
				? sources.get( setupID ).getSpimSource().getVoxelDimensions().unit()
				: "pixels";
		return units;
	}

	/**
	 * Returns an information string on how a detector will be configured to
	 * operate on the specified source with the specified parameters.
	 *
	 * @param sources
	 *            the image data.
	 * @param minSizePixel
	 *            the minimal object size in pixel below which the detectors
	 *            should not downsample the image data.
	 * @param timepoint
	 *            the timepoint to operate on.
	 * @param setupID
	 *            the id of the setup to operate on.
	 * @param radius
	 *            the radius of the object to detect.
	 * @param threshold
	 *            the quality threshold for the detector.
	 * @return an information string.
	 */
	public static final String echoDetectorConfigInfo( final List< SourceAndConverter< ? > > sources, final double minSizePixel, final int timepoint, final int setupID, final double radius, final double threshold )
	{
		final int level = DetectionUtil.determineOptimalResolutionLevel( sources, radius, minSizePixel, timepoint, setupID );
		final AffineTransform3D mipmapTransform = DetectionUtil.getMipmapTransform( sources, timepoint, setupID, level );
		final double sx = Affine3DHelpers.extractScale( mipmapTransform, 0 );
		final double sy = Affine3DHelpers.extractScale( mipmapTransform, 1 );
		final double sz = Affine3DHelpers.extractScale( mipmapTransform, 2 );

		final double[] calibration = DetectionUtil.getPhysicalCalibration( sources, timepoint, setupID, level );
		final String units = getSpatialUnits( sources, setupID );

		final double rx = radius / calibration[ 0 ];
		final double ry = radius / calibration[ 1 ];
		final double rz = radius / calibration[ 2 ];

		final StringBuilder str = new StringBuilder();
		str.append( "Configured detector with parameters:\n" );
		str.append( String.format( "  - spot radius: %.1f %s\n", radius, units ) );
		str.append( String.format( "  - quality threshold: %.1f\n", threshold ) );
		final Source< ? > source = sources.get( setupID ).getSpimSource();
		final int numMipmapLevels = source.getNumMipmapLevels();
		if ( numMipmapLevels > 1 )
		{
			str.append( String.format( "  - will operate on resolution level %d (%.0f x %.0f x %.0f)\n", level, sx, sy, sz ) );
			str.append( String.format( "  - at this level, radius = %.1f %s corresponds to:\n", radius, units ) );
		}
		else
		{
			str.append( String.format( "  - equivalent radius = %.1f %s in pixels:\n", radius, units ) );
		}
		str.append( String.format( "      - %.1f pixels in X.\n", rx ) );
		str.append( String.format( "      - %.1f pixels in Y.\n", ry ) );
		str.append( String.format( "      - %.1f pixels in Z.\n", rz ) );
		return str.toString();
	}

	private WizardUtils()
	{}
}
