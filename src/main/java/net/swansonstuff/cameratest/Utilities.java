/**
 * 
 */
package net.swansonstuff.cameratest;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

/**
 * @author Dan Swanson
 *
 */
public class Utilities {
		
	public static BufferedImage toBufferedImage(Image img) {
		if (img instanceof BufferedImage) {
			return (BufferedImage) img;
		}
		
		ImageObserver observer = new ImageObserver() {		
			public boolean imageUpdate(Image img, int infoflags, int x, int y,
					int width, int height) {
				return false;
			}
		};

		// Create a buffered image with transparency
		BufferedImage bimage = new BufferedImage(img.getWidth(observer), img.getHeight(observer), BufferedImage.TYPE_INT_ARGB);

		// Draw the image on to the buffered image
		Graphics2D bGr = bimage.createGraphics();
		bGr.drawImage(img, 0, 0, null);
		bGr.dispose();

		// Return the buffered image
		return bimage;
	}

}
