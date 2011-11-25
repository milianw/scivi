import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;

import jv.geom.PgElementSet;
import jv.geom.PgPolygonSet;
import jv.object.PsMainFrame;
import jv.project.PgGeometryIf;
import jv.project.PvCameraEvent;
import jv.project.PvCameraListenerIf;
import jv.project.PvGeometryListenerIf;
import jv.project.PvLightIf;
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
	private PgPolygonSet m_silhouette;
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

		// disable lightning
		m_disp.setLightingModel(PvLightIf.MODEL_SURFACE);
		// 3D look is nicer imo
		m_disp.setEnabled3DLook(true);
		// white background (not neccessary)
//		m_disp.setBackgroundColor(Color.white);

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
		assert m_disp.getSelectedGeometry() == geometry;
		viewUpdated();
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
		drawSilhouette(m_disp.getSelectedGeometry());
	}
	private void drawSilhouette(PgGeometryIf g)
	{
		PgElementSet geometry = (PgElementSet) g;
		if (geometry == null) {
			return;
		}
		drawFaceNormalSilhouette(geometry);

		// make geometry completely white
		geometry.setGlobalElementColor(Color.white);
		m_disp.update(geometry);
	}
	private void drawFaceNormalSilhouette(PgElementSet geometry)
	{
		clearSilhouette();
		assert m_silhouette == null;

		m_silhouette = new PgPolygonSet();

		geometry.assureElementNormals();
		assert geometry.hasElementNormals();
		
		// find visible faces
		PdVector ray = m_disp.getCamera().getViewDir();
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
				int a = m_silhouette.addVertex(geometry.getVertex(corner.next.vertex));
				int b = m_silhouette.addVertex(geometry.getVertex(corner.prev.vertex));
				m_silhouette.addPolygon(new PiVector(a, b));
			}
		}
		
		System.out.println("adding silhouette");
		m_silhouette.showVertices(false);
		m_silhouette.showEdgeColorFromVertices(true);
		m_silhouette.setGlobalPolygonColor(Color.black);

		m_disp.addGeometry(m_silhouette);
		m_disp.update(m_silhouette);
	}
	void clearSilhouette()
	{
		if (m_silhouette != null) {
			m_disp.removeGeometry(m_silhouette);
			m_disp.update(m_silhouette);
			m_silhouette = null;
		}
	}
}
