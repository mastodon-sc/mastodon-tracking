package org.mastodon;

public interface HasErrorMessage
{
	/**
	 * Returns <code>true</code> if this process completed successfully. If not,
	 * a meaningful error message can be obtained with
	 * {@link #getErrorMessage()}.
	 * <p>
	 * The output of this method is defined only <b>after</b> the process
	 * completion. Values returned during processing are undefined.
	 *
	 * @return <code>true</code> if the process completed successfully.
	 * @see #getErrorMessage()
	 */
	public boolean isSuccessful();

	/**
	 * Returns a meaningful error message after the process failed to complete
	 * or <code>null</code> if the process completed successfully.
	 * <p>
	 * The output of this method is defined only <b>after</b> the process
	 * completion. Values returned during processing are undefined.
	 *
	 * @return an error message.
	 */
	public String getErrorMessage();
}
