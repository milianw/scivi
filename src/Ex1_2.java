import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;

import jv.object.PsMainFrame;
import jv.project.PgGeometryIf;
import jv.project.PvDisplayIf;
import jv.vecmath.PdVector;
import jv.viewer.PvViewer;
import jv.object.PsConfig;
import jv.geom.PgElementSet;
import jv.loader.PgFileDialog;
import jv.loader.PjImportModel;
import jvx.geom.PwPlatonic;

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
	private Checkbox m_default;
	private Checkbox m_normalMapping;
	private Checkbox m_3DCheckerboard;
	private TextField m_3DCheckerboard_L;
	private Checkbox m_polygonId;
	private Button m_icosahedronButton;

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
		// make sure our assumptions hold
		geometry.assureElementColors();
		geometry.showElementColors(true);
		geometry.assureVertexColors();
		geometry.showVertexColors(true);
		geometry.assureElementNormals();
		// apply colors
		updateGeometry(geometry);
		// add to display
		m_disp.addGeometry(geometry);
		m_disp.selectGeometry(geometry);
		m_disp.fit();
	}
	// update all geometries
	public void update() {
		if (m_disp.getNumGeometries() == 0) {
			return;
		}
		for(PgGeometryIf geometry : m_disp.getGeometries()) {
			updateGeometry((PgElementSet) geometry);
		}
	}
	// f(n) as defined in ex. 2.b for the 3d checkerboard
	public int f(double v, double L) {
		int r = (int) Math.floor(v / L);
		if (r % 2 == 0) {
			return 1;
		} else {
			return 0;
		}
	}
	public void updateGeometry(PgElementSet geometry) {
		if (m_default.getState()) {
			System.out.println("updating geometry: default colors");
			geometry.removeElementColors();
			geometry.removeVertexColors();
			geometry.showVertices(false);
		} else if (m_normalMapping.getState()) {
			geometry.removeVertexColors();
			geometry.showVertices(false);
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
		} else if (m_3DCheckerboard.getState()) {
			geometry.removeElementColors();
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
//			geometry.showElementFromVertexColors(true);
		} else if (m_polygonId.getState()) {
			geometry.removeVertexColors();
			geometry.showVertices(false);
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
		m_disp.update(geometry);
	}
}
