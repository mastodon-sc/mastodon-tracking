package org.mastodon.trackmate.ui.wizard;

import static org.mastodon.trackmate.ui.wizard.descriptors.LogPanel.BLUE_COLOR;
import static org.mastodon.trackmate.ui.wizard.descriptors.LogPanel.ERROR_COLOR;
import static org.mastodon.trackmate.ui.wizard.descriptors.LogPanel.GREEN_COLOR;
import static org.mastodon.trackmate.ui.wizard.descriptors.LogPanel.NORMAL_COLOR;
import static org.mastodon.trackmate.ui.wizard.descriptors.LogPanel.WARN_COLOR;

import java.awt.Color;

import org.mastodon.trackmate.ui.wizard.descriptors.LogPanel;
import org.scijava.app.StatusService;
import org.scijava.app.event.StatusEvent;
import org.scijava.log.AbstractLogService;
import org.scijava.log.LogService;
import org.scijava.plugin.Plugin;

@Plugin( type = WizardLogService.class )
public class WizardLogService extends AbstractLogService implements LogService, StatusService
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

	@Override
	protected void log( final int level, final Object msg )
	{
		final Color color;
		switch ( level )
		{
		case ERROR:
			color = ERROR_COLOR;
			break;
		case WARN:
			color = WARN_COLOR;
			break;
		case INFO:
		default:
			color = NORMAL_COLOR;
			break;
		case DEBUG:
			color = GREEN_COLOR;
			break;
		case TRACE:
			color = BLUE_COLOR;
			break;
		}
		panel.append( msg.toString(), color );
	}

	@Override
	protected void log( final String msg )
	{
		log( INFO, msg );
	}

	@Override
	protected void log( final Throwable t )
	{
		log( INFO, t.getStackTrace() );
	}

	@Override
	public void showProgress( final int value, final int maximum )
	{
		panel.setProgress( ( double ) value / maximum );
	}

	@Override
	public void showStatus( final String message )
	{
		panel.setStatus( message );
	}

	@Override
	public void showStatus( final int progress, final int maximum, final String message )
	{
		showProgress( progress, maximum );
		showStatus( message );
	}

	@Override
	public void showStatus( final int progress, final int maximum, final String message, final boolean warn )
	{
		showProgress( progress, maximum );
		panel.setStatus( message, warn ? WARN_COLOR : NORMAL_COLOR );
	}

	@Override
	public void warn( final String message )
	{
		panel.setStatus( message, WARN_COLOR );
	}

	@Override
	public void clearStatus()
	{
		panel.clearStatus();
	}

	public void clearLog()
	{
		panel.clearLog();
	}

	@Override
	public String getStatusMessage( final String appName, final StatusEvent statusEvent )
	{
		return null;
	}

}
