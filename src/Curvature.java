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

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import jv.geom.PgElementSet;
import jv.geom.PgVectorField;
import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;

/**
 * Computation class for curvature at vertices of a geometry.
 */
public class Curvature {
	private PgElementSet m_geometry;
	private CornerTable m_cornerTable;
	private VertexCurvature[] m_vertexMap;
	private CotanCache m_cotanCache;
	private boolean m_hasTensor;
	public enum WeightingType {
		Uniform,
		Cord,
		Cotangent,
		MeanValue
	}
	public enum SmoothingScheme {
		ForwardEuler,
		GaussSeidel
	}
	public Curvature(PgElementSet geometry)
	{
		m_geometry = geometry;
		m_cornerTable = new CornerTable(m_geometry);
		m_vertexMap = new VertexCurvature[geometry.getNumVertices()];
		m_cotanCache = new CotanCache(m_cornerTable.size());
		m_hasTensor = false;
		computeCurvature();
	}
	/**
	 * @return Geometry for which the curvature was calculated
	 */
	public PgElementSet geometry()
	{
		return m_geometry;
	}
	/**
	 * @return curvature calculations for each vertex
	 */
	VertexCurvature[] curvatures()
	{
		return m_vertexMap;
	}
	/**
	 * Returns true if the curvature tensor was computed, false otherwise.
	 */
	boolean hasTensor()
	{
		return m_hasTensor;
	}
	public class VertexCurvature {
		public VertexCurvature() {
			area = 0;
			meanOp = new PdVector(0, 0, 0);
			gaussian = 0;
			// not computed by default
			B = null;
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
		/**
		 * Symmetric curvature tensor:
		 *
		 *  a | b
		 * ---|---
		 *  b | c
		 */
		public PdMatrix B;
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
		/**
		 * Normalize the mean curvature operator and return it.
		 *
		 * According to the paper by Meyer e.a. this is the
		 * normal of the tangent plane.
		 *
		 * Returns null if mean curvature is zero.
		 */
		public PdVector tangentPlaneNormal()
		{
			if (meanCurvature() == 0) {
				return null;
			}
			assert meanOp.length() > 0;
			PdVector n = (PdVector) meanOp.clone();
			n.normalize();
			return n;
		}
		/**
		 * Return matrix describing the tangent plane.
		 * 
		 * Row 1: tangent plane normal
		 * Row 2: arbitrary normal to tangent plane normal
		 * Row 3: cross product of the other two vectors
		 *
		 * Returns null if mean curvature is zero.
		 */
		public PdMatrix tangentPlane()
		{
			if (meanCurvature() == 0) {
				return null;
			}
			PdVector n = tangentPlaneNormal();
			PdVector x = PdVector.normalToVectorNew(n);
			PdVector y = PdVector.crossNew(n, x);
			PdMatrix ret = new PdMatrix(3, 3);
			ret.setRow(0, n);
			ret.setRow(1, x);
			ret.setRow(2, y);
			return ret;
		}
		/**
		 * Find principle directions (in tangent plane) of
		 * curvature tensor.
		 *
		 * See e.g.: http://www.math.harvard.edu/archive/21b_fall_04/exhibits/2dmatrices/index.html
		 * 
		 * Returned matrix has two rows:
		 * Row 1: major principle direction
		 * Row 2: minor principle direction
		 */
		public PdMatrix principleDirections()
		{
			double D = B.det();
			double a = B.getEntry(0, 0);
			double b = B.getEntry(0, 1);
			double c = B.getEntry(1, 0);
			double d = B.getEntry(1, 1);
			///TODO: this hits, with quite high differences :-/
	//		assert b == c : Math.abs(b-c);
	
			double T_half = 0.5d * (a + d);
			double root = Math.sqrt(T_half * T_half - D);
			double L_1 = T_half + root;
			double L_2 = T_half - root;
			boolean singular = Double.isNaN(L_1) || Double.isNaN(L_2);
			assert L_2 <= L_1 || singular : "L_1: " + L_1 + ", L_2: " + L_2;
	
			PdVector major = new PdVector(2);
			PdVector minor = new PdVector(2);
			if (c != 0 && !singular) {
				major.setEntry(0, L_1 - d);
				major.setEntry(1, c);
				minor.setEntry(0, L_2 - d);
				minor.setEntry(1, c);
				major.normalize();
				minor.normalize();
			} else if (b != 0 && !singular) {
				major.setEntry(0, b);
				major.setEntry(1, L_1 - a);
				minor.setEntry(0, b);
				minor.setEntry(1, L_2 - a);
				major.normalize();
				minor.normalize();
			}
			if (minor.equals(major) || singular
					|| Double.isNaN(minor.length())
					|| Double.isNaN(major.length())) {
				// singular
				major.setEntry(0, 1);
				major.setEntry(1, 0);
				minor.setEntry(0, 0);
				minor.setEntry(1, 1);
			}
			if (minor.dot(major) > 1E-10) {
				System.err.println("Non-Orthogonal principle directions computed:");
				System.err.println("Minor: " + minor.toShortString());
				System.err.println("Major: "+ major.toShortString());
				System.err.println("Dot: " + minor.dot(major));
			}
			PdMatrix ret = new PdMatrix(2, 2);
			ret.setRow(0, major);
			ret.setRow(1, minor);
			return ret;
		}
	}
	/**
	 * Compute tensor fields for given @param geometry, reusing previously
	 * calculated curvature and corner table.
	 *
	 * Results are stored in the curvature objects (see Curvature::B).
	 */
	public void computeCurvatureTensor()
	{
		Set<Integer> visitedVertices = new HashSet<Integer>(m_geometry.getNumVertices());
		for (Corner corner : m_cornerTable.corners()) {
			if (!visitedVertices.add(corner.vertex)) {
				// vertex already handled
				continue;
			}
			VertexCurvature curve = m_vertexMap[corner.vertex];
			if (curve == null) {
				continue;
			}
			// now we find all neighbors and compute:
			// \kappa_{i,j}^N (see page 13)
			// \vec{\delta_{i,j}} (see page 14)
			PdVector x_i = m_geometry.getVertex(corner.vertex);
			PdMatrix tangentPlane = curve.tangentPlane();
			if (tangentPlane == null) {
				System.err.println("skipping zero mean curvature at vertex: " + x_i + ", index: " + corner.vertex);
				continue;
			}
			PdVector n = tangentPlane.getRow(0);
			PdVector t1 = tangentPlane.getRow(1);
			PdVector t2 = tangentPlane.getRow(2);
			ArrayList<Double> kappas = new ArrayList<Double>(5);
			ArrayList<PdVector> deltas = new ArrayList<PdVector>(5);
			for(Corner neighbor : corner.vertexNeighbors()) {
				int j = neighbor.vertex;
				PdVector x_j = m_geometry.getVertex(j);
				// x_i - x_j
				PdVector e = PdVector.subNew(x_i, x_j);
				double e_dot_n = e.dot(n);
				kappas.add(2.0d * e_dot_n / e.sqrLength());
				// (e*n)n - e
				PdVector delta = PdVector.blendNew(e_dot_n, n, -1.0d, e);
				// ... / |(e*n)n-e|
				delta.normalize();
				// now compute coordinates in plane by dotting to t1, t2
				deltas.add(new PdVector(t1.dot(delta), t2.dot(delta)));
			}
			assert kappas.size() == deltas.size();
			// prepare A x = b, we look for the solution x
			// each row is: d1^2 2d1d2 d2^2, with d1,d2 being the coeffs of delta_{i,j}
			PdMatrix A = new PdMatrix(kappas.size(), 3);
			// kappas
			PdVector b = new PdVector(kappas.size());
			for(int i = 0; i < kappas.size(); ++i) {
				PdVector delta = deltas.get(i);
				double d1 = delta.getEntry(0);
				double d2 = delta.getEntry(1);
				A.setEntry(i, 0, d1 * d1);
				A.setEntry(i, 1, 2.0d * d1 * d2);
				A.setEntry(i, 2, d2 * d2);
				b.setEntry(i,  kappas.get(i));
			}
			PdMatrix A_T = new PdMatrix(3, kappas.size());
			A_T.transpose(A);
			PdMatrix K = new PdMatrix();
			K.mult(A_T, A);
			PdVector R = new PdVector();
			R.leftMultMatrix(A_T, b);
			// now apply cramer's rule
			// see e.g.: http://en.wikipedia.org/wiki/Cramer%27s_rule
			assert K.getNumCols() == 3;
			assert K.getNumRows() == 3;
			assert R.getSize() == 3;
			double det = K.det33();
			// x is vector of [l, m, n]
			PdVector x = new PdVector(3);
			for(int k = 0; k < 3; ++k) {
				PdMatrix K2 = PdMatrix.copyNew(K);
				K2.setColumn(k, R);
				x.setEntry(k, K2.det33() / det);
			}

			// build curvature matrix
			PdMatrix B = new PdMatrix(2, 2);
			curve.B = B;
			B.setEntry(0, 0, x.getEntry(0));
			B.setEntry(0, 1, x.getEntry(1));
			B.setEntry(1, 0, x.getEntry(1));
			B.setEntry(1, 1, x.getEntry(2));
		}
		System.out.println("done");
		m_hasTensor = true;
	}
	/**
	 * Compute tensor fields for given @param geometry, reusing previously
	 * calculated curvature tensor in @param curvature and the given corner table.
	 *
	 * This basically just finds the eigenvectors of each B which are then
	 * put into vector fields and returned.
	 *
	 * @param geometry
	 * @param curvature
	 * @param cornerTable
	 * @return array of four vector fields like this: {max, min, -max, -min}
	 */
	public PgVectorField[] computeCurvatureTensorFields()
	{
		if (!hasTensor()) {
			computeCurvatureTensor();
		}
		System.out.println("calculating principle curvature directions");

		PgVectorField[] ret = new PgVectorField[4];

		PgVectorField max = new PgVectorField(3);
		max.setGlobalVectorColor(Color.red);
		max.showIndividualMaterial(true);
		max.setGlobalVectorLength(0.01);
		max.setName("+max");
		max.setBasedOn(PgVectorField.VERTEX_BASED);
		max.setNumVectors(m_geometry.getNumVertices());
		ret[0] = max;

		PgVectorField min = new PgVectorField(3);
		min.setGlobalVectorColor(Color.blue);
		min.showIndividualMaterial(true);
		min.setGlobalVectorLength(0.01);
		min.setName("+min");
		min.setBasedOn(PgVectorField.VERTEX_BASED);
		min.setNumVectors(m_geometry.getNumVertices());
		ret[1] = min;

		for (int i = 0; i < m_vertexMap.length; ++i) {
			VertexCurvature curve = m_vertexMap[i];
			if (curve == null || curve.B == null) {
				continue;
			}
			// now scale up to 3d for display
			PdMatrix p = curve.principleDirections();
			PdVector major = p.getRow(0);
			PdVector minor = p.getRow(1);
			PdMatrix plane = curve.tangentPlane();
			PdVector x = plane.getRow(1);
			PdVector y = plane.getRow(2);
			PdVector minDir = PdVector.blendNew(minor.getEntry(0), x, minor.getEntry(1), y);
			PdVector maxDir = PdVector.blendNew(major.getEntry(0), x, major.getEntry(1), y);
			min.setVector(i, minDir);
			max.setVector(i, maxDir);
		}

		PgVectorField maxNeg = (PgVectorField) max.clone();
		maxNeg.multScalar(-1);
		maxNeg.setName("-max");
		ret[2] = maxNeg;
		PgVectorField minNeg = (PgVectorField) min.clone();
		minNeg.multScalar(-1);
		minNeg.setName("-min");
		ret[3] = minNeg;

		return ret;
	}
	/**
	 * compute curvature values of each vertex in m_geometry
	 */
	private void computeCurvature()
	{
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
		// for bad geometries, like the hand
		HashSet<Integer> blackList = new HashSet<Integer>();
		for(Corner corner : m_cornerTable.corners()) {
			Corner cno = corner.next.opposite;
			if (cno == null) {
				///TODO: what to do in such cases?
				continue;
			}

			//note: alpha, beta, gamma are all in corner.triangle
			//note: all values are apparently in degrees!
			// alpha: angle at x_i in T, between AB and AC
			// compare to angle(P) in paper
			double alpha = m_geometry.getVertexAngle(corner.triangle, corner.localVertexIndex);
			// beta: angle at prev corner, between AB and BC
			// compare to angle(Q)
			double beta = m_geometry.getVertexAngle(corner.triangle, corner.prev.localVertexIndex);
			// gamma: angle at next corner, between AC and BC
			// compare to angle(R)
			double gamma = m_geometry.getVertexAngle(corner.triangle, corner.next.localVertexIndex);

			if (alpha == 0 || beta == 0 || gamma == 0) {
				System.err.println("Zero-angle encountered in triangle, skipping: " + corner.triangle);
				blackList.add(corner.vertex);
				blackList.add(corner.prev.vertex);
				blackList.add(corner.next.vertex);
				continue;
			}
			
			double cotGamma = m_cotanCache.cotan(gamma);

			// edge between A and B, angle is beta
			// compare to PQ
			PdVector AB = PdVector.subNew(m_geometry.getVertex(corner.vertex),
											m_geometry.getVertex(corner.prev.vertex));

			double area = -1;
			// check for obtuse angle
			if (alpha >= 90 || beta >= 90 || gamma >= 90) {
				area = m_geometry.getAreaOfElement(corner.triangle);
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
				PdVector AC = PdVector.subNew(m_geometry.getVertex(corner.vertex),
												m_geometry.getVertex(corner.next.vertex));
				double cotBeta = m_cotanCache.cotan(beta);
				area = 1.0d/8.0d * (AB.sqrLength() * cotGamma + AC.sqrLength() * cotBeta);
				assert area > 0;
			}

			VertexCurvature cache = m_vertexMap[corner.vertex];
			if (cache == null) {
				cache = new VertexCurvature();
				m_vertexMap[corner.vertex] = cache;
			}
			// now e.q. 8, with alpha = our gamma from above, and beta = cnoAngle
			double cnoAngle = m_geometry.getVertexAngle(cno.triangle, cno.localVertexIndex);
			if (cnoAngle == 0) {
				System.err.println("Zero-Angle encountered in triangle " + cno.triangle + ", vertex: " + corner.vertex);
				blackList.add(corner.vertex);
				continue;
			}
			double cotCnoAngle = m_cotanCache.cotan(cnoAngle);
			cache.meanOp.add(cotGamma + cotCnoAngle, AB);
			cache.gaussian += alpha;
			cache.area += area;
		}
		for(int i : blackList) {
			m_vertexMap[i] = null;
		}
	}
	/**
	 * Smoothen tensor field @param curvature, new values will be stored in Curvate.B
	 *
	 * @param geometry
	 * @param cornerTable precomputed corner table for @param geometry
	 * @param curvature precomputed
	 * @param steps number of smoothing steps, must be greater than one
	 * @param stepSize \Delta t, i.e. integration step size, must be greater zero
	 */
	public void smoothTensorField(int steps, double stepSize, WeightingType weightingType,
									SmoothingScheme scheme)
	{
		System.out.println("Smoothening curvature tensor field. steps: " + steps + ", step size: " + stepSize);
		assert steps > 1;
		assert stepSize > 0;
		// project local 2x2 tensors into 3x3 space
		PdMatrix[] globalTensors = new PdMatrix[m_vertexMap.length];
		for(int i = 0; i < m_vertexMap.length; ++i) {
			VertexCurvature curve = m_vertexMap[i];
			if (curve == null || curve.B == null) {
				globalTensors[i] = new PdMatrix(3, 3);
				continue;
			}
			assert curve.B.getNumCols() == 2;
			assert curve.B.getNumRows() == 2;
			PdMatrix tangentPlane = curve.tangentPlane();
			PdVector x = tangentPlane.getRow(1);
			PdVector y = tangentPlane.getRow(2);
			// wow, what a nice api -.-'
			// [ x y ]
			PdMatrix xy = new PdMatrix(3, 2);
			xy.setColumn(0, x);
			xy.setColumn(1, y);
			// [ x ]
			// [ y ]
			PdMatrix xy_over = new PdMatrix(2, 3);
			xy_over.setRow(0, x);
			xy_over.setRow(1, y);
			PdMatrix b_times_xy_over = new PdMatrix();
			b_times_xy_over.mult(curve.B, xy_over);
			assert b_times_xy_over.getNumRows() == 2;
			assert b_times_xy_over.getNumCols() == 3;
			PdMatrix global = new PdMatrix();
			global.mult(xy, b_times_xy_over);
			assert global.getNumCols() == 3;
			assert global.getNumRows() == 3;
			globalTensors[i] = global;
		}
		// smooth global tensors
		for(int step = 0; step < steps; ++step) {
			PdMatrix[] smoothened = new PdMatrix[globalTensors.length];
			HashSet<Integer> visitedVertices = new HashSet<Integer>(globalTensors.length);
			// explicit method for now
			for(Corner c : m_cornerTable.corners()) {
				if (!visitedVertices.add(c.vertex)) {
					// already visited
					continue;
				}
				int i = c.vertex;
				PdMatrix sum = PdMatrix.copyNew(globalTensors[i]);
				PdVector x_i = m_geometry.getVertex(i);
				for(Corner neighbor : c.vertexNeighbors()) {
					int j = neighbor.vertex;
					assert i != j;
					Double weight = null;
					switch (weightingType) {
					case Uniform:
						weight = 1.0d;
						break;
					case Cord:
						weight = 1.0d / PdVector.subNew(x_i, m_geometry.getVertex(j)).length();
						break;
					case Cotangent:
						assert neighbor.prev.vertex != i;
						assert neighbor.prev.opposite != null;
						double theta_1 = m_cotanCache.cotan(
								m_geometry.getVertexAngle(neighbor.prev.triangle,
														neighbor.prev.localVertexIndex));
						double theta_2 = m_cotanCache.cotan(
								m_geometry.getVertexAngle(neighbor.prev.opposite.triangle,
														neighbor.prev.opposite.localVertexIndex));
						weight = (theta_1 + theta_2) * 0.5d;
						break;
					case MeanValue:
						assert neighbor.prev.vertex != i;
						assert neighbor.next.vertex == i;
						double phi_1 = m_cotanCache.tan(
								m_geometry.getVertexAngle(neighbor.next.triangle,
														neighbor.next.localVertexIndex));
						assert neighbor.prev.opposite != null;
						assert neighbor.prev.opposite.next.vertex == i;
						double phi_2 = m_cotanCache.tan(
								m_geometry.getVertexAngle(neighbor.prev.opposite.next.triangle,
														neighbor.prev.opposite.next.localVertexIndex));
						weight = (phi_1 + phi_2) * 0.5d;
						break;
					}
					assert weight != null;
					weight *= stepSize;
					PdMatrix term = PdMatrix.copyNew(
						scheme == SmoothingScheme.ForwardEuler ? globalTensors[j]
							// Gauss-Seidel, use current i.e. potentially smoothened
							: (smoothened[j] == null ? globalTensors[j] : smoothened[j])
					);
					term.sub(globalTensors[i]);
					term.multScalar(weight);
					sum.add(term);
				}
				// note: must not overwrite old values
				smoothened[i] = sum;
			}
			// for the next step, use the "new" smoothened values as "old"
			globalTensors = smoothened;
		}
		// project back into 2x2, globalTensors contains smoothened values now
		for(int i = 0; i < globalTensors.length; ++i) {
			VertexCurvature curve = m_vertexMap[i];
			if (curve == null) {
				continue;
			}
			PdMatrix tangentPlane = curve.tangentPlane();
			if (tangentPlane == null) {
				///TODO: can we not somehow get the smoothened B into here?
				continue;
			}
			PdVector x = tangentPlane.getRow(1);
			PdVector y = tangentPlane.getRow(2);
			// [ x y ]
			PdMatrix xy = new PdMatrix(3, 2);
			xy.setColumn(0, x);
			xy.setColumn(1, y);
			// [ x ]
			// [ y ]
			PdMatrix xy_over = new PdMatrix(2, 3);
			xy_over.setRow(0, x);
			xy_over.setRow(1, y);
			PdMatrix b_times_xy = new PdMatrix();
			b_times_xy.mult(globalTensors[i], xy);
			PdMatrix local = new PdMatrix();
			local.mult(xy_over, b_times_xy);
			assert local.getNumCols() == 2;
			assert local.getNumRows() == 2;
			curve.B = local;
		}
	}
}
