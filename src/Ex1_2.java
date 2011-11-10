import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.ImageIcon;

import jv.object.PsMainFrame;
import jv.project.PgGeometryIf;
import jv.project.PvCameraIf;
import jv.project.PvDisplayIf;
import jv.project.PvLightIf;
import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;
import jv.vecmath.PiVector;
import jv.viewer.PvDisplay;
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
	private PvDisplay m_disp;
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
		m_disp = (PvDisplay) viewer.getDisplay();

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
		// add to display
		m_disp.addGeometry(geometry);
		m_disp.selectGeometry(geometry);
		m_disp.fit();
		// apply colors
		update();
	}
	// update all geometries
	public void update() {
		if (m_geometry == null) {
			return;
		}
		if (m_sv_view_geometry != null && Arrays.asList(m_disp.getGeometries()).contains(m_sv_view_geometry)) {
			m_disp.removeGeometry(m_sv_view_geometry);
			m_sv_view_geometry = null;
		}
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
			geometry.setElementColor(i, new Color(r, g, b));
			++r;
			if (r > 255) {
				r = 0;
				++g;
			}
			if (g > 255) {
				g = 0;
				++b;
			}
			// note: 255,255,255 is forbidden as it's our background
			// for usv/wsv colorization
			if (b > 254) {
				b = 0;
				System.err.println("polygon id color overflow");
				return;
			}
		}
	}
	private PgElementSet getViewPoints(PgElementSet geometry) {
		// create octahedron 
		//TODO: use octahedron and loop-based algorithm...
		//      but how to add a vertex + edge properly?
		//      how to interpolate to the sphere?
//		PgelementSet view = PwPlatonic.getSolid(PwPlatonic.OCTAHEDRON);
		//FIXME: for now we just pick the default sphere...
		PgElementSet view = new PgElementSet(3);
		view.computeSphere(15, 15, 1); // 15^2 = 225 points 
		view.setTransparency(0.8);
		view.showTransparency(true);
		view.setCenter(geometry.getCenter());
		// rescale platonic to encaps the geometry
		// get maximum size of geometry
		PdVector[] bounds = geometry.getBounds();
		double maxGeomSize = Math.max(bounds[0].maxAbs(), bounds[1].maxAbs());
		// get size of platonic
		bounds = view.getBounds();
		double platonicSize = view.getBounds()[0].maxAbs();
		// scale platonic to enclose geometry
		// TODO: *10 is a bit arbitrary, no?
		view.scale(maxGeomSize / platonicSize * 10);
		return view;
	}
	private void renderOffscreen(BufferedImage image) {
		//render
		m_disp.update(m_disp.getCanvas().getGraphics());
		Graphics2D gfx = image.createGraphics();
		m_disp.render();
		gfx.drawImage(m_disp.getImage(), 0, 0, Color.white, null);
	}
	private double[] getSurfaceVisibility(PgElementSet geometry, PgElementSet viewPoints, boolean weighted) {
		// set colors based on polygon id
		setPolygonIdColors(geometry);

		if (weighted) {
			geometry.assureElementNormals();
		}

		//create image
		final BufferedImage image = new BufferedImage(m_disp.getCanvas().getWidth(),
													  m_disp.getCanvas().getHeight(),
													  BufferedImage.TYPE_INT_RGB);

		// disable lighting / unwanted settings that might temper with colors
		boolean wasShowingVertices = geometry.isShowingVertices();
		geometry.showVertices(false);
		boolean wasShowingEdges = geometry.isShowingEdges();
		geometry.showEdges(false);
		int oldLightningModel = m_disp.getLightingModel();
		m_disp.setLightingModel(PvLightIf.MODEL_SURFACE);
		Color oldBackgroundColor = m_disp.getBackgroundColor();
		m_disp.setBackgroundColor(Color.WHITE);
		boolean wasShowingBorder = m_disp.hasPaintTag(PvDisplayIf.PAINT_BORDER);
		m_disp.setPaintTag(PvDisplayIf.PAINT_BORDER, false);
		boolean hadAntiAliasing = m_disp.hasPaintTag(PvDisplayIf.PAINT_ANTIALIAS);
		m_disp.setPaintTag(PvDisplayIf.PAINT_ANTIALIAS, false);
		final long PAINT_FOCUS = 536870912;
		boolean wasShowingFocus	= m_disp.hasPaintTag(PAINT_FOCUS);
		m_disp.setPaintTag(PAINT_FOCUS, false);
		m_disp.setEnabledExternalRendering(true);
		m_disp.setExternalRenderSize(m_disp.getSize().width, m_disp.getSize().height);

		PvCameraIf cam = m_disp.getCamera();
		PdVector oldCamPos = cam.getPosition();
		PdVector oldCamPOI = cam.getInterest();

		double[] usv = new double[geometry.getNumElements()];
		for(int i = 0; i < usv.length; ++i) {
			usv[i] = 0;
		}

		System.out.println("calculating surface visibility from " + viewPoints.getNumVertices() + " view points...");
		for(int v = 0; v < viewPoints.getNumVertices(); ++v) {
			// view vector for weighted surface visibility
			PdVector view = null;
			if (weighted) {
				view = viewPoints.getVertex(v);
				view.sub(viewPoints.getCenter());
				view.normalize();
			}
			// rotate camera
			cam.setPosition(viewPoints.getVertex(v));
			cam.setInterest(viewPoints.getCenter());
			m_disp.update(geometry);
			renderOffscreen(image);
			
			Set<Integer> knownIds = new HashSet<Integer>();
			for(int w = 0; w < image.getWidth(); ++w) {
				for(int h = 0; h < image.getHeight(); ++h) {
					int rgb = image.getRGB(w, h);
					if (rgb == -1) {
						// white, i.e. background color
						continue;
					}
					Color color = new Color(rgb);
					Integer polygonId = color.getRed() + color.getGreen() * 255 + color.getBlue() * 255 * 255;
					if (polygonId < 0 || polygonId >= geometry.getNumElements()) {
						System.err.println("unknown polygonid: " + polygonId + ", c: " + color + ", w:" + w + ", h: " + h);
						continue;
					}
					if (knownIds.add(polygonId)) {
						if (weighted) {
							usv[polygonId] += Math.abs(view.dot(geometry.getElementNormal(polygonId)));
						} else {
							usv[polygonId] += 1.0;
						}
					}
				}
			}
		}
		System.out.println("done");
		// Restore stuff with border, do it after image has been used
		m_disp.setEnabledExternalRendering(false);
		m_disp.setPaintTag(PAINT_FOCUS, wasShowingFocus);
		m_disp.setPaintTag(PvDisplayIf.PAINT_BORDER, wasShowingBorder);
		m_disp.setPaintTag(PvDisplayIf.PAINT_ANTIALIAS, hadAntiAliasing);
		m_disp.setBackgroundColor(oldBackgroundColor);
		m_disp.setLightingModel(oldLightningModel);

		geometry.showVertices(wasShowingVertices);
		geometry.showEdges(wasShowingEdges);

		cam.setPosition(oldCamPos);
		cam.setInterest(oldCamPOI);

		// normalize
		for(int i = 0; i < usv.length; ++i) {
			usv[i] /= viewPoints.getNumVertices();
		}

		return usv;
	}
	private void setSVColors(PgElementSet geometry, boolean weighted) {
		// get view points
		m_sv_view_geometry = getViewPoints(geometry);
		double[] usv = getSurfaceVisibility(geometry, m_sv_view_geometry, weighted);
		// set colors based on usv map
		for(int p = 0; p < geometry.getNumElements(); ++p) {
			float w = (float) usv[p];
			System.out.println("polygon: " + p + ", sv:" + w);
			geometry.setElementColor(p, new Color(w, w, w));
		}
		
		// DEBUG: add view-geometry to see what's going on
//		m_disp.addGeometry(m_sv_view_geometry);
//		m_disp.fit();
	}
	private void setUSVColors(PgElementSet geometry) {
		System.out.println("updating geometry: usv colors");
		setSVColors(geometry, false);
	}
	private void setWSVColors(PgElementSet geometry) {
		System.out.println("updating geometry: wsv colors");
		setSVColors(geometry, true);
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
			PdVector[] eigenVectors = {new PdVector(0, 0, 0),
									   new PdVector(0, 0, 0),
									   new PdVector(0, 0, 0)};
			PdVector eigenValues = new PdVector(0, 0, 0);
			PnJacobi.computeEigenvectors(m, 3, eigenValues, eigenVectors);
//			PnJacobi.printEigenvectors(3, eigenValues, eigenVectors);
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
			m_boundingBox = new PgElementSet(3);
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
						m_boundingBox.addVertex(sum);
					}
				}
			}
			// now add elements to show the edges
			m_boundingBox.setNumElements(6);
			//NOTE: we must keep the ordering intact
			// top
			m_boundingBox.setElement(1, new PiVector(0, 2, 6, 4));
			// bottom
			m_boundingBox.setElement(2, new PiVector(1, 3, 7, 5));
			// left
			m_boundingBox.setElement(3, new PiVector(4, 5, 7, 6));
			// right
			m_boundingBox.setElement(4, new PiVector(0, 1, 3, 2));
			// enough for a bounding box, as we just need the edges
			// and the edges of near + far will all overlap
			// with those of top, bottom, left, right
			m_boundingBox.assureEdgeColors();
			m_boundingBox.showVertices(true);
			m_boundingBox.showEdges(true);
			m_boundingBox.showElements(false);
			m_boundingBox.showEdgeColors(true);
			m_disp.addGeometry(m_boundingBox);
			m_disp.update(m_boundingBox);
			m_disp.fit();
		}
	}
}
