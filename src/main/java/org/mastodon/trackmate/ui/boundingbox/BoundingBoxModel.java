package org.mastodon.trackmate.ui.boundingbox;

import org.mastodon.trackmate.ui.boundingbox.tobdv.BoxRealRandomAccessible;

import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.util.RealRandomAccessibleIntervalSource;
import bdv.util.RealRandomAccessibleSource;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import net.imglib2.RealInterval;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.Bounds;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class BoundingBoxModel implements BoundingBoxOverlay.BoundingBoxOverlaySource
{
	private final BoxRealRandomAccessible< UnsignedShortType > box;

	private RealARGBColorConverterSetup boxConverterSetup;

	private final RealRandomAccessibleSource< UnsignedShortType > boxSource;

	private SourceAndConverter< UnsignedShortType > boxSourceAndConverter;

	private final RealInterval interval;

	private final AffineTransform3D transform;

	public BoundingBoxModel( final RealInterval interval, final AffineTransform3D transform )
	{
		this.interval = interval;
		this.transform = transform;
		box = new BoxRealRandomAccessible<>( interval, new UnsignedShortType( 1000 ), new UnsignedShortType( 0 ) );
		boxSource = new RealRandomAccessibleIntervalSource<>(
				box,
				new Bounds.SmallestContainingInterval( interval ),
				new UnsignedShortType(),
				transform,
				"selection" );
	}

	public void install(  final ViewerPanel viewer, final int boxSetupId )
	{
		// Set up a converter from the source type (UnsignedShortType in this
		// case) to ARGBType.
		final RealARGBColorConverter< UnsignedShortType > converter = new RealARGBColorConverter.Imp1<>( 0, 3000 );
		converter.setColor( new ARGBType( 0x00994499 ) );

		// create a ConverterSetup (can be used by the brightness dialog to
		// adjust the converter settings)
		boxConverterSetup = new RealARGBColorConverterSetup( boxSetupId, converter );
		boxConverterSetup.setViewer( viewer );

		// create a SourceAndConverter (can be added to the viewer for display)
		boxSourceAndConverter = new SourceAndConverter<>( boxSource, converter );
	}

	@Override
	public void getIntervalTransform( final AffineTransform3D transform )
	{
		transform.set( this.transform );
	}

	public SourceAndConverter< UnsignedShortType > getBoxSourceAndConverter()
	{
		return boxSourceAndConverter;
	}

	public RealARGBColorConverterSetup getBoxConverterSetup()
	{
		return boxConverterSetup;
	}

	@Override
	public RealInterval getInterval()
	{
		return interval;
	}
}
