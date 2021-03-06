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
public class VectorField extends BasicUpdateIf
{
	private ArrayList<Term> m_terms;
	private PgPointSet m_termBasePoints;
	public VectorField()
	{
		m_terms = new ArrayList<Term>(10);
		m_termBasePoints = new PgPointSet(2);
		m_termBasePoints.setName("Vector Field Term Base Points");
		m_termBasePoints.showVertices(true);
		m_termBasePoints.showVertexColors(true);
		m_termBasePoints.setGlobalVertexSize(5.0);
	}
	public PgPointSet termBasePoints()
	{
		return m_termBasePoints;
	}
	public PdVector evaluate(PdVector pos)
	{
		PdVector ret = new PdVector(0, 0);
		assert ret.getSize() == 2;
		for(Term t : m_terms) {
			ret.add(t.evaluate(pos));
		}
		assert ret.getSize() == 2;
		return ret;
	}
	public void addTerm(Term term)
	{
		m_termBasePoints.addVertex(term.base());
		m_termBasePoints.setVertexColor(m_termBasePoints.getNumVertices() - 1, term.vertexColor());
		m_termBasePoints.update(m_termBasePoints);
		term.setParent(this);
		m_terms.add(term);
		update(this);
	}
	public void removeLast()
	{
		m_termBasePoints.removeVertex(m_termBasePoints.getNumVertices() - 1);
		m_termBasePoints.update(m_termBasePoints);
		m_terms.remove(m_terms.size() - 1);
		update(this);
	}
	public void removeTerm(int index)
	{
		m_termBasePoints.removeVertex(index);
		m_termBasePoints.update(m_termBasePoints);
		m_terms.remove(index);
		update(this);
	}
	public Term getTerm(int index)
	{
		return m_terms.get(index);
	}
	@Override
	public boolean update(Object event) {
		if (m_terms.contains(event)) {
			update(this);
			return true;
		}
		return super.update(event);
	}
	public void clear()
	{
		for(int i = m_termBasePoints.getNumVertices(); i > 0; --i) {
			m_termBasePoints.removeVertex(i - 1);
		}
		m_terms.clear();
		m_termBasePoints.update(m_termBasePoints);
		update(this);
	}
}

abstract class Term extends BasicUpdateIf
{
	protected PdVector m_base;
	protected double m_strength;
	protected double m_decay;
	Term(PdVector base, double strength, double decay)
	{
		m_base = PdVector.copyNew(base);
		assert m_base.getSize() == 2;
		m_strength = strength;
		m_decay = decay;
	}
	public PdVector base()
	{
		return m_base;
	}
	public void setBase(PdVector base)
	{
		assert base.getSize() == 2;
		m_base = base;
		update(this);
	}
	public double strength()
	{
		return m_strength;
	}
	public void setStrength(double strength)
	{
		m_strength = strength;
		update(this);
	}
	public double decay()
	{
		return m_decay;
	}
	public void setDecay(double decay)
	{
		m_decay = decay;
		update(this);
	}
	public double scaleFactor(PdVector pos)
	{
		return m_strength * Math.exp(- m_decay * PdVector.subNew(m_base, pos).sqrLength());
	}
	abstract public PdVector evaluate(PdVector pos);
	abstract public Color vertexColor();
	abstract public FeatureType type();
}

abstract class AngleTerm extends Term
{
	protected double m_angle;
	public AngleTerm(PdVector base, double strength, double decay, double angle)
	{
		super(base, strength, decay);
		setAngle(angle);
	}
	public double angle()
	{
		return m_angle;
	}
	public void setAngle(double angle)
	{
		m_angle = angle;
		update(this);
	}
}

enum FeatureType {
	Constant,
	Sink,
	Source,
	Saddle,
	Center,
	Focus,
	ConvergingElement,
	DivergingElement,
	Generic,
}

class ConstantTerm extends AngleTerm
{
	protected PdVector m_val;
	public ConstantTerm(PdVector base, double strength, double decay, double theta)
	{
		super(base, strength, decay, theta);
		m_val = new PdVector(Math.cos(theta), Math.sin(theta));
	}
	@Override
	public PdVector evaluate(PdVector pos)
	{
		PdVector ret = PdVector.copyNew(m_val);
		ret.multScalar(scaleFactor(pos));
		return ret;
	}
	@Override
	public Color vertexColor() {
		return Color.white;
	}
	@Override
	public FeatureType type() {
		return FeatureType.Constant;
	}
}

class GenericTerm extends Term
{
	protected PdMatrix m_a;
	protected FeatureType m_type;
	public GenericTerm(PdVector base, PdMatrix A,
						double strength, double decay,
						FeatureType type)
	{
		super(base, strength, decay);

		assert type == FeatureType.Saddle || type == FeatureType.Sink
				|| type == FeatureType.Source || type == FeatureType.Center
				|| type == FeatureType.Focus || type == FeatureType.Generic;

		m_a = PdMatrix.copyNew(A);
		m_type = type;
	}
	@Override
	public PdVector evaluate(PdVector pos) {
		PdVector ret = PdVector.subNew(pos, m_base);
		ret.leftMultMatrix(m_a);
		ret.multScalar(scaleFactor(pos));
		return ret;
	}
	@Override
	public Color vertexColor() {
		switch(m_type) {
		case Focus:
		case Center:
		case Source:
			return Color.green;
		case Sink:
			return Color.red;
		case Saddle:
			return Color.blue;
		default:
			return Color.black;
		}
	}
	@Override
	public FeatureType type() {
		return m_type;
	}
	public PdMatrix coeffs()
	{
		return m_a;
	}
	public void setCoeffs(PdMatrix A)
	{
		m_a = A;
		update(this);
	}
}

class ConvergingElementTerm extends AngleTerm
{
	private PdVector m_n;
	private PdVector m_e;
	public ConvergingElementTerm(PdVector base, double strength, double decay, double theta)
	{
		super(base, strength, decay, theta);
	}
	@Override
	public void setAngle(double angle) {
		super.setAngle(angle);
		m_n = new PdVector(-Math.sin(angle), Math.cos(angle));
		m_e = new PdVector(Math.cos(angle), Math.sin(angle));
	}
	@Override
	public PdVector evaluate(PdVector pos) {
		PdVector sub = PdVector.subNew(pos, m_base);
		PdVector ret = PdVector.copyNew(m_n);
		ret.multScalar(sub.dot(m_n));
		ret.add(m_e);
		ret.multScalar(scaleFactor(pos));
		return ret;
	}
	@Override
	public Color vertexColor() {
		return m_strength < 0 ? Color.red : Color.green;
	}
	@Override
	public FeatureType type() {
		return m_strength < 0 ? FeatureType.ConvergingElement : FeatureType.DivergingElement;
	}
}
