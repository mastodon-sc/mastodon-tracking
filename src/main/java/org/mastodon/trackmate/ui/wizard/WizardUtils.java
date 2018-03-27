package org.mastodon.trackmate.ui.wizard;

import java.io.File;
import java.io.IOException;

import org.mastodon.adapter.FocusModelAdapter;
import org.mastodon.adapter.HighlightModelAdapter;
import org.mastodon.adapter.RefBimap;
import org.mastodon.adapter.SelectionModelAdapter;
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
import org.mastodon.revised.mamut.MamutProject;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.revised.model.mamut.BoundingSphereRadiusStatistics;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.ModelOverlayProperties;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.revised.ui.keymap.Keymap;
import org.mastodon.revised.ui.keymap.KeymapManager;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.Behaviours;

import bdv.tools.InitializeViewerState;
import mpicbg.spim.data.SpimDataException;

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
	public static final ViewerFrameMamut preview( ViewerFrameMamut viewFrame, final SharedBigDataViewerData shared, final Model model )
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
		model.getGraph().addVertex().init( 0, new double[] { 50., 50., 50., }, 20. );

		preview( null, windowManager.getAppModel().getSharedBdvData(), model );
	}
}
