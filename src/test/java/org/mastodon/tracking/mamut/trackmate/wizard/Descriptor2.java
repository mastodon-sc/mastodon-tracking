package org.mastodon.tracking.mamut.trackmate.wizard;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mastodon.tracking.mamut.trackmate.wizard.WizardPanelDescriptor;

public class Descriptor2 extends WizardPanelDescriptor
{

	private static final String ID = "panel 2";

	public Descriptor2()
	{
		panelIdentifier = ID;
		targetPanel = new Descriptor2Panel();
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
