package me.nether.forgiagent.utils;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JPanel;

public class ImagePanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private Image image;

	public void setBackground(Image image) {
		this.image = image;
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), null, null);
	}

	@Override
	public void update(Graphics g) {
		this.setBackground(makeColorGradient(System.currentTimeMillis(), 0.012, 0, 2, 4, 125, 100, 0));
	}

	public static Color makeColorGradient(double frequency1, double mult, double phase1, double phase2, double phase3,
			double center, double width, double len) {
		if (center == 0)
			center = 128;
		if (width == 0)
			width = 127;
		if (len == 0)
			len = 50;

		// for (double i = 0; i < len; ++i) {
		int red = (int) (Math.sin(frequency1 * mult + phase1) * width + center);
		int grn = (int) (Math.sin(frequency1 * mult + phase2) * width + center);
		int blu = (int) (Math.sin(frequency1 * mult + phase3) * width + center);
		// }

		return new Color(red, grn, blu);
	}
}