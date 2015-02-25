package net.swansonstuff.cameratest;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ericjbruno
 */
public class ArduinoHandler implements SerialPortEventListener {
    SerialPort serialPort = null;
    private static final Logger log = LoggerFactory.getLogger(ArduinoHandler.class); 
    
    private static ArrayList<String> PORT_NAMES = new ArrayList<String>(){
    	private static final long	serialVersionUID	= 1L;

    	{ 
    		add("/dev/ttyACM0"); //Linux
    		add("/dev/tty.usbmodem"); // Mac OS X
    		add("/dev/usbdev"); // Linux
    		//"/dev/tty", // Linux
    		add("/dev/serial"); // Linux
    		add("COM3"); // Windows
    	}};
    
    private String appName;
    private OutputStream output;
    
    private static final int TIME_OUT = 1000; // Port open timeout
    private static final int DATA_RATE = 9600; // Arduino serial port

    public boolean initialize() {
        try {
        	Enumeration<?> portEnum = null;
        	
        	for (String portName : PORT_NAMES) {
    			System.setProperty("gnu.io.rxtx.SerialPorts",portName);
        		portEnum = CommPortIdentifier.getPortIdentifiers();
        		if (portEnum != null && portEnum.hasMoreElements()) {
        			log.info("Using port: "+portName);
        			break;
        		}
        	}
        	
            CommPortIdentifier portId = null;

            // Enumerate system ports and try connecting to Arduino over each
            //
            while (portId == null && portEnum.hasMoreElements()) {
                // Iterate through your host computer's serial port IDs
                //
                CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
                log.info( "[initialize] Trying port" + currPortId.getName() );
                for (String portName : PORT_NAMES) {
                    if ( currPortId.getName().equals(portName) 
                      || currPortId.getName().startsWith(portName)) {

                        // Try to connect to the Arduino on this port
                        //
                        // Open serial port
                        serialPort = (SerialPort)currPortId.open(appName, TIME_OUT);
                        portId = currPortId;
                        log.info( "[initialize] Connected on port" + currPortId.getName() );
                        break;
                    }
                }
            }
        
            if (portId == null || serialPort == null) {
            	log.error( "[initialize] Oops... Could not connect to Arduino");
                return false;
            }
        
            // set port parameters
            serialPort.setSerialPortParams(DATA_RATE,
                            SerialPort.DATABITS_8,
                            SerialPort.STOPBITS_1,
                            SerialPort.PARITY_NONE);

            // add event listeners
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);

            // Give the Arduino some time
            try { Thread.sleep(2000); } catch (InterruptedException ie) {}
            
            return true;
        }
        catch ( Exception e ) { 
            e.printStackTrace();
        }
        return false;
    }
    
    public void sendData(String data) {
        try {
            log.debug("[sendData] Sending data: '" + data +"'");            
            output = serialPort.getOutputStream();
            output.write( data.getBytes() );
        } 
        catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    //
    // This should be called when you stop using the port
    //
    public synchronized void close() {
        if ( serialPort != null ) {
            serialPort.removeEventListener();
            serialPort.close();
        }
    }

    //
    // Handle serial port event
    //
    public synchronized void serialEvent(SerialPortEvent oEvent) {
        log.trace("[serialEvent] Event received: " + oEvent.toString());
        try {
            switch (oEvent.getEventType() ) {
                case SerialPortEvent.DATA_AVAILABLE: 
                	int availableBytes = serialPort.getInputStream().available();
                	byte[] readBuffer = new byte[availableBytes];
                    if (availableBytes > 0) {
                        // Read the serial port
                    	serialPort.getInputStream().read(readBuffer, 0, availableBytes);
                    }
                    String inputLine = new String(readBuffer);
                    System.out.print(inputLine);
                    break;

                default:
                    break;
            }
        } 
        catch (Exception e) {
            log.error(e.toString(), e);
        }
    }
    
    public static void addPort(String portName) {
    	PORT_NAMES.add(portName);
    }

    public ArduinoHandler() {
        appName = getClass().getName();
    }
    
    public static void main(String[] args) {
    	log.info("[main] Using: "+System.getProperty("java.library.path"));
        ArduinoHandler test = new ArduinoHandler();
        if ( test.initialize() ) {
            test.sendData("180\n45\n");
            try { Thread.sleep(2000); } catch (InterruptedException ie) {}
            test.sendData("0\n");
            try { Thread.sleep(2000); } catch (InterruptedException ie) {}
            test.close();
        }

        // Wait 5 seconds then shutdown
        try { Thread.sleep(2000); } catch (InterruptedException ie) {}
    }
}



