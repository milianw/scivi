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

import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;

/**
 * Two-Dimensional vector field
 */
public class VectorField
{
	public VectorField()
	{
		m_terms = new ArrayList<Term>(10);
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
		m_terms.add(term);
	}
	private ArrayList<Term> m_terms;
}

interface Term
{
	public PdVector evaluate(PdVector pos);
}

class ConstantTerm implements Term
{
	public ConstantTerm(PdVector value)
	{
		m_val = PdVector.copyNew(value);
	}
	@Override
	public PdVector evaluate(PdVector pos)
	{
		return m_val;
	}
	PdVector m_val;
}

class GenericTerm implements Term
{
	PdVector m_pos;
	PdMatrix m_a;
	double m_strength;
	double m_decay;
	public GenericTerm(PdVector pos, PdMatrix A, double strength, double decay)
	{
		m_pos = PdVector.copyNew(pos);
		m_a = PdMatrix.copyNew(A);
		m_strength = strength;
		m_decay = decay;
	}
	@Override
	public PdVector evaluate(PdVector pos) {
		PdVector ret = PdVector.subNew(pos, m_pos);
		ret.leftMultMatrix(m_a);
		ret.multScalar(m_strength * Math.exp(-m_decay * ret.length()));
		return ret;
	}
}
