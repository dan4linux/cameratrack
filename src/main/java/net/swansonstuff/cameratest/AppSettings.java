package net.swansonstuff.cameratest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppSettings {
	
		public static final String	SETTING_BAND_HEIGHT	= "band.height";
		public static final String	SETTING_BAND_WIDTH	= "band.width";


		private Map<String, Runnable> callbacks = new HashMap<>();
		File settingsFile;
		Properties props = new Properties();
		private static final Logger log = LoggerFactory.getLogger(AppSettings.class);
		
		public AppSettings() {
			try {
				this.settingsFile = new File(System.getProperty("user.home")+"/camtest.prp");
				props.load(new FileReader(settingsFile));
			} catch (Exception e) {
				log.error("Error loading settings: {}", e.getMessage());
			}
		}
		
		public void set(String key, String value) {
			props.put(key, value);
			if (callbacks.containsKey(key)) {
				callbacks.get(key).run();
			}
			save();
		}
		
		public String get(String key) {
			return get(key, "");
		}

		public String get(String key, String defaultValue) {
			return props.getProperty(key, defaultValue);
		}

		public void save(){
			try {
				props.store(new FileOutputStream(settingsFile), null);
			} catch (Exception e) {
				log.error("Error saving settings: {}", e.getMessage());
			}
		}
	
		public void registerCallback(String setting, Runnable callback) {
			callbacks.put(setting, callback);
		}

}
