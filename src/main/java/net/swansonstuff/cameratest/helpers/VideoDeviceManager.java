/**
 * This class uses video4linux to enumerate the 
 * video devices on the system and acts as the 
 * point of contact for the video devices
 */
package net.swansonstuff.cameratest.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import net.swansonstuff.cameratest.VideoDevice;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dan Swanson
 *
 */
public class VideoDeviceManager {
	
	public enum VideoDeviceManagerSubsystem {
		v4l("video4linux", "/usr/bin/v4l2-ctl", "--list-devices", "%s -c %s=%s", "-l -d %s");
		
		private String name;
		private String command;
		private String listArg;
		private String changeArg;
		private String deviceOptions;
		
		private VideoDeviceManagerSubsystem(String name, String command, String listArg, String changeArg, String deviceOptions) {
			this.setName(name);
			this.setCommand(command);
			this.setListArg(listArg);
			this.setChangeArg(changeArg);
			this.setDeviceOptions(deviceOptions);
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @return the command
		 */
		public String getCommand() {
			return command;
		}

		/**
		 * @param command the command to set
		 */
		public void setCommand(String command) {
			this.command = command;
		}

		/**
		 * @return the listArg
		 */
		public String getListArg() {
			return listArg;
		}

		/**
		 * @param listArg the listArg to set
		 */
		public void setListArg(String listArg) {
			this.listArg = listArg;
		}

		/**
		 * @return the changeArg
		 */
		public String getChangeArg() {
			return changeArg;
		}

		/**
		 * @param changeArg the changeArg to set
		 */
		public void setChangeArg(String changeArg) {
			this.changeArg = changeArg;
		}

		/**
		 * @return the deviceOptions
		 */
		public String getDeviceOptions() {
			return deviceOptions;
		}

		/**
		 * @param deviceOptions the deviceOptions to set
		 */
		public void setDeviceOptions(String deviceOptions) {
			this.deviceOptions = deviceOptions;
		}
	}
		
	private static VideoDeviceManager instance;
	private static final Vector<VideoDevice> deviceList = new Vector<>();
	private static final Logger log = LoggerFactory.getLogger(VideoDeviceManager.class);
	private VideoDeviceManagerSubsystem subsystem;
	private VideoDevice currentDevice;

	/**
	 * initializer for singleton instance
	 */
	private VideoDeviceManager() {
		subsystem = VideoDeviceManagerSubsystem.v4l;
		init();
	}
	
	public synchronized static VideoDeviceManager getInstance() {
		if (instance == null) {
			log.info("[getInstance] Initializing video device manager");
			instance = new VideoDeviceManager();
		}
		return instance;
	}
	
	/**
	 * @return the deviceList
	 */
	public Vector<VideoDevice> retreiveDevices() {
		return deviceList;
	}
	
	/**
	 * @return the currentDevice
	 */
	public VideoDevice getCurrentDevice() {
		return currentDevice;
	}

	/**
	 * @param currentDevice the currentDevice to set
	 */
	public void setCurrentDevice(VideoDevice device) throws Exception {
		for (VideoDevice foundDevice : deviceList) {
			if (foundDevice.getName().equals(device.getName()) && foundDevice.getLocation().equals(device.getLocation())) {
				this.currentDevice = foundDevice;
			}
		}
	}
	
	public VideoDevice searchDevice(String searchString) {
		for (VideoDevice foundDevice : deviceList) {
			if (foundDevice.getName().equalsIgnoreCase(searchString) || foundDevice.getLocation().equalsIgnoreCase(searchString)) {
				return foundDevice;
			}
		}
		return null;

	}

	/**
	 * @throws Exception
	 */
	private void sanityCheck() throws Exception {
		if (deviceList == null || deviceList.size() < 1) {
			throw new Exception("No Video Devices Detected");
		}
	}

	private void init() {
		try {
			reinit();
		} catch (Exception e) {
			log.error("[init] {}", e.getMessage(), e);
		}
	}
		
	@SuppressWarnings("finally")
	public boolean reinit() throws Exception {
		try {
			log.debug("[reinit] resetting device list");
			Process cmdLine = Runtime.getRuntime().exec(new String[] {subsystem.command, subsystem.listArg});
			BufferedReader br = new BufferedReader(new InputStreamReader(cmdLine.getInputStream()));
			deviceList.clear();
			readDevices(br, deviceList);
			logError(cmdLine);
			sanityCheck();
			return true;
		} catch (IOException e) {
			log.error("[reinit]: {}", e.getMessage(), e);
		} finally {
			return false;
		}
	}

