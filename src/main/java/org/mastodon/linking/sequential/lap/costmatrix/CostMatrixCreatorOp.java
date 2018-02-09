package org.mastodon.linking.sequential.lap.costmatrix;

import org.mastodon.collection.RefList;
import org.mastodon.linking.sequential.lap.linker.SparseCostMatrix;

import net.imagej.ops.special.function.NullaryFunctionOp;

/**
 * Interface for function that can generate a {@link SparseCostMatrix} from
 * assignment candidates.
 *
 * @author Jean-Yves Tinevez
 * @param <K>
 *            the type of source objects.
 * @param <J>
 *            the type of target objects.
 *
 */
public interface CostMatrixCreatorOp< K, J > extends NullaryFunctionOp< SparseCostMatrix >
{

	/**
	 * Returns the list of sources in the generated cost matrix.
	 *
	 * @return the list of object, such that <code>sourceList.get( i )</code> is
	 *         the source corresponding to the row <code>i</code> in the
	 *         generated cost matrix.
	 * @see #getTargetList()
	 */
	public RefList< K > getSourceList();

	/**
	 * Returns the list of targets in the generated cost matrix.
	 *
	 * @return the list of objects, such that <code>targetList.get( j )</code>
	 *         is the target corresponding to the column <code>j</code> in the
	 *         generated cost matrix.
	 * @see #getSourceList()
	 */
	public RefList< J > getTargetList();

	/**
	 * Returns the value of the no-linking alternative cost for the specified
	 * source.
	 *
	 * @param source
	 *            the source object.
	 * @return the alternative cost. Belongs to the list returned by
	 *         {@link #getSourceList()}.
	 */
	public double getAlternativeCostForSource( K source );

	/**
	 * Returns the value of the no-linking alternative cost for the specified
	 * target.
	 *
	 * @param target
	 *            the target object. Belongs to the list returned by
	 *            {@link #getTargetList()}.
	 * @return the alternative cost.
	 */
	public double getAlternativeCostForTarget( J target );

	/**
	 * Returns an error message in case the cost matrix calculation failed.
	 *
	 * @return an error message.
	 */
	public String getErrorMessage();

}
