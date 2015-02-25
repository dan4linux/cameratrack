/**
 * This class manages access to the arduino by buffering position updates and periodically updating the arduino with a new position
 */
package net.swansonstuff.cameratest;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dan Swanson
 *
 */
public class ArduinoManager {
	
	private static ArduinoManager instance;
	private static final Logger log = LoggerFactory.getLogger(ArduinoManager.class);
	
	private Timer timer;
	private long delay = 250;
	int[] positions = new int[30];
	int nextPosition = 0;
	ArduinoHandler arduino;

	/**
	 * 
	 */
	private ArduinoManager() {
		initTimer();
		arduino = new ArduinoHandler();
		arduino.initialize();
	}
	
	public static synchronized ArduinoManager getInstance() {
		if (instance == null) {
			instance = new ArduinoManager();
		}
		return instance;
	}

	/**
	 * 
	 */
	private void initTimer() {
		if (timer != null) {
			timer.cancel();
		}
		timer = new Timer(true);
		timer.schedule(generateTimerTask(), delay, delay);
	}

	private TimerTask generateTimerTask() {
		return new TimerTask() {
			
			@Override
			public void run() {
				log.trace("[timer-run] running");
				int total = 0;
				int count = 0;
				for (int pos : positions) {
					if (pos > 0) {
						total += pos;
						count++;
					}
				}
				if (total > 0) {
					int data = total / count;
					log.debug(String.format("[timer-run] sending total:%d, count:%d, data:%d", total, count, (data > 180) ? 180 : data));
					arduino.sendData(""+((data > 180) ? 180 : data)+"\n");
				}
			}
		};
	}
	
	public synchronized void updatePosition(int position) {
		positions[nextPosition++] = position;
		if (nextPosition >= positions.length) {
			nextPosition = 0;
		}
	}
	
	public static void main (String[] args) throws Exception {
		ArduinoManager m = ArduinoManager.getInstance();
		for (int x=1; x < 180; x++) {
			for (int y = 0; y < m.positions.length; y++) {
				m.updatePosition(x);
			}
			Thread.sleep(1000);
		}
	}

}
