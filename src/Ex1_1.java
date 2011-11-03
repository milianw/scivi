import java.awt.*;
import java.io.File;

import jv.object.PsMainFrame;
import jv.project.PgGeometry;
import jv.project.PvDisplayIf;
import jv.viewer.PvViewer;
import jv.object.PsConfig;
import jv.geom.PgPointSet;
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

		// Create viewer for viewing 3d geometries, and register frame.
		PvViewer viewer = new PvViewer(null, frame);

		// Get default display from viewer
		PvDisplayIf disp = viewer.getDisplay();
		
		// load file
		PjImportModel importer = new PjImportModel();
		String dataDir = PsConfig.getCodeBase() + "../proj1/";
		File dir = new File(dataDir);
		String[] files = dir.list();
		if (files == null) {
			System.err.println("Could not open data dir: " + dataDir);
			System.err.println("see README for how to run this app");
			return;
		}
		for(String file : files) {
			if (!file.endsWith(".obj")) {
				continue;
			}
			System.out.println("loading data from file: " + dataDir + file);
			if (importer.load(dataDir + file)) {
				PgPointSet geometry = (PgPointSet)importer.getGeometry().clone();
				System.out.println("euler characteristic for this file:" + geometry.getEulerCharacteristic());
			} else {
				System.err.println("PjModel.start(): failed loading geometry from file = " + dataDir + file);
			}
		}

		/*
		// Add display to frame
		frame.add((Component)disp, BorderLayout.CENTER);
		frame.pack();
		// Position of left upper corner and size of frame when run as application.
		frame.setBounds(new Rectangle(420, 5, 640, 550));
		frame.setVisible(true);
		*/
	}
}