	/**
	 * @param br
	 * @throws IOException
	 * 
	 * Example:
	 * 
dan@dan-Vostro-410 ~/Desktop $ v4l2-ctl --list-devices
UVC Camera (046d:0825) (usb-0000:00:1d.7-1):
	/dev/video0

HD Webcam C525 (usb-0000:00:1d.7-2):
	/dev/video1

dan@dan-Vostro-410 ~/Desktop $
	 *
	 */
	public void readDevices(BufferedReader br, List<VideoDevice> devices) throws IOException {
		String line;
		String deviceName = "";
		String deviceLocation = "";
		while ((line = br.readLine())!= null) {
			log.trace("[readDevices] checking: {}", line);
			if ( ! line.startsWith(" ") && ! line.startsWith("\t") ) {
				// Not line that has common device name
				line = line.trim();
				if (line.length() < 1) {
					// it's the line that separates entries
					if (StringUtils.isEmpty(deviceName) || StringUtils.isEmpty(deviceLocation)) {
						log.warn("[readDevices] ignoring device '{}' '{}'", deviceName, deviceLocation);
					} else {
						log.info("[readDevices] adding device {}:{}", deviceName, deviceLocation);
						devices.add(new VideoDevice(deviceName, deviceLocation));
					}
					deviceName = "";
					deviceLocation = "";
					continue;
				} else {
					// it's the line with the device name
					log.trace("[readDevices] detected device name: {}", line);
					deviceName = line;
				}
			} else {
				// it's the line with the device location
				log.trace("[readDevices] detected device location: {}", line);
				deviceLocation = line.trim();
			}
		}
	}
	
	public int getDeviceCount() {
		return deviceList.size();
	}

	public void updateDeviceSetting(Object key, Object value) {
		try {
			if (currentDevice == null) {
				throw new IOException("currentDevice is not configured");
			}
			log.info("[updateDeviceSetting] running {} {}", subsystem.command, String.format(subsystem.changeArg, currentDevice.getLocation(), key, value));
			Process cmdLine = Runtime.getRuntime().exec(new String[] {subsystem.command, String.format(subsystem.changeArg, currentDevice.getLocation(), key, value)});
			logError(cmdLine);
			loadDeviceSettings();
		} catch (IOException e) {
			log.error("[updateDeviceSetting] {}", e.getMessage(), e);
		}
	}

	/**
	 * @param cmdLine
	 * @throws IOException
	 */
	private void logError(Process cmdLine) throws IOException {
		if (cmdLine.exitValue() > 0) {
			BufferedReader br = new BufferedReader(new InputStreamReader(cmdLine.getErrorStream()));
			StringWriter sw = new StringWriter();
			while (br.ready()) {
				sw.append(br.readLine());
			}
			log.error("[logError] {}", sw.toString()); 
		}
	}

	public Properties getDeviceSettings() {
		if (currentDevice.getSettings().isEmpty()) {
			loadDeviceSettings();
		}
		return currentDevice.getSettings();
	}

	private void loadDeviceSettings() {
		try {
			if (currentDevice == null) {
				throw new Exception("currentDevice is not configured");
			}
			log.info("[loadDeviceSettings] running: {} {}", subsystem.command, String.format(subsystem.deviceOptions, currentDevice.getLocation()));
			Process cmdLine = Runtime.getRuntime().exec((subsystem.command+" "+String.format(subsystem.deviceOptions, currentDevice.getLocation())).split(" +"));
			cmdLine.waitFor();
			BufferedReader br = new BufferedReader(new InputStreamReader(cmdLine.getInputStream()));
			readDeviceSettings(br);
			logError(cmdLine);
		} catch (Exception e) {
			log.error("[loadDeviceSettings] {}", e.getMessage(), e);
		}
	}

	public void readDeviceSettings(BufferedReader br) throws IOException {
		String line;
		String[] parts;
		while ((line = br.readLine())!= null) {
			line = line.trim();
			parts = line.split(" +");
			if (parts.length > 1) {
				for (String part : parts) {
					if (part.startsWith("value=")) {
						currentDevice.getSettings().put(parts[0], part.substring(6).trim());
					}
				}
			} else {
				log.warn("[readDeviceSettings] ignoring: {}", line);
			}
		}
	}

}
