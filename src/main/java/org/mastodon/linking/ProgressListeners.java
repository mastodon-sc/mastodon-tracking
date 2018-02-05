package org.mastodon.linking;

import org.mastodon.revised.ui.ProgressListener;

public class ProgressListeners
{

	public static ProgressListener voidLogger()
	{
		return new ProgressListener()
		{

			@Override
			public void showStatus( final String string )
			{}

			@Override
			public void showProgress( final int current, final int total )
			{}

			@Override
			public void clearStatus()
			{}
		};

	}

	public static ProgressListener defaultLogger()
	{
		return new ProgressListener()
		{

			@Override
			public void showStatus( final String string )
			{
				System.out.println( string );
			}

			@Override
			public void showProgress( final int current, final int total )
			{
				System.out.println( String.format( "  completed %.1f %%", 100. * current / total ) );
			}

			@Override
			public void clearStatus()
			{}
		};
	}

}
