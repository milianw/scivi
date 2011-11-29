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

import java.io.File;

import jv.object.PsMainFrame;
import jv.object.PsConfig;
import jv.geom.PgPointSet;
import jv.loader.PgFileDialog;
import jv.loader.PjImportModel;

/**
 * Solution to the first exercise
 * 
 * @author		Milian Wolff
 * @version		28.10.2011, 1.00 created
 */
public class Ex1_1 {
	public static void main(String args[]) {
		// Create toplevel window of application containing the applet
		PsMainFrame frame	= new PsMainFrame("SciVis - Exercise 1 - Milian Wolff", args);

		// get dir/first file
		PgFileDialog dlg = new PgFileDialog(frame, "open geometry", 0);
		dlg.setDirectory(PsConfig.getCodeBase() + "../proj1");
		dlg.show();
		if (!dlg.isFileSelected()) {
			return;
		}

		// load file
		PjImportModel importer = new PjImportModel();
		File dir = new File(new File(dlg.getFullFileName()).getParent());
		String[] files = dir.list();
		if (files == null) {
			System.err.println("Could not open data dir: " + dir.getAbsolutePath());
			System.err.println("see README for how to run this app");
			return;
		}
		for(String file : files) {
			if (!file.endsWith(".obj")) {
				continue;
			}
			file = dir.getAbsolutePath() + "/" + file;
			System.out.println("loading data from file: " + file);
			if (importer.load(file)) {
				PgPointSet geometry = (PgPointSet)importer.getGeometry().clone();
				System.out.println("euler characteristic for this file:" + geometry.getEulerCharacteristic());
			} else {
				System.err.println("PjModel.start(): failed loading geometry from file = " + file);
			}
		}
	}
}
