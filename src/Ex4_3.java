/*
	Copyright 2012 Milian Wolff <mail@milianw.de>
	
	This program is free software; you can redistribute it and/or
	modify it under the terms of the GNU General Public License as
	published by the Free Software Foundation; either version 2 of 
	the License, or (at your option) any later version.
	
	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.Timer;

import jv.geom.PgPointSet;
import jv.geom.PgPolygonSet;
import jv.geom.PgVectorField;
import jv.number.PuComplex;
import jv.number.PuDouble;
import jv.object.PsUpdateIf;
import jv.project.PgGeometryIf;
import jv.project.PvCameraIf;
import jv.project.PvDisplayIf;
import jv.project.PvGeometryListenerIf;
import jv.project.PvPickEvent;
import jv.project.PvPickListenerIf;
import jv.vecmath.PdBary;
import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;
import jv.vecmath.PiVector;
import jvx.surface.PgDomain;
import jvx.surface.PgDomainDescr;
import jvx.vector.PwLIC;

/**
 * @author		Milian Wolff
 * @version		10.01.2012, 1.00 created
 */
public class Ex4_3
	extends ProjectBase
	implements PvGeometryListenerIf, PsUpdateIf, PvPickListenerIf, ItemListener, ActionListener
{
	private PwLIC m_lic;
	private PgDomain m_domain;
	private PgVectorField m_vec;
	private TensorField m_field;
	private InterpolatedTensorField m_interpolatedField;
	private TensorFieldPanel m_tensorPanel;
	private Checkbox m_add;
	private Checkbox m_remove;
	private Checkbox m_select;
	private PuDouble m_flowRotate;
	private Checkbox m_flowReflect;
	private Timer m_timer;
	private PgPointSet m_degeneratePoints;
	private Button m_clear;
	private PgPolygonSet m_separatrices;
	private Checkbox m_showSeparatrices;
	private JComboBox m_direction;
	private enum Direction {
		Major,
		Minor
	}
	
	public static void main(String[] args)
	{
		new Ex4_3(args);
	}

	public Ex4_3(String[] args)
	{
		super(args, "SciVis - Project 4 - Exercise 3 - Milian Wolff");

		m_timer = new Timer(100, this);

		m_disp.setMajorMode(PvDisplayIf.MODE_INITIAL_PICK);

		m_field = new TensorField();
		m_field.setParent(this);
		m_disp.addGeometry(m_field.termBasePoints());

		// listener
		m_disp.addGeometryListener(this);

		m_domain = new PgDomain(2);
		m_domain.setName("Domain for Tensor Field");
		m_domain.setDimOfElements(3);
		m_domain.showVectorFields(false);
		m_domain.showEdges(false);
		
		PgDomainDescr descr = m_domain.getDescr();
		descr.setSize( -10., -10., 10., 10.);
		descr.setDiscr(10, 10);
		//update vector field if descritization changes
		descr.addUpdateListener(this);
		m_domain.compute();
		
		m_vec = new PgVectorField(2);
		m_vec.setName("Vector Field");
		m_vec.setBasedOn(PgVectorField.VERTEX_BASED);
		m_vec.setNumVectors(m_domain.getNumVertices());
		m_vec.setGeometry(m_domain);
		m_vec.setGlobalVectorColor(Color.BLACK);
		m_domain.addVectorField(m_vec);
		
		m_lic = new PwLIC();
		m_lic.setGeometry(m_domain);
		m_lic.setStandalone(false);
		m_lic.setFast(true);
		m_lic.setLICSize(50);
		m_lic.setParent(this);

		m_disp.selectCamera(PvCameraIf.CAMERA_ORTHO_XY);
		m_disp.addGeometry(m_domain);
		m_disp.update(m_domain);

		m_disp.addPickListener(this);
		m_disp.fit();

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0;

		c.fill = GridBagConstraints.CENTER;
		m_panel.add(boldLabel("Flow Direction"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy++;

		c.gridwidth = 1;
		m_panel.add(new Label("Eigen Direction:"), c);
		c.gridx++;
		m_direction = new JComboBox();
		m_direction.addItemListener(this);
		m_panel.add(m_direction, c);
		m_direction.addItem(Direction.Major);
		m_direction.addItem(Direction.Minor);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 2;

		m_flowReflect = new Checkbox("Reflect Flow", false);
		m_flowReflect.addItemListener(this);
		m_panel.add(m_flowReflect, c);
		c.gridy++;

		m_flowRotate = new PuDouble("Rotate Flow", this);
		m_flowRotate.setBounds(0, 360);
		m_panel.add(m_flowRotate.getInfoPanel(), c);
		c.gridy++;

		c.fill = GridBagConstraints.CENTER;
		m_panel.add(boldLabel("Action"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy++;

		CheckboxGroup group = new CheckboxGroup();
		m_add = new Checkbox("Add", group, true);
		m_add.addItemListener(this);
		m_panel.add(m_add, c);
		c.gridy++;

		m_remove = new Checkbox("Remove", group, false);
		m_remove.addItemListener(this);
		m_panel.add(m_remove, c);
		c.gridy++;

		m_select = new Checkbox("Select", group, false);
		m_select.addItemListener(this);
		m_panel.add(m_select, c);
		c.gridy++;

		m_clear = new Button("Clear");
		m_clear.addActionListener(this);
		m_panel.add(m_clear, c);
		c.gridy++;

		c.fill = GridBagConstraints.CENTER;
		m_panel.add(boldLabel("Separatrices"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy++;

		m_showSeparatrices = new Checkbox("Show Separatrices", true);
		m_showSeparatrices.addItemListener(this);
		m_panel.add(m_showSeparatrices, c);
		c.gridy++;

		c.fill = GridBagConstraints.CENTER;
		m_panel.add(boldLabel("Singularity"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy++;

		m_tensorPanel = new TensorFieldPanel();
		m_panel.add(m_tensorPanel, c);
		c.gridy++;

		c.weighty = 1;
		m_panel.add(Box.createVerticalBox(), c);
		m_disp.selectGeometry(m_field.termBasePoints());
		m_disp.fit();

		updateVectorField_internal();

		m_frame.setSize(1000, 800);
		m_frame.setVisible(true);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getSource();
		if (source == m_add) {
			m_tensorPanel.setTypeChangeEnabled(true);
			m_disp.setMajorMode(PvDisplayIf.MODE_INITIAL_PICK);
			System.out.println("click into the display to add a feature");
		} else if (source == m_remove) {
			m_tensorPanel.setTypeChangeEnabled(false);
			m_disp.setMajorMode(PvDisplayIf.MODE_PICK);
			System.out.println("click near a feature to remove it");
		} else if (source == m_select) {
			m_tensorPanel.setTypeChangeEnabled(false);
			m_disp.setMajorMode(PvDisplayIf.MODE_PICK);
			System.out.println("click near a feature to select it");
		} else if (source == m_flowReflect) {
			updateVectorField();
		} else if (source == m_showSeparatrices) {
			if (m_showSeparatrices.getState()) {
				m_disp.addGeometry(m_separatrices);
			} else {
				m_disp.removeGeometry(m_separatrices);
			}
			m_disp.update(m_separatrices);
		} else if (source == m_direction) {
			updateVectorField();
		} else {
			assert false : "Unhandled item changed: " + source;
		}
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == m_clear) {
			m_field.clear();
			updateVectorField();
		} else {
			assert e.getSource() == m_timer;
			m_timer.stop();
			if (!m_lic.isComputingLIC()) {
				updateVectorField_internal();
			} else {
				m_lic.stopLIC();
				updateVectorField();
			}
		}
	}
	/**
	 * delay actual recomputation
	 */
	public void updateVectorField() {
		m_timer.restart();
	}
	/**
	 * (Re)Compute vector field.
	 */
	public void updateVectorField_internal() {
		m_interpolatedField = new InterpolatedTensorField(m_domain, m_field);
		updateLICImage();

		if (m_degeneratePoints != null) {
			m_disp.removeGeometry(m_degeneratePoints);
		}

		m_degeneratePoints = new PgPointSet(2);
		m_degeneratePoints.setName("Calculated Degenerate Points");
		m_degeneratePoints.showVertices(true);
		m_degeneratePoints.setGlobalVertexSize(3.0);
		m_degeneratePoints.showVertexColors(true);

		if (m_separatrices != null && m_disp.containsGeometry(m_separatrices)) {
			m_disp.removeGeometry(m_separatrices);
		}
		m_separatrices = new PgPolygonSet(2);
		m_separatrices.setName("Separatrices");
		m_separatrices.showVertices(true);
		m_separatrices.setGlobalVertexColor(Color.orange);
		m_separatrices.setGlobalPolygonColor(Color.orange);

		/*
		LineTracer trace = new ClassicalRungeKuttaTracer(field);
		*/

		int i = 0;
		for(DegeneratePoint p : m_interpolatedField.findDegeneratePoints()) {
			m_degeneratePoints.addVertex(p.position);
			Color c = null;
			switch(p.type) {
			case Wedge:
				c = Color.yellow;
				findSeparatrices(p);
				break;
			case Trisector:
				c = Color.blue;
				findSeparatrices(p);
				break;
			case HigherOrder:
				c = Color.white;
				break;
			}
			m_degeneratePoints.setVertexColor(i, c);
			++i;
		}
		System.out.println("degenerate points found: " + m_degeneratePoints.getNumVertices());
		m_disp.addGeometry(m_degeneratePoints);
		if (m_showSeparatrices.getState()) {
			m_disp.addGeometry(m_separatrices);
		}
		m_disp.selectGeometry(m_field.termBasePoints());
	}
	public void findSeparatrices(DegeneratePoint p)
	{
		assert p.type == DegeneratePoint.Type.Trisector
			|| p.type == DegeneratePoint.Type.Wedge;
		InterpolatedTensorField.ElementField field = m_interpolatedField.fieldIn(p.element);
		double a = field.a.getEntry(0, 0);
		double b = field.a.getEntry(0, 1);
		double c = field.a.getEntry(1, 0);
		double d = field.a.getEntry(1, 1);
		// find roots u = tan\theta from equation:
		// d u^3 + (c+2b) u^2 + (2a -d) u - c = 0
		ArrayList<PuComplex> roots = Utils.cubicRoots(d, (c+2d*b), (2d*a-d), -c);
		int base = m_separatrices.addVertex(p.position);
		for(PuComplex r : roots) {
			if (Math.abs(r.im()) > 1E-6) {
				// ignore complex roots
				continue;
			}
			final double theta = Math.atan(r.re());
			final PdVector n1 = PdVector.addNew(p.position,
					new PdVector(Math.cos(theta), Math.sin(theta)));

			final double theta2 = theta < 0 ? theta + Math.PI : theta - Math.PI;
			final PdVector n2 = PdVector.addNew(p.position,
					new PdVector(Math.cos(theta2), Math.sin(theta2)));

			int v = m_separatrices.addVertex(n1);
			m_separatrices.addPolygon(new PiVector(base, v));
			int v2 = m_separatrices.addVertex(n2);
			m_separatrices.addPolygon(new PiVector(base, v2));
			/*
			FIXME: find out which direction is major/minor
			System.out.println("theta = " + Math.toDegrees(theta) + ", theta2 = " + Math.toDegrees(theta2));
			PdVector node = null;
			double c1 = Math.abs(field.a.leftMultMatrix(null, PdVector.blendNew(1, p.position, 0.1, n1)).dot(n1));
			double c2 = Math.abs(field.a.leftMultMatrix(null, PdVector.blendNew(1, p.position, 0.1, n2)).dot(n2));
			if (c1 > c2) {
				node = m_direction.getSelectedItem() == Direction.Major ? n1 : n2;
			} else {
				node = m_direction.getSelectedItem() == Direction.Major ? n2 : n1;
			}
			System.out.println(c1 + " VS " + c2);
			node.add(p.position);
			int v = m_separatrices.addVertex(node);
			m_separatrices.addPolygon(new PiVector(base, v));
			*/
		}
	}
	/**
	 * (Re)Compute vector field.
	 */
	public void updateLICImage() {
		if (m_field.termBasePoints().getNumVertices() == 0) {
			PdVector.setConstant(m_vec.getVectors(), 1);
			m_lic.startLIC();
			return;
		}

		double theta = Math.toRadians(m_flowRotate.getValue());
		PdMatrix R = m_flowReflect.getState() 
				? Utils.reflectionMatrix(theta / 2)
				: Utils.rotationMatrix(theta / 2);
		PdMatrix R_t = PdMatrix.copyNew(R);
		R_t.transpose();

		PdVector[] V_y_field = new PdVector[m_domain.getNumVertices()];
		for(int i = 0; i < m_domain.getNumVertices(); ++i) {
			PdVector pos = m_domain.getVertex(i);
			PdMatrix m = m_field.evaluate(pos);
			m.rightMult(R_t);
			m.leftMult(R);
			PdMatrix eV = Utils.solveEigen2x2(m, null, true);
			PdVector E;
			if (m_direction.getSelectedItem() == Direction.Major) {
				E = eV.getRow(0);
			} else {
				E = eV.getRow(1);
			}
			// pick V_x such that its x-component is always >= 0
			PdVector V_x = PdVector.copyNew(E);
			if (V_x.getEntry(0) < 0) {
				V_x.multScalar(-1);
			}
			// pick V_y such that its y-component is always >= 0
			PdVector V_y = PdVector.copyNew(E);
			if (V_y.getEntry(1) < 0) {
				V_y.multScalar(-1);
			}
			assert V_x.getEntry(0) >= 0;
			assert V_y.getEntry(1) >= 0;
			m_vec.setVector(i, V_x);
			V_y_field[i] = V_y;
			assert m_vec.getVector(i).getSize() == 2 : m_vec.getVector(i).getSize();
		}

		//get the two lic textures
		BufferedImage lic1 = generateLICImage();

		for(int i = 0; i < V_y_field.length; ++i) {
			m_vec.setVector(i, V_y_field[i]);
		}
		BufferedImage lic2 = generateLICImage();
		
		//get weights
		double[][] weights = computeBlendWeights(R);
		
		//compute final texture
		int width = m_lic.getTextureSize().width;
		int height = m_lic.getTextureSize().height;
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				double col = (lic1.getRGB(i, j)&0xff)*weights[i][j] + (lic2.getRGB(i, j)&0xff)*(1-weights[i][j]);  
				result.setRGB(i, j, Color.HSBtoRGB(0f, 0f, (float)col/256));
				
//				//for debugging try also to look at this
//				result.setRGB(i, j, Color.HSBtoRGB(0f, 0f, (float)weights[i][j]));
//				//or this
//				result.setRGB(i, j, lic1.getRGB(i, j));
			}
		}
		
		m_domain.getTexture().setImage(result);
		m_disp.update(m_domain);
	}

	/**
	 * @param dir
	 * @return
	 */
	private BufferedImage generateLICImage() {
		//run lic and wait till computation is done
		m_vec.update(m_vec);
		m_lic.startLIC();
		while(m_lic.isComputingLIC())
			try {
				synchronized (this) {
					wait(100);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
		
		//get lic image
		Dimension dim = m_lic.getTextureSize();
		BufferedImage lic = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_RGB);
		lic.getGraphics().drawImage(m_domain.getTextureImage(), 0, 0, null);
		
		return lic;
	}

	/**
	 * @param vertex	Position in global coordinates.
	 * @param idx		Index of element.
	 * @param bary		Position in barizentric coordinates.
	 * @param rotation	Rotation matrix
	 * @return	Weight for blending.
	 */
	private double computeWeight(PdVector vertex, int idx, PdBary bary, PdMatrix rotation)
	{
		PdMatrix T = m_interpolatedField.evaluateIn(vertex, idx);
		PdMatrix eV = Utils.solveEigen2x2(T, null, true);
		// NOTE: it seems like we need to weight by W_x = cos^2 \theta for major
		// and by W_y = sin^2 \theta for minor direction!
		PdVector E;
		if (m_direction.getSelectedItem() == Direction.Major) {
			E = eV.getRow(0);
		} else {
			E = eV.getRow(1);
		}
		E.leftMultMatrix(rotation);
		double rho = E.length();
		// x-coordinate it cos \theta for major, and -sin \theta for minor
		// hence its square is just what we need!
		double weight = E.getEntry(0) / rho;
		return weight * weight;
	}

	/**
	 * @return	Weights for blending.
	 */
	private double[][] computeBlendWeights(PdMatrix rotation)
	{
		int width = m_lic.getTextureSize().width;
		int height = m_lic.getTextureSize().height;
		double[][] weights = new double[width][height];
		
		//generate texture containg weights
		PdVector[] verts = m_domain.getVertices();
		for(int i = 0; i < m_domain.getNumElements(); i++){
			PiVector face = m_domain.getElement(i);
			//texture coordinates
			PdVector[] coords = m_domain.getElementTexture(i);
			//bounding box in texture
			double u_min = Float.MAX_VALUE, u_max = 0, v_min = Float.MAX_VALUE, v_max = 0;
			for(PdVector coord : coords){
				if(coord.m_data[0] < u_min) u_min = coord.m_data[0];
				if(coord.m_data[0] > u_max) u_max = coord.m_data[0];
				if(coord.m_data[1] < v_min) v_min = coord.m_data[1];
				if(coord.m_data[1] > v_max) v_max = coord.m_data[1];
			}
			u_min = Math.floor(u_min*width);
			u_max = Math.ceil(u_max*width);
			v_min = Math.floor(v_min*height);
			v_max = Math.ceil(v_max*height);
			//rasterize triangle
			for(double u = u_min; u < u_max; u++){
				for (double v = v_min; v < v_max; v++) {
					PdBary bary = new PdBary(3);
					PdBary.getBary(bary, new PdVector(u/width,v/height), coords);
					if(bary.isInside(1./m_domain.getDescr().getNumULines())){
						//compute weight
						PdVector vertex = bary.getVertex(null, verts[face.m_data[0]], verts[face.m_data[1]], verts[face.m_data[2]]);
						weights[(int)(u)][height-(int)(v)-1] = computeWeight(vertex, i, bary, rotation);
					}
				}
			}
		}
		
		return weights;
	}
	@Override
	public boolean update(Object event) {
		if (event == m_lic) {
			m_disp.update(m_domain);
			return true;
		} else if (event == m_field) {
			updateVectorField();
			return true;
		} else if (event == m_flowRotate) {
			updateVectorField();
			return true;
		} else if (event == m_domain.getDescr()) {
			m_lic.setGeometry(m_domain);
			m_lic.update(m_lic);
			updateVectorField();
			return true;
		}
		return false;
	}
	@Override
	public PsUpdateIf getFather() {
		return null;
	}
	@Override
	public void setParent(PsUpdateIf parent) {
		System.err.println("set parent: " + parent);
	}

	@Override
	public void dragDisplay(PvPickEvent pos) {
		// ignore this
	}

	@Override
	public void dragInitial(PvPickEvent pos) {
		// ignored
	}

	@Override
	public void dragVertex(PgGeometryIf geom, int index, PdVector vertex) {
		// ignored
		if (geom == m_field.termBasePoints()) {
			m_field.getTerm(index).setBase(geom.getVertex(index));
		}
	}

	@Override
	public void markVertices(PvPickEvent pos) {
		// ignored
	}

	@Override
	public void pickDisplay(PvPickEvent pos) {
		// ignored
	}

	@Override
	public void pickInitial(PvPickEvent pos) {
		TensorTerm term = m_tensorPanel.createTerm(pos.getVertex());
		m_field.addTerm(term);
		m_tensorPanel.setTerm(term);
	}

	@Override
	public void pickVertex(PgGeometryIf geom, int index, PdVector vertex) {
		if (geom == m_field.termBasePoints()) {
			if (m_remove.getState()) {
				m_field.removeTerm(index);
				m_tensorPanel.setTerm(null);
			} else {
				assert m_select.getState();
				m_tensorPanel.setTerm(m_field.getTerm(index));
			}
		}
	}

	@Override
	public void unmarkVertices(PvPickEvent pos) {
		// ignored
	}
}