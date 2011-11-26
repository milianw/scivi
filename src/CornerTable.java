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
		for(int i = 0; i < table.size(); i += 2) {
			CTRow a = table.get(i);
			CTRow b = table.get(i+1);
			assert a.min == b.min;
			assert a.max == b.max;
			a.c.opposite = b.c;
			b.c.opposite = a.c;
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