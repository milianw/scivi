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

class LineTracer
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
			PdVector f = m_function.evaluate(cur);
			if (f == null) {
				break;
			}
			cur = PdVector.blendNew(1.0, cur, stepSize, f);
			int v = output.addVertex(cur);
			output.addPolygon(new PiVector(v-1, v));
		}
	}
}