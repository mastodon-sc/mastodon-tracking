/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2023 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.tracking.linking.sequential.lap.costmatrix;

import org.mastodon.collection.RefList;
import org.mastodon.tracking.linking.sequential.lap.linker.SparseCostMatrix;

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
