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
import jv.vecmath.PdVector;

@SuppressWarnings("serial")
public class TensorFieldPanel extends JPanel implements ItemListener
{
	private JComboBox m_typeCombo;
	private Panel[] m_panels;
	private AbstractTensorUIItem[] m_items;

	public TensorFieldPanel()
	{
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 1;
		c.gridy = 0;
		c.gridx = 0;
		c.anchor = GridBagConstraints.NORTH;

		m_panels = new Panel[TensorFeatureType.values().length];
		m_items = new AbstractTensorUIItem[TensorFeatureType.values().length];

		add(new Label("Type:"), c);
		c.gridx++;
		m_typeCombo = new JComboBox();
		add(m_typeCombo, c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 2;
		for(TensorFeatureType t : TensorFeatureType.values()) {
			Panel panel = new Panel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

			m_panels[t.ordinal()] = panel;
			AbstractTensorUIItem item = AbstractTensorUIItem.createItem(t, panel);
			m_items[t.ordinal()] = item;
			m_typeCombo.addItem(t);

			panel.setVisible(false);
			add(panel, c);
		}
		m_typeCombo.setSelectedIndex(TensorFeatureType.Wedge.ordinal());
		m_panels[m_typeCombo.getSelectedIndex()].setVisible(true);
		m_typeCombo.addItemListener(this);
	}

	public void setTypeChangeEnabled(boolean enabled)
	{
		m_typeCombo.setEnabled(enabled);
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
	public TensorTerm createTerm(PdVector base)
	{
		if (base.getSize() == 3) {
			base.setSize(2);
		}
		assert base.getSize() == 2;
		return m_items[m_typeCombo.getSelectedIndex()].createTerm(base);
	}
	public void setTerm(TensorTerm term)
	{
		if (term != null) {
			m_typeCombo.setSelectedIndex(term.type().ordinal());
		}
		m_items[m_typeCombo.getSelectedIndex()].setTerm(term);
	}
}

abstract class AbstractTensorUIItem extends BasicUpdateIf
{
	protected PuDouble m_strength;
	protected PuDouble m_decay;
	protected TensorTerm m_term;
	protected PuDouble m_angle;
	AbstractTensorUIItem(Panel panel)
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
		m_angle = new PuDouble("Angle");
		m_angle.setBounds(0, 360);
		m_angle.setValue(45);
		m_angle.addUpdateListener(this);
		panel.add(m_angle.getInfoPanel());
	}
	abstract public TensorTerm createTerm(PdVector base);
	public void setTerm(TensorTerm term)
	{
		m_term = term;
		if (term != null) {
			m_strength.setValue(term.strength());
			m_decay.setValue(term.decay());
			m_angle.setValue(Math.toDegrees(term.rotationAngle()));
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
			} else if (event == m_angle) {
				m_term.setRotation(angle());
			}
		}
		return super.update(event);
	}
	double angle()
	{
		return Math.toRadians(m_angle.getValue());
	}
	double strength()
	{
		return m_strength.getValue();
	}
	double decay()
	{
		return m_decay.getValue();
	}
	public static AbstractTensorUIItem createItem(TensorFeatureType t, Panel panel)
	{
		switch(t) {
		case Constant:
			return new ConstantTensorUIItem(panel);
		case Wedge:
			return new WedgeUIItem(panel);
		}
//		assert false : "Unhandled type: " + t;
		return null;
	}
}

class ConstantTensorUIItem extends AbstractTensorUIItem
{
	public ConstantTensorUIItem(Panel panel)
	{
		super(panel);
	}
	@Override
	public TensorTerm createTerm(PdVector base) {
		return new ConstantTensorTerm(base, strength(), decay(), angle());
	}
}

class WedgeUIItem extends AbstractTensorUIItem
{
	public WedgeUIItem(Panel panel) {
		super(panel);
	}
	@Override
	public TensorTerm createTerm(PdVector base) {
		return new WedgeTerm(base, strength(), decay(), angle());
	}
}