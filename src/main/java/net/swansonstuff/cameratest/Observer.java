/**
 * 
 */
package net.swansonstuff.cameratest;

import java.awt.image.ImageObserver;

/**
 * @author swansons
 *
 */
public class Observer implements ImageObserver{

	/**
	 * 
	 */
	public Observer() {
		// TODO Auto-generated constructor stub
	}

	public boolean complete = false;
	
	@Override
	public boolean imageUpdate(Image img, int infoflags, int x, int y,
			int width, int height) {
		System.out.println("io update: "+infoflags);
		complete = (1 | ImageObserver.ALLBITS) == ImageObserver.ALLBITS;
		return complete;
	}

}
