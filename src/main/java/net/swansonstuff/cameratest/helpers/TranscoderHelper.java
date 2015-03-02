/**
 * 
 */
package net.swansonstuff.cameratest.helpers;

/**
 * @author Dan Swanson
 *
 */
public interface TranscoderHelper {
	
	public Process getProcess(String videoSource);
	public Process getProcess();
	public void setFrameRate(int frameRate);
	public int getFrameRate();

}
