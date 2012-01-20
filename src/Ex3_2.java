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
		findSingularities();
	}

	class ElementField
	{
		PdMatrix a;
		PdVector b;
	}
	private ArrayList<ElementField> m_interpolated;
	private void interpolate()
	{
		m_interpolated = new ArrayList<ElementField>(m_geometry.getNumElements());
		for(int i = 0; i < m_geometry.getNumElements(); ++i) {
			PiVector vertices = m_geometry.getElement(i);
			assert vertices.getSize() == 3;
			PdMatrix A = new PdMatrix(3, 3);
			for(int j = 0; j < 3; ++j) {
				PdVector v = m_field.getVector(vertices.getEntry(j));
				A.setEntry(j, 0, v.getEntry(0));
				A.setEntry(j, 1, v.getEntry(1));
				A.setEntry(j, 2, 1);
			}
			PdVector b_x = PdVector.copyNew(A.getColumn(0));
			PdVector b_y = PdVector.copyNew(A.getColumn(1));
			PdVector solvedX = Curvature.solveCramer(A, b_x);
			PdVector solvedY = Curvature.solveCramer(A, b_y);
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
			m_interpolated.add(i, elementField);
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
		PdMatrix jacobian;
		double maxEigenValue;
		PdVector maxEigenDirection;
		double minEigenValue;
		double minEigenDirection;
	}
	private void findSingularities()
	{
		
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
//		descr.setMaxSize(-10., -10., 10., 10.);
		descr.setSize( -10., -10., 10., 10.);
//		descr.setDiscrBounds(2, 2, 50, 50);
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
//		c.gridwidth = 3;
		m_add = new Checkbox("Add", group, true);
		m_add.addItemListener(this);
		m_panel.add(m_add, c);
//		c.gridx++;
		c.gridy++;

		m_remove = new Checkbox("Remove", group, false);
		m_remove.addItemListener(this);
		m_panel.add(m_remove, c);
//		c.gridx++;
		c.gridy++;

		m_select = new Checkbox("Select", group, false);
		m_select.addItemListener(this);
		m_panel.add(m_select, c);
//		c.gridx++;
//		c.gridy++;

		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 3;

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
