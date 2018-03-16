package org.mastodon.trackmate.ui.wizard;

import java.util.ArrayList;
import java.util.List;

import org.scijava.Context;

public class WizardExample
{

	public static void main( final String[] args )
	{
		final List< WizardPanelDescriptor > l = new ArrayList<>();
		l.add( new Descriptor1() );
		l.add( new Descriptor2() );
		l.add( new Descriptor3() );
		final ListWizardSequence sequence = new ListWizardSequence( l  );
		final Wizard wizard = new Wizard( new Context() );
		wizard.show( sequence );
	}
}
