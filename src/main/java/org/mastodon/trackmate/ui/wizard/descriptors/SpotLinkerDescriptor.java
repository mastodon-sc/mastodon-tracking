package org.mastodon.trackmate.ui.wizard.descriptors;

import java.util.Collection;
import java.util.Map;

import org.mastodon.linking.mamut.SpotLinkerOp;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.trackmate.TrackMate;
import org.mastodon.trackmate.ui.wizard.WizardLogService;
import org.mastodon.trackmate.ui.wizard.WizardPanelDescriptor;
import org.scijava.Context;
import org.scijava.Contextual;
import org.scijava.NullContextException;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.SciJavaPlugin;

public abstract class SpotLinkerDescriptor extends WizardPanelDescriptor implements SciJavaPlugin, Contextual
{
	@Parameter
	private Context context;

	protected WizardLogService log;

	public void setLog( final WizardLogService log )
	{
		this.log = log;
	}

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
	 * Returns the classes of the linkers this panel can configure.
	 *
	 * @return the linkers this panel can configure.
	 */
	public abstract Collection< Class< ? extends SpotLinkerOp > > getTargetClasses();

	/**
	 * Returns a default settings map, suitable to be configured with this
	 * descriptor.
	 *
	 * @return a default settings map.
	 */
	public abstract Map< String, Object > getDefaultSettings();

	public abstract void setTrackMate( TrackMate trackmate );

	public abstract void setWindowManager( final WindowManager windowManager );

}
