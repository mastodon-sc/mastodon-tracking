package org.mastodon.tracking.mamut.trackmate.wizard;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;

import org.mastodon.app.MastodonIcons;
import org.scijava.Context;
import org.scijava.plugin.Parameter;

public class Wizard
{

	@Parameter
	private Context context;

	private final JFrame frame;

	private final WizardLogService wizardLogService;

	public Wizard( final StyledDocument log )
	{
		this.frame = new JFrame();
		this.wizardLogService = new WizardLogService( log );
	}

	public Wizard()
	{
		this( new DefaultStyledDocument() );
	}

	public WizardLogService getLogService()
	{
		return wizardLogService;
	}

	public void show( final WizardSequence sequence, final String title )
	{
		context.inject( wizardLogService );
		final WizardController controller = new WizardController( sequence, wizardLogService );
		frame.getContentPane().removeAll();
		frame.getContentPane().add( controller.getWizardPanel() );
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		frame.setSize( 300, 600 );
		frame.setTitle( title );
		controller.init();
		frame.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
		frame.addWindowListener( controller );
		frame.setIconImage( MastodonIcons.MASTODON_ICON_MEDIUM.getImage() );
		frame.setVisible( true );
	}
}
