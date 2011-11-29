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

import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;

public class Curvature {
	public Curvature() {
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
		assert minor.dot(major) <= 1E-10
				: minor.toShortString() + major.toShortString() + ", dot: " + minor.dot(major);
		PdMatrix ret = new PdMatrix(2, 2);
		ret.setRow(0, major);
		ret.setRow(1, minor);
		return ret;
	}
}
