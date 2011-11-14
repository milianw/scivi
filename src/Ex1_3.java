import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import jv.object.PsMainFrame;
import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;
import jv.vecmath.PiVector;
import jv.viewer.PvDisplay;
import jv.viewer.PvViewer;
import jv.object.PsConfig;
import jv.geom.PgEdgeStar;
import jv.geom.PgElementSet;
import jv.loader.PgFileDialog;
import jv.loader.PjImportModel;
import jvx.geom.PwPlatonic;
import jvx.numeric.PnJacobi;

/**
 * Solution to the third exercise
 * 
 * @author		Milian Wolff
 * @version		28.10.2011, 1.00 created
 */
public class Ex1_3 implements ActionListener {
	private Button m_openButton;
	private PsMainFrame m_frame;
	private PvDisplay m_disp;
	// main geometry
	private PgElementSet m_geometry;
	private Button m_icosahedronButton;
	private Button m_boundingBoxButton;
	private int m_boundingBoxStatus;
	private PgElementSet m_boundingBox;

	public Ex1_3(String args[]) {
		// Create toplevel window of application containing the applet
		m_frame	= new PsMainFrame("SciVis - Exercise 3 - Milian Wolff", args);

		// Create viewer for viewing 3d geometries, and register m_frame.
		PvViewer viewer = new PvViewer(null, m_frame);

		// Get default display from viewer
		m_disp = (PvDisplay) viewer.getDisplay();
		m_disp.setEnabledZBuffer(true);

		// Add display to m_frame
		m_frame.add((Component)m_disp, BorderLayout.CENTER);
		
        Panel buttons = new Panel();
//        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        buttons.setLayout(new GridBagLayout());
        m_frame.add(buttons, BorderLayout.EAST);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth  = 2;
        c.gridx = 0;
        c.gridy = 0;

        // open geometry button
        m_openButton = new Button("Open Geometry");
        m_openButton.addActionListener(this);
        buttons.add(m_openButton, c);

        // open icosahedron button
        m_icosahedronButton = new Button("Open Icosahedron");
        m_icosahedronButton.addActionListener(this);
        c.gridy++;
        buttons.add(m_icosahedronButton, c);

        // show bounding box
        m_boundingBoxButton = new Button("Bounding Box");
        m_boundingBoxButton.addActionListener(this);
        m_boundingBoxStatus = 0;
        c.gridy++;
        buttons.add(m_boundingBoxButton, c);
        
		m_frame.pack();

		// Position of left upper corner and size of m_frame when run as application.
		m_frame.setBounds(new Rectangle(420, 5, 640, 550));
		m_frame.setVisible(true);
	}
	public static void main(String args[]) {
		new Ex1_3(args);
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		Object source = event.getSource();
		if (source == m_openButton) {
			openFile();
		} else if (source == m_icosahedronButton) {
			openIcosahedron();
		} else if (source == m_boundingBoxButton) {
			toggleBoundingBox();
		}
	}
	public void openFile() {
		PgFileDialog dlg = new PgFileDialog(m_frame, "open geometry", 0);
		dlg.setDirectory(PsConfig.getCodeBase() + "../proj1");
		dlg.show();
		if (dlg.isFileSelected()) {
			openFile(dlg.getFullFileName());
		}
		
	}
	public void openFile(String file) {
		// load file
		PjImportModel importer = new PjImportModel();
		System.out.println("loading data from file: " + file);
		if (importer.load(file)) {
			openGeometry((PgElementSet)importer.getGeometry().clone());
			System.out.println("loading succeeded");
		} else {
			System.err.println("failed loading geometry from file: " + file);
		}
	}
	public void openIcosahedron() {
		PgElementSet geom = PwPlatonic.getSolid(PwPlatonic.ICOSAHEDRON);
		geom.setName("Icosahedron");
		openGeometry(geom);
	}
	public void openGeometry(PgElementSet geometry) {
		// remove existing geometry
		m_disp.removeGeometries();
		m_boundingBox = null;
		m_boundingBoxStatus = 0;
		m_geometry = geometry;
		// add to display
		m_disp.addGeometry(geometry);
		m_disp.selectGeometry(geometry);
		m_disp.fit();
		m_disp.update(m_geometry);
	}
	private void toggleBoundingBox() {
		if (m_geometry == null) {
			return;
		}
		m_boundingBoxStatus++;
		if (m_boundingBoxStatus > 3) {
			m_boundingBoxStatus = 0;
		}
		PdMatrix m = null;

		if (m_boundingBox != null) {
			// always hide old variant
			m_disp.removeGeometry(m_boundingBox);
			m_disp.update(m_boundingBox);
			m_boundingBox = null;
		}

		if (m_boundingBoxStatus == 0) {
			// remove bounding box
			System.out.println("Hiding Bounding Box");
			// nothing to do
		} else if (m_boundingBoxStatus == 1) {
			// create bounding box based on first and second moment
			System.out.println("Showing Default Bounding Box");
			// center of mass
			PdVector c = m_geometry.getCenterOfGravity();
			// fill covariance matrix
			m = new PdMatrix(3,3);
			for(int v = 0; v < m_geometry.getNumVertices(); ++v) {
				PdVector p = m_geometry.getVertex(v);
				for(int i = 0; i < 3; ++i) {
					for(int j = 0; j < 3; ++j) {
						double val = (p.getEntry(i) - c.getEntry(i))
								   * (p.getEntry(j) - c.getEntry(j));
						m.setEntry(i, j, m.getEntry(i, j) + val);
					}
				}
			}
		} else if (m_boundingBoxStatus == 2){
			// create bounding box based on first + second moment while taking total area into account
			// we do that by weighting each vertex by the sum of the third of all adjacent triangles
			System.out.println("Showing Area Including Bounding Box");
			// center of mass
			PdVector c = m_geometry.getCenterOfGravity();
			// fill covariance matrix
			m = new PdMatrix(3,3);
			for(int p = 0; p < m_geometry.getNumElements(); ++p) {
				PdVector[] vertices = m_geometry.getElementVertices(p);
				// calculate area
				// see also: http://en.wikipedia.org/wiki/Triangle#Computing_the_area_of_a_triangle
				double area = 0.5 * PdVector.crossNew(vertices[0], vertices[1]).length();
				for(int v = 0; v < vertices.length; ++v) {
					PdVector vertex = vertices[v];
					for(int i = 0; i < 3; ++i) {
						for(int j = 0; j < 3; ++j) {
							double val = (vertex.getEntry(i) - c.getEntry(i))
									   * (vertex.getEntry(j) - c.getEntry(j))
									   * area / 3;
							m.setEntry(i, j, m.getEntry(i, j) + val);
						}
					}
				}
			}
		} else {
			System.out.println("Showing Normal Based Bounding Box");
			// create bounding box based on element normals + area of the element
			// center of mass
			PdVector c = m_geometry.getCenterOfGravity();
			// fill covariance matrix
			m = new PdMatrix(3,3);
			m_geometry.assureElementNormals();
			for(int p = 0; p < m_geometry.getNumElements(); ++p) {
				PdVector[] vertices = m_geometry.getElementVertices(p);
				// calculate area
				// see also: http://en.wikipedia.org/wiki/Triangle#Computing_the_area_of_a_triangle
				double area = 0.5 * PdVector.crossNew(vertices[0], vertices[1]).length();
				// sum normal weighted by area
				PdVector normal = m_geometry.getElementNormal(p);
				for(int i = 0; i < 3; ++i) {
					for(int j = 0; j < 3; ++j) {
						double val = (normal.getEntry(i) - c.getEntry(i))
								   * (normal.getEntry(j) - c.getEntry(j))
								   * area;
						m.setEntry(i, j, m.getEntry(i, j) + val);
					}
				}
			}
		}
		if (m != null) {
			m_boundingBox = getBoundingBox(m);
			m_disp.addGeometry(m_boundingBox);
			m_disp.update(m_boundingBox);
		}
	}
	private PgElementSet getBoundingBox(PdMatrix m) {
		// get eigen values: note, we first have to init the output vars O_o stupid api...
		PdVector[] eigenVectors = {new PdVector(0, 0, 0),
								   new PdVector(0, 0, 0),
								   new PdVector(0, 0, 0)};
		PdVector eigenValues = new PdVector(0, 0, 0);
		PnJacobi.computeEigenvectors(m, 3, eigenValues, eigenVectors);
//		PnJacobi.printEigenvectors(3, eigenValues, eigenVectors);
		//note: looks like the result is already sorted
		// 0 -> minor, 1 -> middle, 2 -> major
		// now find dimensions of bounding box by getting the biggest + smallest
		// projections of vertices onto the eigen vectors
		double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
		double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
		for(int v = 0; v < m_geometry.getNumVertices(); ++v) {
			PdVector p = m_geometry.getVertex(v);
			for(int i = 0; i < 3; ++i) {
				double dot = p.dot(eigenVectors[i]);
				if (dot > max[i]) {
					max[i] = dot;
				} else if (dot < min[i]) {
					min[i] = dot;
				}
			}
		}
		// now visualize bounding box via new element set
		PgElementSet box = new PgElementSet(3);
		box.setName("Bounding Box");
		// first add the vertices, basically possible additions/subtractions of x,y,z 
		// top right far: 0
		// bottom right far: 1
		// top right near: 2
		// bottom right near: 3
		// top left far: 4
		// bottom left far: 5
		// top left near: 6
		// bottom left near: 7
		PdVector x = eigenVectors[0];
		PdVector y = eigenVectors[1];
		PdVector z = eigenVectors[2];
		for (int xSign = -1; xSign <= 1; xSign += 2) {
			for (int ySign = -1; ySign <= 1; ySign += 2) {
				for (int zSign = -1; zSign <= 1; zSign += 2) {
					PdVector sum = new PdVector(0, 0, 0);
					// x * xSign + y * ySign + z * zSign - stupid api!
					for(int i = 0; i < 3; ++i) {
						sum.setEntry(i, x.getEntry(i) * (xSign < 0 ? min[0] : max[0])
										+ y.getEntry(i) * (ySign < 0 ? min[1] : max[1])
										+ z.getEntry(i) * (zSign < 0 ? min[2] : max[2]));
					}
					box.addVertex(sum);
				}
			}
		}
		// now add elements to show the edges
		box.setNumElements(6);
		//NOTE: we must keep the ordering intact
		// top
		box.setElement(1, new PiVector(0, 2, 6, 4));
		// bottom
		box.setElement(2, new PiVector(1, 3, 7, 5));
		// left
		box.setElement(3, new PiVector(4, 5, 7, 6));
		// right
		box.setElement(4, new PiVector(0, 1, 3, 2));
		// enough for a bounding box, as we just need the edges
		// and the edges of near + far will all overlap
		// with those of top, bottom, left, right
		box.assureEdgeColors();
		box.showVertices(true);
		box.showEdges(true);
		box.showElements(false);
		box.showEdgeColors(true);
		box.showEdgeColorFromVertices(true);
		box.setEnabledEdges(true);
		box.makeEdgeStars();

		System.out.println(box.getNumEdgeStars());
		Color[] colors = new Color[box.getNumEdgeStars()];
		// top right far: 0
		// bottom right far: 1
		// top right near: 2
		// bottom right near: 3
		// top left far: 4
		// bottom left far: 5
		// top left near: 6
		// bottom left near: 7
		for(int i = 0; i < box.getNumEdgeStars(); ++i) {
			PgEdgeStar star = box.getEdgeStar(i);
			int v1 = star.getVertexInd(0);
			int v2 = star.getVertexInd(1);
			if (v2 - v1 == 1) {
				colors[i] = Color.red;
			} else if (v2 - v1 == 2) {
				colors[i] = Color.green;
			} else {
				colors[i] = Color.blue;
			}
		}
		box.setEdgeColors(colors);
		// top right far:
		PdVector vol = (PdVector) box.getVertex(0).clone();
		// subtract bottom left near:
		vol.sub(box.getVertex(7));
		double volume = Math.abs(vol.getEntry(0) * vol.getEntry(1) * vol.getEntry(2));
		System.out.println("Bounding box volumen: " + volume);
		return box;
	}
}
