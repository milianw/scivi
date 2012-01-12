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
import java.util.ArrayList;

import jv.geom.PgPointSet;
import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;

/**
 * Two-Dimensional vector field
 */
public class VectorField
{
	private ArrayList<Term> m_terms;
	private PgPointSet m_points;
	public VectorField()
	{
		m_terms = new ArrayList<Term>(10);
		m_points = new PgPointSet(2);
		m_points.showVertices(true);
		m_points.showVertexColors(true);
	}
	public PgPointSet pointSet()
	{
		return m_points;
	}
	public PdVector evaluate(PdVector pos)
	{
		PdVector ret = new PdVector(0, 0);
		for(Term t : m_terms) {
			ret.add(t.evaluate(pos));
		}
		return ret;
	}
	public void addTerm(Term term)
	{
		m_points.addVertex(term.base());
		m_points.setVertexColor(m_points.getNumVertices() - 1, term.vertexColor());
		m_points.update(m_points);
		m_terms.add(term);
	}
	public void removeLast()
	{
		m_points.removeVertex(m_points.getNumVertices() - 1);
		m_points.update(m_points);
		m_terms.remove(m_terms.size() - 1);
	}
}

abstract class Term
{
	protected PdVector m_base;
	Term(PdVector base)
	{
		m_base = PdVector.copyNew(base);
	}
	public PdVector base()
	{
		return m_base;
	}
	abstract public PdVector evaluate(PdVector pos);
	abstract public Color vertexColor();
}

class ConstantTerm extends Term
{
	protected PdVector m_val;
	public ConstantTerm(PdVector base, PdVector value)
	{
		super(base);
		m_val = PdVector.copyNew(value);
	}
	@Override
	public PdVector evaluate(PdVector pos)
	{
		return m_val;
	}
	@Override
	public Color vertexColor() {
		return Color.white;
	}
}

class GenericTerm extends Term
{
	PdMatrix m_a;
	double m_strength;
	double m_decay;
	Color m_color;
	public GenericTerm(PdVector base, PdMatrix A, double strength, double decay, Color color)
	{
		super(base);
		m_a = PdMatrix.copyNew(A);
		m_strength = strength;
		m_decay = decay;
		m_color = color;
	}
	@Override
	public PdVector evaluate(PdVector pos) {
		PdVector ret = PdVector.subNew(pos, m_base);
		ret.leftMultMatrix(m_a);
		ret.multScalar(m_strength * Math.exp(-m_decay * ret.length()));
		return ret;
	}
	@Override
	public Color vertexColor() {
		return m_color;
	}
}

class ConvergingElementTerm extends Term
{
	private double m_strength;
	private double m_decay;
	private Color m_color;
	private PdVector m_n;
	private PdVector m_e;
	public ConvergingElementTerm(PdVector base, double strength, double decay, double theta, Color color)
	{
		super(base);
		m_strength = strength;
		m_decay = decay;
		m_color = color;
		m_n = new PdVector(-Math.sin(theta), Math.cos(theta));
		m_e = new PdVector(Math.cos(theta), Math.sin(theta));
	}
	@Override
	public PdVector evaluate(PdVector pos) {
		PdVector sub = PdVector.subNew(pos, m_base);
		PdVector ret = PdVector.copyNew(m_n);
		ret.multScalar(sub.dot(m_n));
		ret.add(m_e);
		ret.multScalar(m_strength * Math.exp(-m_decay * sub.length()));
		return ret;
	}
	@Override
	public Color vertexColor() {
		return m_color;
	}
}
