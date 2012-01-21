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

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.Timer;

import jv.geom.PgElementSet;
import jv.geom.PgVectorField;
import jv.number.PuDouble;
import jv.object.PsUpdateIf;
import jv.project.PgGeometryIf;
import jv.project.PvCameraIf;
import jv.project.PvDisplayIf;
import jv.project.PvGeometryListenerIf;
import jv.project.PvPickEvent;
import jv.project.PvPickListenerIf;
import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;
import jv.vecmath.PiVector;
import jvx.surface.PgDomain;
import jvx.surface.PgDomainDescr;
import jvx.vector.PwLIC;

class InterpolatedField
{
	private PgElementSet m_geometry;
	private PgVectorField m_field;
	public InterpolatedField(PgElementSet geometry, PgVectorField field)
	{
		m_geometry = geometry;
		m_field = field;
		interpolate();
	}

	class ElementField
	{
		PdMatrix a;
		PdVector b;
		public PdVector evaluate(PdVector pos)
		{
			PdVector ret = new PdVector(2);
			a.leftMultMatrix(ret, pos);
			ret.add(b);
			return ret;
		}
	}
	private ElementField[] m_interpolated;
	private void interpolate()
	{
		m_interpolated = new ElementField[m_geometry.getNumElements()];
		for(int i = 0; i < m_geometry.getNumElements(); ++i) {
			PiVector vertices = m_geometry.getElement(i);
			assert vertices.getSize() == 3;
			PdMatrix A = new PdMatrix(3, 3);
			PdVector b_x = new PdVector(3);
			PdVector b_y = new PdVector(3);
			// build coefficient matrix A (from position of vertices)
			// and expected results b_x, b_y (from vector field)
			for(int j = 0; j < 3; ++j) {
				// A gets the vertex positions in a row, and last column entry = 1
				PdVector v = m_geometry.getVertex(vertices.getEntry(j));
				assert v.getSize() == 2;
				A.setEntry(j, 0, v.getEntry(0));
				A.setEntry(j, 1, v.getEntry(1));
				A.setEntry(j, 2, 1);
				// result b_x, b_y get the x and y values of the field
				PdVector f = m_field.getVector(vertices.getEntry(j));
				assert f.getSize() == 2;
				b_x.setEntry(j, f.getEntry(0));
				b_y.setEntry(j, f.getEntry(1));
			}
			// interpolate
			PdVector solvedX = Curvature.solveCramer(A, b_x);
			PdVector solvedY = Curvature.solveCramer(A, b_y);
			// filter out bad stuff, field might be zero e.g....
			if (Double.isNaN(solvedX.length()) || Double.isNaN(solvedY.length())) {
				continue;
			}
			// store result as 2x2 matrix and 2dim vector
			ElementField elementField = new ElementField();
			elementField.a = new PdMatrix(2, 2);
			elementField.b = new PdVector(2);
			for(int j = 0; j < 3; ++j) {
				if (j == 2) {
					elementField.b.setEntry(0, solvedX.getEntry(2));
					elementField.b.setEntry(1, solvedY.getEntry(2));
				} else {
					elementField.a.setEntry(0, j, solvedX.getEntry(j));
					elementField.a.setEntry(1, j, solvedY.getEntry(j));
				}
			}
			m_interpolated[i] = elementField;
		}
	}

	enum SingularityType {
		Source,
		Sink,
		Saddle
	}
	class Singularity
	{
		SingularityType type;
		PdVector position;
		int element;
		PdMatrix jacobian;
		double maxEigenValue;
		PdVector maxEigenDirection;
		double minEigenValue;
		double minEigenDirection;
	}
	ArrayList<Singularity> m_singularities;
	public ArrayList<Singularity> findSingularities()
	{
		if (m_singularities == null) {
			m_singularities = new ArrayList<Singularity>(10);
			for(int i = 0; i < m_interpolated.length; ++i) {
				ElementField field = m_interpolated[i];
				if (field == null) {
					continue;
				}
				PdVector b = PdVector.copyNew(field.b);
				b.multScalar(-1);
				PdVector pos = Curvature.solveCramer(field.a, b);
				if (pos.length() == 0) {
					continue;
				}
				if (inTriangle(i, pos)) {
					Singularity singularity = new Singularity();
					singularity.position = pos;
					m_singularities.add(singularity);
				}
			}
			m_singularities.trimToSize();
		}
		return m_singularities;
	}
	private boolean onSameSide(PdVector p1, PdVector a, PdVector b, PdVector c)
	{
		PdVector c_min_b = PdVector.subNew(c, b);
		PdVector cp1 = PdVector.crossNew(c_min_b, PdVector.subNew(p1, b));
		PdVector cp2 = PdVector.crossNew(c_min_b, PdVector.subNew(a, b));
		return cp1.dot(cp2) >= 0;
	}
	private boolean inTriangle(int i, PdVector p)
	{
		PdVector[] vertices = m_geometry.getElementVertices(i);
		PdVector a = vertices[0];
		PdVector b = vertices[1];
		PdVector c = vertices[2];
		return onSameSide(p, a, b, c) && onSameSide(p, b, a, c) && onSameSide(p, c, a, b);
	}
}

