/*
	Copyright 2012 Milian Wolff <mail@milianw.de>
	
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

import jv.geom.PgPointSet;
import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;

/**
 * 2x2 symmetric tensor field
 */
public class TensorField extends BasicUpdateIf
{
	private ArrayList<TensorTerm> m_terms;
	private PgPointSet m_termBasePoints;
	private PdMatrix m_rot;
	private PdMatrix m_rot_t;
	public TensorField()
	{
		m_terms = new ArrayList<TensorTerm>(10);
		m_termBasePoints = new PgPointSet(2);
		m_termBasePoints.setName("Tensor Field Base Points");
		m_termBasePoints.showVertices(true);
		m_termBasePoints.showVertexColors(true);
		m_termBasePoints.setGlobalVertexSize(5.0);
		// no rotation by default
		m_rot = new PdMatrix(2, 2);
		m_rot.setEntry(0, 0, 1);
		m_rot.setEntry(1, 1, 1);
		m_rot_t = PdMatrix.copyNew(m_rot);
	}
	public void setRotation(PdMatrix rot)
	{
		m_rot = PdMatrix.copyNew(rot);
		m_rot_t = PdMatrix.copyNew(m_rot);
		m_rot_t.transpose();
	}
	public PgPointSet termBasePoints()
	{
		return m_termBasePoints;
	}
	public PdMatrix evaluate(PdVector pos)
	{
		PdMatrix ret = new PdMatrix(2, 2);
		for(TensorTerm t : m_terms) {
			ret.add(t.evaluate(pos));
		}
		ret.rightMult(m_rot_t);
		ret.leftMult(m_rot);
		return ret;
	}
	public void addTerm(TensorTerm term)
	{
		m_termBasePoints.addVertex(term.base());
		m_termBasePoints.setVertexColor(m_termBasePoints.getNumVertices() - 1, term.vertexColor());
		m_termBasePoints.update(m_termBasePoints);
		term.setParent(this);
		m_terms.add(term);
		update(this);
	}
	public void removeLast()
	{
		m_termBasePoints.removeVertex(m_termBasePoints.getNumVertices() - 1);
		m_termBasePoints.update(m_termBasePoints);
		m_terms.remove(m_terms.size() - 1);
		update(this);
	}
	public void removeTerm(int index)
	{
		m_termBasePoints.removeVertex(index);
		m_termBasePoints.update(m_termBasePoints);
		m_terms.remove(index);
		update(this);
	}
	public TensorTerm getTerm(int index)
	{
		return m_terms.get(index);
	}
	@Override
	public boolean update(Object event) {
		if (m_terms.contains(event)) {
			update(this);
			return true;
		}
		return super.update(event);
	}
	public void clear()
	{
		for(int i = m_termBasePoints.getNumVertices(); i > 0; --i) {
			m_termBasePoints.removeVertex(i - 1);
		}
		m_terms.clear();
		m_termBasePoints.update(m_termBasePoints);
		update(this);
	}
}

enum TensorFeatureType {
	Constant,
	Wedge,
	Trisector,
	Node,
	Center,
	Saddle,
	Generic
}

abstract class TensorTerm extends BasicUpdateIf
{
	protected PdVector m_base;
	protected double m_strength;
	protected double m_decay;
	protected double m_angle;
	protected PdMatrix m_rotation;
	protected PdMatrix m_rotation_transposed;
	TensorTerm(PdVector base, double strength, double decay, double rotation)
	{
		m_base = PdVector.copyNew(base);
		assert m_base.getSize() == 2;
		m_strength = strength;
		m_decay = decay;
		setRotation(rotation);
	}
	public PdVector base()
	{
		return m_base;
	}
	public void setBase(PdVector base)
	{
		assert base.getSize() == 2;
		m_base = base;
		update(this);
	}
	public double strength()
	{
		return m_strength;
	}
	public void setStrength(double strength)
	{
		m_strength = strength;
		update(this);
	}
	public double decay()
	{
		return m_decay;
	}
	public void setDecay(double decay)
	{
		m_decay = decay;
		update(this);
	}
	public double rotationAngle()
	{
		return m_angle;
	}
	public PdMatrix rotationMatrix()
	{
		return m_rotation;
	}
	// theta in radians!
	public void setRotation(double theta)
	{
		m_angle = theta;
		m_rotation = Utils.rotationMatrix(theta/2);
		m_rotation_transposed = PdMatrix.copyNew(m_rotation);
		m_rotation_transposed.transpose();
		update(this);
	}
	public double scaleFactor(PdVector pos)
	{
		return m_strength * Math.exp(- m_decay * PdVector.subNew(m_base, pos).sqrLength());
	}
	public PdMatrix evaluate(PdVector pos)
	{
		PdMatrix ret = new PdMatrix(2, 2);
		// note: x,y in m_base reference system
		evaluate(pos.getEntry(0) - m_base.getEntry(0), pos.getEntry(1) - m_base.getEntry(1), ret);
		ret.rightMult(m_rotation_transposed);
		ret.leftMult(m_rotation);
		ret.multScalar(scaleFactor(pos));
		return ret;
	}
	abstract protected void evaluate(double x, double y, PdMatrix ret);
	abstract public Color vertexColor();
	abstract public TensorFeatureType type();
}

