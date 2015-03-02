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
		boolean showFrameNumberOverlay = false;
		
		String usbCamCmd = String.format("ffmpeg %s -f mjpeg -r %s %s -qscale 2 %s",getLastVideoSource(),getFrameRate(), ((resolutionFilter ) ? " -crop="+resolution : ""),fileName);
		String streamCmd = "ffmpeg -probesize 32768 -i "+getLastVideoSource()+" -f mjpeg  -qscale "+fileName;

		String overlay = "-vf \"drawtext=fontfile=/usr/share/cups/fonts/FreeMono.ttf: timecode='01\\:00\\:00\\:00': r=25: x=(w-tw)/2: y=h-(2*lh): fontcolor=white: box=1: boxcolor=0x00000099\"";
		String cmd = "";

		if (getLastVideoSource().trim().toLowerCase().startsWith("/dev/")) {
			cmd = "ffmpeg -nostats -f v4l2 -video_size "+resolution+ " -i "+getLastVideoSource()+" -f mjpeg -r "+getFrameRate()+((resolutionFilter ) ? " -crop="+resolution : "")+"  -qscale 2 "+fileName;
		} else {
			cmd = "ffmpeg  -nostats -probesize 32768 -i "+getLastVideoSource()+" -f mjpeg -r "+getFrameRate()+((resolutionFilter ) ? " -crop="+resolution : "")+"  -qscale 2 "+((showFrameNumberOverlay) ? overlay : "")+" "+fileName;
		}
		return cmd;
	}

}
