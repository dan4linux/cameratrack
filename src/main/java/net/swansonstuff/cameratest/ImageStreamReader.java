/**
 * 
 */
package net.swansonstuff.cameratest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
	InputStream is;
	
	public ImageStreamReader(String fileName,MjpegParserListener listener) {
		this.fileName = fileName;
		this.listener = listener;
	}

	public ImageStreamReader(InputStream is, MjpegParserListener listener) {
		this.is = is;
		this.listener = listener;
	}

	public void run() {
		if (is == null) {
			try {
				is = new FileInputStream(fileName);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		process(is);

	}

	/**
	 * @param is
	 */
	private void process(InputStream is) {
		try {

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