/**
 * Solution to fourth exercise of the second project
 * 
 * @author		Milian Wolff
 * @version		10.01.2012, 1.00 created
 */
public class Ex3_2
	extends ProjectBase
	implements PvGeometryListenerIf, PsUpdateIf, PvPickListenerIf, ItemListener, ActionListener
{
	private PwLIC m_lic;
	private PgDomain m_domain;
	private PgVectorField m_vec;
	private VectorField m_field;
	private VectorFieldPanel m_singularityPanel;
	private Checkbox m_add;
	private Checkbox m_remove;
	private Checkbox m_select;
	private PuDouble m_flowRotate;
	private Checkbox m_flowReflect;
	private Timer m_timer;

	public static void main(String[] args)
	{
		new Ex3_2(args);
	}

	public Ex3_2(String[] args)
	{
		super(args, "SciVis - Project 3 - Exercise 2 - Milian Wolff");

		m_timer = new Timer(100, this);

		m_disp.setMajorMode(PvDisplayIf.MODE_INITIAL_PICK);

		m_field = new VectorField();
		m_field.setParent(this);
		m_disp.addGeometry(m_field.termBasePoints());

		// listener
		m_disp.addGeometryListener(this);

		m_domain = new PgDomain(2);
		m_domain.setName("Domain for Vector Field");
		m_domain.setDimOfElements(3);
		m_domain.showVectorFields(false);
		m_domain.showEdges(false);
		
		PgDomainDescr descr = m_domain.getDescr();
		descr.setSize( -10., -10., 10., 10.);
		descr.setDiscr(10, 10);
		m_domain.compute();
		
		m_vec = new PgVectorField(2);
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
		c.gridwidth = 3;
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0;

		c.fill = GridBagConstraints.CENTER;
		m_panel.add(boldLabel("Flow Direction"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy++;

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

		c.fill = GridBagConstraints.CENTER;
		m_panel.add(boldLabel("Singularity"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy++;

		m_singularityPanel = new VectorFieldPanel();
		m_panel.add(m_singularityPanel, c);
		c.gridy++;

		c.weighty = 1;
		m_panel.add(Box.createVerticalBox(), c);
		m_disp.fit();

		updateVectorField_internal();

		m_frame.setSize(1000, 800);
		m_frame.setVisible(true);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getSource();
		if (source == m_add) {
			m_disp.setMajorMode(PvDisplayIf.MODE_INITIAL_PICK);
			System.out.println("click into the display to add a feature");
		} else if (source == m_remove) {
			m_disp.setMajorMode(PvDisplayIf.MODE_PICK);
			System.out.println("click near a feature to remove it");
		} else if (source == m_select) {
			m_disp.setMajorMode(PvDisplayIf.MODE_PICK);
			System.out.println("click near a feature to select it");
		} else if (source == m_flowReflect) {
			updateVectorField();
		} else {
			assert false : "Unhandled item changed: " + source;
		}
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		assert e.getSource() == m_timer;
		m_timer.stop();
		updateVectorField_internal();
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
		PdMatrix A = new PdMatrix(2, 2);
		double theta = Math.toRadians(m_flowRotate.getValue());
		A.setEntry(0, 0, Math.cos(theta));
		A.setEntry(0, 1, -Math.sin(theta));
		A.setEntry(1, 0, Math.sin(theta));
		A.setEntry(1, 1, Math.cos(theta));
		if (m_flowReflect.getState()) {
			A.setEntry(0, 1, A.getEntry(0, 1) * -1);
			A.setEntry(1, 1, A.getEntry(1, 1) * -1);
		}

		for(int i = 0; i < m_domain.getNumVertices(); ++i) {
			PdVector pos = m_domain.getVertex(i);
			PdVector vec = m_field.evaluate(pos);
			vec.leftMultMatrix(A);
			m_vec.setVector(i, vec);
			assert m_vec.getVector(i).getSize() == 2 : m_vec.getVector(i).getSize();
		}
		m_vec.update(m_vec);
		m_lic.startLIC();
		assert m_vec != null;
		assert m_domain != null;
		InterpolatedField field = new InterpolatedField(m_domain, m_vec);
		System.out.println("singularities: " + field.findSingularities().size());
		for(int i = 0; i < field.findSingularities().size(); ++i) {
			
		}
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
		}
		return false;
	}
	@Override
	public PsUpdateIf getFather() {
		return null;
	}
	@Override
	public void setParent(PsUpdateIf parent) {
		// TODO Auto-generated method stub
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
		Term term = m_singularityPanel.createTerm(pos.getVertex());
		m_field.addTerm(term);
		m_singularityPanel.setTerm(term);
	}

	@Override
	public void pickVertex(PgGeometryIf geom, int index, PdVector vertex) {
		if (geom == m_field.termBasePoints()) {
			if (m_remove.getState()) {
				m_field.removeTerm(index);
				m_singularityPanel.setTerm(null);
			} else {
				assert m_select.getState();
				m_singularityPanel.setTerm(m_field.getTerm(index));
			}
		}
	}

	@Override
	public void unmarkVertices(PvPickEvent pos) {
		// ignored
	}
}
