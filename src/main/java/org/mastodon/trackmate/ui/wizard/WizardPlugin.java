package org.mastodon.trackmate.ui.wizard;

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.menu;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mastodon.app.ui.ViewMenuBuilder;
import org.mastodon.plugin.MastodonPlugin;
import org.mastodon.plugin.MastodonPluginAppModel;
import org.mastodon.revised.mamut.MamutAppModel;
import org.mastodon.revised.mamut.WindowManager;
import org.scijava.AbstractContextual;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.RunnableAction;

public abstract class WizardPlugin extends AbstractContextual implements MastodonPlugin
{
	private final AbstractNamedAction runWizardAction;

	private MastodonPluginAppModel pluginAppModel;

	private final String actionName;

	private final String commandName;

	private final String menuPath;

	private final String[] keyStrokes;

	/**
	 * Instantiates a new wizard-based Mastodon plugin.
	 *
	 * @param actionName
	 *            the name of the action triggered by this plugin, and that will
	 *            launch the wizard. <i>E.g.</i> "[trackmate] run linking
	 *            wizard".
	 * @param commandName
	 *            the command name to show in the menus for this plugin.
	 *            <i>E.g.</i> "Linking wizard...".
	 * @param menuPath
	 *            the menu path in which to install this plugin. Menu path must
	 *            be specified as a string, separated bye the
	 *            '<code>&gt;</code>' char. <i>E.g.</i> "Plugins > TrackMate".
	 * @param keyStrokes
	 *            the keystrokes to associate to this plugin, as an array of
	 *            <code>String</code>s. <i>E.g.</i>
	 *            <code>new String[] { "not mapped" }</code>.
	 */
	protected WizardPlugin( final String actionName, final String commandName, final String menuPath, final String[] keyStrokes )
	{
		this.actionName = actionName;
		this.commandName = commandName;
		this.menuPath = menuPath;
		this.keyStrokes = keyStrokes;
		runWizardAction = new RunnableAction( actionName, this::runWizard );
		updateEnabledActions();
	}

	/**
	 * Returns the sequence to be used in the wizard that will be shown when
	 * this plugin is executed.
	 *
	 * @param pluginAppModel
	 *            the app model.
	 * @param wizard
	 *            the wizard that will display the sequence.
	 * @return the {@link WizardSequence}.
	 */
	public abstract WizardSequence getWizardSequence( MastodonPluginAppModel pluginAppModel, Wizard wizard );

	@Override
	public void setAppModel( final MastodonPluginAppModel model )
	{
		this.pluginAppModel = model;
		updateEnabledActions();
	}

	@Override
	public List< ViewMenuBuilder.MenuItem > getMenuItems()
	{
		ViewMenuBuilder.MenuItem mi = item( actionName );
		final String[] split = menuPath.split( ">" );
		for ( int i = split.length - 1; i >= 0; i-- )
			mi = menu( split[ i ].trim(), mi );
		return Arrays.asList( mi );
	}

	@Override
	public Map< String, String > getMenuTexts()
	{
		final Map< String, String > menuTexts = new HashMap<>();
		menuTexts.put( actionName, commandName );
		return menuTexts;
	}

	@Override
	public void installGlobalActions( final Actions actions )
	{
		actions.namedAction( runWizardAction, keyStrokes );
	}

	private void updateEnabledActions()
	{
		final MamutAppModel appModel = ( pluginAppModel == null ) ? null : pluginAppModel.getAppModel();
		runWizardAction.setEnabled( appModel != null );
	}

	private void runWizard()
	{
		if ( pluginAppModel != null )
		{
			final WindowManager windowManager = pluginAppModel.getWindowManager();
			final Wizard wizard = new Wizard( windowManager.getContext() );
			final WizardSequence sequence = getWizardSequence( pluginAppModel, wizard );
			wizard.show( sequence, commandName );
		}
	}
}
