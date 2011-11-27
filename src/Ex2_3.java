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

class Curvature {
	public Curvature() {
		area = 0;
		meanOp = new PdVector(0, 0, 0);
		gaussian = 0;
	}
	/**
	 * mean curvature normal operator
	 *
	 * note: not normalized! i.e. misses 1/(2*area)
	 * see eq. 8.
	 **/
	public PdVector meanOp;
	/**
	 * gaussian curvature Operator
	 * note: not normalized! i.e. is just the sum of angles, misses (2pi - ...)/area
	 * note: in degree!
	 * @see gaussianCurvature()
	 **/
	public double gaussian;
	/**
	 * mixed area
	 * see fig. 4
	 */
	public double area;
	public double gaussianCurvature() {
		return (2.0d * Math.PI - Math.toRadians(gaussian)) / area;
	}
	public double meanCurvature() {
		// note: 1/2 from K as vector, another 1/2 for K_H
		return 1.0d / (4.0 * area) * meanOp.length();
	}
	public double minimumCurvature() {
		return meanCurvature() - Math.sqrt(delta());
	}
	public double maximumCurvature() {
		return meanCurvature() + Math.sqrt(delta());
	}
	public double delta() {
		return Math.max(0, Math.pow(meanCurvature(), 2) - gaussianCurvature());
	}
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
	private Checkbox m_minimumCurvature;
	private Checkbox m_maximumCurvature;
	private Checkbox m_curvatureTensor;
	private CurvatureType m_curvatureType;
	private enum CurvatureType {
		Disable,
		Mean,
		Gaussian,
		Minimum,
		Maximum,
		Tensor
	}
	private Checkbox m_colorByMax;
	private Checkbox m_colorByDeviation;
	private ColorType m_colorType;
	private enum ColorType {
		Maximum,
		Deviation
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
		// curvature method choice
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

		// minimum curvature
		m_minimumCurvature = new Checkbox("Minimum", group, false);
		m_minimumCurvature.addItemListener(this);
		c.gridy++;
		m_panel.add(m_minimumCurvature, c);

		// minimum curvature
		m_maximumCurvature = new Checkbox("Maximum", group, false);
		m_maximumCurvature.addItemListener(this);
		c.gridy++;
		m_panel.add(m_maximumCurvature, c);

		// curvature tensor
		m_curvatureTensor = new Checkbox("Tensor", group, false);
		m_curvatureTensor.addItemListener(this);
		c.gridy++;
		m_panel.add(m_curvatureTensor, c);

		m_curvatureType = CurvatureType.Mean;
		group.setSelectedCheckbox(m_meanCurvature);

		c.gridy++;
		c.fill = GridBagConstraints.CENTER;
		m_panel.add(new Label("Colorization"), c);
		// color method choice
		CheckboxGroup group2 = new CheckboxGroup();

		// color by maximum
		m_colorByMax = new Checkbox("Maximum", group2, false);
		m_colorByMax.addItemListener(this);
		c.gridy++;
		m_panel.add(m_colorByMax, c);
		c.fill = GridBagConstraints.HORIZONTAL;

		// color by deviation
		m_colorByDeviation = new Checkbox("Deviation", group2, false);
		m_colorByDeviation.addItemListener(this);
		c.gridy++;
		m_panel.add(m_colorByDeviation, c);
		c.fill = GridBagConstraints.HORIZONTAL;

		m_colorType = ColorType.Deviation;
		group2.setSelectedCheckbox(m_colorByDeviation);

