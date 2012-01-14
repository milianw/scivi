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
import java.awt.GridBagConstraints;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;

import jv.geom.PgElementSet;
import jv.geom.PgVectorField;
import jv.number.PuInteger;
import jv.vecmath.PdVector;

/*
 * 
 * @author		Milian Wolff
 * @version		10.01.2012, 1.00 created
 */
public class Ex3_1
	extends ProjectBase
	implements ItemListener
{
	private JComboBox m_fieldCombo;
	private PgElementSet m_geometry;
	private PgVectorField m_field;
	private PuInteger m_N;
	private enum Fields {
		A,
		B,
		C,
		D,
		E,
		F
	}

	public static void main(String[] args)
	{
		new Ex3_1(args);
	}

	public Ex3_1(String[] args)
	{
		super(args, "SciVis - Project 3 - Exercise 1 - Milian Wolff");

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0;

		m_fieldCombo = new JComboBox();
		for(Fields f : Fields.values()) {
			m_fieldCombo.addItem(f);
		}
		m_fieldCombo.addItemListener(this);
		m_panel.add(m_fieldCombo, c);
		c.gridy++;
		
		m_geometry = new PgElementSet(3);
		m_geometry.computeSphere(50, 50, 1);
		m_geometry.showEdges(false);
		m_geometry.showTransparency(true);
		m_disp.showAxes(true);
		m_disp.addGeometry(m_geometry);
		m_disp.update(m_geometry);

		m_field = new PgVectorField(3);
		m_field.setBasedOn(PgVectorField.VERTEX_BASED);
		m_field.setNumVectors(m_geometry.getNumVertices());
		m_field.setGeometry(m_geometry);
		m_field.showVectorArrows(true);
		m_geometry.addVectorField(m_field);
		m_geometry.showVectorFields(true);
		updateVectorField();

		m_N = new PuInteger("N (for e)");
		m_N.setBounds(1, 10);
		m_panel.add(m_N.getInfoPanel(), c);
		c.gridy++;
		
		m_disp.fit();

		show();
	}

	@Override
	public void itemStateChanged(ItemEvent e)
	{
		assert e.getSource() == m_fieldCombo;
		updateVectorField();
	}
	
	protected void updateVectorField()
	{
		for(int i = 0; i < m_geometry.getNumVertices(); ++i) {
			PdVector pos = m_geometry.getVertex(i);
			double theta = Math.acos(pos.getEntry(2));
			double phi = Math.atan2(pos.getEntry(1), pos.getEntry(0));
			m_field.setVector(i, getVector(theta, phi, (Fields) m_fieldCombo.getSelectedItem()));
		}
		m_disp.update(m_geometry);
	}

	private PdVector getVector(double theta, double phi, Fields type)
	{
		PdVector e_theta = new PdVector(Math.cos(theta) * Math.cos(phi),
										Math.cos(theta) * Math.sin(phi),
										-Math.sin(theta));
		PdVector e_phi = new PdVector(-Math.sin(phi),
										Math.cos(phi),
										0);
		switch(type) {
		case A:
			return new PdVector(0, 0, 0);
		case B:
			e_theta.multScalar(Math.cos(theta) * Math.cos(phi));
			e_phi.multScalar(-Math.sin(phi));
			e_theta.add(e_phi);
			return e_theta;
		case C:
			e_theta.multScalar(Math.sin(theta/2) * Math.cos(phi));
			e_phi.multScalar(-Math.sin(phi/2) * Math.sin(theta));
			e_theta.add(e_phi);
			return e_theta;
		case D:
			e_theta.multScalar(2*Math.sin(theta)*Math.cos(theta)*(1-2*Math.sin(phi) * Math.sin(phi)));
			e_phi.multScalar(-2*Math.sin(theta) * Math.sin(phi/2));
			e_theta.add(e_phi);
			return e_theta;
		}
		return null;
	}
}
