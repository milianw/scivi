import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;

import jv.geom.PgElementSet;
import jv.object.PsMainFrame;
import jv.project.PgGeometryIf;
import jv.project.PvCameraEvent;
import jv.project.PvCameraListenerIf;
import jv.project.PvGeometryListenerIf;
import jv.vecmath.PdVector;
import jv.vecmath.PiVector;
import jv.viewer.PvDisplay;
import jv.viewer.PvViewer;

/**
 * Solution to second exercise of second project
 * 
 * @author		Milian Wolff
 * @version		24.11.2011, 1.00 created
 */
public class Ex2_2 implements PvGeometryListenerIf, PvCameraListenerIf {
	public static void main(String[] args)
	{
		new Ex2_2(args);
	}

	private PsMainFrame m_frame;
	private PvDisplay m_disp;
	private PgElementSet m_silhouette;
	public Ex2_2(String args[])
	{
		// Create toplevel window of application containing the applet
		m_frame	= new PsMainFrame("SciVis - Project 2 - Exercise 1 - Milian Wolff", args);

		// Create viewer for viewing 3d geometries, and register m_frame.
		PvViewer viewer = new PvViewer(null, m_frame);

		// Get default display from viewer
		m_disp = (PvDisplay) viewer.getDisplay();
		m_disp.setEnabledZBuffer(true);
		m_disp.setEnabledAntiAlias(true);
		m_disp.addGeometryListener(this);
		m_disp.addCameraListener(this);

		// Add display to m_frame
		m_frame.add((Component)m_disp, BorderLayout.CENTER);

		m_frame.pack();
		// Position of left upper corner and size of m_frame when run as application.
		m_frame.setBounds(new Rectangle(420, 5, 640, 550));
		m_frame.setVisible(true);
	}
	//BEGIN: PvGeometryListenerIf
	@Override
	public void addGeometry(PgGeometryIf geometry)
	{
		m_disp.update(geometry);
	}
	@Override
	public void removeGeometry(PgGeometryIf arg0)
	{
		// TODO Auto-generated method stub
	}
	@Override
	public void selectGeometry(PgGeometryIf arg0)
	{
		// TODO Auto-generated method stub
	}
	@Override
	public String getName()
	{
		return "Ex2_2";
	}
	//END PvGeometryListenerIf
	//BEGIN PvCameraListenerIf
	@Override
	public void dragCamera(PvCameraEvent arg0)
	{
		viewUpdated();
	}
	@Override
	public void pickCamera(PvCameraEvent arg0)
	{
		viewUpdated();
	}
	//END PvCameraListenerIf
	private void viewUpdated()
	{
		PdVector ray = m_disp.getCamera().getViewDir();
		drawFaceNormalSilhouette(ray, (PgElementSet) m_disp.getSelectedGeometry());
	}
	private void drawFaceNormalSilhouette(PdVector ray, PgElementSet geometry)
	{
		if (m_silhouette != null) {
			m_disp.removeGeometry(m_silhouette);
			m_disp.update(m_silhouette);
		}
		m_silhouette = new PgElementSet();

		geometry.assureElementNormals();
		assert geometry.hasElementNormals();
		
		// find visible faces
		Set<Integer> visibleFaces = new HashSet<Integer>();
		for(int i = 0; i < geometry.getNumElements(); ++i) {
			double dot = ray.dot(geometry.getElementNormal(i));
			// if the dot product is zero, the face is either visible or hidden :-/
			// we ignore this case, assuming that it only happens for faces somewhere
			// in the middle of the visible surface, hence they do not play a role
			// in finding the silhouette anyways.
			// so we are only interested in the faces with _negative_ dot product
			// as those are visible to us (we are looking along the direction of ray)
			if (dot < 0) {
				visibleFaces.add(i);
			}
		}
		// find visible edges by iterating over the corner table
		CornerTable table = new CornerTable(geometry);
		for(Corner corner : table.corners()) {
			// an edge is part of the silhouette if 
			// a) it is part of a visible face
			// b) either it has adjacent face
			// c) or its adjacent face is not visible
			if (visibleFaces.contains(corner.triangle)
				&& (corner.opposite == null || !visibleFaces.contains(corner.opposite.triangle)))
			{
				///TODO: could we just use something like this:
//				m_disp.getGraphics().drawLine() ?
				// maybe via the transmatrix T?
//				PdMatrix T = m_disp.getCamera().getTransMatrix(PvDisplayIf.MATRIX_TRANS);
				int a = m_silhouette.addVertex(geometry.getVertex(corner.next.vertex));
				int b = m_silhouette.addVertex(geometry.getVertex(corner.prev.vertex));
				int c = m_silhouette.addVertex(geometry.getVertex(corner.vertex));
				m_silhouette.addElement(new PiVector(a, b, c));
			}
		}
		
		System.out.println("adding silhouette");
		m_silhouette.showVertices(false);
		m_silhouette.showElements(false);
		m_silhouette.assureEdgeColors();
		m_silhouette.showEdgeColors(true);
		m_silhouette.showEdgeColorFromVertices(true);
		m_silhouette.setEnabledEdges(true);
		m_silhouette.makeEdgeStars();
		m_silhouette.showEdges(true);
		m_disp.addGeometry(m_silhouette);
		geometry.setVisible(false);
		m_disp.update(geometry);
		m_disp.update(m_silhouette);
	}
}
