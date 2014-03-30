/**
 * 
 */
package net.swansonstuff.cameratest;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import net.sf.jipcam.axis.MjpegFrameParser;
import net.sf.jipcam.axis.MjpegParserListener;

/**
 * @author Dan Swanson
 *
 */
public class ImageStreamReader extends Thread {
	
	private MjpegFrameParser parser;
	private String fileName;
	private MjpegParserListener listener;
	
	public ImageStreamReader(String fileName,MjpegParserListener listener) {
		this.fileName = fileName;
		this.listener = listener;
	}
	
	public void run() {

		try {
			InputStream is = new FileInputStream(new File(fileName));

			parser = new MjpegFrameParser(is);
			parser.addMjpegParserListener(listener);
			parser.start();
			while (!isInterrupted()) {
				sleep(1000);
			}
		} catch(Exception e) {
			System.err.println("Caught exception: "+e.getMessage());
			e.printStackTrace();
		} finally {
			if (parser != null) {
				parser.stop();
			}
		}

	}
	
	public void shutdown() {
		if (parser != null) {
			parser.stop();
		}
	}


}
