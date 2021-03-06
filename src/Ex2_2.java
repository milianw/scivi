/*
	Copyright 2011 Milian Wolff <mail@milianw.de>

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
import java.awt.Label;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import jv.geom.PgElementSet;
import jv.geom.PgPolygonSet;
import jv.project.PgGeometryIf;
import jv.project.PvCameraEvent;
import jv.project.PvCameraListenerIf;
import jv.project.PvLightIf;

/**
 * Solution to second exercise of second project
 * 
 * @author		Milian Wolff
 * @version		24.11.2011, 1.00 created
 */
public class Ex2_2 extends ProjectBase implements PvCameraListenerIf, ItemListener {
	public static void main(String[] args)
	{
		new Ex2_2(args);
	}
	private PgPolygonSet m_silhouette;
	private Checkbox m_vertexSilhouette;
	private Checkbox m_faceSilhouette;
	private Checkbox m_disableSilhouette;
	boolean m_showSilhouette;
	private Silhouette.Type m_silhouetteType;
	public Ex2_2(String args[])
	{
		super(args, "SciVis - Project 2 - Exercise 2 - Milian Wolff");
		// listener
		m_disp.addGeometryListener(this);
		m_disp.addCameraListener(this);

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;

		c.gridy++;
		c.fill = GridBagConstraints.CENTER;
		m_panel.add(new Label("Silhouette"), c);

		c.fill = GridBagConstraints.HORIZONTAL;
		// disable silhouette
		m_disableSilhouette = new Checkbox("Hide Silhouette", false);
		m_disableSilhouette.addItemListener(this);
		c.gridy++;
		m_panel.add(m_disableSilhouette, c);

		// silhouette method choice
		CheckboxGroup group = new CheckboxGroup();
		// face based silhouette (2.a)
		m_faceSilhouette = new Checkbox("Face Based", group, false);
		m_faceSilhouette.addItemListener(this);
		c.gridy++;
		m_panel.add(m_faceSilhouette, c);

		// default colors
		m_vertexSilhouette = new Checkbox("Vertex Based", group, true);
		m_vertexSilhouette.addItemListener(this);
		c.gridy++;
		m_panel.add(m_vertexSilhouette, c);

		m_silhouetteType = Silhouette.Type.FaceBased;
		m_showSilhouette = true;
		group.setSelectedCheckbox(m_faceSilhouette);

		show();
	}
	//BEGIN: PvGeometryListenerIf
	@Override
	public void addGeometry(PgGeometryIf geometry) {
		// hide other geometries
		if (geometry == m_silhouette) {
			return;
		}
		for(PgGeometryIf other : m_disp.getGeometries()) {
			if (other == geometry || other == m_silhouette) {
				continue;
			} else {
				System.out.println("removing geometry: " + geometry.getName());
				m_disp.removeGeometry(other);
			}
		}
	}
	@Override
	public void selectGeometry(PgGeometryIf geometry)
	{
		assert m_disp.getSelectedGeometry() == geometry;
		viewUpdated();
	}
	//END PvGeometryListenerIf
	//BEGIN PvCameraListenerIf
	@Override
	public void dragCamera(PvCameraEvent arg0)
	{
		viewUpdated();
	}
	@Override
	public void pickCamera(PvCameraEvent arg0)
	{
		viewUpdated();
	}
	//END PvCameraListenerIf
	//BEGIN ItemListener
	@Override
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getSource();
		if (source == m_disableSilhouette) {
			m_showSilhouette = !m_disableSilhouette.getState();
		} else if (source == m_faceSilhouette) {
			m_silhouetteType = Silhouette.Type.FaceBased;
		} else if (source == m_vertexSilhouette) {
			m_silhouetteType = Silhouette.Type.VertexBased;
		} else {
			assert false;
		}
		viewUpdated();
	}
	//END ItemListener
	private void viewUpdated()
	{
		System.out.println("updating view");
		if (m_disp.getSelectedGeometry() == m_silhouette) {
			return;
		}
		PgElementSet geometry = (PgElementSet) m_disp.getSelectedGeometry();
		if (geometry == null) {
			return;
		}

		// clear last silhouette if needed
		clearSilhouette();

		if (m_showSilhouette) {
			m_silhouette = Silhouette.create(geometry, m_silhouetteType,
											 m_disp.getCamera().getPosition());
			assert m_silhouette != null;
			System.out.println("got silhouette: " + m_silhouette.getName());
			System.out.println("adding: " + m_silhouette.getName());
			m_silhouette.showVertices(false);
			m_silhouette.showEdgeColorFromVertices(true);
			m_silhouette.setGlobalPolygonColor(Color.black);

			assert !m_disp.containsGeometry(m_silhouette);
			m_disp.addGeometry(m_silhouette);
			assert m_disp.containsGeometry(m_silhouette);
			m_disp.update(m_silhouette);

			// disable lightning to get completely white surface
			m_disp.setLightingModel(PvLightIf.MODEL_SURFACE);
			// 3D look is nicer imo
			m_disp.setEnabled3DLook(true);

			geometry.setGlobalElementColor(Color.white);
			m_disp.update(geometry);
		} else {
			assert m_silhouette == null;
			// restore settings
			geometry.setGlobalElementColor(Color.cyan);
			m_disp.update(geometry);
			m_disp.setLightingModel(PvLightIf.MODEL_LIGHT);
			m_disp.setEnabled3DLook(false);
		}
	}
	private void clearSilhouette()
	{
		if (m_silhouette != null) {
			assert m_disp.containsGeometry(m_silhouette);
			m_disp.removeGeometry(m_silhouette);
			assert !m_disp.containsGeometry(m_silhouette);
			m_disp.update(m_silhouette);
			m_silhouette = null;
		}
	}
}
