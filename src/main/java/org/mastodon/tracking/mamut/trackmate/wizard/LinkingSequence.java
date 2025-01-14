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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.model.Model;
import org.mastodon.tracking.mamut.linking.LinkCostFeature;
import org.mastodon.tracking.mamut.linking.SpotLinkerOp;
import org.mastodon.tracking.mamut.trackmate.PluginProvider;
import org.mastodon.tracking.mamut.trackmate.TrackMate;
import org.mastodon.tracking.mamut.trackmate.wizard.descriptors.ChooseLinkerDescriptor;
import org.mastodon.tracking.mamut.trackmate.wizard.descriptors.ExecuteLinkingDescriptor;
import org.mastodon.tracking.mamut.trackmate.wizard.descriptors.LinkingTargetDescriptor;
import org.mastodon.tracking.mamut.trackmate.wizard.descriptors.SpotLinkerDescriptor;
import org.scijava.Context;

import net.imagej.ops.OpService;
import net.imagej.ops.special.inplace.Inplaces;

public class LinkingSequence implements WizardSequence
{

	private final TrackMate trackmate;

	/**
	 * The previously chosen SpotLinkerDescriptor (<code>null</code> if it is
	 * the first time).
	 */
	private SpotLinkerDescriptor previousLinkerPanel = null;

	private WizardPanelDescriptor current;

	private final Map< WizardPanelDescriptor, WizardPanelDescriptor > next;

	private final Map< WizardPanelDescriptor, WizardPanelDescriptor > previous;

	private final PluginProvider< SpotLinkerDescriptor > descriptorProvider;

	private final LinkingTargetDescriptor linkingTargetDescriptor;

	private final ChooseLinkerDescriptor chooseLinkerDescriptor;

	private final ExecuteLinkingDescriptor executeLinkingDescriptor;

	private final WizardLogService logService;

	private final ProjectModel appModel;

	public LinkingSequence( final TrackMate trackmate, final ProjectModel appModel, final WizardLogService logService )
	{
		this.trackmate = trackmate;
		this.appModel = appModel;
		this.logService = logService;

		this.linkingTargetDescriptor = new LinkingTargetDescriptor( trackmate, appModel, logService );
		this.chooseLinkerDescriptor = new ChooseLinkerDescriptor( trackmate, appModel.getContext() );
		this.executeLinkingDescriptor = new ExecuteLinkingDescriptor( trackmate, logService.getPanel() );

		this.next = getForwardSequence();
		this.previous = getBackwardSequence();

		descriptorProvider = new PluginProvider<>( SpotLinkerDescriptor.class );
		final Context context = appModel.getContext();
		context.inject( descriptorProvider );

		this.current = init();
	}

	private Map< WizardPanelDescriptor, WizardPanelDescriptor > getBackwardSequence()
	{
		final Map< WizardPanelDescriptor, WizardPanelDescriptor > map = new HashMap<>( 5 );
		map.put( chooseLinkerDescriptor, linkingTargetDescriptor );
		map.put( linkingTargetDescriptor, null );
		return map;
	}

	private Map< WizardPanelDescriptor, WizardPanelDescriptor > getForwardSequence()
	{
		final Map< WizardPanelDescriptor, WizardPanelDescriptor > map = new HashMap<>( 5 );
		map.put( linkingTargetDescriptor, chooseLinkerDescriptor );
		map.put( executeLinkingDescriptor, null );
		return map;
	}

	/**
	 * Determines and registers the descriptor used to configure the linker
	 * chosen in the {@link ChooseLinkerDescriptor}.
	 *
	 * @return a suitable {@link SpotLinkerDescriptor}.
	 */
	private SpotLinkerDescriptor getConfigDescriptor()
	{
		final Class< ? extends SpotLinkerOp > linkerClass = trackmate.getSettings().values.getLinker();
		final List< String > linkerPanelNames = descriptorProvider.getNames();
		for ( final String key : linkerPanelNames )
		{
			final SpotLinkerDescriptor linkerConfigDescriptor = descriptorProvider.getInstance( key );
			if ( linkerConfigDescriptor.getTargetClasses().contains( linkerClass ) )
			{
				if ( linkerConfigDescriptor == previousLinkerPanel )
					return linkerConfigDescriptor;

				previousLinkerPanel = linkerConfigDescriptor;
				if ( linkerConfigDescriptor.getContext() == null )
					appModel.getContext().inject( linkerConfigDescriptor );
				final Map< String, Object > defaultSettings = getLinkerDefaultSettings( linkerClass );

				// Pass as much parameter as we can from the old settings.
				final Map< String, Object > oldSettings = trackmate.getSettings().values.getLinkerSettings();
				for ( final String pkey : defaultSettings.keySet() )
				{
					if ( !oldSettings.containsKey( pkey ) )
						continue;
					defaultSettings.put( pkey, oldSettings.get( pkey ) );
				}

				trackmate.getSettings().linkerSettings( defaultSettings );
				linkerConfigDescriptor.setTrackMate( trackmate );
				linkerConfigDescriptor.setWindowManager( appModel.getWindowManager() );
				linkerConfigDescriptor.setLogger( logService );
				linkerConfigDescriptor.setStatusService( logService );
				linkerConfigDescriptor.getPanelComponent().setSize( chooseLinkerDescriptor.getPanelComponent().getSize() );
				return linkerConfigDescriptor;
			}
		}
		throw new RuntimeException( "Could not find a descriptor that can configure " + linkerClass );
	}

	private Map< String, Object > getLinkerDefaultSettings( final Class< ? extends SpotLinkerOp > linkerCl )
	{
		// Instantiate a dummy linker.
		final Model model = appModel.getModel();
		final LinkCostFeature linkCostFeature = LinkCostFeature.getOrRegister(
				model.getFeatureModel(), model.getGraph().edges().getRefPool() );
		final OpService ops = appModel.getContext().getService( OpService.class );

		final SpotLinkerOp linker =
				( SpotLinkerOp ) Inplaces.binary1( ops, linkerCl, model.getGraph(),
						model.getSpatioTemporalIndex(),
						new HashMap< String, Object >(),
						model.getFeatureModel(),
						linkCostFeature );
		return linker.getDefaultSettings();
	}

	@Override
	public WizardPanelDescriptor next()
	{
		if ( current == chooseLinkerDescriptor )
		{
			final SpotLinkerDescriptor configDescriptor = getConfigDescriptor();
			next.put( chooseLinkerDescriptor, configDescriptor );
			next.put( configDescriptor, executeLinkingDescriptor );
			previous.put( configDescriptor, chooseLinkerDescriptor );
			previous.put( executeLinkingDescriptor, configDescriptor );
		}

		current = next.get( current );
		return current;
	}

	@Override
	public boolean hasNext()
	{
		return current != executeLinkingDescriptor;
	}

	@Override
	public WizardPanelDescriptor current()
	{
		return current;
	}

	@Override
	public WizardPanelDescriptor previous()
	{
		current = previous.get( current );
		return current;
	}

	@Override
	public boolean hasPrevious()
	{
		return current != linkingTargetDescriptor;
	}

	@Override
	public WizardPanelDescriptor init()
	{
		return linkingTargetDescriptor;
	}
}
