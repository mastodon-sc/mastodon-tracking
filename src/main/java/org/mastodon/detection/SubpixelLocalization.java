/*
 * Copied from ImgLib2, adapted to use as an Op.
 */
package org.mastodon.detection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import Jama.LUDecomposition;
import Jama.Matrix;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import net.imagej.ops.special.function.BinaryFunctionOp;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RealPoint;
import net.imglib2.RealPositionable;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;

/**
 * Refine a set of peaks to subpixel coordinates.
 * <p>
 * A List {@link RefinedPeak} for the given list of {@link Localizable} is
 * computed by, for each peak, fitting a quadratic function to the image and
 * computing the subpixel coordinates of the extremum. This is an iterative
 * procedure. If the extremum is shifted more than 0.5 in one or more the fit is
 * repeated at the corresponding integer coordinates. This is repeated to
 * convergence, for a maximum number of iterations, or until the integer
 * coordinates move out of the valid image.
 *
 * @author Stephan Preibisch
 * @author Tobias Pietzsch
 * @author Jean-Yves Tinevez
 * @param <P>
 *            the type of the {@link Localizable} instance used to specify where
 *            are the peaks.
 * @param <T>
 *            the pixel value type.
 */
@Plugin( type = SubpixelLocalization.class )
public class SubpixelLocalization< P extends Localizable, T extends RealType< T > >
		extends AbstractBinaryFunctionOp< RandomAccessible< T >, List< P >, List< RefinedPeak< P > > >
		implements BinaryFunctionOp< RandomAccessible< T >, List< P >, List< RefinedPeak< P > > >
{

	@Parameter
	private ThreadService threadService;

	/**
	 * In which interval the {@code img} contains valid pixels. If null, an
	 * infinite {@code img} is assumed. Integer peaks must lie within a 1-pixel
	 * border of this interval.
	 */
	@Parameter( required = false,
			description = "In which interval the {@code img} contains valid pixels. If null, an "
					+ "infinite {@code img} is assumed. Integer peaks must lie within a 1-pixel "
					+ "border of this interval." )
	private Interval validInterval;

	/**
	 * Maximum number of iterations for each peak.
	 */
	@Parameter( required = false )
	private int maxNumMoves = 4;

	/**
	 * If we allow an increasing maxima tolerance we will not change the base
	 * position that easily. Sometimes it simply jumps from left to right and
	 * back, because it is 4.51 (i.e. goto 5), then 4.49 (i.e. goto 4) Then we
	 * say, ok, lets keep the base position even if the subpixel location is
	 * 0.6...
	 */
	@Parameter( required = false )
	private boolean allowMaximaTolerance = false;

	/**
	 * Whether (invalid) {@link RefinedPeak} should be created for peaks where
	 * the fitting procedure did not converge.
	 */
	@Parameter( required = false )
	private boolean returnInvalidPeaks = false;

	/**
	 * By how much to increase the tolerance per iteration.
	 */
	@Parameter( required = false )
	private float maximaTolerance = 0.01f;

	@Parameter( required = false )
	private boolean[] allowedToMoveInDim;

	@Override
	public List< RefinedPeak< P > > calculate( final RandomAccessible< T > img, final List< P > peaks )
	{
		if ( null == allowedToMoveInDim )
		{
			allowedToMoveInDim = new boolean[ img.numDimensions() ];
			Arrays.fill( allowedToMoveInDim, true );
		}

		final int numPeaks = peaks.size();
		final ArrayList< RefinedPeak< P > > allRefinedPeaks = new ArrayList< RefinedPeak< P > >( numPeaks );

		if ( numPeaks == 0 )
			return allRefinedPeaks;

		final int numThreads = Runtime.getRuntime().availableProcessors();
		final int numTasks = numThreads <= 1 ? 1 : ( int ) Math.min( numPeaks, numThreads * 20 );
		final int taskSize = numPeaks / numTasks;

		final List< RefinedPeak< P > > synchronizedAllRefinedPeaks = Collections.synchronizedList( allRefinedPeaks );
		for ( int taskNum = 0; taskNum < numTasks; ++taskNum )
		{
			final int fromIndex = taskNum * taskSize;
			final int toIndex = ( taskNum == numTasks - 1 ) ? numPeaks : fromIndex + taskSize;
			final Runnable r = new Runnable()
			{
				@Override
				public void run()
				{
					final ArrayList< RefinedPeak< P > > refinedPeaks = refinePeaks(
							peaks.subList( fromIndex, toIndex ),
							img, validInterval, returnInvalidPeaks, maxNumMoves, allowMaximaTolerance, maximaTolerance, allowedToMoveInDim );
					synchronizedAllRefinedPeaks.addAll( refinedPeaks );
				}
			};
			try
			{
				threadService.getExecutorService().submit( r ).get();
			}
			catch ( final InterruptedException e )
			{
				e.printStackTrace();
			}
			catch ( final ExecutionException e )
			{
				e.printStackTrace();
			}
		}
		return allRefinedPeaks;
	}

	/**
	 * Refine a set of peaks to subpixel coordinates. Single-threaded version.
	 * <p>
	 * A List {@link RefinedPeak} for the given list of {@link Localizable} is
	 * computed by, for each peak, fitting a quadratic function to the image and
	 * computing the subpixel coordinates of the extremum. This is an iterative
	 * procedure. If the extremum is shifted more than 0.5 in one or more the
	 * fit is repeated at the corresponding integer coordinates. This is
	 * repeated to convergence, for a maximum number of iterations, or until the
	 * integer coordinates move out of the valid image.
	 *
	 * @param peaks
	 *            List of integer peaks.
	 * @param img
	 *            Pixel values.
	 * @param validInterval
	 *            In which interval the {@code img} contains valid pixels. If
	 *            null, an infinite {@code img} is assumed. Integer peaks must
	 *            lie within a 1-pixel border of this interval.
	 * @param returnInvalidPeaks
	 *            Whether (invalid) {@link RefinedPeak} should be created for
	 *            peaks where the fitting procedure did not converge.
	 * @param maxNumMoves
	 *            maximum number of iterations for each peak.
	 * @param allowMaximaTolerance
	 *            If we allow an increasing maxima tolerance we will not change
	 *            the base position that easily. Sometimes it simply jumps from
	 *            left to right and back, because it is 4.51 (i.e. goto 5), then
	 *            4.49 (i.e. goto 4) Then we say, ok, lets keep the base
	 *            position even if the subpixel location is 0.6...
	 * @param maximaTolerance
	 *            By how much to increase the tolerance per iteration.
	 * @param allowedToMoveInDim
	 *            specifies, per dimension, whether the base location is allowed
	 *            to be moved in the iterative procedure.
	 * @return refined list of peaks.
	 */
	private static < T extends RealType< T >, P extends Localizable > ArrayList< RefinedPeak< P > > refinePeaks(
			final List< P > peaks, final RandomAccessible< T > img, final Interval validInterval, final boolean returnInvalidPeaks,
			final int maxNumMoves, final boolean allowMaximaTolerance, final float maximaTolerance, final boolean[] allowedToMoveInDim )
	{
		final ArrayList< RefinedPeak< P > > refinedPeaks = new ArrayList< RefinedPeak< P > >();

		final int n = img.numDimensions();

		// the current position for the quadratic fit
		final Point currentPosition = new Point( n );

		// gradient vector and Hessian matrix at the current position
		final Matrix g = new Matrix( n, 1 );
		final Matrix H = new Matrix( n, n );

		// the current subpixel offset extimate
		final RealPoint subpixelOffset = new RealPoint( n );

		// bounds checking necessary?
		final boolean canMoveOutside = ( validInterval == null );
		final Interval interval = canMoveOutside ? null : Intervals.expand( validInterval, -1 );

		// the cursor for the computation
		final RandomAccess< T > access = canMoveOutside ? img.randomAccess() : img.randomAccess( validInterval );

		for ( final P p : peaks )
		{
			currentPosition.setPosition( p );

			// fit n-dimensional quadratic function to the extremum and
			// if the extremum is shifted more than 0.5 in one or more
			// directions we test whether it is better there
			// until we
			// - converge (find a stable extremum)
			// - move out of the Img
			// - achieved the maximal number of moves
			boolean foundStableMaxima = false;
			for ( int numMoves = 0; numMoves < maxNumMoves; ++numMoves )
			{
				// check validity of the current location
				if ( !( canMoveOutside || Intervals.contains( interval, currentPosition ) ) )
				{
					break;
				}

				quadraticFitOffset( currentPosition, access, g, H, subpixelOffset );

				// test all dimensions for their change
				// if the absolute value of the subpixel location
				// is bigger than 0.5 we move into that direction
				//
				// Normally, above an offset of 0.5 the base position
				// has to be changed, e.g. a subpixel location of 4.7
				// would mean that the new base location is 5 with an offset of
				// -0.3
				//
				// If we allow an increasing maxima tolerance we will
				// not change the base position that easily. Sometimes
				// it simply jumps from left to right and back, because
				// it is 4.51 (i.e. goto 5), then 4.49 (i.e. goto 4)
				// Then we say, ok, lets keep the base position even if
				// the subpixel location is 0.6...
				foundStableMaxima = true;
				final double threshold = allowMaximaTolerance ? 0.5 + numMoves * maximaTolerance : 0.5;
				for ( int d = 0; d < n; ++d )
				{
					final double diff = subpixelOffset.getDoublePosition( d );
					if ( Math.abs( diff ) > threshold )
					{
						if ( allowedToMoveInDim[ d ] )
						{
							// move to another base location
							currentPosition.move( diff > 0 ? 1 : -1, d );
							foundStableMaxima = false;
						}
					}
				}
				if ( foundStableMaxima )
				{
					break;
				}
			}

			if ( foundStableMaxima )
			{
				// compute the function value (intensity) of the fit
				double value = 0;
				for ( int d = 0; d < n; ++d )
				{
					value += g.get( d, 0 ) * subpixelOffset.getDoublePosition( d );
				}
				value *= 0.5;
				access.setPosition( currentPosition );
				value += access.get().getRealDouble();

				// set the results if everything went well
				subpixelOffset.move( currentPosition );
				refinedPeaks.add( new RefinedPeak< P >( p, subpixelOffset, value, true ) );
			}
			else if ( returnInvalidPeaks )
			{
				refinedPeaks.add( new RefinedPeak< P >( p, p, 0, false ) );
			}
		}

		return refinedPeaks;
	}

	/**
	 * Estimate subpixel {@code offset} of extremum of quadratic function fitted
	 * at {@code p}.
	 *
	 * @param p
	 *            integer position at which to fit quadratic.
	 * @param access
	 *            access to the image values.
	 * @param g
	 *            a <em>n</em> vector where <em>n</em> is the dimensionality of
	 *            the image. (This is a temporary variable to store the
	 *            gradient).
	 * @param H
	 *            a <em>n &times; n</em> matrix where <em>n</em> is the
	 *            dimensionality of the image. (This is a temporary variable to
	 *            store the Hessian).
	 * @param offset
	 *            subpixel offset of extremum wrt. {@code p} is stored here.
	 */
	private static < T extends RealType< T > > void quadraticFitOffset( final Localizable p, final RandomAccess< T > access, final Matrix g, final Matrix H, final RealPositionable offset )
	{
		final int n = p.numDimensions();

		access.setPosition( p );
		final double a1 = access.get().getRealDouble();
		for ( int d = 0; d < n; ++d )
		{
			// @formatter:off
			// gradient
			// we compute the derivative for dimension d like this
			//
			// | a0 | a1 | a2 |
			//        ^
			//        |
			// Original position of access
			//
			// g(d) = (a2 - a0)/2
			// we divide by 2 because it is a jump over two pixels
			// @formatter:on
			access.bck( d );
			final double a0 = access.get().getRealDouble();
			access.move( 2, d );
			final double a2 = access.get().getRealDouble();
			g.set( d, 0, ( a2 - a0 ) * 0.5 );

			// Move back to center point
			access.bck( d );

			// @formatter:off
			// Hessian
			// diagonal element for dimension d
			// computed from the row a in the input
			//
			// | a0 | a1 | a2 |
			//        ^
			//        |
			// Original position of access
			//
			// H(dd) = (a2-a1) - (a1-a0)
			//       = a2 - 2*a1 + a0
			// @formatter:on
			H.set( d, d, a2 - 2 * a1 + a0 );

			// off-diagonal Hessian elements H(de) = H(ed) are computed as a
			// combination of dimA (dimension a) and dimB (dimension b), i.e. we
			// always operate in a two-dimensional plane
			// ______________________
			// | a0b0 | a1b0 | a2b0 |
			// | a0b1 | a1b1 | a2b1 |
			// | a0b2 | a1b2 | a2b2 |
			// ----------------------
			// where a1b1 is the original position of the access
			//
			// H(ab) = ( (a2b2-a0b2)/2 - (a2b0 - a0b0)/2 )/2
			//
			// we divide by 2 because these are always jumps over two pixels
			for ( int e = d + 1; e < n; ++e )
			{
				// We start from center point.
				access.fwd( d );
				access.fwd( e );
				final double a2b2 = access.get().getRealDouble();
				access.move( -2, d );
				final double a0b2 = access.get().getRealDouble();
				access.move( -2, e );
				final double a0b0 = access.get().getRealDouble();
				access.move( 2, d );
				final double a2b0 = access.get().getRealDouble();
				// back to the original position
				access.bck( d );
				access.fwd( e );
				final double v = ( a2b2 - a0b2 - a2b0 + a0b0 ) * 0.25;
				H.set( d, e, v );
				H.set( e, d, v );
			}
		}

		// Do not move in a plane if the matrix is singular.
		final LUDecomposition decomp = new LUDecomposition( H );
		if ( decomp.isNonsingular() )
		{
			final Matrix minusOffset = decomp.solve( g );
			for ( int d = 0; d < n; ++d )
			{
				offset.setPosition( -minusOffset.get( d, 0 ), d );
			}
		}
		else
		{
			for ( int d = 0; d < n; d++ )
			{
				offset.setPosition( 0l, d );
			}
		}
	}

}
