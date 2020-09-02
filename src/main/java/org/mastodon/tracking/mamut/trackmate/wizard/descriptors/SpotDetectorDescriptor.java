package org.mastodon.tracking.mamut.trackmate.wizard.descriptors;

import static org.mastodon.tracking.mamut.trackmate.wizard.descriptors.LogPanel.ERROR_COLOR;
import static org.mastodon.tracking.mamut.trackmate.wizard.descriptors.LogPanel.NORMAL_COLOR;
import static org.mastodon.tracking.mamut.trackmate.wizard.descriptors.LogPanel.WARN_COLOR;

import java.awt.Color;
import java.util.Collection;
import java.util.Map;

import javax.swing.JLabel;

import org.mastodon.mamut.WindowManager;
import org.mastodon.tracking.mamut.detection.SpotDetectorOp;
import org.mastodon.tracking.mamut.trackmate.TrackMate;
import org.mastodon.tracking.mamut.trackmate.wizard.WizardPanelDescriptor;
import org.scijava.Context;
import org.scijava.Contextual;
import org.scijava.NullContextException;
import org.scijava.log.AbstractLogService;
import org.scijava.log.LogLevel;
import org.scijava.log.LogMessage;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.SciJavaPlugin;

public abstract class SpotDetectorDescriptor extends WizardPanelDescriptor implements SciJavaPlugin, Contextual
{

	@Parameter
	private Context context;

	// -- Contextual methods --

	@Override
	public Context context()
	{
		if ( context == null )
			throw new NullContextException();
		return context;
	}

	@Override
	public Context getContext()
	{
		return context;
	}

	/**
	 * Returns the classes of the detectors this panel can configure.
	 *
	 * @return the detectors this panel can configure.
	 */
	public abstract Collection< Class< ? extends SpotDetectorOp > > getTargetClasses();

	public abstract void setTrackMate( TrackMate trackmate );

	public abstract void setWindowManager( final WindowManager windowManager );

	/**
	 * Returns a default settings map, suitable to be configured with this
	 * descriptor.
	 *
	 * @return a default settings map.
	 */
	public abstract Map< String, Object > getDefaultSettings();

	protected final static class JLabelLogger extends AbstractLogService
	{

		private final JLabel lbl;

		protected JLabelLogger( final JLabel lbl )
		{
			this.lbl = lbl;
		}

		@Override
		protected void messageLogged( final LogMessage message )
		{
			lbl.setText( message.text() );
			final Color fg;
			switch ( message.level() )
			{
			default:
				fg = NORMAL_COLOR;
				break;
			case LogLevel.ERROR:
				fg = ERROR_COLOR;
				break;
			case LogLevel.WARN:
				fg = WARN_COLOR;
				break;
			}
			lbl.setForeground( fg );
		}
	}
}
