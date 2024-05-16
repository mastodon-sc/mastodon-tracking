/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2024 Tobias Pietzsch, Jean-Yves Tinevez
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
package org.mastodon.tracking.mamut.trackmate.wizard;

import static org.mastodon.tracking.mamut.trackmate.wizard.descriptors.LogPanel.BLUE_COLOR;
import static org.mastodon.tracking.mamut.trackmate.wizard.descriptors.LogPanel.ERROR_COLOR;
import static org.mastodon.tracking.mamut.trackmate.wizard.descriptors.LogPanel.GREEN_COLOR;
import static org.mastodon.tracking.mamut.trackmate.wizard.descriptors.LogPanel.NORMAL_COLOR;
import static org.mastodon.tracking.mamut.trackmate.wizard.descriptors.LogPanel.WARN_COLOR;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.text.StyledDocument;

import org.mastodon.tracking.mamut.trackmate.wizard.descriptors.LogPanel;
import org.scijava.app.AppService;
import org.scijava.app.StatusService;
import org.scijava.app.event.StatusEvent;
import org.scijava.log.DefaultLogger;
import org.scijava.log.LogLevel;
import org.scijava.log.LogListener;
import org.scijava.log.LogMessage;
import org.scijava.log.LogSource;
import org.scijava.log.Logger;
import org.scijava.service.AbstractService;

public class WizardLogService extends AbstractService implements LogListener, StatusService, Logger
{

	private final LogPanel panel;

	private final LogSource source;

	private final List< LogListener > listeners;

	public WizardLogService(final StyledDocument log)
	{
		this.panel = new LogPanel( log );
		this.source = LogSource.newRoot();
		this.listeners = new CopyOnWriteArrayList<>();
	}

	public LogPanel getPanel()
	{
		return panel;
	}

	@Override
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
	public void messageLogged( final LogMessage message )
	{
		notifyListeners( message );
		log( message.level(), message.text() );
	}

	@Override
	public LogSource getSource()
	{
		return source;
	}

	@Override
	public void alwaysLog( final int level, final Object msg, final Throwable t )
	{
		messageLogged( new LogMessage( source, level, msg, t ) );
	}

	@Override
	public Logger subLogger( final String name, final int level )
	{
		final LogSource source = getSource().subSource( name );
		final int actualLevel = source.hasLogLevel() ? source.logLevel() : level;
		return new DefaultLogger( this, source, actualLevel );
	}

	@Override
	public void addLogListener( final LogListener listener )
	{
		listeners.add( listener );
	}

	@Override
	public void removeLogListener( final LogListener listener )
	{
		listeners.remove( listener );
	}

	@Override
	public void notifyListeners( final LogMessage message )
	{
		for ( final LogListener listener : listeners )
			listener.messageLogged( message );
	}

	@Override
	public int getLevel()
	{
		return 0;
	}

	@Override
	public String getStatusMessage( final String appName, final StatusEvent statusEvent )
	{
		final String message = statusEvent.getStatusMessage();
		if ( !"".equals( message ) )
			return message;
		final AppService appService = context().getService( AppService.class );
		return appService.getApp( appName ).getInfo( false );
	}
}
