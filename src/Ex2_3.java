import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.Label;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.HashSet;

import jv.geom.PgElementSet;
import jv.project.PgGeometryIf;
import jv.project.PvGeometryListenerIf;
import jv.vecmath.PdVector;

class CurvatureCalculationCache {
	public CurvatureCalculationCache() {
		meanOp = new PdVector(0, 0, 0);
		area = 0;
	}
	// mean curvature normal operator
	// note: not normalized! i.e. misses 1/(2*area)
	// see eq. 8.
	public PdVector meanOp;
	// mixed area
	// see fig. 4
	public double area;
}

class CotanCache {
	public CotanCache(int size)
	{
		map = new HashMap<Double, Double>(size);
	}
	double cotan(double degree)
	{
		assert degree > 0 : degree;
		if (degree == 90) {
			return 0;
		}
		Double val = map.get(degree);
		if (val == null) {
			assert Math.tan(Math.toRadians(degree)) != 0 : degree;
			val = 1.0d / Math.tan(Math.toRadians(degree));
			assert !val.isNaN() : degree;
			assert !val.isInfinite() : degree;
			map.put(degree, val);
		}
		return val;
	}
	private HashMap<Double, Double> map;
}

/**
 * Solution to third exercise of second project
 * 
 * @author		Milian Wolff
 * @version		26.11.2011, 1.00 created
 */
