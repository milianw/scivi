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

import java.util.HashSet;
import java.util.Set;

import jv.geom.PgElementSet;
import jv.geom.PgPolygonSet;
import jv.vecmath.PdVector;
import jv.vecmath.PiVector;

/**
 * Find the silhouette of a given geometry.
 */
public class Silhouette {
	public enum Type {
		FaceBased,
		VertexBased
	}
	public static PgPolygonSet create(PgElementSet geometry, Type type, PdVector viewer)
	{
		switch(type) {
		case FaceBased:
			return createFaceBasedSilhouette(geometry, viewer);
		case VertexBased:
			return createVertexBasedSilhouette(geometry, viewer);
		}
		assert false : "unhandled type: " + type;
		return null;
	}
	public static PgPolygonSet createFaceBasedSilhouette(PgElementSet geometry, PdVector viewer)
	{
		PgPolygonSet silhouette = new PgPolygonSet();
		silhouette.setName("Face Based Silhouette of " + geometry.getName());

		// find visible faces
		Set<Integer> visibleFaces = new HashSet<Integer>();
		geometry.assureElementNormals();
		assert geometry.hasElementNormals();
		for(int i = 0; i < geometry.getNumElements(); ++i) {
			PdVector ray = PdVector.subNew(geometry.getCenterOfElement(null, i), viewer);
			double dot = ray.dot(geometry.getElementNormal(i));
			// if the dot product is zero, the face is either visible or hidden :-/
			// we ignore this case, assuming that it only happens for faces somewhere
			// in the middle of the visible surface, hence they do not play a role
			// in finding the silhouette anyways.
			// so we are only interested in the faces with _negative_ dot product
			// as those are visible to us (we are looking along the direction of ray)
			if (dot < 0) {
				visibleFaces.add(i);
			}
		}
		// find visible edges by iterating over the corner table
		CornerTable table = new CornerTable(geometry);
		for(Corner corner : table.corners()) {
			// an edge is part of the silhouette if 
			// a) it is part of a visible face
			// b) either it has adjacent face
			// c) or its adjacent face is not visible
			if (visibleFaces.contains(corner.triangle)
				&& (corner.opposite == null || !visibleFaces.contains(corner.opposite.triangle)))
			{
				int a = silhouette.addVertex(geometry.getVertex(corner.next.vertex));
				int b = silhouette.addVertex(geometry.getVertex(corner.prev.vertex));
				silhouette.addPolygon(new PiVector(a, b));
			}
		}
		return silhouette;
	}
	/**
	 * interpolate linearly between p1 with visibility a and p2 with visibility b
	 *
	 * @returns zero level
	 */
	private static PdVector findZeroLevel(PdVector p1, double a, PdVector p2, double b)
	{
		if (b == 0) {
			return (PdVector) p2.clone();
		} else if (a == 0) {
			return (PdVector) p1.clone();
		}
		// edge points from p1 to p2
		// hence our "x" axis starts at p1
		PdVector edge = PdVector.subNew(p2, p1);
		double x0 = a / (a - b);

		assert x0 >= -1 && x0 <= 1;
		assert (b-a) * x0 + a <= 1E-10;
		edge.multScalar(x0);
		return PdVector.addNew(p1, edge);
	}
	public static PgPolygonSet createVertexBasedSilhouette(PgElementSet geometry, PdVector viewer)
	{
		PgPolygonSet silhouette = new PgPolygonSet();
		silhouette.setName("Vertex Based Silhouette of " + geometry.getName());

		// find visible faces by linear interpolation of dot product of vertices
		// we iterate over all edges, if the dot product flips sign between
		// corner base vertex and next and prev vertex, we draw the zero level set
		// to find it we interpolate the dot products (cmp. barycentric coordinates)
		CornerTable table = new CornerTable(geometry);
		for(Corner corner : table.corners()) {
			PdVector ray = PdVector.subNew(geometry.getVertex(corner.vertex), viewer);
			// TODO: optimize: only compute visibility (i.e. dot product) once for each vertex
			// but see whether this is actually noticeably faster
			double a = ray.dot(geometry.getVertexNormal(corner.vertex));
			double b = ray.dot(geometry.getVertexNormal(corner.next.vertex));
			double c = ray.dot(geometry.getVertexNormal(corner.prev.vertex));
			// we look for faces with one visible and two hidden vertices
			// or vice versa, i.e. two invisible and one visible vertex
			// via the corner table we look for the corner that is the
			// single visible or hidden vertex
			if ((a <= 0 && b >= 0 && c >= 0) || (a >= 0 && b <= 0 && c <= 0)) {
				// a is our single vertex, find the zero level set via interpolation
				int v1 = silhouette.addVertex(
						findZeroLevel(geometry.getVertex(corner.vertex), a,
									  geometry.getVertex(corner.next.vertex), b)
				);
				int v2 = silhouette.addVertex(
						findZeroLevel(geometry.getVertex(corner.vertex), a,
									  geometry.getVertex(corner.prev.vertex), c)
				);
				silhouette.addPolygon(new PiVector(v1, v2));
				continue;
			}
		}
		return silhouette;
	}
}