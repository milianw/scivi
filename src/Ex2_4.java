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
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Label;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;

import javax.swing.JComboBox;

import jv.geom.PgElementSet;
import jv.geom.PgPolygonSet;
import jv.geom.PgVectorField;
import jv.number.PuDouble;
import jv.number.PuInteger;
import jv.project.PgGeometryIf;
import jv.project.PvCameraEvent;
import jv.project.PvCameraListenerIf;
import jv.project.PvDisplayIf;
import jv.project.PvGeometryListenerIf;
import jv.project.PvLightIf;
import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;
import jv.vecmath.PiVector;


/**
 * Solution to fourth exercise of the second project
 * 
 * @author		Milian Wolff
 * @version		26.11.2011, 1.00 created
 */
public class Ex2_4 extends ProjectBase implements PvGeometryListenerIf, ItemListener,
													ActionListener, PvCameraListenerIf
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
	private boolean m_rendering;
	private DisplayImage m_img;
	private PgPolygonSet m_lastMajor;
	private PgPolygonSet m_lastMinor;
	public Ex2_4(String[] args)
	{
		super(args, "SciVis - Project 2 - Exercise 4 - Milian Wolff");

		Font boldFont = new Font("Dialog", Font.BOLD, 12);

		m_rendering = false;

		m_img = new DisplayImage();

		// listener
		m_disp.addGeometryListener(this);
		m_disp.addCameraListener(this);

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
	/*
	 * remove other geometries when opening a new one
	 * @see ProjectBase#addGeometry(jv.project.PgGeometryIf)
	 */
	@Override
	public void addGeometry(PgGeometryIf geometry) {
		if (m_rendering) {
			return;
		}
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
	@Override
	public void selectGeometry(PgGeometryIf geometry) {
		updateView();
	}
	@Override
	public void dragCamera(PvCameraEvent event) {
		updateView();
	}
	@Override
	public void pickCamera(PvCameraEvent event) {
		updateView();
	}
	protected void renderOffscreen(BufferedImage image)
	{
		//render
		assert m_disp.isEnabledExternalRendering();
		assert m_disp.getCanvas().getGraphics() != null;
		m_disp.update(m_disp.getCanvas().getGraphics());
		Graphics2D gfx = image.createGraphics();
		m_disp.render();
		gfx.drawImage(m_disp.getImage(), 0, 0, Color.white, null);
	}
	private BufferedImage createImage()
	{
		return new BufferedImage(m_disp.getCanvas().getWidth(),
								 m_disp.getCanvas().getHeight(),
								 BufferedImage.TYPE_INT_RGB);
	}
	private BufferedImage getSilhouette(PgElementSet geometry)
	{
		// always re-compute silhouette, position might have changed
		// TODO: cache when pos has _not_ changed?
		PgPolygonSet silhouette = Silhouette.createVertexBasedSilhouette(geometry,
											m_disp.getCamera().getPosition());

		m_disp.addGeometry(silhouette);
		m_disp.update(silhouette);

		int oldLightningModel = m_disp.getLightingModel();
		m_disp.setLightingModel(PvLightIf.MODEL_SURFACE);
		assert m_disp.containsGeometry(geometry);

		BufferedImage image = createImage();
		renderOffscreen(image);

		m_disp.removeGeometry(silhouette);
		m_disp.update(silhouette);

		m_disp.setLightingModel(oldLightningModel);
		
		return image;
	}
	private BufferedImage getGrayScale(PgElementSet geometry)
	{
		m_disp.update(geometry);
		BufferedImage image = createImage();
		renderOffscreen(image);
		return image;
	}
	private BufferedImage getTraceImage(PgPolygonSet trace)
	{
		m_disp.addGeometry(trace);
		m_disp.update(trace);

		int oldLightningModel = m_disp.getLightingModel();
		m_disp.setLightingModel(PvLightIf.MODEL_SURFACE);

		BufferedImage image = createImage();
		renderOffscreen(image);

		m_disp.setLightingModel(oldLightningModel);

		m_disp.removeGeometry(trace);
		m_disp.update(trace);

		return image;
	}
	private PgPolygonSet getTrace(Curvature curvature, TensorType direction)
	{
		PgPolygonSet ret = new PgPolygonSet();
		PgElementSet geometry = curvature.geometry();
		ret.setName("trace of " + geometry.getName() + ", dir: " + direction);
		Curvature.VertexCurvature[] curvatures = curvature.curvatures();
		for(int i = 0; i < curvatures.length; ++i) {
			Curvature.VertexCurvature curve = curvatures[i];
			if (curve == null) {
				continue;
			}
			///TODO: use tensor field directly?
			PdMatrix xy_principle = curve.principleDirections();
			PdVector principle_dir = xy_principle.getRow(direction == TensorType.Minor ? 1 : 0);
			principle_dir.multScalar(0.03);
			PdMatrix plane = curve.tangentPlane();
			PdVector x = plane.getRow(1);
			PdVector y = plane.getRow(2);
			PdVector dir = PdVector.blendNew(principle_dir.getEntry(0), x, principle_dir.getEntry(1), y);
			int a = ret.addVertex(geometry.getVertex(i));
			int b = ret.addVertex(PdVector.addNew(dir, geometry.getVertex(i)));
			ret.addPolygon(new PiVector(a, b));
		}
		ret.showVertices(false);
		return ret;
	}
	private int grayScale(int rgb)
	{
		Color c = new Color(rgb);
		return (int) Math.round(0.212671f * c.getRed() + 0.715160f * c.getGreen()
								+ 0.072169f * c.getBlue());
	}
	private void updateView()
	{
		PgElementSet geometry = currentGeometry();
		if (geometry == null || m_rendering) {
			return;
		}

		assert !m_rendering;
		m_rendering = true;

		System.out.println("updating view");
		if (m_lastCurvature == null || m_lastCurvature.geometry() != geometry) {
			m_lastCurvature = new Curvature(geometry);
			m_lastCurvature.computeCurvatureTensor();
			m_lastCurvature.smoothTensorField(10, 1, Curvature.WeightingType.MeanValue,
												Curvature.SmoothingScheme.GaussSeidel);
			//TODO: update
			m_lastMajor = getTrace(m_lastCurvature, TensorType.Major);
			m_lastMinor = getTrace(m_lastCurvature, TensorType.Minor);
		}

		m_disp.setEnabledExternalRendering(true);
		m_disp.setExternalRenderSize(m_disp.getSize().width, m_disp.getSize().height);

		// disable lighting / unwanted settings that might temper with colors
		boolean wasShowingVertices = geometry.isShowingVertices();
		geometry.showVertices(false);
		boolean wasShowingEdges = geometry.isShowingEdges();
		geometry.showEdges(false);
		boolean wasShowingElementColors = geometry.isShowingElementColors();
		geometry.showElementColors(false);
		Color oldElementColor = geometry.getGlobalElementColor();
		geometry.setGlobalElementColor(Color.white);

		m_disp.update(geometry);

		Color oldBackgroundColor = m_disp.getBackgroundColor();
		m_disp.setBackgroundColor(Color.WHITE);
		boolean wasShowingBorder = m_disp.hasPaintTag(PvDisplayIf.PAINT_BORDER);
		m_disp.setPaintTag(PvDisplayIf.PAINT_BORDER, false);
		boolean hadAntiAliasing = m_disp.hasPaintTag(PvDisplayIf.PAINT_ANTIALIAS);
		m_disp.setPaintTag(PvDisplayIf.PAINT_ANTIALIAS, false);
		final long PAINT_FOCUS = 536870912;
		boolean wasShowingFocus	= m_disp.hasPaintTag(PAINT_FOCUS);
		m_disp.setPaintTag(PAINT_FOCUS, false);

		BufferedImage grayScale = getGrayScale(geometry);
		BufferedImage silhouette = getSilhouette(geometry);
		BufferedImage minor = getTraceImage(m_lastMinor);
		BufferedImage major = getTraceImage(m_lastMajor);

		//composite image
		BufferedImage compositedImage = new BufferedImage(m_disp.getCanvas().getWidth(),
				m_disp.getCanvas().getHeight(), BufferedImage.TYPE_INT_RGB);

		final int white = Color.white.getRGB();
		final int black = Color.black.getRGB();
		for(int w = 0; w < compositedImage.getWidth(); ++w) {
			for(int h = 0; h < compositedImage.getHeight(); ++h) {
				if (silhouette.getRGB(w, h) == black) {
					// always paint silhouette
					compositedImage.setRGB(w, h, black);
					continue;
				}
				boolean isMajor = major.getRGB(w, h) == black;
				boolean isMinor = minor.getRGB(w, h) == black;
				int grayness = grayScale(grayScale.getRGB(w, h));
				boolean isBlack = false;
				if (grayness > 170) {
					// bright, paint white
					isBlack = false;
				} else if (grayness < 85) {
					// gray, paint selected
					isBlack = m_tensorType == TensorType.Major ? isMajor : isMinor;
				} else {
					// dark, paint both
					isBlack = isMajor || isMinor;
				}
				compositedImage.setRGB(w, h, isBlack ? black : white);
			}
		}

		m_img.setImage(compositedImage);

		// Restore stuff with border, do it after image has been used
		m_disp.setEnabledExternalRendering(false);
		m_disp.setPaintTag(PAINT_FOCUS, wasShowingFocus);
		m_disp.setPaintTag(PvDisplayIf.PAINT_BORDER, wasShowingBorder);
		m_disp.setPaintTag(PvDisplayIf.PAINT_ANTIALIAS, hadAntiAliasing);
		m_disp.setBackgroundColor(oldBackgroundColor);

		geometry.showVertices(wasShowingVertices);
		geometry.showEdges(wasShowingEdges);
		geometry.showElementColors(wasShowingElementColors);
		geometry.setGlobalElementColor(oldElementColor);

		m_disp.update(geometry);
		m_rendering = false;
	}
}