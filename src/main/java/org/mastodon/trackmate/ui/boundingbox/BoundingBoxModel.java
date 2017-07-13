package org.mastodon.trackmate.ui.boundingbox;

import bdv.tools.boundingbox.BoxRealRandomAccessible;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.transformation.TransformedSource;
import bdv.util.RealRandomAccessibleSource;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import net.imglib2.Interval;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class BoundingBoxModel extends BoxRealRandomAccessible< UnsignedShortType >
{

	private RealARGBColorConverterSetup boxConverterSetup;

	private RealRandomAccessibleSource< UnsignedShortType > boxSource;

	private SourceAndConverter< UnsignedShortType > boxSourceAndConverter;

	private final TransformedSource< UnsignedShortType > ts;

	public BoundingBoxModel( final Interval interval, final AffineTransform3D transform )
	{
		super( interval, new UnsignedShortType( 1000 ), new UnsignedShortType( 0 ) );
		this.boxSource =
				new RealRandomAccessibleSource< UnsignedShortType >(
						BoundingBoxModel.this,
						new UnsignedShortType(),
						"selection" )
				{
					@Override
					public Interval getInterval( final int t, final int level )
					{
						return BoundingBoxModel.this.getInterval();
					}
				};
		this.ts = new TransformedSource<>( boxSource );
		ts.setFixedTransform( transform );
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
		boxSourceAndConverter = new SourceAndConverter<>( ts, converter );
	}

	public void getIntervalTransform( final AffineTransform3D transform )
	{
		ts.getSourceTransform( 0, 0, transform );
	}

	public SourceAndConverter< UnsignedShortType > getBoxSourceAndConverter()
	{
		return boxSourceAndConverter;
	}

	public RealARGBColorConverterSetup getBoxConverterSetup()
	{
		return boxConverterSetup;
	}

}
