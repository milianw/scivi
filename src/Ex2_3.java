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
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.Label;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;

import jv.geom.PgElementSet;
import jv.geom.PgVectorField;
import jv.number.PuDouble;
import jv.number.PuInteger;
import jv.object.PsUpdateIf;
import jv.project.PgGeometryIf;
import jv.project.PvGeometryListenerIf;

/**
 * Solution to third exercise of second project
 * 
 * @author		Milian Wolff
 * @version		26.11.2011, 1.00 created
 */
public class Ex2_3 extends ProjectBase implements PvGeometryListenerIf, ItemListener,
													ActionListener, PsUpdateIf
{
	public static void main(String[] args)
	{
		new Ex2_3(args);
	}
	private JComboBox m_curvatureCombo;
	private CurvatureType m_curvatureType;
	private enum CurvatureType {
		Mean,
		Gaussian,
		Minimum,
		Maximum
	}
	private JComboBox m_color;
	private ColorType m_colorType;
	private enum ColorType {
		NoColors,
		Maximum,
		Deviation
	}
	private Checkbox m_curvatureTensor;
	private boolean m_displayTensor;
	private PuDouble m_vectorLength;
	private JComboBox m_tensor;
	private TensorType m_tensorType;
	private enum TensorType {
		Minor,
		Major,
		MinorAndMajor
	}
	private Button m_smoothTensor;
	private Button m_resetTensor;
	private PuInteger m_smoothSteps;
	private PuDouble m_smoothStepSize;
	private JComboBox m_weighting;
	private Curvature.WeightingType m_weightingType;
	private JComboBox m_smoothing;
	private Curvature.SmoothingScheme m_smoothingScheme;
	public Ex2_3(String[] args)
	{
		super(args, "SciVis - Project 2 - Exercise 3 - Milian Wolff");

		// listener
		m_disp.addGeometryListener(this);
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 0;

		c.gridy++;
		c.fill = GridBagConstraints.CENTER;
		m_panel.add(boldLabel("Curvature"), c);
		c.fill = GridBagConstraints.HORIZONTAL;

		// curvature method choice
		c.gridy++;
		c.gridwidth = 1;
		m_panel.add(new Label("Show:"), c);
		c.gridx = 1;
		m_curvatureCombo = new JComboBox();
		m_curvatureCombo.addItem(CurvatureType.Mean);
		m_curvatureCombo.addItem(CurvatureType.Gaussian);
		m_curvatureCombo.addItem(CurvatureType.Maximum);
		m_curvatureCombo.addItem(CurvatureType.Minimum);
		m_curvatureType = CurvatureType.Mean;
		m_curvatureCombo.setSelectedItem(m_curvatureType);
		m_curvatureCombo.addItemListener(this);
		m_panel.add(m_curvatureCombo, c);
		c.gridwidth = 2;
		c.gridx = 0;

		// color type
		c.gridy++;
		c.gridwidth = 1;
		m_panel.add(new Label("Color By:"), c);
		c.gridx = 1;
		m_color = new JComboBox();
		m_color.addItemListener(this);
		m_color.addItem(ColorType.NoColors);
		m_color.addItem(ColorType.Maximum);
		m_color.addItem(ColorType.Deviation);
		m_colorType = ColorType.Deviation;
		m_color.setSelectedItem(m_colorType);
		m_panel.add(m_color, c);
		c.gridwidth = 2;
		c.gridx = 0;

		// curvature tensor
		c.gridy++;
		c.fill = GridBagConstraints.CENTER;
		m_panel.add(boldLabel("Tensor"), c);
		c.fill = GridBagConstraints.HORIZONTAL;

		// display or hide
		m_curvatureTensor = new Checkbox("Display", false);
		m_curvatureTensor.addItemListener(this);
		c.gridy++;
		m_panel.add(m_curvatureTensor, c);

		// step size (\Delta t)
		m_vectorLength = new PuDouble("Vector Length", this);
		m_vectorLength.init();
		m_vectorLength.setValue(0.03);
		m_vectorLength.setBounds(0, 1);
		c.gridy++;
		m_panel.add(m_vectorLength.getInfoPanel(), c);

		c.gridy++;
		c.gridwidth = 1;
		m_panel.add(new Label("Show:"), c);
		c.gridx = 1;
		m_tensor = new JComboBox();
		m_tensor.addItemListener(this);
		m_tensor.addItem(TensorType.Minor);
		m_tensor.addItem(TensorType.Major);
		m_tensor.addItem(TensorType.MinorAndMajor);
		m_tensorType = TensorType.Minor;
		m_tensor.setSelectedItem(m_tensorType);
		m_panel.add(m_tensor, c);
		c.gridwidth = 2;
		c.gridx = 0;

		// smoothening
		c.gridy++;
		c.fill = GridBagConstraints.CENTER;
		m_panel.add(boldLabel("Smoothening"), c);
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
		m_smoothTensor.setEnabled(false);
		m_smoothTensor.addActionListener(this);
		m_panel.add(m_smoothTensor, c);

		// reset/recalculate tensor
		m_resetTensor = new Button("Reset Tensor");
		m_resetTensor.setEnabled(false);
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
		if (source == m_curvatureCombo) {
			m_curvatureType = (CurvatureType) m_curvatureCombo.getSelectedItem();
		} else if (source == m_curvatureTensor) {
			m_displayTensor = m_curvatureTensor.getState();
			m_smoothTensor.setEnabled(m_displayTensor);
			m_resetTensor.setEnabled(m_displayTensor);
		} else if (source == m_tensor) {
			m_tensorType = (TensorType) m_tensor.getSelectedItem();
		} else if (source == m_weighting) {
			m_weightingType = (Curvature.WeightingType) m_weighting.getSelectedItem();
			return;
		} else if (source == m_color) {
			m_colorType = (ColorType) m_color.getSelectedItem();
		} else if (source == m_smoothing) {
			m_smoothingScheme = (Curvature.SmoothingScheme) m_smoothing.getSelectedItem();
		} else {
			assert false : source;
		}
		updateView();
	}
	@Override
	public void actionPerformed(ActionEvent e) {
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
	@Override
	public boolean update(Object e) {
		if (e == m_vectorLength) {
			if (m_lastTensorField != null) {
				assert m_lastCurvature != null;
				for(PgVectorField field : m_lastTensorField) {
					field.setGlobalVectorLength(m_vectorLength.getValue());
				}
				m_disp.update(m_lastCurvature.geometry());
			}
		}
		return false;
	}
	@Override
	public PsUpdateIf getFather() {
		return null;
	}
	@Override
	public String getName() {
		return super.getName();
	}
	@Override
	public void setParent(PsUpdateIf arg0) {
		// wah - what to do?
		assert false;
	}
	@Override
	public void selectGeometry(PgGeometryIf geometry)
	{
		updateView();
	}
	@Override
	public void removeGeometry(PgGeometryIf geometry) {
		if (m_lastCurvature != null && geometry == m_lastCurvature.geometry()) {
			// clear cache
			m_lastCurvature = null;
			m_lastTensorField = null;
		}
		super.removeGeometry(geometry);
	}
	/*
	 * remove other geometries when opening a new one
	 * @see ProjectBase#addGeometry(jv.project.PgGeometryIf)
	 */
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
	/**
	 * update view after settings changed, i.e. new color scheme or similar
	 */
	private void updateView()
	{
		PgElementSet geometry = currentGeometry();
		if (geometry == null) {
			return;
		}
		clearCurvature(geometry);
		switch(m_curvatureType) {
		case Gaussian:
		case Mean:
		case Minimum:
		case Maximum:
			setCurvatureColors(geometry, m_curvatureType, m_colorType,
					m_displayTensor, m_tensorType);
			break;
		}
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
	/**
	 * given curvature values @param curvature, set vertex colors
	 * of @param geometry, by mapping the curvature values to the
	 * HSV color space. This is done by normalizing the (possibly
	 * negative values, see @hasNegative) to the range [0-1] by
	 * first finding the absolute maximum of the curvature values,
	 * then dividing each entry by that value.
	 *
	 * It turns out that this is a not-so-good color scheme,
	 * see setColorsFromDeviation for a better alternative.
	 *
	 * @param geometry
	 * @param curvature
	 * @param hasNegative must be true if the values passed can
	 * be negative, e.g. for gaussian or minimum curvatures
	 */
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
	/**
	 * Set vertex colors of @param geometry based on associated entry in @param curvature.
	 * Again, we get colors from the HSV color space, but this time we normalize
	 * the values into the range [0-1] by calculating the standard deviation of
	 * the calculated curvatures. Then we interpolate the values to our desired
	 * range by capping the value at the tripled standard deviation interval
	 * around the mean value. Values above or below are set the hue = 0 = 1 = red.
	 *
	 * @param geometry
	 * @param curvature
	 * @param hasNegative
	 */
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
		// deviation is normalized to +- 0.5 in the tripled standard deviation interval
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
	// cache
	private Curvature m_lastCurvature;
	private PgVectorField[] m_lastTensorField;
	private void setCurvatureColors(PgElementSet geometry, CurvatureType type, ColorType colorType,
									boolean displayTensor, TensorType tensorType)
	{
		assert type == CurvatureType.Mean ||
				type == CurvatureType.Gaussian ||
				type == CurvatureType.Minimum ||
				type == CurvatureType.Maximum;

		boolean wasCached = true;
		if (m_lastCurvature == null || m_lastCurvature.geometry() != geometry) {
			m_lastCurvature = new Curvature(geometry);
			// gets updated on-demand, see below
			m_lastTensorField = null;
		}
		System.out.println("setting colors: " + colorType + ", " + type);
		Curvature.VertexCurvature[] curvature = m_lastCurvature.curvatures();
		double values[] = new double[curvature.length];
		double totalGaussian = 0;
		for (int i = 0; i < curvature.length; ++i) {
			Curvature.VertexCurvature c = curvature[i];
			if (c == null) {
				values[i] = 0;
				continue;
			}
			assert c.area > 0;
			if (type == CurvatureType.Mean) {
				values[i] = c.meanCurvature();
				assert values[i] >= 0;
			} else if (type == CurvatureType.Minimum) {
				values[i] = c.minimumCurvature();
			} else if (type == CurvatureType.Maximum) {
				values[i] = c.maximumCurvature();
				assert values[i] >= 0;
			} else {
				assert type == CurvatureType.Gaussian;
				values[i] = c.gaussianCurvature();
			}
			totalGaussian += Math.toRadians(c.gaussian);
		}
		boolean hasNegative = type == CurvatureType.Gaussian || type == CurvatureType.Minimum;
		switch(colorType) {
		case Deviation:
			setColorsFromDeviation(geometry, values, hasNegative);
			break;
		case Maximum:
			setColorsFromMaxAbs(geometry, values, hasNegative);
			break;
		case NoColors:
			break;
		}
		System.out.println("sum of theta_ij: " + totalGaussian);
		System.out.println("divided by 2pi: " + (totalGaussian / (2.0d * Math.PI)));
		System.out.println("vertices: " + geometry.getNumVertices());
		System.out.println("faces: " + geometry.getNumElements());
		System.out.println("edges: " + geometry.makeEdgeStars().length);
		System.out.println("total angle deficit:" + (geometry.getNumVertices() - totalGaussian / (2.0d * Math.PI)));
		System.out.println("euler characteristic:" + geometry.getEulerCharacteristic());

		if (displayTensor) {
			PgVectorField[] tensorField;
			if (wasCached && m_lastTensorField != null) {
				tensorField = m_lastTensorField;
			} else {
				tensorField = m_lastCurvature.computeCurvatureTensorFields();
				m_lastTensorField = tensorField;
			}
			assert tensorField != null;
			assert tensorField.length == 4;
			for(int i = 0; i < 4; ++i) {
				switch (tensorType) {
				case Major:
					if (i != 0 && i != 2) {
						// skip minor
						continue;
					}
					break;
				case Minor:
					if (i != 1 && i != 3) {
						// skip major
						continue;
					}
					break;
				case MinorAndMajor:
					// add all
					break;
				}
				tensorField[i].setGlobalVectorLength(m_vectorLength.getValue());
				geometry.addVectorField(tensorField[i]);
			}
			geometry.setGlobalVectorLength(0.05);
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
		geometry.removeAllVectorFields();
		m_disp.update(geometry);
	}
}