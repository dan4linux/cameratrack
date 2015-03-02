/**
 * 
 */
package net.swansonstuff.cameratest.helpers;

import java.io.IOException;

/**
 * @author Dan Swanson
 *
 */
public abstract class AbstractHelper implements TranscoderHelper{
	
	/**
	 * 
	 */
	public AbstractHelper() {
	}
			
	String fileName = "pipe:1";
	Process transcoder;

	boolean resolutionFilter = false;
	String resolution = "640x480";
	private int frameRate = 14;
	private String lastVideoSource = "";
	private Process process;
	
	public abstract String getStartCommand();

	
	/**
	 * @return the lastVideoSource
	 */
	public String getLastVideoSource() {
		return this.lastVideoSource;
	}
	
	/**
	 * @param lastVideoSource the lastVideoSource to set
	 */
	public void setLastVideoSource(String lastVideoSource) {
		this.lastVideoSource = lastVideoSource;
	}
	
	public Process getProcess(String videoSource) {
		
		try {
			setLastVideoSource(videoSource);
			String cmd = getStartCommand();
			System.out.println("Running: "+cmd);
			return Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void stopProcess() {
		getProcess().destroy();
	}
	
	public Process restart() {
		stopProcess();
		setProcess(getProcess(this.lastVideoSource));
		return getProcess();
	}
	
	private void setProcess(Process process) {
		this.process = process;
	}
	
	public Process getProcess() {
		return this.process;
	}


	/**
	 * @return the desiredFrameRate
	 */
	public int getFrameRate() {
		return frameRate;
	}


	/**
	 * @param desiredFrameRate the desiredFrameRate to set
	 */
	public void setFrameRate(int frameRate) {
		this.frameRate = frameRate;
	}

}
