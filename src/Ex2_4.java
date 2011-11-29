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

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Label;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;

import jv.geom.PgVectorField;
import jv.number.PuDouble;
import jv.number.PuInteger;
import jv.object.PsUpdateIf;
import jv.project.PvGeometryListenerIf;


/**
 * Solution to fourth exercise of the second project
 * 
 * @author		Milian Wolff
 * @version		26.11.2011, 1.00 created
 */
public class Ex2_4 extends ProjectBase implements PvGeometryListenerIf, ItemListener,
													ActionListener
{
	public static void main(String[] args)
	{
		new Ex2_4(args);
	}
	private JComboBox m_methodCombo;
	private Method m_method;
	private enum Method {
		MethodA,
		MethodB
	}
	private JComboBox m_tensor;
	private TensorType m_tensorType;
	private enum TensorType {
		Minor,
		Major
	}
	private Button m_smoothTensor;
	private Button m_resetTensor;
	private PuInteger m_smoothSteps;
	private PuDouble m_smoothStepSize;
	private JComboBox m_weighting;
	private Curvature.WeightingType m_weightingType;
	private JComboBox m_smoothing;
	private Curvature.SmoothingScheme m_smoothingScheme;
	private Curvature m_lastCurvature;
	private PgVectorField[] m_lastTensorField;
	public Ex2_4(String[] args)
	{
		super(args, "SciVis - Project 2 - Exercise 4 - Milian Wolff");

		Font boldFont = new Font("Dialog", Font.BOLD, 12);

		// listener
		m_disp.addGeometryListener(this);
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 0;

		c.gridy++;
		c.fill = GridBagConstraints.CENTER;
		Label l = new Label("Streamlines");
		l.setFont(boldFont);
		m_panel.add(l, c);
		c.fill = GridBagConstraints.HORIZONTAL;

		// curvature method choice
		c.gridy++;
		c.gridwidth = 1;
		m_panel.add(new Label("Method:"), c);
		c.gridx = 1;
		m_methodCombo = new JComboBox();
		m_methodCombo.addItem(Method.MethodA);
		m_methodCombo.addItem(Method.MethodB);
		m_method = Method.MethodA;
		m_methodCombo.setSelectedItem(m_method);
		m_methodCombo.addItemListener(this);
		m_panel.add(m_methodCombo, c);
		c.gridwidth = 2;
		c.gridx = 0;

		// curvature direction choice
		c.gridy++;
		c.gridwidth = 1;
		m_panel.add(new Label("Direction:"), c);
		c.gridx = 1;
		m_tensor = new JComboBox();
		m_tensor.addItem(TensorType.Minor);
		m_tensor.addItem(TensorType.Major);
		m_tensorType = TensorType.Minor;
		m_tensor.setSelectedItem(m_tensorType);
		m_tensor.addItemListener(this);
		m_panel.add(m_tensor, c);
		c.gridwidth = 2;
		c.gridx = 0;

		// smoothening
		c.gridy++;
		c.fill = GridBagConstraints.CENTER;
		l = new Label("Smoothening");
		l.setFont(boldFont);
		m_panel.add(l, c);
		c.fill = GridBagConstraints.HORIZONTAL;

		// number of steps for smoothing
		m_smoothSteps = new PuInteger("Steps");
		m_smoothSteps.init();
		m_smoothSteps.setBounds(1, 1000);
		m_smoothSteps.setValue(10);
		c.gridy++;
		m_panel.add(m_smoothSteps.getInfoPanel(), c);

		// step size (\Delta t)
		m_smoothStepSize = new PuDouble("Step size");
		m_smoothStepSize.init();
		m_smoothStepSize.setValue(0.1);
		m_smoothStepSize.setBounds(0, 10);
		c.gridy++;
		m_panel.add(m_smoothStepSize.getInfoPanel(), c);

		// weighting
		c.gridy++;
		c.gridwidth = 1;
		m_panel.add(new Label("Weighting:"), c);
		c.gridx = 1;
		m_weighting = new JComboBox();
		m_weighting.addItemListener(this);
		m_weighting.addItem(Curvature.WeightingType.Uniform);
		m_weighting.addItem(Curvature.WeightingType.Cord);
		m_weighting.addItem(Curvature.WeightingType.Cotangent);
		m_weighting.addItem(Curvature.WeightingType.MeanValue);
		m_weightingType = Curvature.WeightingType.Uniform;
		m_weighting.setSelectedItem(m_weightingType);
		m_panel.add(m_weighting, c);
		c.gridx = 0;
		c.gridwidth = 2;

		// method
		c.gridy++;
		c.gridwidth = 1;
		m_panel.add(new Label("Scheme:"), c);
		c.gridx = 1;
		m_smoothing = new JComboBox();
		m_smoothing.addItemListener(this);
		m_smoothing.addItem(Curvature.SmoothingScheme.ForwardEuler);
		m_smoothing.addItem(Curvature.SmoothingScheme.GaussSeidel);
		m_smoothingScheme = Curvature.SmoothingScheme.ForwardEuler;
		m_smoothing.setSelectedItem(m_smoothingScheme);
		m_panel.add(m_smoothing, c);
		c.gridx = 0;
		c.gridwidth = 2;

		// smooth tensor
		c.gridy++;
		c.gridwidth = 1;
		c.gridx = 0;
		c.fill = GridBagConstraints.CENTER;
		m_smoothTensor = new Button("Smooth Tensor");
		m_smoothTensor.addActionListener(this);
		m_panel.add(m_smoothTensor, c);

		// reset/recalculate tensor
		m_resetTensor = new Button("Reset Tensor");
		m_resetTensor.addActionListener(this);
		c.gridx = 1;
		m_panel.add(m_resetTensor, c);
		c.gridx = 0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.HORIZONTAL;

		show();
		m_frame.setBounds(new Rectangle(420, 5, 1024, 550));
	}
	@Override
	public void itemStateChanged(ItemEvent e)
	{
		Object source = e.getSource();
		if (source == m_tensor) {
			m_tensorType = (TensorType) m_tensor.getSelectedItem();
		} else if (source == m_weighting) {
			m_weightingType = (Curvature.WeightingType) m_weighting.getSelectedItem();
			return;
		} else if (source == m_smoothing) {
			m_smoothingScheme = (Curvature.SmoothingScheme) m_smoothing.getSelectedItem();
		} else if (source == m_methodCombo) {
			m_method = (Method) m_methodCombo.getSelectedItem();
		} else {
			assert false : source;
		}
		updateView();
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		if (source == m_smoothTensor) {
			if (m_lastCurvature == null) {
				return;
			}
			m_lastCurvature.smoothTensorField(m_smoothSteps.getValue(), m_smoothStepSize.getValue(),
												m_weightingType, m_smoothingScheme);
			m_lastTensorField = m_lastCurvature.computeCurvatureTensorFields();
			updateView();
		} else if (source == m_resetTensor) {
			if (m_lastCurvature == null) {
				return;
			}
			m_lastCurvature.computeCurvatureTensor();
			m_lastTensorField = m_lastCurvature.computeCurvatureTensorFields();
			updateView();
		} else {
			assert false : "unhandled action source: " + source;
		}
	}
	private void updateView()
	{
		
	}
}