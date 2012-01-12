import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.Rectangle;

import jv.geom.PgElementSet;
import jv.object.PsMainFrame;
import jv.project.PgGeometryIf;
import jv.project.PvGeometryListenerIf;
import jv.viewer.PvDisplay;
import jv.viewer.PvViewer;

public class ProjectBase implements PvGeometryListenerIf {
	protected PsMainFrame m_frame;
	protected PvDisplay m_disp;
	protected Panel m_panel;
	public ProjectBase(String[] args, String title)
	{
		// Create toplevel window of application containing the applet
		m_frame	= new PsMainFrame(title, args);

		// Create viewer for viewing 3d geometries, and register m_frame.
		PvViewer viewer = new PvViewer(null, m_frame);

		// Get default display from viewer
		m_disp = (PvDisplay) viewer.getDisplay();
		m_disp.setEnabledZBuffer(true);
		m_disp.setEnabledAntiAlias(true);

		// Add display to m_frame
		m_frame.add((Component)m_disp, BorderLayout.CENTER);

		// buttons
		m_panel = new Panel();
		m_panel.setLayout(new GridBagLayout());
		m_frame.add(m_panel, BorderLayout.EAST);

		// Position of left upper corner and size of m_frame when run as application.
		m_frame.setBounds(new Rectangle(420, 5, 640, 550));
	}
	/**
	 * call this at the end of your constructor
	 */
	protected void show()
	{
		m_frame.pack();
		m_frame.setVisible(true);
	}

	//BEGIN: PvGeometryListenerIf
	@Override
	public void addGeometry(PgGeometryIf geometry)
	{
		// do nothing
	}
	@Override
	public void removeGeometry(PgGeometryIf geometry)
	{
		// do nothing
	}
	@Override
	public void selectGeometry(PgGeometryIf geometry)
	{
		// do nothing
	}
	@Override
	public String getName()
	{
		return getClass().getName();
	}
	//END PvGeometryListenerIf
	/**
	 * @return currently selected PgElementSet or null
	 */
	public PgElementSet currentGeometry()
	{
		try {
			return (PgElementSet) m_disp.getSelectedGeometry();
		} catch (Exception e) {
			return null;
		}
	}
}