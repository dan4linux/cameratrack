package net.swansonstuff.cameratest;

import java.io.Serializable;
import java.util.Properties;

public class VideoDevice implements Serializable {

	private static final long	serialVersionUID	= 1L;
	private String name;
	private String Location;
	private Properties settings;

	public VideoDevice() {
		this.setSettings(new Properties());
	}

	public VideoDevice(String name, String location) {
		this();
		setName(name);
		setLocation(location);
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
	 * @return the location
	 */
	public String getLocation() {
		return Location;
	}

	/**
	 * @param location the location to set
	 */
	public void setLocation(String location) {
		this.Location = location;
	}

	/**
	 * @return the settings
	 */
	public Properties getSettings() {
		return settings;
	}

	/**
	 * @param settings the settings to set
	 */
	public void setSettings(Properties settings) {
		this.settings = settings;
	}
	
	@Override
	public String toString() {
		return String.format("%15s(%s)", (getName().length() > 15) ? getName().substring(0, 12) + "..." : getName(), getLocation());
	}

}
