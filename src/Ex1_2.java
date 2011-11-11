import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jv.object.PsMainFrame;
import jv.project.PvCameraIf;
import jv.project.PvDisplayIf;
import jv.project.PvLightIf;
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
		PgElementSet geom = PwPlatonic.getSolid(PwPlatonic.ICOSAHEDRON);
		geom.setName("Icosahedron");
		openGeometry(geom);
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
		geometry.showElementFromVertexColors(true);
		geometry.showVertices(false);
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
	private PgElementSet getViewPoints(PgElementSet geometry, int minViewPoints) {
		if (false) {
			// platonic solid solution, which sucks
			return getViewPointsPlatonic(geometry, minViewPoints);
		}
		// better: xsophe grid, see e.g. http://books.google.com/books?id=pQe3y-lHMw0C&lpg=PA150&ots=sg7wrAQDwz&dq=sophe%20grid&pg=PA150#v=onepage&q=sophe%20grid&f=false
		PdVector[] bounds = geometry.getBounds();
		double maxGeomSize = Math.max(bounds[0].maxAbs(), bounds[1].maxAbs());
		double radius = maxGeomSize * 4;
		PgElementSet view = new PgElementSet(3);
		int N = (int) Math.ceil(-1.5 + 0.5 * Math.sqrt(5 + 2 * minViewPoints));
		// add zenith
		view.addVertex(new PdVector(0, 0, radius));
		view.addVertex(new PdVector(0, 0, -radius));
		for(int i = 1; i < N; ++i) {
			double theta = Math.PI / 2 * i / (N-1);
			for (int j = 0; j < i; ++j) {
				double phi = Math.PI / 2 * j / i;
				for(int k = 0; k < 4; ++k) {
					phi += k * Math.PI / 2;
					PdVector vertex = new PdVector(
							radius * Math.sin(theta) * Math.cos(phi),
							radius * Math.sin(theta) * Math.sin(phi),
							radius * Math.cos(theta)
					);
					if (i != N-1) {
						// also add for -z but don't duplicate äquator
						PdVector mVertex = (PdVector) vertex.clone();
						mVertex.setEntry(2, -mVertex.getEntry(2));
						mVertex.add(geometry.getCenter());
						view.addVertex(mVertex);
					}
					vertex.add(geometry.getCenter());
					view.addVertex(vertex);
				}
			}
		}
		view.setName("XSophe Grid " + view.getNumVertices());
		view.showVertices(true);
		return view;
	}
	private PgElementSet getViewPointsPlatonic(PgElementSet geometry, int minViewPoints) {
		// create octahedron 
		PgElementSet view = PwPlatonic.getSolid(PwPlatonic.OCTAHEDRON);

		PdVector center = geometry.getCenter();
		view.setCenter(center);
		// rescale platonic to encaps the geometry
		// get maximum size of geometry
		PdVector[] bounds = geometry.getBounds();
		double maxGeomSize = Math.max(bounds[0].maxAbs(), bounds[1].maxAbs());
		// get size of platonic
		bounds = view.getBounds();
		double platonicSize = view.getBounds()[0].maxAbs();
		// scale platonic to enclose geometry
		// TODO: scale factor is a bit arbitrary, no?
		view.scale(maxGeomSize / platonicSize * 4);

		/* DEBUG:
		view.showVertices(true);
		view.setTransparency(0.8);
		view.showTransparency(true);
		view.showVertexLabels(true);
		*/

		// calculate radius of our platonic solid
		// just get length of vector from any vertex to the center
		PdVector rV = new PdVector(0, 0, 0);
		rV.add(view.getVertex(0));
		rV.sub(center);
		double radius = rV.length();

		// loop and refine structure until we have enough points
		while(view.getNumVertices() < minViewPoints) {
			PgElementSet newView = new PgElementSet();
			// copy old vertices
			// NOTE: newView.setVertices(view.getVertices()) DOES NOT WORK - :-@
			for(int v = 0; v < view.getNumVertices(); ++v) {
				newView.addVertex(view.getVertex(v));
			}
			// make sure we don't count edges twice
			// stupid javaview doesn't let me iterate over the edges :-X
			Map<Integer, Integer> known = new HashMap<Integer, Integer>();
			for(int p = 0; p < view.getNumElements(); ++p) {
				PiVector vertices = view.getElement(p);
				// first get point in middle of element
				PdVector elementCenter = new PdVector(0, 0, 0);
				for(int i = 0; i < vertices.getSize(); ++i) {
					elementCenter.add(view.getVertex(vertices.getEntry(i)));
				}
				// scale to sphere
				elementCenter.normalize();
				elementCenter.multScalar(view.getVertex(0).length());
				int pC = newView.addVertex(elementCenter);

				// now also get points between vertices of the element
				for(int i = 0; i < vertices.getSize(); ++i) {
					int a = vertices.getEntry(i);
					int b = vertices.getEntry((i == vertices.getSize() - 1) ? 0 : i + 1);
					// some number to make sure we don't try to add the
					// center point between two vertices twice
					// (since it's on an edge, it will always be shared between
					//  adjacent elements)
					Integer index = Math.min(a, b) + Math.max(a, b) * view.getNumVertices();
					int c = -1;
					if (known.containsKey(index)) {
						// already handled
						c = known.get(index);
					} else {
						// calculate vector to point between a and b
						PdVector cV = new PdVector(0, 0, 0);
						cV.add(view.getVertex(a));
						cV.add(view.getVertex(b));
						cV.multScalar(0.5);
						// now make a vector from center to point between a and b
						PdVector sV = new PdVector(0, 0, 0);
						sV.add(cV);
						sV.sub(center);
						// now scale it to sphere
						sV.multScalar(1.0 - sV.length() / radius);
						// now add it to cV and be done with it...
						cV.add(sV);
						c = newView.addVertex(cV);
						known.put(index, c);
					}
					// a - pC - c
					newView.addElement(new PiVector(a, pC, c));
					// b - c - pC
					newView.addElement(new PiVector(b, c, pC));
				}
			}
// DEBUG:			m_disp.addGeometry(view);
			view = newView;
/* DEBUG:
			view.showVertices(true);
			view.setTransparency(0.8);
			view.showTransparency(true);
			view.showVertexLabels(true);
			System.out.println(view.getNumVertices());
			break;
*/
		}

		view.setName("Subdivided Platonic Solid " + view.getNumVertices());

		return view;
	}
	private void renderOffscreen(BufferedImage image) {
		//render
		m_disp.update(m_disp.getCanvas().getGraphics());
		Graphics2D gfx = image.createGraphics();
		gfx.drawImage(m_disp.getImage(), 0, 0, Color.white, null);
		m_disp.render();
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
		PdVector oldCamPos = (PdVector) cam.getPosition().clone();
		PdVector oldCamPOI = (PdVector) cam.getInterest().clone();

		double[] usv = new double[geometry.getNumElements()];
		for(int i = 0; i < usv.length; ++i) {
			usv[i] = 0;
		}

		double[] viewWeights = new double[viewPoints.getNumVertices()];

		System.out.println("calculating surface visibility from " + viewPoints.getNumVertices() + " view points...");
		for(int v = 0; v < viewPoints.getNumVertices(); ++v) {
			// view vector for weighted surface visibility
			PdVector view = null;
			if (weighted) {
				view = (PdVector) viewPoints.getVertex(v).clone();
				view.sub(viewPoints.getCenter());
				view.normalize();

				viewWeights[v] = 0;
			} else {
				viewWeights[v] = 1;
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
					if (!knownIds.add(rgb)) {
						// already handled
						continue;
					}
					Color color = new Color(rgb);
					int polygonId = color.getRed() + color.getGreen() * 255 + color.getBlue() * 255 * 255;
					if (polygonId < 0 || polygonId >= geometry.getNumElements()) {
						System.err.println("unknown polygonid: " + polygonId + ", c: " + color + ", w:" + w + ", h: " + h);
						continue;
					}
					if (weighted) {
						double weight = Math.abs(view.dot(geometry.getElementNormal(polygonId)));
						usv[polygonId] += weight;
						if (weight > viewWeights[v]) {
							viewWeights[v] = weight;
						}
					} else {
						usv[polygonId] += 1.0;
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
		if (weighted) {
			// weight by sum of weighted views
			double normalization = 0;
			for(int i = 0; i < viewWeights.length; ++i) {
				normalization += viewWeights[i];
			}

			for(int i = 0; i < usv.length; ++i) {
				usv[i] /= normalization;
			}
		} else {
			for(int i = 0; i < usv.length; ++i) {
				usv[i] /= viewPoints.getNumVertices();
			}
		}

		return usv;
	}
	private void setSVColors(PgElementSet geometry, boolean weighted) {
		// get view points
		m_sv_view_geometry = getViewPoints(geometry, 200);
		double[] usv = getSurfaceVisibility(geometry, m_sv_view_geometry, weighted);
		// set colors based on usv map
		for(int p = 0; p < geometry.getNumElements(); ++p) {
			float w = (float) usv[p];
//			System.out.println("polygon: " + p + ", sv:" + w);
			geometry.setElementColor(p, new Color(w, w, w));
		}
		
		// DEBUG: add view-geometry to see what's going on
		m_disp.addGeometry(m_sv_view_geometry);
		m_disp.fit();
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
