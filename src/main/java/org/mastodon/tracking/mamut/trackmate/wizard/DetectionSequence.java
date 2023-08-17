/*-
 * #%L
 * mastodon-tracking
 * %%
 * Copyright (C) 2017 - 2022 Tobias Pietzsch, Jean-Yves Tinevez
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
import org.mastodon.tracking.mamut.detection.SpotDetectorOp;
import org.mastodon.tracking.mamut.trackmate.PluginProvider;
import org.mastodon.tracking.mamut.trackmate.TrackMate;
import org.mastodon.tracking.mamut.trackmate.wizard.descriptors.BoundingBoxDescriptor;
import org.mastodon.tracking.mamut.trackmate.wizard.descriptors.ChooseDetectorDescriptor;
import org.mastodon.tracking.mamut.trackmate.wizard.descriptors.ExecuteDetectionDescriptor;
import org.mastodon.tracking.mamut.trackmate.wizard.descriptors.SetupIdDecriptor;
import org.mastodon.tracking.mamut.trackmate.wizard.descriptors.SpotDetectorDescriptor;
import org.scijava.Context;

import mpicbg.spim.data.generic.AbstractSpimData;

public class DetectionSequence implements WizardSequence
{

	private final TrackMate trackmate;

	private final SetupIdDecriptor setupIdDecriptor;

	private final BoundingBoxDescriptor boundingBoxDescriptor;

	private final ChooseDetectorDescriptor chooseDetectorDescriptor;

	private final ExecuteDetectionDescriptor executeDetectionDescriptor;

	/**
	 * The previously chosen SpotDetectorDescriptor (<code>null</code> if it is
	 * the first time).
	 */
	private SpotDetectorDescriptor previousDetectorPanel = null;

	private WizardPanelDescriptor current;

	private final Map< WizardPanelDescriptor, WizardPanelDescriptor > next;

	private final Map< WizardPanelDescriptor, WizardPanelDescriptor > previous;

	private final PluginProvider< SpotDetectorDescriptor > descriptorProvider;

	private final WizardLogService logService;

	private final ProjectModel appModel;

	public DetectionSequence( final TrackMate trackmate, final ProjectModel appModel, final WizardLogService logService )
	{
		this.trackmate = trackmate;
		this.appModel = appModel;
		this.logService = logService;
		final AbstractSpimData< ? > spimData = appModel.getSharedBdvData().getSpimData();
		this.setupIdDecriptor = new SetupIdDecriptor( trackmate.getSettings(), spimData, logService );
		setupIdDecriptor.setContext( appModel.getContext() );
		this.boundingBoxDescriptor = new BoundingBoxDescriptor( trackmate.getSettings(), appModel, logService );
		this.chooseDetectorDescriptor = new ChooseDetectorDescriptor( trackmate, appModel.getContext() );
		this.executeDetectionDescriptor = new ExecuteDetectionDescriptor( trackmate, logService.getPanel() );
		this.current = init();

		this.next = getForwardSequence();
		this.previous = getBackwardSequence();

		descriptorProvider = new PluginProvider<>( SpotDetectorDescriptor.class );
		final Context context = appModel.getContext();
		context.inject( descriptorProvider );
	}

	private Map< WizardPanelDescriptor, WizardPanelDescriptor > getBackwardSequence()
	{
		final Map< WizardPanelDescriptor, WizardPanelDescriptor > map = new HashMap<>( 5 );
		map.put( setupIdDecriptor, null );
		map.put( boundingBoxDescriptor, setupIdDecriptor );
		map.put( chooseDetectorDescriptor, boundingBoxDescriptor );
		return map;
	}

	private Map< WizardPanelDescriptor, WizardPanelDescriptor > getForwardSequence()
	{
		final Map< WizardPanelDescriptor, WizardPanelDescriptor > map = new HashMap<>( 5 );
		map.put( setupIdDecriptor, boundingBoxDescriptor );
		map.put( boundingBoxDescriptor, chooseDetectorDescriptor );
		map.put( executeDetectionDescriptor, null );
		return map;
	}

	/**
	 * Determines and registers the descriptor used to configure the detector
	 * chosen in the {@link ChooseDetectorDescriptor}.
	 *
	 * @return a suitable {@link SpotDetectorDescriptor}.
	 */
	private SpotDetectorDescriptor getConfigDescriptor()
	{
		final Class< ? extends SpotDetectorOp > detectorClass = trackmate.getSettings().values.getDetector();


		final List< String > linkerPanelNames = descriptorProvider.getNames();
		for ( final String key : linkerPanelNames )
		{
			final SpotDetectorDescriptor detectorConfigDescriptor = descriptorProvider.getInstance( key );
			if ( detectorConfigDescriptor.getTargetClasses().contains( detectorClass ) )
			{
				if ( detectorConfigDescriptor == previousDetectorPanel )
					return detectorConfigDescriptor;

				previousDetectorPanel = detectorConfigDescriptor;
				if ( detectorConfigDescriptor.getContext() == null )
					appModel.getContext().inject( detectorConfigDescriptor );

				/*
				 * Copy as much settings as we can to the potentially new config descriptor.
				 */
				final Map< String, Object > oldSettings = new HashMap<>( trackmate.getSettings().values.getDetectorSettings() );
				final Map< String, Object > defaultSettings = detectorConfigDescriptor.getDefaultSettings();
				for ( final String skey : defaultSettings.keySet() )
					defaultSettings.put( skey, oldSettings.get( skey ) );
				trackmate.getSettings().detectorSettings( defaultSettings );

				detectorConfigDescriptor.setTrackMate( trackmate );
				detectorConfigDescriptor.setAppModel( appModel );
				detectorConfigDescriptor.setLogger( logService );
				detectorConfigDescriptor.setStatusService( logService );
				detectorConfigDescriptor.getPanelComponent().setSize( chooseDetectorDescriptor.getPanelComponent().getSize() );
				return detectorConfigDescriptor;
			}
		}
		throw new RuntimeException( "Could not find a descriptor that can configure " + detectorClass );
	}

	@Override
	public WizardPanelDescriptor next()
	{
		if ( current == chooseDetectorDescriptor )
		{
			final SpotDetectorDescriptor configDescriptor = getConfigDescriptor();
			next.put( chooseDetectorDescriptor, configDescriptor );
			next.put( configDescriptor, executeDetectionDescriptor );
			previous.put( configDescriptor, chooseDetectorDescriptor );
			previous.put( executeDetectionDescriptor, configDescriptor );
		}

		current = next.get( current );
		return current;
	}

	@Override
	public boolean hasNext()
	{
		return current != executeDetectionDescriptor;
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
		return current != setupIdDecriptor;
	}

	@Override
	public WizardPanelDescriptor init()
	{
		return setupIdDecriptor;
	}
}
