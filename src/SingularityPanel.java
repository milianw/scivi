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
public class SingularityPanel extends JPanel implements ItemListener
{
	private JComboBox m_typeCombo;
	private Panel[] m_panels;
	private AbstractUIItem[] m_items;

	public SingularityPanel()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		m_panels = new Panel[AbstractUIItem.Type.values().length];
		m_items = new AbstractUIItem[AbstractUIItem.Type.values().length];

		m_typeCombo = new JComboBox();
		add(m_typeCombo);
		for(AbstractUIItem.Type t : AbstractUIItem.Type.values()) {
			Panel panel = new Panel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

			m_panels[t.ordinal()] = panel;
			AbstractUIItem item = AbstractUIItem.createItem(t, panel);
			m_items[t.ordinal()] = item;
			m_typeCombo.addItem(t);

			panel.setVisible(false);
			add(panel);
		}
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
		return m_items[m_typeCombo.getSelectedIndex()].createTerm(base);
	}
}


abstract class AbstractUIItem
{
	public enum Type {
		Constant,
		Sink,
		Source,
		Saddle,
		Center,
		Generic,
	}
	protected PuDouble m_strength;
	protected PuDouble m_decay;
	AbstractUIItem(Panel panel)
	{
		m_strength = new PuDouble("Strength");
		m_strength.setBounds(0, 10, 0.1, 1);
		m_strength.setValue(1);
		panel.add(m_strength.getInfoPanel());
		m_decay = new PuDouble("Decay");
		m_decay.setBounds(0, 10, 0.1, 1);
		m_decay.setValue(0.5);
		panel.add(m_decay.getInfoPanel());
	}
	abstract public Term createTerm(PdVector base);
	
	public static AbstractUIItem createItem(Type t, Panel panel)
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
		case Generic:
			return new GenericUIItem(panel);
		}
		return new ConstantUIItem(panel);
	}
}

class ConstantUIItem extends AbstractUIItem
{
	private PuDouble m_theta;
	public ConstantUIItem(Panel panel) {
		super(panel);
		m_theta = new PuDouble("Angle");
		m_theta.setBounds(0, 360);
		m_theta.setValue(0);
		panel.add(m_theta.getInfoPanel());
	}

	@Override
	public Term createTerm(PdVector base) {
		double theta = Math.toRadians(m_theta.getValue());
		PdVector vec = new PdVector(Math.cos(theta), Math.sin(theta));
		vec.multScalar(m_strength.getValue());
		return new ConstantTerm(base, vec);
	}
}

class GenericUIItem extends AbstractUIItem
{
	protected PuDouble[] m_a;
	public GenericUIItem(Panel panel) {
		super(panel);
		m_a = new PuDouble[4];
		for(int i = 0; i < 4; ++i) {
			int x = i % 2;
			int y = i/2 % 2;
			PuDouble a = new PuDouble("A[" + x + ", " + y + "]");
			a.setBounds(-1, 1, 0.1, 0.2);
			a.setValue(x == y ? 1 : 0);
			panel.add(a.getInfoPanel());
			m_a[i] = a;
		}
	}
	@Override
	public Term createTerm(PdVector base) {
		PdMatrix A = new PdMatrix(2, 2);
		for(int i = 0; i < 4; ++i) {
			int x = i % 2;
			int y = i/2 % 2;
			A.setEntry(x, y, m_a[i].getValue());
		}
		return new GenericTerm(base, A, m_strength.getValue(), m_decay.getValue(), Color.black);
	}
}

class SinkUIItem extends AbstractUIItem
{
	public SinkUIItem(Panel panel) {
		super(panel);
	}
	@Override
	public Term createTerm(PdVector base) {
		PdMatrix A = new PdMatrix(2, 2);
		A.setConstant(0);
		A.setEntry(0, 0, -1);
		A.setEntry(1, 1, -1);
		return new GenericTerm(base, A, m_strength.getValue(), m_decay.getValue(), Color.red);
	}
}

class SourceUIItem extends AbstractUIItem
{
	public SourceUIItem(Panel panel) {
		super(panel);
	}
	@Override
	public Term createTerm(PdVector base) {
		PdMatrix A = new PdMatrix(2, 2);
		A.setConstant(0);
		A.setEntry(0, 0, 1);
		A.setEntry(1, 1, 1);
		return new GenericTerm(base, A, m_strength.getValue(), m_decay.getValue(), Color.green);
	}
}

class SaddleUIItem extends AbstractUIItem
{
	public SaddleUIItem(Panel panel) {
		super(panel);
	}
	@Override
	public Term createTerm(PdVector base) {
		PdMatrix A = new PdMatrix(2, 2);
		A.setConstant(0);
		A.setEntry(0, 0, 1);
		A.setEntry(1, 1, -1);
		return new GenericTerm(base, A, m_strength.getValue(), m_decay.getValue(), Color.blue);
	}
}
class CenterUIItem extends AbstractUIItem
{
	public CenterUIItem(Panel panel) {
		super(panel);
	}
	@Override
	public Term createTerm(PdVector base) {
		PdMatrix A = new PdMatrix(2, 2);
		A.setConstant(0);
		A.setEntry(0, 1, -1);
		A.setEntry(1, 0, 1);
		return new GenericTerm(base, A, m_strength.getValue(), m_decay.getValue(), Color.yellow);
	}
}
