package org.mastodon.trackmate.ui.wizard.descriptors;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mastodon.trackmate.ui.wizard.WizardPanelDescriptor;

public class Descriptor2 extends WizardPanelDescriptor
{

	static final String ID = "panel 2";

	public Descriptor2()
	{
		panelIdentifier = ID;
		targetPanel = new Descriptor2Panel();
	}

	@Override
	public String getNextPanelDescriptorIdentifier()
	{
		return Descriptor3.ID;
	}

	private class Descriptor2Panel extends JPanel
	{
		private static final long serialVersionUID = 1L;

		public Descriptor2Panel()
		{
			final JLabel jLabel = new JLabel( "you look" );
			add( jLabel );
		}

	}
}
