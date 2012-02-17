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

import java.util.ArrayList;

import jv.geom.PgElementSet;
import jv.geom.PgVectorField;
import jv.project.PvPickEvent;
import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;
import jv.vecmath.PiVector;

class Singularity
{
	enum Type {
		Source,
		Sink,
		Saddle
	}
	Type type;
	PdVector position;
	int element;
	PdMatrix jacobian;
	PdMatrix eigenVectors;
	PdVector eigenValues;
}

class InterpolatedField implements LineTracer.Functor
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
			PdVector solvedX = Utils.solveCramer(A, b_x);
			PdVector solvedY = Utils.solveCramer(A, b_y);
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

	private ArrayList<Singularity> m_singularities;
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
				PdVector pos = Utils.solveCramer(field.a, b);
				if (pos.length() == 0) {
					continue;
				}
				if (inTriangle(i, pos)) {
					Singularity singularity = new Singularity();
					singularity.position = pos;
					singularity.element = i;
					singularity.jacobian = field.a;
					singularity.eigenValues = new PdVector(2);
					singularity.eigenVectors = Utils.solveEigen2x2(singularity.jacobian,
																	singularity.eigenValues, true);
					double se1 = Math.signum(singularity.eigenValues.getEntry(0));
					double se2 = Math.signum(singularity.eigenValues.getEntry(1));
					if (se1 != se2) {
						singularity.type = Singularity.Type.Saddle;
					} else if (se1 < 0) {
						singularity.type = Singularity.Type.Sink;
					} else {
						singularity.type = Singularity.Type.Source;
					}
					m_singularities.add(singularity);
				}
			}
			m_singularities.trimToSize();
		}
		return m_singularities;
	}
	private boolean inTriangle(int i, PdVector p)
	{
		return Utils.inTriangle(p, m_geometry.getElementVertices(i));
	}
	private int elementAt(PdVector pos)
	{
		assert pos.getSize() == 2;

		// TODO: optimize?
		PdVector up = new PdVector(pos.getEntry(0), pos.getEntry(1), +1);
		PdVector down = new PdVector(0, 0, -1);
		PvPickEvent event = m_geometry.intersectionWithLine(up, down);
		if (event == null || event.getElementInd() == -1) {
			// out of bounds
			return -1;
		}
		assert PdVector.subNew(event.getVertex(), pos).length() < 1E-10;
		int element = event.getElementInd();
		if (!inTriangle(element, pos)) {
			return -1;
		}

		assert inTriangle(element, pos)
			: pos.toShortString() + ", " + element;

		return element;
	}
	public PdVector evaluate(PdVector pos)
	{
		int i = elementAt(pos);
		if (i == -1) {
			return null;
		}
		ElementField field = m_interpolated[i];

		PdVector ret = PdVector.copyNew(pos);
		ret.leftMultMatrix(field.a);
		ret.add(field.b);
		return ret;
	}
	@Override
	public PdVector[] stopPoints() {
		ArrayList<Singularity> singularities = findSingularities();
		PdVector[] ret = new PdVector[singularities.size()];
		int i = 0;
		for(Singularity s : singularities) {
			ret[i++] = s.position;
		}
		return ret;
	}
}
