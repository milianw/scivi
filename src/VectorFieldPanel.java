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

import java.awt.Checkbox;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import jv.number.PuDouble;
import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;

@SuppressWarnings("serial")
public class VectorFieldPanel extends JPanel implements ItemListener
{
	private JComboBox m_typeCombo;
	private Panel[] m_panels;
	private AbstractUIItem[] m_items;

	public VectorFieldPanel()
	{
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 1;
		c.gridy = 0;
		c.gridx = 0;
		c.anchor = GridBagConstraints.NORTH;

		m_panels = new Panel[FeatureType.values().length];
		m_items = new AbstractUIItem[FeatureType.values().length];

		add(new Label("Type:"), c);
		c.gridx++;
		m_typeCombo = new JComboBox();
		add(m_typeCombo, c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 2;
		for(FeatureType t : FeatureType.values()) {
			Panel panel = new Panel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

			m_panels[t.ordinal()] = panel;
			AbstractUIItem item = AbstractUIItem.createItem(t, panel);
			m_items[t.ordinal()] = item;
			m_typeCombo.addItem(t);

			panel.setVisible(false);
			add(panel, c);
		}
		m_typeCombo.setSelectedIndex(FeatureType.Generic.ordinal());
		m_panels[m_typeCombo.getSelectedIndex()].setVisible(true);
		m_typeCombo.addItemListener(this);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == m_typeCombo) {
			for(int i = 0; i < m_panels.length; ++i) {
				m_panels[i].setVisible(i == m_typeCombo.getSelectedIndex());
			}
			validate();
		}
	}
	public Term createTerm(PdVector base)
	{
		if (base.getSize() == 3) {
			base.setSize(2);
		}
		assert base.getSize() == 2;
		return m_items[m_typeCombo.getSelectedIndex()].createTerm(base);
	}
	public void setTerm(Term term)
	{
		if (term != null) {
			m_typeCombo.setSelectedIndex(term.type().ordinal());
		}
		m_items[m_typeCombo.getSelectedIndex()].setTerm(term);
	}
}

abstract class AbstractUIItem extends BasicUpdateIf
{
	protected PuDouble m_strength;
	protected PuDouble m_decay;
	protected Term m_term;
	AbstractUIItem(Panel panel)
	{
		m_strength = new PuDouble("Strength");
		m_strength.setBounds(0, 10, 0.1, 1);
		m_strength.setValue(5);
		m_strength.addUpdateListener(this);
		panel.add(m_strength.getInfoPanel());
		m_decay = new PuDouble("Decay");
		m_decay.setBounds(0, 1, 0.01, 0.1);
		m_decay.setValue(0.1);
		m_decay.addUpdateListener(this);
		panel.add(m_decay.getInfoPanel());
	}
	abstract public Term createTerm(PdVector base);
	public void setTerm(Term term)
	{
		m_term = term;
		if (term != null) {
			m_strength.setValue(term.strength());
			m_decay.setValue(term.decay());
		}
	}
	@Override
	public boolean update(Object event) {
		if (m_term != null) {
			if (event == m_strength) {
				m_term.setStrength(m_strength.getValue());
				return true;
			} else if (event == m_decay) {
				m_term.setDecay(m_decay.getValue());
				return true;
			}
		}
		return super.update(event);
	}
	public static AbstractUIItem createItem(FeatureType t, Panel panel)
	{
		switch(t) {
		case Constant:
			return new ConstantUIItem(panel);
		case Sink:
			return new SinkUIItem(panel);
		case Source:
			return new SourceUIItem(panel);
		case Saddle:
			return new SaddleUIItem(panel);
		case Center:
			return new CenterUIItem(panel);
		case Focus:
			return new FocusUIItem(panel);
		case ConvergingElement:
			return new ConvergingElementUIItem(panel);
		case DivergingElement:
			return new DivergingElementUIItem(panel);
		case Generic:
			return new GenericUIItem(panel);
		}
		assert false : "Unhandled type: " + t;
		return null;
	}
}

