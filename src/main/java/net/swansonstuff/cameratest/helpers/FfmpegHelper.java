/**
 * 
 */
package net.swansonstuff.cameratest.helpers;

/**
 * @author Dan Swanson
 *
 */
public class FfmpegHelper extends AbstractHelper {

	/**
	 * 
	 */
	public FfmpegHelper() {
	}

	/* (non-Javadoc)
	 * @see net.swansonstuff.cameratest.helpers.AbstractHelper#getStartCommand()
	 */
	@Override
	public String getStartCommand() {
		String usbCamCmd = String.format("ffmpeg %s -f mjpeg -r %s %s -qscale 2 %s",getLastVideoSource(),getFrameRate(), ((resolutionFilter ) ? " -crop="+resolution : ""),fileName);
		String streamCmd = "ffmpeg -probesize 32768 -i "+getLastVideoSource()+" -f mjpeg  -qscale "+fileName;

		String cmd = "";

		if (getLastVideoSource().trim().toLowerCase().startsWith("/dev/")) {
			cmd = "ffmpeg -nostats -f v4l2 -video_size "+resolution+ " -i "+getLastVideoSource()+" -f mjpeg -r "+getFrameRate()+((resolutionFilter ) ? " -crop="+resolution : "")+"  -qscale 2 "+fileName;
		} else {
			cmd = "ffmpeg -nostats -probesize 32768 -i "+getLastVideoSource()+" -f mjpeg -r "+getFrameRate()+((resolutionFilter ) ? " -crop="+resolution : "")+"  -qscale 2 "+fileName;
		}
		return cmd;
	}

}
