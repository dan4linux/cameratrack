/**
 * 
 */
package net.swansonstuff.cameratest;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.httpclient.HttpURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.jipcam.axis.MjpegFrame;
import net.sf.jipcam.axis.MjpegFrameParser;
import net.sf.jipcam.axis.MjpegParserEvent;
import net.sf.jipcam.axis.MjpegParserListener;

/**
 * @author Dan Swanson
 *
 */
public class CameraTest {
	
	private static Logger log = LoggerFactory.getLogger(CameraTest.class);

	private CameraTrack imageInJframe;
	
	/**
	 * 
	 */
	public CameraTest() {
	}
	

}
