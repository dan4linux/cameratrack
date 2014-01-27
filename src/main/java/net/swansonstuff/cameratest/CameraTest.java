/**
 * 
 */
package net.swansonstuff.cameratest;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.httpclient.HttpURL;

import com.android.dx.util.ByteArray;

import net.sf.jipcam.axis.MjpegFrame;
import net.sf.jipcam.axis.MjpegFrameParser;
import net.sf.jipcam.axis.MjpegInputStream;
import net.sf.jipcam.axis.MjpegPanel;
import net.sf.jipcam.axis.MjpegParserEvent;
import net.sf.jipcam.axis.MjpegParserListener;

/**
 * @author swansons
 *
 */
public class CameraTest implements MjpegParserListener {
	
	
	class ImageImplement extends JPanel {

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
				label.repaint();
		}

	}

	public class ImageInJframe extends JFrame {

		ImageImplement panel = new ImageImplement();
		
		public ImageInJframe(){
			add(panel);
			setVisible(true);
			setSize(400,400);
			setDefaultCloseOperation(EXIT_ON_CLOSE);
		}
		
		public void setImage(Image i) {
			panel.setImage(i);
		}
	}


	// Read more: http://mrbool.com/showing-images-in-a-swing-jframe-in-java/24594#ixzz2r5V6HUQA
		
		

	private byte[] buf;
	private WritableRaster lastRaster;
	private WritableRaster currentRaster;
	ImageInJframe imageInJframe;
	ImageObserver observer = new ImageObserver() {

	};

	/**
	 * 
	 */
	public CameraTest() {
		// TODO Auto-generated constructor stub
	}

	public void run(String[] args) throws Exception{
		imageInJframe = new ImageInJframe();
		HttpURLConnection cameraUrl = (HttpURLConnection) new URL("http://192.168.101.201/axis-cgi/mjpg/video.cgi?resolution=320x240&showlength=1&req_fps=10&deltatime=1").openConnection();
		cameraUrl.setUseCaches(false);
		MjpegFrameParser parser = new MjpegFrameParser(cameraUrl.getInputStream());
		parser.addMjpegParserListener(this);
		parser.start();
	}

	private void processFrame(MjpegFrame frame) {

		if (frame.getBytes().length > 0) {
			
			BufferedImage bi = toBufferedImage(frame.getImage());
			long startTime = System.currentTimeMillis();
			int verticalCenter = bi.getHeight() / 2;
			int bandWidth = 10;
			int halfBand = bandWidth / 2;
			currentRaster = (WritableRaster) bi.getData(new Rectangle(0, verticalCenter - halfBand, bi.getWidth(), verticalCenter + halfBand));
			BufferedImage newbi =  new BufferedImage(currentRaster.getWidth(), currentRaster.getHeight() - currentRaster.getMinY(), bi.getType());
			Graphics2D graphics = newbi.createGraphics();
			graphics.drawImage(bi, 0, 0, currentRaster.getWidth(), currentRaster.getHeight(),
					newbi.getMinX(), newbi.getMinY(), newbi.getWidth(), newbi.getHeight(), observer
					);
			while (observer.complete)
			graphics.dispose();
			imageInJframe.setImage(bi);
			int diff = 0;
			int DATA_BUCKETS = 160;
			int step = bi.getWidth() / DATA_BUCKETS;
			int allowedColorVariance = 100;
			
			String[] deltaMap = new String[(int) Math.ceil(1.0 * bi.getWidth() / step)];
			int deltaIndex = 0;
			
			if (lastRaster != null) {
				for (int x = 0; x < bi.getWidth(); x = x + step ) {
					
					int offPixels = getMismatchedPixelCount(allowedColorVariance, x);
					
					if (1.0 * offPixels / bandWidth > 0.50) {
						// mark as very different
						deltaMap[deltaIndex++] = "O";
						diff++;
					} else {
						// mark as close enough
						deltaMap[deltaIndex++] = ".";
					}
				}
			
				for (String c : deltaMap) {
					System.out.print(c);
				}
				System.out.println(" Processing frame in "+(System.currentTimeMillis() - startTime)+"ms.  Difference("+diff+") is "+(100.0 * diff / DATA_BUCKETS )+"%");

			}
		} else {
			System.out.print(".");
		}
		lastRaster = currentRaster;
	}

	private int getMismatchedPixelCount(int allowedColorVariance, int x) {
		byte[] currentRGB;
		byte[] previousRGB;
		int offPixels = 0;
		for (int y = currentRaster.getMinY(); y < currentRaster.getHeight(); y++){
			currentRGB = (byte[]) currentRaster.getDataElements(x, y, null);
			previousRGB = (byte[]) lastRaster.getDataElements(x, y, null);
			if (
					(Math.abs(currentRGB[0] - previousRGB[0]) > allowedColorVariance) ||
					(Math.abs(currentRGB[1] - previousRGB[1]) > allowedColorVariance) ||
					(Math.abs(currentRGB[2] - previousRGB[2]) > allowedColorVariance)
					) {
				offPixels++;
			}
		}
		return offPixels;
	}
	
	public static BufferedImage toBufferedImage(Image img)
	{
	    if (img instanceof BufferedImage)
	    {
	        return (BufferedImage) img;
	    }

	    // Create a buffered image with transparency
	    BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

	    // Draw the image on to the buffered image
	    Graphics2D bGr = bimage.createGraphics();
	    bGr.drawImage(img, 0, 0, null);
	    bGr.dispose();

	    // Return the buffered image
	    return bimage;
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		new CameraTest().run(args);
		while (true) {
			try {
			Thread.sleep(100);
			} catch(InterruptedException ie) {
				break;
			}
		}
	}

	@Override
	public void onMjpegParserEvent(MjpegParserEvent event) {		
		try {
		processFrame(event.getMjpegFrame());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
