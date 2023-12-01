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
package org.mastodon.tracking.detection;

public interface DetectionCreatorFactory
{

	/**
	 * Creates a {@link DetectionCreator} for the specified time-point.
	 *
	 * @param timepoint
	 *            the time-point in which the detection are created.
	 * @return a new {@link DetectionCreator} instance.
	 */
	public DetectionCreator create( int timepoint );

	/**
	 * Interface for classes that can add the detections of a single time-point
	 * to some data structure.
	 */
	public static interface DetectionCreator
	{

		/**
		 * Creates a detection object from the coordinates specified by the
		 * detector. All coordinates are specified with respect to the global
		 * coordinate system.
		 *
		 * @param pos
		 *            the position of the detection.
		 * @param radius
		 *            the radius of the detection.
		 * @param quality
		 *            the quality of the detection.
		 */
		public void createDetection( double[] pos, double radius, double quality );

		/**
		 * Method called before a batch of detections is added to the output via
		 * the {@link #createDetection(double[], double, double)} method.
		 */
		public void preAddition();

		/**
		 * Method called after a batch of detections is added to the output via
		 * the {@link #createDetection(double[], double, double)} method.
		 */
		public void postAddition();
	}

}