abstract class AbstractAngleUIItem extends AbstractUIItem
{
	protected PuDouble m_angle;
	public AbstractAngleUIItem(Panel panel)
	{
		super(panel);
		m_angle = new PuDouble("Angle");
		m_angle.setBounds(0, 360);
		m_angle.setValue(45);
		m_angle.addUpdateListener(this);
		panel.add(m_angle.getInfoPanel());
	}
	double angle()
	{
		return Math.toRadians(m_angle.getValue());
	}
	@Override
	public void setTerm(Term term) {
		super.setTerm(term);
		if (term != null) {
			try {
				AngleTerm t = (AngleTerm) term;
				m_angle.setValue(Math.toDegrees(t.angle()));
			} catch(Exception e) {
				
			}
		}
	}
	@Override
	public boolean update(Object event) {
		if (event == m_angle && m_term != null) {
			try {
			((AngleTerm) m_term).setAngle(angle());
			return true;
			} catch(Exception e) {
				
			}
		}
		return super.update(event);
	}
}

class ConstantUIItem extends AbstractAngleUIItem
{
	public ConstantUIItem(Panel panel)
	{
		super(panel);
	}

	@Override
	public Term createTerm(PdVector base)
	{
		return new ConstantTerm(base, m_strength.getValue(), m_decay.getValue(), angle());
	}
}

class GenericUIItem extends AbstractUIItem
{
	protected PuDouble[] m_a;
	public GenericUIItem(Panel panel)
	{
		super(panel);
		m_a = new PuDouble[4];
		for(int i = 0; i < 4; ++i) {
			int x = i % 2;
			int y = i/2 % 2;
			PuDouble a = new PuDouble("A[" + x + ", " + y + "]");
			a.setBounds(-1, 1, 0.1, 0.2);
			a.setValue(x == y ? 1 : 0);
			a.addUpdateListener(this);
			panel.add(a.getInfoPanel());
			m_a[i] = a;
		}
	}
	@Override
	public Term createTerm(PdVector base)
	{
		PdMatrix A = new PdMatrix(2, 2);
		for(int i = 0; i < 4; ++i) {
			int x = i % 2;
			int y = i/2 % 2;
			A.setEntry(x, y, m_a[i].getValue());
		}
		return new GenericTerm(base, A, m_strength.getValue(), m_decay.getValue(),
				FeatureType.Generic);
	}
	@Override
	public void setTerm(Term term) {
		super.setTerm(term);
		if (term != null) {
			GenericTerm t = (GenericTerm) term;
			PdMatrix A = t.coeffs();
			for(int i = 0; i < 4; ++i) {
				int x = i % 2;
				int y = i/2 % 2;
				m_a[i].setValue(A.getEntry(x, y));
			}
		}
	}
	@Override
	public boolean update(Object event) {
		if (m_term != null) {
			for(int i = 0; i < 4; ++i) {
				if (event == m_a[i]) {
					int x = i % 2;
					int y = i/2 % 2;
					((GenericTerm) m_term).coeffs().setEntry(x, y, m_a[i].getValue());
					m_term.update(this);
					return true;
				}
			}
		}
		return super.update(event);
	}
}

class SinkUIItem extends AbstractUIItem
{
	public SinkUIItem(Panel panel)
	{
		super(panel);
	}
	@Override
	public Term createTerm(PdVector base)
	{
		PdMatrix A = new PdMatrix(2, 2);
		A.setConstant(0);
		A.setEntry(0, 0, -1);
		A.setEntry(1, 1, -1);
		return new GenericTerm(base, A, m_strength.getValue(), m_decay.getValue(), FeatureType.Sink);
	}
}

