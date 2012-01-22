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

import jv.geom.PgPolygonSet;
import jv.vecmath.PdVector;
import jv.vecmath.PiVector;

abstract class LineTracer
{
	interface Functor
	{
		public PdVector evaluate(PdVector at);
	}

	protected Functor m_function;
	LineTracer(Functor function)
	{
		m_function = function;
	}
	void trace(PgPolygonSet output, PdVector seed,
				int steps, double stepSize)
	{
		output.addVertex(seed);
		PdVector cur = seed;
		for(int s = 1; s <= steps; ++s) {
			cur = next(cur, stepSize);
			if (cur == null) {
				break;
			}
			int v = output.addVertex(cur);
			output.addPolygon(new PiVector(v-1, v));
		}
	}
	protected abstract PdVector next(PdVector y_i, double h);
}

class ExplicitEulerTracer extends LineTracer
{
	public ExplicitEulerTracer(Functor function) {
		super(function);
	}
	@Override
	protected PdVector next(PdVector y_i, double h) {
		PdVector f = m_function.evaluate(y_i);
		if (f == null) {
			return null;
		}
		return PdVector.blendNew(1.0, y_i, h, f);
	}
}

class ClassicalRungeKuttaTracer extends LineTracer
{
	public ClassicalRungeKuttaTracer(Functor function) {
		super(function);
	}
	@Override
	protected PdVector next(PdVector y_i, double h) {
		PdVector ret = PdVector.copyNew(y_i);
		PdVector k_1 = m_function.evaluate(y_i);
		if (k_1 == null) {
			return null;
		}
		k_1.multScalar(h);
		PdVector k_2 = m_function.evaluate(PdVector.blendNew(1.0, y_i, 0.5, k_1));
		if (k_2 == null) {
			return null;
		}
		k_2.multScalar(h);
		PdVector k_3 = m_function.evaluate(PdVector.blendNew(1.0, y_i, 0.5, k_2));
		if (k_3 == null) {
			return null;
		}
		k_3.multScalar(h);
		PdVector k_4 = m_function.evaluate(PdVector.blendNew(1.0, y_i, 1.0, k_3));
		if (k_4 == null) {
			return null;
		}
		k_4.multScalar(h);
		ret.add(1.0/6.0, k_1);
		ret.add(1.0/3.0, k_2);
		ret.add(1.0/3.0, k_3);
		ret.add(1.0/6.0, k_4);
		return ret;
	}
}
