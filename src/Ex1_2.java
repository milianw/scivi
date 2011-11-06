import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.swing.ImageIcon;

import jv.object.PsMainFrame;
import jv.project.PgGeometryIf;
import jv.project.PvDisplayIf;
import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;
import jv.vecmath.PiVector;
import jv.viewer.PvViewer;
import jv.object.PsConfig;
import jv.geom.PgElementSet;
import jv.loader.PgFileDialog;
import jv.loader.PjImportModel;
import jvx.geom.PwPlatonic;
import jvx.numeric.PnJacobi;

/**
 * Solution to the first exercise
 * 
 * @author		Milian Wolff
 * @version		28.10.2011, 1.00 created
 */
public class Ex1_2 implements ActionListener, ItemListener {
	private Button m_openButton;
	private PsMainFrame m_frame;
	private PvDisplayIf m_disp;
	// main geometry
	private PgElementSet m_geometry;
	private Checkbox m_default;
	private Checkbox m_normalMapping;
	private Checkbox m_3DCheckerboard;
	private TextField m_3DCheckerboard_L;
	private Checkbox m_polygonId;
	private Button m_icosahedronButton;
	// unweighted surface visibility
	private Checkbox m_usv;
	// weighted surface visibility
	private Checkbox m_wsv;
	// geometry that surrounds selected geometry
	// its vertices are the view points for the usv/wsv calculations 
	private PgElementSet m_sv_view_geometry;
	private Button m_boundingBoxButton;
	private int m_boundingBoxStatus;
	private PgElementSet m_boundingBox;

