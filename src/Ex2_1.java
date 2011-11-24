import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import jv.geom.PgElementSet;
import jv.geom.PgPointSet;
import jv.loader.PgFileDialog;
import jv.loader.PjImportModel;
import jv.object.PsConfig;
import jv.object.PsMainFrame;
import jv.vecmath.PdVector;
import jv.vecmath.PiVector;

class Corner {
	public Corner()
	{
		
	}
	public Corner prev;
	public Corner next;
	public Corner opposite;
	public int vertex;
	public int triangle;
	///FIXME: implement
	public int edge;
	///TODO: probably not required
	public int index;
}

/**
 * Private struct that resembles a row in the
 * initial corner-table creation routine
 */
class CTRow implements Comparable<CTRow> {
	public CTRow(Corner corner) {
		c = corner;
		min = Math.min(c.prev.vertex, c.next.vertex);
		max = Math.max(c.prev.vertex, c.next.vertex);
	}
	@Override
	public int compareTo(CTRow o) {
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
		int j = 0;
		for(int i = 0; i < geometry.getNumElements(); ++i) {
			PiVector vertices = geometry.getElement(i);
			Corner a = new Corner();
			a.vertex = vertices.getEntry(0);
			a.triangle = i;
			a.index = j++;
			Corner b = new Corner();
			b.vertex = vertices.getEntry(1);
			b.triangle = i;
			b.index = j++;
			Corner c = new Corner();
			c.vertex = vertices.getEntry(2);
			c.triangle = i;
			c.index = j++;
			
			a.prev = c;
			a.next = b;
			b.prev = a;
			b.next = c;
			c.prev = b;
			c.next = a;
			
			///FIXME: set edge

			m_corners.add(a);
			m_corners.add(b);
			m_corners.add(c);
			
			table.add(new CTRow(a));
			table.add(new CTRow(b));
			table.add(new CTRow(c));
		}
		
		// sort table by min index
		Collections.sort(table);
		// find pairs and associate c.opposite
		for(int i = 0; i < table.size(); i += 2) {
			CTRow a = table.get(i);
			CTRow b = table.get(i+1);
			assert a.min == b.min;
			assert a.max == b.max;
			a.c.opposite = b.c;
			b.c.opposite = a.c;
		}
	}
	ArrayList<Corner> corners() {
		return m_corners;
	}
	private ArrayList<Corner> m_corners; 
}

/**
 * Solution to first exercise of second project
 * 
 * @author		Milian Wolff
 * @version		24.11.2011, 1.00 created
 */
public class Ex2_1 {
	public static void main(String args[])
	{
		new Ex2_1(args);
	}
	public Ex2_1(String args[])
	{
		// Create toplevel window of application containing the applet
		PsMainFrame frame	= new PsMainFrame("SciVis - Project 2 - Exercise 1 - Milian Wolff", args);

		// open file
		PgFileDialog dlg = new PgFileDialog(frame, "open geometry", 0);
		dlg.setDirectory("./data");
		dlg.show();
		if (!dlg.isFileSelected()) {
			return;
		}
		
		// load geometry
		PjImportModel importer = new PjImportModel();
		if (!importer.load(dlg.getFullFileName())) {
			System.err.println("could not open geometry: " + dlg.getFullFileName());
			System.exit(1);
			return;
		}
		PgElementSet geometry = (PgElementSet)importer.getGeometry().clone();

		System.out.println("loaded geometry: " + dlg.getFullFileName());
		System.out.println("elements: " + geometry.getNumElements() + ", vertices: " + geometry.getNumVertices());
		CornerTable table = new CornerTable(geometry);
		System.out.println("constructed corner table, running tests");
		for(Corner c : table.corners()) {
			/*
			System.out.println(
				c.index + " | " + c.triangle + " | " + c.vertex + " | " +
				c.prev.index + " | " + c.next.index + " | " + c.opposite.index);
			 */
			// properly initialized prev/next
			assert c.next != null;
			assert c.prev != null;
			// next and prev in same triangle
			assert c.next.triangle == c.triangle;
			assert c.prev.triangle == c.triangle;
			// opposite can be null for triangles at the end of a non-closed surface
			if (c.opposite != null) {
				Corner o = c.opposite;
				// different triangle
				assert c.triangle != o.triangle;
				// same edge though
				assert c.edge == o.edge;
				// and hence same next + prev vertices
				assert c.next.vertex == o.prev.vertex;
				assert c.prev.vertex == o.next.vertex;
				// and opposite is myself again
				assert o.opposite == c;
			}
			// for the fun of it the tests from the slide
			if (c.prev.opposite != null) {
				// c.p.o.n and c.n share the same vertex but are different corners
				assert c.prev.opposite.next.vertex == c.next.vertex;
				assert c.prev.opposite != c.next;
				// c.p.o.p and c share the same vertex but are different corners
				assert c.prev.opposite.prev.vertex == c.vertex;
				assert c.prev.opposite.prev != c;
			}
			// same as above
			if (c.next.opposite != null) {
				assert c.next.opposite.prev.vertex == c.prev.vertex;
				assert c.next.opposite.prev != c.prev;
				assert c.next.opposite.next.vertex == c.vertex;
				assert c.next.opposite.next != c;
			}
			// now the last one
			if (c.prev.opposite != null) {
				Corner cpop = c.prev.opposite.prev;
				assert cpop.vertex == c.vertex;
				if (cpop.prev.opposite != null) {
					Corner cpoppop = cpop.prev.opposite.prev;
					assert cpoppop.vertex == c.vertex;
					assert cpoppop != c;
					assert cpop != cpoppop;
				}
			}
		}
		System.out.println("done, it worked - neat. exiting now");
		System.exit(0);
	}
}