		show();
	}
	@Override
	public void itemStateChanged(ItemEvent e)
	{
		Object source = e.getSource();
		if (source == m_meanCurvature) {
			m_curvatureType = CurvatureType.Mean;
		} else if (source == m_gaussianCurvature) {
			m_curvatureType = CurvatureType.Gaussian;
		} else if (source == m_curvatureTensor) {
			m_curvatureType = CurvatureType.Tensor;
		} else if (source == m_disableCurvature) {
			m_curvatureType = CurvatureType.Disable;
		} else if (source == m_maximumCurvature) {
			m_curvatureType = CurvatureType.Maximum;
		} else if (source == m_minimumCurvature) {
			m_curvatureType = CurvatureType.Minimum;
		} else if (source == m_colorByDeviation) {
			m_colorType = ColorType.Deviation;
		} else if (source == m_colorByMax) {
			m_colorType = ColorType.Maximum;
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
				System.out.println("removing geometry " + other.getName());
				m_disp.removeGeometry(other);
			}
		}
	}
	private void updateView()
	{
		PgElementSet geometry;
		try {
			geometry = (PgElementSet) m_disp.getSelectedGeometry();
		} catch (Exception e) {
			return;
		}

		clearCurvature(geometry);
		switch(m_curvatureType) {
		case Disable:
			// nothing to do, we always clear before
			break;
		case Gaussian:
		case Mean:
		case Minimum:
		case Maximum:
			setCurvatureColors(geometry, m_curvatureType, m_colorType);
			break;
		case Tensor:
			setCurvatureTensor(geometry);
			break;
		}
	}
	private void setCurvatureTensor(PgElementSet geometry)
	{
		// TODO Auto-generated method stub
		
	}
	private double[] getCurvature(PgElementSet geometry, CurvatureType type)
	{
		assert type == CurvatureType.Mean || type == CurvatureType.Gaussian
				|| type == CurvatureType.Minimum || type == CurvatureType.Maximum;
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
		Curvature[] vertexMap = new Curvature[geometry.getNumVertices()];
		// for bad geometries, like the hand
		HashSet<Integer> blackList = new HashSet<Integer>();
		for(Corner corner : table.corners()) {
			Corner cno = corner.next.opposite;
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

			Curvature cache = vertexMap[corner.vertex];
			if (cache == null) {
				cache = new Curvature();
				vertexMap[corner.vertex] = cache;
			}
			if (type == CurvatureType.Mean) {
				// now e.q. 8, with alpha = our gamma from above, and beta = cnoAngle
				double cnoAngle = geometry.getVertexAngle(cno.triangle, cno.localVertexIndex);
				if (cnoAngle == 0) {
					System.err.println("Zero-Angle encountered in triangle " + cno.triangle + ", vertex: " + corner.vertex);
					blackList.add(corner.vertex);
					continue;
				}
				double cotCnoAngle = cotanCache.cotan(cnoAngle);
				cache.meanOp.add(cotGamma + cotCnoAngle, AB);
			} else {
				cache.gaussian += alpha;
			}
			cache.area += area;
		}
		// calculate actual values
		double ret[] = new double[vertexMap.length];
		double total = 0;
		for(int i = 0; i < vertexMap.length; ++i) {
			if (blackList.contains(i)) {
				continue;
			}
			Curvature curvature = vertexMap[i];
			if (curvature == null) {
				continue;
			}
			assert curvature.area > 0;
			if (type == CurvatureType.Mean) {
				ret[i] = curvature.meanCurvature();
				assert ret[i] >= 0;
			} else if (type == CurvatureType.Minimum) {
				ret[i] = curvature.minimumCurvature();
			} else if (type == CurvatureType.Maximum) {
				ret[i] = curvature.maximumCurvature();
				assert ret[i] >= 0;
			} else {
				assert type == CurvatureType.Gaussian;
				ret[i] = curvature.gaussianCurvature();
				total += Math.toRadians(curvature.gaussian);
			}
		}
		if (type == CurvatureType.Gaussian) {
			System.out.println("total gaussian curvature: " + total);
			System.out.println("divided by 2pi: " + (total / (2.0d * Math.PI)));
		}
		return ret;
	}
	private double absMax(double[] l)
	{
		assert l.length > 0;
		double max = l[0];
		for(int i = 1; i < l.length; ++i) {
			max = Math.max(max, Math.abs(l[i]));
		}
		return max;
	}
	private void setColorsFromMaxAbs(PgElementSet geometry, double[] curvature, boolean hasNegative)
	{
		assert curvature.length == geometry.getNumVertices();
		// find maximum
		double max = absMax(curvature);
		assert max > 0;
		// assign colors
		for(int i = 0; i < curvature.length; ++i) {
			double mean = curvature[i];
			assert mean <= max;
			assert mean < Double.POSITIVE_INFINITY;
			float normalized = (float) (mean / max);
			if (hasNegative) {
				assert normalized >= -1;
				assert normalized <= 1;
				normalized = (normalized + 1.0f) / 2.0f;
			} else {
				assert mean >= 0;
			}
			assert normalized >= 0;
			assert normalized <= 1;
			geometry.setVertexColor(i,
					Color.getHSBColor(normalized, 1.0f, 1.0f)
					);
		}
		System.out.println("max curvature: " + max);
	}
	private void setColorsFromDeviation(PgElementSet geometry, double[] curvature, boolean hasNegative)
	{
		assert curvature.length == geometry.getNumVertices();
		assert curvature.length > 1;
		double mean = 0;
		for(double c : curvature) {
			mean += c;
		}
		mean /= curvature.length;
		// now calculate variance
		double variance = 0;
		for(double c : curvature) {
			variance += Math.pow(c - mean, 2);
		}
		variance /= curvature.length - 1;
		double standardDeviation = Math.sqrt(variance);
		// now set colors based on deviation:
		// zero deviation is hue of 0.5
		// deviation is normalized to +- 0.5 in the trippled standard deviation interval
		// anything higher just gets the maximum hue of 1 or 0 (both are red)
		for(int i = 0; i < curvature.length; ++i) {
			double c = curvature[i];
			double deviation = c - mean;
			float hue = ((float) deviation / (3.0f * (float) standardDeviation) + 1.0f) / 2.0f;
			if (hue > 1) {
				hue = 1;
			} else if (hue < 0) {
				hue = 0;
			}
			geometry.setVertexColor(i, Color.getHSBColor(hue, 1.0f, 1.0f));
		}
	}
	private void setCurvatureColors(PgElementSet geometry, CurvatureType type, ColorType colorType)
	{
		assert type == CurvatureType.Mean ||
				type == CurvatureType.Gaussian ||
				type == CurvatureType.Minimum ||
				type == CurvatureType.Maximum;
		System.out.println("calculating curvature of geometry " + geometry.getName() + ", type: " + type);
		double[] curvature = getCurvature(geometry, type);
		System.out.println("done, setting colors via type: " + colorType);
		boolean hasNegative = type == CurvatureType.Gaussian || type == CurvatureType.Minimum;
		switch(colorType) {
		case Deviation:
			setColorsFromDeviation(geometry, curvature, hasNegative);
			break;
		case Maximum:
			setColorsFromMaxAbs(geometry, curvature, hasNegative);
			break;
		}
		System.out.println("done, updating display");
		geometry.showElementColors(true);
		geometry.showElementFromVertexColors(true);
		m_disp.update(geometry);
	}
	private void clearCurvature(PgElementSet geometry)
	{
		geometry.removeElementColors();
		geometry.removeVertexColors();
		geometry.showElementFromVertexColors(false);
		m_disp.update(geometry);
	}
}