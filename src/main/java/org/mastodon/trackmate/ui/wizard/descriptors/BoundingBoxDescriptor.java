package org.mastodon.trackmate.ui.wizard.descriptors;

import static org.mastodon.detection.DetectorKeys.KEY_SETUP_ID;

import java.awt.GridLayout;
import java.util.List;

import javax.swing.JPanel;

import org.mastodon.revised.bdv.SharedBigDataViewerData;
import org.mastodon.revised.mamut.BdvManager.BdvWindow;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.trackmate.Settings;
import org.mastodon.trackmate.ui.boundingbox.BoundingBoxMamut2;
import org.mastodon.trackmate.ui.wizard.WizardPanelDescriptor;
import org.scijava.ui.behaviour.io.InputTriggerConfig;

import bdv.viewer.ViewerFrame;

public class BoundingBoxDescriptor extends WizardPanelDescriptor
{


	public static final String IDENTIFIER = "Setup bounding-box";

	private final Settings settings;

	private final WindowManager wm;

	private BoundingBoxMamut2 bb;

	public BoundingBoxDescriptor( final Settings settings, final WindowManager wm )
	{
		this.settings = settings;
		this.wm = wm;
		this.panelIdentifier = IDENTIFIER;
		this.targetPanel = new BoundingBoxPanel();

	}

	@Override
	public void displayingPanel()
	{
		final JPanel panel = ( JPanel ) targetPanel;
		panel.removeAll();

		final List< BdvWindow > bdvWindows = wm.getMamutWindowModel().getBdvWindows();
		final ViewerFrame viewerFrame;
		if ( bdvWindows == null || bdvWindows.isEmpty() )
			viewerFrame = wm.createBigDataViewer();
		else
			viewerFrame = bdvWindows.get( 0 ).getViewerFrame();

//		viewerFrame.toFront();
//		viewerFrame.repaint();

		final SharedBigDataViewerData data = wm.getSharedBigDataViewerData();
		final InputTriggerConfig keyconf = wm.getMamutWindowModel().getInputTriggerConfig();
		final int setupID = ( int ) settings.values.getDetectorSettings().get( KEY_SETUP_ID );
		this.bb = new BoundingBoxMamut2( keyconf, viewerFrame, data, setupID );
		panel.add( bb.getControlPanel() );
		panel.revalidate();
		panel.repaint();
	}

	@Override
	public void aboutToHidePanel()
	{
		if (null != bb)
			bb.uninstall();
	}

	@Override
	public String getBackPanelDescriptorIdentifier()
	{
		return SetupIdDecriptor.IDENTIFIER;
	}

	@Override
	public String getNextPanelDescriptorIdentifier()
	{
		return Descriptor1.ID;
	}



	private class BoundingBoxPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		public BoundingBoxPanel()
		{
			super( new GridLayout() );
		}


	}
}