class WedgeTerm extends TensorTerm
{
	public WedgeTerm(PdVector base, double strength, double decay, double rotation)
	{
		super(base, strength, decay, rotation);
	}
	@Override
	public TensorFeatureType type()
	{
		return TensorFeatureType.Wedge;
	}
	@Override
	public Color vertexColor()
	{
		return Color.cyan;
	}
	@Override
	public void evaluate(double x, double y, PdMatrix ret)
	{
		ret.setEntry(0, 0, x);
		ret.setEntry(0, 1, y);
		ret.setEntry(1, 0, y);
		ret.setEntry(1, 1, -x);
	}
}

class TrisectorTerm extends TensorTerm
{
	public TrisectorTerm(PdVector base, double strength, double decay, double rotation)
	{
		super(base, strength, decay, rotation);
	}
	@Override
	public TensorFeatureType type()
	{
		return TensorFeatureType.Trisector;
	}
	@Override
	public Color vertexColor()
	{
		return Color.orange;
	}
	@Override
	public void evaluate(double x, double y, PdMatrix ret)
	{
		ret.setEntry(0, 0, x);
		ret.setEntry(0, 1, -y);
		ret.setEntry(1, 0, -y);
		ret.setEntry(1, 1, -x);
	}
}

class ConstantTensorTerm extends TensorTerm
{
	public ConstantTensorTerm(PdVector base, double strength, double decay, double rotation)
	{
		super(base, strength, decay, rotation);
	}
	@Override
	public TensorFeatureType type() {
		return TensorFeatureType.Constant;
	}
	@Override
	public Color vertexColor() {
		return Color.white;
	}
	@Override
	protected void evaluate(double x, double y, PdMatrix ret) {
		//FIXME: what are the correct values here?
		ret.setEntry(0, 0, 1);
		ret.setEntry(1, 1, -1);
	}
}

class NodeTensorTerm extends TensorTerm
{
	public NodeTensorTerm(PdVector base, double strength, double decay, double rotation)
	{
		super(base, strength, decay, rotation);
	}
	@Override
	public TensorFeatureType type() {
		return TensorFeatureType.Node;
	}
	@Override
	protected void evaluate(double x, double y, PdMatrix ret) {
		ret.setEntry(0, 0, x*x - y*y);
		ret.setEntry(0, 1, 2*x*y);
		ret.setEntry(1, 0, 2*x*y);
		ret.setEntry(1, 1, -x*x + y*y);
	}
	@Override
	public Color vertexColor() {
		return Color.green;
	}
}

class CenterTensorTerm extends TensorTerm
{
	public CenterTensorTerm(PdVector base, double strength, double decay, double rotation)
	{
		super(base, strength, decay, rotation);
	}
	@Override
	public TensorFeatureType type() {
		return TensorFeatureType.Center;
	}
	@Override
	protected void evaluate(double x, double y, PdMatrix ret) {
		ret.setEntry(0, 0, y*y - x*x);
		ret.setEntry(0, 1, - 2*x*y);
		ret.setEntry(1, 0, - 2*x*y);
		ret.setEntry(1, 1, -y*y + x*x);
	}
	@Override
	public Color vertexColor() {
		return Color.yellow;
	}
}

class SaddleTensorTerm extends TensorTerm
{
	public SaddleTensorTerm(PdVector base, double strength, double decay, double rotation)
	{
		super(base, strength, decay, rotation);
	}
	@Override
	public TensorFeatureType type() {
		return TensorFeatureType.Saddle;
	}
	@Override
	protected void evaluate(double x, double y, PdMatrix ret) {
		ret.setEntry(0, 0, x*x - y*y);
		ret.setEntry(0, 1, - 2*x*y);
		ret.setEntry(1, 0, - 2*x*y);
		ret.setEntry(1, 1, -x*x + y*y);
	}
	@Override
	public Color vertexColor() {
		return Color.blue;
	}
}

class GenericTensorTerm extends TensorTerm
{
	private int m_n;
	public GenericTensorTerm(int n, PdVector base, double strength, double decay, double rotation)
	{
		super(base, strength, decay, rotation);
		m_n = n;
	}
	@Override
	public TensorFeatureType type() {
		return TensorFeatureType.Generic;
	}
	@Override
	protected void evaluate(double x, double y, PdMatrix ret) {
		final double D = Math.sqrt(x*x - y*y);
		final double theta = Math.atan2(y, x);
		assert !Double.isNaN(theta);
		//TODO: ??
		double a = 1;
		double b = 0;
		double c = 0;
		double d = 1;
		assert a*d - b*c > 0;
		ret.setEntry(0, 0, a * Math.cos(m_n * theta) + b * Math.sin(m_n * theta));
		ret.setEntry(0, 1, c * Math.cos(m_n * theta) + d * Math.sin(m_n * theta));
		ret.setEntry(1, 0, ret.getEntry(0, 1));
		ret.setEntry(1, 1, -ret.getEntry(0, 0));
		ret.multScalar(Math.pow(D, m_n));
	}
	@Override
	public Color vertexColor() {
		return Color.blue;
	}
	public int n() {
		return m_n;
	}
	public void setN(int n) {
		m_n = n;
		update(this);
	}
}