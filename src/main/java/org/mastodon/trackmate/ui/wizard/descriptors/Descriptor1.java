package org.mastodon.trackmate.ui.wizard.descriptors;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mastodon.trackmate.ui.wizard.WizardPanelDescriptor;

public class Descriptor1 extends WizardPanelDescriptor
{

	static final String ID = "panel 1";


	public Descriptor1()
	{
		panelIdentifier = ID;
		targetPanel = new Descriptor1Panel();
	}

	@Override
	public String getNextPanelDescriptorIdentifier()
	{
		return Descriptor2.ID;
	}

	@Override
	public String getBackPanelDescriptorIdentifier()
	{
		return ChooseDetectorDescriptor.IDENTIFIER;
	}

	private class Descriptor1Panel extends JPanel
	{
		private static final long serialVersionUID = 1L;

		public Descriptor1Panel()
		{
			final JLabel jLabel = new JLabel( "Heyyyy" );
			add( jLabel );
		}

	}

}
