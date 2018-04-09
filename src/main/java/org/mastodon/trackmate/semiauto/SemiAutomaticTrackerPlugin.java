package org.mastodon.trackmate.semiauto;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mastodon.app.ui.ViewMenuBuilder.MenuItem;
import org.mastodon.model.SelectionModel;
import org.mastodon.plugin.MastodonPlugin;
import org.mastodon.plugin.MastodonPluginAppModel;
import org.mastodon.revised.mamut.MamutMenuBuilder;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.Spot;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;

import bdv.spimdata.SpimDataMinimal;
import net.imagej.ops.OpService;
import net.imagej.ops.special.computer.Computers;

@Plugin( type = SemiAutomaticTrackerPlugin.class )
public class SemiAutomaticTrackerPlugin implements MastodonPlugin
{

	private static final String ACTION_1 = "[semiautotrack] track";

	private static final String[] ACTION_1_KEYS = new String[] { "ctrl T" };

	@Parameter
	private OpService ops;

	private MastodonPluginAppModel appModel;

	private static Map< String, String > menuTexts = new HashMap<>();

	static
	{
		menuTexts.put( ACTION_1, "Semi-automatic tracking" );
	}

	private final AbstractNamedAction action1 = new AbstractNamedAction( ACTION_1 )
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			if ( null == appModel )
				return;

			final SelectionModel< Spot, Link > selectionModel = appModel.getAppModel().getSelectionModel();
			final Collection< Spot > spots = selectionModel.getSelectedVertices();
			if ( spots == null || spots.isEmpty() )
				return;

			final Map< String, Object > settings = SemiAutomaticTrackerKeys.getDefaultDetectorSettingsMap();
			final Model model = appModel.getAppModel().getModel();
			
			final SpimDataMinimal spimData = ( SpimDataMinimal ) appModel.getAppModel().getSharedBdvData().getSpimData();
			final SemiAutomaticTracker tracker = ( SemiAutomaticTracker ) Computers.binary(
					ops, SemiAutomaticTracker.class, model, spots, settings,
					spimData );
			tracker.compute( spots, settings, model );
		}
	};

	@Override
	public Map< String, String > getMenuTexts()
	{
		return menuTexts;
	}

	@Override
	public List< MenuItem > getMenuItems()
	{
		return Arrays.asList(
				MamutMenuBuilder.menu( "Plugins",
						MamutMenuBuilder.menu( "TrackMate",
								MamutMenuBuilder.item( ACTION_1 ) ) ) );
	}

	@Override
	public void installGlobalActions( final Actions actions )
	{
		actions.namedAction( action1, ACTION_1_KEYS );
	}

	@Override
	public void setAppModel( final MastodonPluginAppModel appModel )
	{
		this.appModel = appModel;
	}

}
