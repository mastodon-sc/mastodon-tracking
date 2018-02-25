package org.mastodon.trackmate.ui.boundingbox;

import org.mastodon.trackmate.ui.boundingbox.tobdv.BoxRealRandomAccessible;

import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.util.BdvFunctions;
import bdv.util.RealRandomAccessibleIntervalSource;
import bdv.util.RealRandomAccessibleSource;
import bdv.viewer.DisplayMode;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import bdv.viewer.VisibilityAndGrouping;
import net.imglib2.RealInterval;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.Bounds;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class BoundingBoxSource
{
	private final BoxRealRandomAccessible< UnsignedShortType > box;

	private final RealARGBColorConverterSetup boxConverterSetup;

	private final RealRandomAccessibleSource< UnsignedShortType > boxSource;

	private final SourceAndConverter< UnsignedShortType > boxSourceAndConverter;

	private final ViewerPanel viewer;

	private final SetupAssignments setupAssignments;

	public BoundingBoxSource(
			final RealInterval interval, // bounding box model
			final AffineTransform3D transform, // bounding box model
			final ViewerPanel viewer,
			final SetupAssignments setupAssignments )
	{
		this.viewer = viewer;
		this.setupAssignments = setupAssignments;

		box = new BoxRealRandomAccessible<>( interval, new UnsignedShortType( 1000 ), new UnsignedShortType( 0 ) );
		boxSource = new RealRandomAccessibleIntervalSource<>(
				box,
				new Bounds.SmallestContainingInterval( interval ),
				new UnsignedShortType(),
				transform,
				"selection" );

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

	public void removeFromViewer()
	{
		viewer.removeSource( boxSource );
		setupAssignments.removeSetup( boxConverterSetup );
	}
}
