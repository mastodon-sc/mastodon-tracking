package org.mastodon.trackmate.ui.boundingbox;

import org.mastodon.trackmate.ui.boundingbox.BoundingBoxOverlay.BoundingBoxOverlaySource;

import bdv.tools.boundingbox.BoxRealRandomAccessible;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.util.BdvFunctions;
import bdv.util.RealRandomAccessibleSource;
import bdv.viewer.DisplayMode;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import bdv.viewer.VisibilityAndGrouping;
import net.imglib2.Interval;
import net.imglib2.RealInterval;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;

/**
 * A BDV source (and converter etc) representing the BoundingBox.
 *
 * @author Tobias Pietzsch
 * @author Jean-Yves Tinevez
 */
public class BoundingBoxVirtualSource implements BoundingBoxSource
{
	private final BoxRealRandomAccessible< UnsignedShortType > box;

	private final RealARGBColorConverterSetup boxConverterSetup;

	private final RealRandomAccessibleSource< UnsignedShortType > boxSource;

	private final SourceAndConverter< UnsignedShortType > boxSourceAndConverter;

	private final ViewerPanel viewer;

	private final SetupAssignments setupAssignments;

	public BoundingBoxVirtualSource(
			final String name,
			final BoundingBoxOverlaySource bbSource,
			final ViewerPanel viewer,
			final SetupAssignments setupAssignments )
	{
		this.viewer = viewer;
		this.setupAssignments = setupAssignments;

		final RealInterval interval = bbSource.getInterval();
		final AffineTransform3D transform = new AffineTransform3D();
		bbSource.getTransform( transform );

		box = new BoxRealRandomAccessible<>( interval, new UnsignedShortType( 1000 ), new UnsignedShortType( 0 ) );
		boxSource = new RealRandomAccessibleSource< UnsignedShortType >( box,	new UnsignedShortType(),	name )
		{
			@Override
			public synchronized void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
			{
				bbSource.getTransform( transform );
			}

			@Override
			public Interval getInterval( final int t, final int level )
			{
				return Intervals.smallestContainingInterval( bbSource.getInterval() );
			}
		};

		/*
		 * Set up a converter from the source type (UnsignedShortType in this
		 * case) to ARGBType.
		 */
		final RealARGBColorConverter< UnsignedShortType > converter = new RealARGBColorConverter.Imp1<>( 0, 3000 );
		converter.setColor( new ARGBType( 0x00994499 ) );

		/*
		 * Create a ConverterSetup (can be used by the brightness dialog to
		 * adjust the converter settings)
		 */
		final int boxSetupId = BdvFunctions.getUnusedSetupId( setupAssignments );
		boxConverterSetup = new RealARGBColorConverterSetup( boxSetupId, converter );
		boxConverterSetup.setViewer( viewer );

		// create a SourceAndConverter (can be added to the viewer for display)
		boxSourceAndConverter = new SourceAndConverter<>( boxSource, converter );
	}

	@Override
	public void addToViewer()
	{
		final VisibilityAndGrouping vg = viewer.getVisibilityAndGrouping();
		if ( vg.getDisplayMode() != DisplayMode.FUSED )
		{
			final int numSources = vg.numSources();
			for ( int i = 0; i < numSources; ++i )
				vg.setSourceActive( i, vg.isSourceVisible( i ) );
			vg.setDisplayMode( DisplayMode.FUSED );
		}

		viewer.addSource( boxSourceAndConverter );
		vg.setSourceActive( boxSource, true );
		vg.setCurrentSource( boxSource );

		setupAssignments.addSetup( boxConverterSetup );
	}

	@Override
	public void removeFromViewer()
	{
		viewer.removeSource( boxSource );
		setupAssignments.removeSetup( boxConverterSetup );
	}
}