public class Ex2_3 extends ProjectBase implements PvGeometryListenerIf, ItemListener {
	public static void main(String[] args)
	{
		new Ex2_3(args);
	}
	private Checkbox m_disableCurvature;
	private Checkbox m_gaussianCurvature;
	private Checkbox m_meanCurvature;
	private Curvature m_curvatureType;
	private Checkbox m_curvatureTensor;
	private enum Curvature {
		Disable,
		Mean,
		Gaussian,
		Tensor
	}
	public Ex2_3(String[] args)
	{
		super(args, "SciVis - Project 2 - Exercise 3 - Milian Wolff");
		// listener
		m_disp.addGeometryListener(this);
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;

		c.gridy++;
		c.fill = GridBagConstraints.CENTER;
		m_panel.add(new Label("Curvature"), c);
		// silhouette method choice
		CheckboxGroup group = new CheckboxGroup();

		c.fill = GridBagConstraints.HORIZONTAL;
		// disable curvature
		m_disableCurvature = new Checkbox("Disabled", group, false);
		m_disableCurvature.addItemListener(this);
		c.gridy++;
		m_panel.add(m_disableCurvature, c);

		// mean curvature
		m_meanCurvature = new Checkbox("Mean", group, true);
		m_meanCurvature.addItemListener(this);
		c.gridy++;
		m_panel.add(m_meanCurvature, c);

		// gaussian curvature
		m_gaussianCurvature = new Checkbox("Gaussian", group, false);
		m_gaussianCurvature.addItemListener(this);
		c.gridy++;
		m_panel.add(m_gaussianCurvature, c);

		// curvature tensor
		m_curvatureTensor = new Checkbox("Tensor", group, false);
		m_curvatureTensor.addItemListener(this);
		c.gridy++;
		m_panel.add(m_curvatureTensor, c);

		m_curvatureType = Curvature.Mean;
		group.setSelectedCheckbox(m_meanCurvature);

		show();
	}
	@Override
	public void itemStateChanged(ItemEvent e)
	{
		Object source = e.getSource();
		if (source == m_meanCurvature) {
			m_curvatureType = Curvature.Mean;
		} else if (source == m_gaussianCurvature) {
			m_curvatureType = Curvature.Gaussian;
		} else if (source == m_curvatureTensor) {
			m_curvatureType = Curvature.Tensor;
		} else if (source == m_disableCurvature) {
			m_curvatureType = Curvature.Disable;
		} else {
			assert false;
		}
		updateView();
	}
	@Override
	public void selectGeometry(PgGeometryIf geometry)
	{
		updateView();
	}
	@Override
	public void addGeometry(PgGeometryIf geometry) {
		// hide other geometries
		for(PgGeometryIf other : m_disp.getGeometries()) {
			if (other == geometry) {
				continue;
			} else {
				m_disp.removeGeometry(other);
			}
		}
	}
	private void updateView()
	{
		PgElementSet geometry = (PgElementSet) m_disp.getSelectedGeometry();
		clearCurvature(geometry);
		switch(m_curvatureType) {
		case Disable:
			// nothing to do, we always clear before
			break;
		case Gaussian:
			setGaussianCurvature(geometry);
			break;
		case Mean:
			setMeanCurvature(geometry);
			break;
		case Tensor:
			setCurvatureTensor(geometry);
		}
	}
	private void setCurvatureTensor(PgElementSet geometry)
	{
		// TODO Auto-generated method stub
		
	}
	private void setMeanCurvature(PgElementSet geometry) {
		CornerTable table = new CornerTable(geometry);
		CotanCache cotanCache = new CotanCache(table.size());
		// iterate over all corners, each time adding the partial 
		// contribution to the mixed area and mean curvature normal operator
		// note: each corner is one summand of the sums in eq. 8 / fig 4.
		// we take xi = corner.vertex
		// and xj = corner.prev.vertex
		// hence the angles are:
		// alpha = angle(corner.next.vertex)
		// beta = angle(corner.next.opposite.vertex)
		// note: we must take obtuse triangles into account and
		// can only sum parts of the voronoi cell up at each time
		// the e.q. for that is given in sec. 3.3 on page 8
		CurvatureCalculationCache[] vertexMap = new CurvatureCalculationCache[geometry.getNumVertices()];
		// for bad geometries, like the hand
		HashSet<Integer> blackList = new HashSet<Integer>();
		for(Corner corner : table.corners()) {
			Corner cno = corner.next.opposite;
			assert cno != null;
			if (cno == null) {
				///TODO: what to do in such cases?
				continue;
			}

			//note: alpha, beta, gamma are all in corner.triangle
			//note: all values are apparently in degrees!
			// alpha: angle at x_i in T, between AB and AC
			// compare to angle(P) in paper
			double alpha = geometry.getVertexAngle(corner.triangle, corner.localVertexIndex);
			// beta: angle at prev corner, between AB and BC
			// compare to angle(Q)
			double beta = geometry.getVertexAngle(corner.triangle, corner.prev.localVertexIndex);
			// gamma: angle at next corner, between AC and BC
			// compare to angle(R)
			double gamma = geometry.getVertexAngle(corner.triangle, corner.next.localVertexIndex);

			if (alpha == 0 || beta == 0 || gamma == 0) {
				System.err.println("Zero-angle encountered in triangle, skipping: " + corner.triangle);
				blackList.add(corner.vertex);
				blackList.add(corner.prev.vertex);
				blackList.add(corner.next.vertex);
				continue;
			}
			
			double cotGamma = cotanCache.cotan(gamma);

			// edge between A and B, angle is beta
			// compare to PQ
			PdVector AB = PdVector.subNew(geometry.getVertex(corner.vertex),
											geometry.getVertex(corner.prev.vertex));

			double area = -1;
			// check for obtuse angle
			if (alpha >= 90 || beta >= 90 || gamma >= 90) {
				area = geometry.getAreaOfElement(corner.triangle);
				assert area > 0;
				// check if angle of T at x is obtuse
				if (alpha > 90) {
					area /= 2.0d;
				} else {
					area /= 4.0d;
				}
			} else {
				// voronoi region of x in t:
				// edge between A and C, angle is gamma
				// compare to PR
				PdVector AC = PdVector.subNew(geometry.getVertex(corner.vertex),
												geometry.getVertex(corner.next.vertex));
				double cotBeta = cotanCache.cotan(beta);
				area = 1.0d/8.0d * (AB.sqrLength() * cotGamma + AC.sqrLength() * cotBeta);
				assert area > 0;
			}

			CurvatureCalculationCache cache = vertexMap[corner.vertex];
			if (cache == null) {
				cache = new CurvatureCalculationCache();
				vertexMap[corner.vertex] = cache;
			}
			// now e.q. 8, with alpha = our gamma from above, and beta = cnoAngle
			double cnoAngle = geometry.getVertexAngle(cno.triangle, cno.localVertexIndex);
			if (cnoAngle == 0) {
				System.err.println("Zero-Angle encountered in triangle " + cno.triangle + ", vertex: " + corner.vertex);
				blackList.add(corner.vertex);
				continue;
			}
			double cotCnoAngle = cotanCache.cotan(cnoAngle);
			cache.meanOp.add(cotGamma + cotCnoAngle, AB);
			cache.area += area;
		}
		// find maximum for normalization
		double meanCurvature[] = new double[vertexMap.length];
		double maxMean = 0;
		for(int i = 0; i < vertexMap.length; ++i) {
			if (blackList.contains(i)) {
				continue;
			}
			CurvatureCalculationCache cache = vertexMap[i];
			assert cache != null;
			meanCurvature[i] = 1.0d / (4.0 * cache.area) * cache.meanOp.length();
			assert meanCurvature[i] >= 0;
			if (meanCurvature[i] > maxMean) {
				System.out.println("new max: " + i);
			}
			maxMean = Math.max(meanCurvature[i], maxMean);
		}
		vertexMap = null;
		for(int i = 0; i < meanCurvature.length; ++i) {
			double mean = meanCurvature[i];
			assert mean <= maxMean;
			assert mean < Double.POSITIVE_INFINITY;
			float normalized = (float) (mean / maxMean);
			assert normalized >= 0.0f;
			assert normalized <= 1.0f;
			geometry.setVertexColor(i,
					Color.getHSBColor(normalized, 1.0f, 0.8f)
					);
		}
		System.out.println("max mean: " + maxMean);
		geometry.showElementColors(true);
		geometry.showElementFromVertexColors(true);
		m_disp.update(geometry);
	}
	private void setGaussianCurvature(PgElementSet geometry) {
		// TODO Auto-generated method stub
		
	}
	private void clearCurvature(PgElementSet geometry)
	{
		geometry.removeElementColors();
		geometry.removeVertexColors();
	}
}