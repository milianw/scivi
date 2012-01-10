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
import java.awt.Rectangle;

import jv.geom.PgVectorField;
import jv.object.PsUpdateIf;
import jv.project.PgGeometryIf;
import jv.project.PvCameraIf;
import jv.project.PvGeometryListenerIf;
import jv.project.PvPickEvent;
import jv.project.PvPickListenerIf;
import jv.vecmath.PdVector;
import jvx.surface.PgDomain;
import jvx.surface.PgDomainDescr;
import jvx.vector.PwLIC;

/**
 * Solution to fourth exercise of the second project
 * 
 * @author		Milian Wolff
 * @version		10.01.2012, 1.00 created
 */
public class Ex3_1
	extends ProjectBase
	implements PvGeometryListenerIf, PsUpdateIf, PvPickListenerIf
{
	private PwLIC m_lic;
	private PgDomain m_domain;
	private PgVectorField m_vec;
	private VectorField m_field;

	public static void main(String[] args)
	{
		new Ex3_1(args);
	}

	public Ex3_1(String[] args)
	{
		super(args, "SciVis - Project 3 - Exercise 1 - Milian Wolff");

		m_field = new VectorField();
		m_field.addTerm(new ConstantTerm(new PdVector(1, 1)));

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
		
		m_vec = new PgVectorField(3);
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

		updateVectorField();

		show();
		m_frame.setBounds(new Rectangle(420, 5, 1024, 550));
	}

	/**
	 * (Re)Compute vector field.
	 */
	public void updateVectorField() {
		//compute ramdom vector field
		for(int i = 0; i < m_domain.getNumVertices(); ++i) {
			PdVector pos = m_domain.getVertex(i);
			m_vec.setVector(i, m_field.evaluate(pos));
		}
		m_vec.update(m_vec);
		m_lic.startLIC();
	}
	@Override
	public boolean update(Object event) {
		if (event == m_lic) {
			m_disp.update(m_domain);
			return true;
		}

		System.err.println("update: " + event);
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
		// ignored
		System.out.println("pick initial:");
		System.out.println(pos.getVertex());
		m_field.addTerm(new SinkTerm(pos.getVertex()));
		updateVectorField();
	}

	@Override
	public void pickVertex(PgGeometryIf geom, int index, PdVector vertex) {
		// ignored
	}

	@Override
	public void unmarkVertices(PvPickEvent pos) {
		// ignored
	}

}
