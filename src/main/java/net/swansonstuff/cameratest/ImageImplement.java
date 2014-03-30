package net.swansonstuff.cameratest;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class ImageImplement extends JPanel {

	/**
	 * 
	 */
	private static Logger log = LoggerFactory.getLogger(ImageImplement.class);
	private static final long	serialVersionUID	= 1L;
	private Image img;
	
	JLabel label = new JLabel();
	int count = 0;

	public ImageImplement() {
		super();
		add(label);
		
	}
	
	public void paintComponent(Graphics g) {
		g.drawImage(img, 0, 0, null);
	}

	public void setImage(final Image i) {
		if (img == null) {
			Dimension size = new Dimension(i.getWidth(null), i.getHeight(null));
			setPreferredSize(size);
			setMinimumSize(size);
			setMaximumSize(size);
			setSize(size);
			setLayout(null);			
		}
		
		img = i;
		label.setIcon(new ImageIcon(i));
		repaint();
	}

}