	public Ex1_2(String args[]) {
		// Create toplevel window of application containing the applet
		m_frame	= new PsMainFrame("SciVis - Exercise 1 - Milian Wolff", args);

		// Create viewer for viewing 3d geometries, and register m_frame.
		PvViewer viewer = new PvViewer(null, m_frame);

		// Get default display from viewer
		m_disp = viewer.getDisplay();

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
        
        // color choice
        CheckboxGroup group = new CheckboxGroup();

        // default colors
        m_default = new Checkbox("Default Colors", group, true);
        m_default.addItemListener(this);
        c.gridy++;
        buttons.add(m_default, c);

        // normal mapping
        m_normalMapping = new Checkbox("Normal Map", group, false);
        m_normalMapping.addItemListener(this);
        c.gridy++;
        buttons.add(m_normalMapping, c);

        // 3d checkerboard
        m_3DCheckerboard = new Checkbox("3D Checkerboard", group, false);
        m_3DCheckerboard.addItemListener(this);
        c.gridy++;
        buttons.add(m_3DCheckerboard, c);
        m_3DCheckerboard_L = new TextField();
        m_3DCheckerboard_L.addActionListener(this);
        m_3DCheckerboard_L.setText("1");
        c.gridy++;
        c.gridwidth = 1;
        buttons.add(new Label("L:"), c);
        c.gridx = 1;
        buttons.add(m_3DCheckerboard_L, c);
        c.gridx = 0;
        c.gridwidth = 2;

        // polygon ID
        m_polygonId = new Checkbox("Polygon ID", group, false);
        m_polygonId.addItemListener(this);
        c.gridy++;
        buttons.add(m_polygonId, c);

        // unweighted surface visibility
        m_usv = new Checkbox("USV", group, false);
        m_usv.addItemListener(this);
        c.gridy++;
        buttons.add(m_usv, c);

        // weighted surface visibility
        m_wsv = new Checkbox("WSV", group, false);
        m_wsv.addItemListener(this);
        c.gridy++;
        buttons.add(m_wsv, c);

		m_frame.pack();

		// Position of left upper corner and size of m_frame when run as application.
		m_frame.setBounds(new Rectangle(420, 5, 640, 550));
		m_frame.setVisible(true);
	}
	public static void main(String args[]) {
		new Ex1_2(args);
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		Object source = event.getSource();
		if (source == m_openButton) {
			openFile();
		} else if (source == m_3DCheckerboard_L) {
			update();
		} else if (source == m_icosahedronButton) {
			openIcosahedron();
		} else if (source == m_boundingBoxButton) {
			toggleBoundingBox();
		}
	}
	@Override
	public void itemStateChanged(ItemEvent e) {
		update();
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
		openGeometry(PwPlatonic.getSolid(PwPlatonic.ICOSAHEDRON));
	}
	public void openGeometry(PgElementSet geometry) {
		// remove existing geometry
		m_disp.removeGeometries();
		m_sv_view_geometry = null;
		m_boundingBox = null;
		m_boundingBoxStatus = 0;
		m_geometry = geometry;
		// make sure our assumptions hold
		geometry.assureElementColors();
		geometry.showElementColors(true);
		geometry.assureVertexColors();
		geometry.showVertexColors(true);
		geometry.assureElementNormals();
		// apply colors
		update();
		// add to display
		m_disp.addGeometry(geometry);
		m_disp.selectGeometry(geometry);
		m_disp.fit();
	}
	// update all geometries
	public void update() {
		if (m_geometry == null) {
			return;
		}
		if (m_sv_view_geometry != null) {
//			m_disp.removeGeometry(m_sv_view_geometry);
			m_sv_view_geometry = null;
		}
		m_disp.setLightingModel(1);
		m_geometry.removeElementColors();
		m_geometry.removeVertexColors();
		m_geometry.showVertices(false);
		if (m_default.getState()) {
			System.out.println("updating geometry: default colors");
			// nothing to do
		} else if (m_normalMapping.getState()) {
			setNormalMappingColors(m_geometry);
		} else if (m_3DCheckerboard.getState()) {
			set3DCheckerboardColors(m_geometry);
		} else if (m_polygonId.getState()) {
			setPolygonIdColors(m_geometry);
		} else if (m_usv.getState()) {
			setUSVColors(m_geometry);
		} else if (m_wsv.getState()) {
			setWSVColors(m_geometry);
		}
		m_disp.update(m_geometry);
	}
	private void setNormalMappingColors(PgElementSet geometry) {
		System.out.println("updating geometry: normal mapping");
		for (int i = 0; i < geometry.getNumElements(); ++i) {
			PdVector normal = geometry.getElementNormal(i);
			Color c = new Color(
				(int) (255 * Math.abs(normal.getEntry(0))),
				(int) (255 * Math.abs(normal.getEntry(1))),
				(int) (255 * Math.abs(normal.getEntry(2)))
			);
			geometry.setElementColor(i, c);
		}
	}
	// f(n) as defined in ex. 2.b for the 3d checkerboard
	private int f(double v, double L) {
		int r = (int) Math.floor(v / L);
		if (r % 2 == 0) {
			return 1;
		} else {
			return 0;
		}
	}
	private void set3DCheckerboardColors(PgElementSet geometry) {
		geometry.showVertices(true);
		System.out.println("updating geometry: 3D checkerboard");
		double L = 0;
		try {
			L = Double.valueOf(m_3DCheckerboard_L.getText());
		} catch(NumberFormatException ex) {
			L = 0;
		}
		if (L <= 0) {
			System.err.println("invalid L!");
			return;
		}
		for(int i = 0; i < geometry.getNumVertices(); ++i) {
			PdVector vertex = geometry.getVertex(i);
			Color c = new Color(
				255 * f(vertex.getEntry(0), L),
				255 * f(vertex.getEntry(1), L),
				255 * f(vertex.getEntry(2), L)
			);
			geometry.setVertexColor(i, c);
		}
//		geometry.showElementFromVertexColors(true);
	}
	private void setPolygonIdColors(PgElementSet geometry) {
		System.out.println("updating geometry: polygon ID");
		int r = 0; 
		int g = 0;
		int b = 0;
		for(int i = 0; i < geometry.getNumElements(); ++i) {
			++r;
			if (r > 255) {
				r = 0;
				++g;
			}
			if (g > 255) {
				g = 0;
				++b;
			}
			if (b > 255) {
				b = 0;
				System.err.println("polygon id color overflow");
			}
			geometry.setElementColor(i, new Color(r, g, b));
		}
	}
	private void setUSVColors(PgElementSet geometry) {
		m_disp.setLightingModel(0);
		// create octahedron 
		//TODO: use octahedron and loop-based algorithm...
		//      but how to add a vertex + edge properly?
		//      how to interpolate to the sphere?
//		m_sv_view_geometry = PwPlatonic.getSolid(PwPlatonic.OCTAHEDRON);
		m_sv_view_geometry = new PgElementSet();
		m_sv_view_geometry.computeSphere(15, 15, 1);
		System.out.println(m_sv_view_geometry.getNumVertices());
		m_sv_view_geometry.setTransparency(0.8);
		m_sv_view_geometry.showTransparency(true);
		m_sv_view_geometry.setCenter(geometry.getCenter());
		// rescale platonic to encaps the geometry
		// get maximum size of geometry
		PdVector[] bounds = geometry.getBounds();
		double maxGeomSize = Math.max(bounds[0].maxAbs(), bounds[1].maxAbs());
		// get size of platonic
		bounds = m_sv_view_geometry.getBounds();
		double platonicSize = m_sv_view_geometry.getBounds()[0].maxAbs();
		// scale platonic to enclose geometry
		m_sv_view_geometry.scale(maxGeomSize / platonicSize * 10);
		System.out.println("geom size: " + maxGeomSize + ", platonic: " + platonicSize);
		// add view-geometry to see what's going on
//		m_disp.addGeometry(m_sv_view_geometry);
		
		setPolygonIdColors(geometry);
		m_disp.setBackgroundColor(new Color(255, 255, 255, 0));
		
		for(int v = 0; v < m_sv_view_geometry.getNumVertices(); ++v) {
			// rotate camera
			m_disp.getCamera().setPosition(m_sv_view_geometry.getVertex(v));
			// update, just to make sure we get the updated view
			m_disp.update(geometry);
			// get image buffer
			BufferedImage image = (BufferedImage) m_disp.getImage();
			Set<Integer> knownIds = new HashSet<Integer>();
			for(int w = 0; w < image.getWidth(); ++w) {
				for(int h = 0; h < image.getHeight(); ++h) {
					Color color = new Color(image.getRGB(w, h));
					if (color.getAlpha() == 0) {
						continue;
					}
					Integer polygonId = color.getRed() + color.getGreen() * 255 + color.getBlue() * 255 * 255;
					if (polygonId < 0 || polygonId >= geometry.getNumElements()) {
						System.err.println("unknown polygonid: " + polygonId + "c: " + color);
						continue;
					}
					if (knownIds.add(polygonId)) {
						System.out.println("id: " + polygonId + "known: " + knownIds.size());
					}
				}
			}
			break;
		}
		
	}
	private void setWSVColors(PgElementSet geometry) {
		System.err.println("weighted surface visibility: not yet implemented...");
	}
	private void toggleBoundingBox() {
		if (m_geometry == null) {
			return;
		}
		m_boundingBoxStatus++;
		if (m_boundingBoxStatus > 1) {
			m_boundingBoxStatus = 0;
		}
		if (m_boundingBoxStatus == 0) {
			// remove bounding box
			System.out.println("Hiding Bounding Box");
			m_disp.removeGeometry(m_boundingBox);
			m_disp.update(m_boundingBox);
			m_boundingBox = null;
		} else {
			// create bounding box
			System.out.println("Showing Bounding Box");
			// center of mass
			PdVector c = m_geometry.getCenterOfGravity();
			// fill covariance matrix
			PdMatrix m = new PdMatrix(3,3);
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
			// get eigen values: note, we first have to init the output vars O_o stupid api...
			PdVector[] eigenVectors = new PdVector[3]; 
			for(int i = 0; i < 3; ++i) {
				eigenVectors[i] = new PdVector(0, 0, 0);
			}
			PdVector eigenValues = new PdVector(0, 0, 0);
			PnJacobi.computeEigenvectors(m, 3, eigenValues, eigenVectors);
//			PnJacobi.printEigenvectors(3, eigenValues, eigenVectors);
			PdVector x = eigenVectors[0];
			PdVector y = eigenVectors[1];
			PdVector z = eigenVectors[2];
			// now visualize bounding box via new element set
			m_boundingBox = new PgElementSet();
			// first add the vertices, basically possible additions/subtractions of x,y,z 
			for (int xSign = -1; xSign <= 1; xSign += 2) {
				for (int ySign = -1; ySign <= 1; ySign += 2) {
					for (int zSign = -1; zSign <= 1; zSign += 2) {
						PdVector sum = new PdVector(0, 0, 0);
						// x * xSign + y * ySign + z * zSign - stupid api!
						for(int i = 0; i < 3; ++i) {
							sum.setEntry(i, x.getEntry(i) * xSign
											+ y.getEntry(i) * ySign
											+ z.getEntry(i) * zSign);
						}
						m_boundingBox.addVertex(sum);
					}
				}
			}
			// now also show some edges by connecting all
			// vertices that share two coordinates
			int edgeNum = 0;
			m_boundingBox.setNumEdges(12);
			for(int i = 0; i < m_boundingBox.getNumVertices(); ++i) {
				PdVector pi = m_boundingBox.getVertex(i);
				for(int j = i; j < m_boundingBox.getNumVertices(); ++j) {
					PdVector pj = m_boundingBox.getVertex(j);
					int sameCoordinates = 0;
					for(int k = 0; k < 3; ++k) {
						if (pi.getEntry(k) == pj.getEntry(k)) {
							sameCoordinates++;
						}
					}
					if (sameCoordinates == 2) {
						// connect vertices
						PiVector edge = new PiVector(i, j);
						m_boundingBox.setEdge(edgeNum, edge);
						//FIXME: HOW DO I SET THE EDGE COLOR?
						++edgeNum;
					}
				}
			}
			m_boundingBox.showVertices(true);
			//FIXME: HOW DO I MAKE THE EDGES VISIBLE?
			m_boundingBox.showEdges(true);
			m_boundingBox.showEdgeColors(true);
			m_disp.addGeometry(m_boundingBox);
			m_disp.update(m_boundingBox);
			m_disp.fit();
		}
	}
}
