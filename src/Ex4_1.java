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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;

import jv.geom.PgVectorField;
import jv.project.PvCameraIf;
import jv.project.PvDisplayIf;
import jv.project.PvGeometryListenerIf;
import jv.vecmath.PdVector;
import jvx.surface.PgDomain;
import jvx.surface.PgDomainDescr;


class Term4a extends Term {
	@Override
	public PdVector evaluate(PdVector pos) {
		PdVector ret = new PdVector(2);
		double x = pos.getEntry(0) - m_base.getEntry(0);
		double y = pos.getEntry(1) - m_base.getEntry(1);
		ret.setEntry(0, x);
		ret.setEntry(1, -y);
		ret.multScalar(scaleFactor(pos));
		return ret;
	}
	@Override
	public FeatureType type() {
		return FeatureType.Generic;
	}
	@Override
	public Color vertexColor() {
		return Color.pink;
	}
	public Term4a(PdVector base, double strength, double decay) {
		super(base, strength, decay);
	}
}

class Term4b extends Term {
	@Override
	public PdVector evaluate(PdVector pos) {
		PdVector ret = new PdVector(2);
		double x = pos.getEntry(0) - m_base.getEntry(0);
		double y = pos.getEntry(1) - m_base.getEntry(1);
		ret.setEntry(0, x * x - y * y);
		ret.setEntry(1, - 2 * x * y);
		ret.multScalar(scaleFactor(pos));
		return ret;
	}
	@Override
	public FeatureType type() {
		return FeatureType.Generic;
	}
	@Override
	public Color vertexColor() {
		return Color.pink;
	}
	public Term4b(PdVector base, double strength, double decay) {
		super(base, strength, decay);
	}
}

class Term4c extends Term {
	@Override
	public PdVector evaluate(PdVector pos) {
		PdVector ret = new PdVector(2);
		double x = pos.getEntry(0) - m_base.getEntry(0);
		double y = pos.getEntry(1) - m_base.getEntry(1);
		ret.setEntry(0, x * (1 - Math.sqrt(x*x + y*y)) - y);
		ret.setEntry(1, x + y * (1 - Math.sqrt(x*x + y*y)));
		ret.multScalar(scaleFactor(pos));
		return ret;
	}
	@Override
	public FeatureType type() {
		return FeatureType.Generic;
	}
	@Override
	public Color vertexColor() {
		return Color.pink;
	}
	public Term4c(PdVector base, double strength, double decay) {
		super(base, strength, decay);
	}
}


/**
 * @author		Milian Wolff
 * @version		17.02.2012, 1.00 created
 */
public class Ex4_1
	extends ProjectBase
	implements PvGeometryListenerIf, ItemListener
{
	private PgDomain m_domain;
	private PgVectorField m_vec;
	private VectorField m_field;
	private JComboBox m_fieldType;

	public static void main(String[] args)
	{
		new Ex4_1(args);
	}

	public Ex4_1(String[] args)
	{
		super(args, "SciVis - Project 4 - Exercise 1 - Milian Wolff");

		m_disp.setMajorMode(PvDisplayIf.MODE_INITIAL_PICK);

		m_field = new VectorField();

		// listener
		m_disp.addGeometryListener(this);

		m_domain = new PgDomain(2);
		m_domain.setName("Domain for Vector Field");
		m_domain.setDimOfElements(3);
		m_domain.showVectorFields(true);
		m_domain.showEdges(false);
		
		PgDomainDescr descr = m_domain.getDescr();
		descr.setSize( -10., -10., 10., 10.);
		descr.setDiscr(30, 30);
		m_domain.compute();
		
		m_vec = new PgVectorField(2);
		m_vec.setName("Vector Field");
		m_vec.setBasedOn(PgVectorField.VERTEX_BASED);
		m_vec.setNumVectors(m_domain.getNumVertices());
		m_vec.setGeometry(m_domain);
		m_vec.setGlobalVectorColor(Color.BLACK);
		m_domain.addVectorField(m_vec);

		m_disp.selectCamera(PvCameraIf.CAMERA_ORTHO_XY);
		m_disp.addGeometry(m_domain);
		m_disp.update(m_domain);

		m_disp.fit();

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 3;
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0;

		c.fill = GridBagConstraints.CENTER;
		m_panel.add(boldLabel("Field"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy++;

		m_fieldType = new JComboBox();
		m_fieldType.addItem("A");
		m_fieldType.addItem("B");
		m_fieldType.addItem("C");
		m_fieldType.addItemListener(this);
		m_panel.add(m_fieldType, c);
		c.gridy++;

		m_disp.selectGeometry(m_field.termBasePoints());
		m_disp.fit();

		updateVectorField();

		m_frame.setSize(1000, 800);
		m_frame.setVisible(true);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getSource();
		assert source == m_fieldType;
		updateVectorField();
	}
	public void updateVectorField() {
		m_field.clear();
		Term t = null;
		double strength = 1;
		double decay = 0;
		PdVector center = m_domain.getCenterOfBndBox();
		PdVector base = null;
		for(PdVector v : m_domain.getVertices()) {
			if (base == null) {
				base = v;
			} else if (PdVector.dist(base, center) > PdVector.dist(v, center)) {
				base = v;
			}
		}
		switch (m_fieldType.getSelectedIndex()) {
		case 0:
			t = new Term4a(base, strength, decay);
			break;
		case 1:
			t = new Term4b(base, strength, decay);
			break;
		case 2:
			t = new Term4c(base, strength, decay);
			break;
		}
		assert t != null;
		m_field.addTerm(t);

		for(int i = 0; i < m_domain.getNumVertices(); ++i) {
			PdVector pos = m_domain.getVertex(i);
			if (pos.equals(base)) {
				m_vec.setVector(i, new PdVector(0, 0));
				continue;
			}
			PdVector vec = m_field.evaluate(pos);
			vec.setLength(m_domain.getEdgeLength(0, 1) * 0.5);
			m_vec.setVector(i, vec);
			assert m_vec.getVector(i).getSize() == 2 : m_vec.getVector(i).getSize();
		}
		m_domain.showVectorArrows(true);
		m_domain.setGlobalVectorColor(Color.black);
		m_domain.setGlobalElementColor(Color.white);
		m_vec.update(m_vec);
		m_disp.update(m_domain);
	}
}
