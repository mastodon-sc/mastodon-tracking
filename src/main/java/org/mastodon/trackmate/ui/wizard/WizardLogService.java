package org.mastodon.trackmate.ui.wizard;

import static org.mastodon.trackmate.ui.wizard.descriptors.LogPanel.BLUE_COLOR;
import static org.mastodon.trackmate.ui.wizard.descriptors.LogPanel.ERROR_COLOR;
import static org.mastodon.trackmate.ui.wizard.descriptors.LogPanel.GREEN_COLOR;
import static org.mastodon.trackmate.ui.wizard.descriptors.LogPanel.NORMAL_COLOR;
import static org.mastodon.trackmate.ui.wizard.descriptors.LogPanel.WARN_COLOR;

import java.awt.Color;

import org.mastodon.trackmate.ui.wizard.descriptors.LogPanel;
import org.scijava.log.LogLevel;

public class WizardLogService
{

	private final LogPanel panel;

	public WizardLogService()
	{
		this.panel = new LogPanel();
	}

	public LogPanel getPanel()
	{
		return panel;
	}

	public void log( final int level, final Object msg )
	{
		final Color color;
		switch ( level )
		{
		case LogLevel.ERROR:
			color = ERROR_COLOR;
			break;
		case LogLevel.WARN:
			color = WARN_COLOR;
			break;
		case LogLevel.INFO:
		default:
			color = NORMAL_COLOR;
			break;
		case LogLevel.DEBUG:
			color = GREEN_COLOR;
			break;
		case LogLevel.TRACE:
			color = BLUE_COLOR;
			break;
		}
		panel.append( msg.toString(), color );
	}

	public void log( final String msg )
	{
		log( LogLevel.INFO, msg );
	}

	public void log( final Throwable t )
	{
		log( LogLevel.INFO, t.getStackTrace() );
	}

	public void showProgress( final int value, final int maximum )
	{
		panel.setProgress( ( double ) value / maximum );
	}

	public void showStatus( final String message )
	{
		panel.setStatus( message );
	}

	public void showStatus( final int progress, final int maximum, final String message )
	{
		showProgress( progress, maximum );
		showStatus( message );
	}

	public void showStatus( final int progress, final int maximum, final String message, final boolean warn )
	{
		showProgress( progress, maximum );
		panel.setStatus( message, warn ? WARN_COLOR : NORMAL_COLOR );
	}

	public void warn( final String message )
	{
		panel.setStatus( message, WARN_COLOR );
	}

	public void clearStatus()
	{
		panel.clearStatus();
	}

	public void clearLog()
	{
		panel.clearLog();
	}

}
