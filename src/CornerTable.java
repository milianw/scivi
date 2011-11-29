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

import java.util.ArrayList;
import java.util.Collections;

import jv.geom.PgElementSet;
import jv.vecmath.PiVector;

class Corner {
	public Corner prev;
	public Corner next;
	public Corner opposite;
	public int vertex;
	public int triangle;
	public int localVertexIndex;
	/**
	 * find neighbors of current vertex by iterating over the corner table
	 * starting with prev and then jumping to .o.p of that corner
	 * until we reach next and quit.
	 *
	 * If we reach a corner with .o.p == null, we reverse the direction
	 * and start with next until we reach .o.n == null
	 *
	 * @return array of corners that indicate the neighbors of
	 * the current vertex
	 */
	public Corner[] vertexNeighbors() {
		ArrayList<Corner> neighbors = new ArrayList<Corner>(10);
		Corner i = prev;
		boolean usePrev = true;
		while(true) {
			neighbors.add(i);
			if ((usePrev && i.vertex == next.vertex) ||
				(!usePrev && i.vertex == prev.vertex))
			{
				// we just handled the last neighbor - stop
				break;
			} else {
				if (usePrev) {
					i = i.prev.opposite;
					if (i == null) {
						i = next;
						usePrev = false;
					}
				} else {
					i = i.next.opposite;
					if (i == null) {
						break;
					}
				}
			}
		}
		Corner[] ret = new Corner[neighbors.size()];
		neighbors.toArray(ret);
		return ret;
	}
}

/**
 * Private struct that resembles a row in the
 * initial corner-table creation routine
 */
class CTRow implements Comparable<CTRow> {
	public CTRow(Corner corner)
	{
		c = corner;
		min = Math.min(c.prev.vertex, c.next.vertex);
		max = Math.max(c.prev.vertex, c.next.vertex);
	}
	/**
	 * Sort first by min, then by max in ascending order
	 */
	@Override
	public int compareTo(CTRow o)
	{
		if (min < o.min) {
			return -1;
		} else if (min == o.min) {
			if (max < o.max) {
				return -1;
			} else if (max == o.max) {
				return 0;
			} else {
				return 1;
			}
		} else {
			return 1;
		}
	}
	public Corner c;
	public int min;
	public int max;
}

class CornerTable {
	public CornerTable(PgElementSet geometry)
	{
		// actual corners
		m_corners = new ArrayList<Corner>(geometry.getNumElements() * 3);
		// temporary table to find c.opposite
		ArrayList<CTRow> table = new ArrayList<CTRow>(geometry.getNumElements() * 3);
		for(int i = 0; i < geometry.getNumElements(); ++i) {
			PiVector vertices = geometry.getElement(i);
			Corner a = new Corner();
			a.vertex = vertices.getEntry(0);
			a.localVertexIndex = 0;
			a.triangle = i;
			Corner b = new Corner();
			b.vertex = vertices.getEntry(1);
			b.localVertexIndex = 1;
			b.triangle = i;
			Corner c = new Corner();
			c.vertex = vertices.getEntry(2);
			c.localVertexIndex = 2;
			c.triangle = i;
			
			a.prev = c;
			a.next = b;
			b.prev = a;
			b.next = c;
			c.prev = b;
			c.next = a;

			m_corners.add(a);
			m_corners.add(b);
			m_corners.add(c);
			
			table.add(new CTRow(a));
			table.add(new CTRow(b));
			table.add(new CTRow(c));
		}
		
		// sort table by min index, see CTRow::compareTo
		Collections.sort(table);
		// find pairs and associate c.opposite
		// thanks to sorting, every two consecutive rows
		// are opposite to each other
		for(int i = 0; i < table.size() - 1; i++) {
			CTRow a = table.get(i);
			CTRow b = table.get(i+1);
			if (a.min != b.min || a.max != b.max) {
				continue;
			}
			assert a.max == b.max;
			a.c.opposite = b.c;
			b.c.opposite = a.c;
			++i;
		}
	}
	public ArrayList<Corner> corners()
	{
		return m_corners;
	}
	public int size()
	{
		return m_corners.size();
	}
	public Corner corner(int i)
	{
		return m_corners.get(i);
	}
	private ArrayList<Corner> m_corners; 
}