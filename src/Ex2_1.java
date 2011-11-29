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

import jv.geom.PgElementSet;
import jv.loader.PgFileDialog;
import jv.loader.PjImportModel;
import jv.object.PsMainFrame;

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
