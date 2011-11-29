import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.JFrame;

class ImagePanel extends Panel {
	BufferedImage m_image;

	public ImagePanel()
	{
	}

	public void paint(Graphics g)
	{
		if (m_image != null) {
			g.drawImage(m_image, 0, 0, null);
		}
	}
	public void setImage(BufferedImage image)
	{
		m_image = image;
	}
}

public class DisplayImage extends JFrame {
	private ImagePanel m_panel;
	public DisplayImage()
	{
		m_panel = new ImagePanel();
		getContentPane().add(m_panel);
	}
	public void setImage(BufferedImage image) {
		m_panel.setImage(image);
		setSize(image.getWidth(), image.getHeight());
		m_panel.setSize(image.getWidth(), image.getHeight());
		m_panel.repaint();
		update(getGraphics());
		if (!isVisible()) {
			setVisible(true);
		}
		System.out.println("update image");
	}
}