class SourceUIItem extends AbstractUIItem
{
	public SourceUIItem(Panel panel)
	{
		super(panel);
	}
	@Override
	public Term createTerm(PdVector base)
	{
		PdMatrix A = new PdMatrix(2, 2);
		A.setConstant(0);
		A.setEntry(0, 0, 1);
		A.setEntry(1, 1, 1);
		return new GenericTerm(base, A, m_strength.getValue(), m_decay.getValue(), FeatureType.Source);
	}
}

class SaddleUIItem extends AbstractUIItem
{
	public SaddleUIItem(Panel panel)
	{
		super(panel);
	}
	@Override
	public Term createTerm(PdVector base)
	{
		PdMatrix A = new PdMatrix(2, 2);
		A.setConstant(0);
		A.setEntry(0, 0, 1);
		A.setEntry(1, 1, -1);
		return new GenericTerm(base, A, m_strength.getValue(), m_decay.getValue(), FeatureType.Saddle);
	}
}

class CenterUIItem extends AbstractUIItem implements ItemListener
{
	Checkbox m_clockwise;
	public CenterUIItem(Panel panel)
	{
		super(panel);
		m_clockwise = new Checkbox("Clockwise");
		m_clockwise.addItemListener(this);
		panel.add(m_clockwise);
	}
	@Override
	public Term createTerm(PdVector base)
	{
		PdMatrix A = new PdMatrix(2, 2);
		A.setConstant(0);
		int direction = m_clockwise.getState() ? -1 : 1;
		A.setEntry(0, 1, -1 * direction);
		A.setEntry(1, 0, 1 * direction);
		return new GenericTerm(base, A, m_strength.getValue(), m_decay.getValue(), FeatureType.Center);
	}
	@Override
	public void setTerm(Term term) {
		super.setTerm(term);
		if (term != null) {
			m_clockwise.setState(((GenericTerm) term).coeffs().getEntry(0, 1) == 1);
		}
	}
	@Override
	public void itemStateChanged(ItemEvent e) {
		assert e.getSource() == m_clockwise;
		if (m_term != null) {
			GenericTerm t = (GenericTerm) m_term;
			t.coeffs().multScalar(-1);
			m_term.update(this);
		}
	}
}

class FocusUIItem extends AbstractAngleUIItem
{
	public FocusUIItem(Panel panel)
	{
		super(panel);
	}
	@Override
	public Term createTerm(PdVector base)
	{
		return new GenericTerm(base, getCoeffs(), m_strength.getValue(),
				m_decay.getValue(), FeatureType.Focus);
	}
	public PdMatrix getCoeffs()
	{
		PdMatrix A = new PdMatrix(2, 2);
		double theta = angle();
		A.setEntry(0, 0, Math.cos(theta));
		A.setEntry(0, 1, -Math.sin(theta));
		A.setEntry(1, 0, Math.sin(theta));
		A.setEntry(1, 1, Math.cos(theta));
		return A;
	}
	@Override
	public void setTerm(Term term) {
		if (term != null) {
			m_angle.setValue(Math.toDegrees(Math.acos(((GenericTerm) term).coeffs().getEntry(0, 0))));
		}
		super.setTerm(term);
	}
	@Override
	public boolean update(Object event) {
		if (m_term != null && event == m_angle) {
			((GenericTerm) m_term).setCoeffs(getCoeffs());
			return true;
		}
		return super.update(event);
	}
}

class ConvergingElementUIItem extends AbstractAngleUIItem
{
	public ConvergingElementUIItem(Panel panel) {
		super(panel);
	}
	@Override
	public Term createTerm(PdVector base) {
		return new ConvergingElementTerm(base, -m_strength.getValue(), m_decay.getValue(), angle());
	}
}

class DivergingElementUIItem extends AbstractAngleUIItem
{
	public DivergingElementUIItem(Panel panel) {
		super(panel);
	}
	@Override
	public Term createTerm(PdVector base) {
		return new ConvergingElementTerm(base, m_strength.getValue(), m_decay.getValue(), angle());
	}
}
