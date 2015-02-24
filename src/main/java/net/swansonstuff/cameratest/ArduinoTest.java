/**
 * 
 */
package net.swansonstuff.cameratest;

/**
 * @author Dan Swanson
 *
 */
public class ArduinoTest {
	
	final String PORT;

	/**
	 * 
	 */
	public ArduinoTest() {
		PORT = "/dev/ttyACM0";
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new ArduinoTest().run();
	}

	private void run() {
	}

}
