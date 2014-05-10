package net.swansonstuff.cameratest;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openimaj.image.ImageUtilities;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sarxos.webcam.Webcam;
import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.ToolFactory;

import net.sf.jipcam.axis.MjpegFrame;
import net.sf.jipcam.axis.MjpegFrameParser;
import net.sf.jipcam.axis.MjpegInputStream;
import net.sf.jipcam.axis.MjpegParserEvent;
import net.sf.jipcam.axis.MjpegParserListener;

public class CameraTrack extends JFrame implements MjpegParserListener, ActionListener, ComponentListener {
	
	class Settings {
		
		File settingsFile;
		Properties props = new Properties();
		
		public Settings() {
			try {
				this.settingsFile = new File(System.getProperty("user.home")+"/camtest.prp");
				props.load(new FileReader(settingsFile));
			} catch (Exception e) {
				log.error("Error loading settings: {}", e.getMessage());
			}
		}
		
		public void set(String key, String value) {
			props.put(key, value);
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
	}

	class FrameCounter {
		int lastFrameCount = 0;
		public FrameCounter() {			
		}
		
		public void update() {
			CameraTrack.this.fps =  CameraTrack.this.frameCounter - lastFrameCount;
			lastFrameCount = CameraTrack.this.frameCounter;
		}
	}

	/**
	 *
	 * http://mrbool.com/showing-images-in-a-swing-jframe-in-java/24594#ixzz2r5V6HUQA
	 * http://www.axis.com/techsup/cam_servers/dev/cam_http_api_2.php#api_blocks_image_video_mjpg_video
	 * http://<camera>/axis-cgi/mjpg/video.cgi[?<parameter>=<value>[&<parameter>=<value>...]]
	 */
	private static final Logger log = LoggerFactory.getLogger(CameraTrack.class);
	private static final long	serialVersionUID	= 1L;
	ImageImplement panel = new ImageImplement();
	int bandWidth = 10;
	int bandPos = 50;
	private int sensitivitySetting = 15;
	int allowedColorVariance = 100;			
	private boolean autoCalibration = true;
	public static final String	MANUAL_CALIBRATION	= "Manual Calibration";
	public static final String	AUTO_CALIBRATION	= "Auto Calibration";

	private WritableRaster lastRaster;
	private WritableRaster currentRaster;

	private static final HaarCascadeDetector detector = new HaarCascadeDetector();
	private List<DetectedFace> faces = null;
	private boolean	faceDetectionEnabled = false;
	private boolean	adjusting;
	private static final Stroke STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[] { 1.0f }, 0.0f);

	public static final String ANSI_SAVE_POS = "\u001B[s";
	public static final String ANSI_RESTORE_POS = "\u001B[u";
	private int frameCounter = 0;
	private int fps = 0;
	final FrameCounter fpsCounter = new FrameCounter();
	int desiredFrameRate = 14;
	int manualFrameDelayMillis = 0;
	private final Settings settings;
	int maxSensitivity = 0;


	public CameraTrack(){
		this.settings= new Settings(); 
		init();
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		new CameraTrack().run(args);
	}

	public void run(String[] args) throws Exception{
				
		Timer fpsMonitor = new Timer(true);
		fpsMonitor.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				fpsCounter.update();
			}
			
		}, 0, 1000);
		

		// for ip camera
		/* 
		HttpURLConnection cameraUrl = (HttpURLConnection) new URL("http://192.168.102.14/axis-cgi/mjpg/video.cgi").openConnection();
		cameraUrl.setUseCaches(false);
		MjpegFrameParser parser = new MjpegFrameParser(cameraUrl.getInputStream());
		 */

		// for video streams via ffmpeg
		if ((args == null) || (args.length < 1) && (args[0].equals(""))) {
			System.err.println("Unable to start: Stream source required.");
			System.exit(1);
		}
		String streamName = args[0];
		String fileName = "pipe:1";

		boolean resolutionFilter = false;
		String resolution = "640x480";
		String sourceVideo4linux = "-f v4l2 -i /dev/video0";
		//String sourceVideoFile = "-i http://clipdownload.livestream.com/mogulus-user-files/chfirstassemblyalexandria/2013/12/22/dea9c13d-5a18-42b5-8725-e56c5f311c98.mp4/ac662c5bc242a22887b8bd427c3cfe3b/536C643A";
		String sourceVideoFile = "-i /home/dan/Downloads/aj-office-prank.mov";
		//String sourceVideoFile = "-i /home/dan/Desktop/churchtest.mp4";
		String usbCamCmd = "ffmpeg "+sourceVideoFile+" -f mjpeg -r "+desiredFrameRate+((resolutionFilter ) ? " -crop="+resolution : "")+"  -qscale 2 "+fileName;

		String streamCmd = "ffmpeg -probesize 32768 -i "+streamName+" -f mjpeg  -qscale "+fileName;

		System.out.println("Running: "+usbCamCmd);
		Process transcoder = Runtime.getRuntime().exec(usbCamCmd);

		//ImageStreamReader isr = new ImageStreamReader(transcoder.getInputStream(), this);
		//isr.start();

		System.out.print("\n"+ANSI_SAVE_POS);
		while (true) {
			try {
				MjpegFrame frame;
				//BufferedReader brStdOut = new BufferedReader(new InputStreamReader());
				MjpegInputStream brStdOut = new MjpegInputStream(transcoder.getInputStream());
				long startTime = System.currentTimeMillis();

				System.out.print(ANSI_SAVE_POS);

				while ((frame = brStdOut.readMjpegFrame()) != null) {
					//log.info("got frame in {}ms", System.currentTimeMillis() - startTime);
					processFrame(frame);
					startTime = System.currentTimeMillis();
				}

				int exitValue = transcoder.exitValue();
				System.out.println("Got exit code "+exitValue);
				String output;
				BufferedReader brStdErr = new BufferedReader(new InputStreamReader(transcoder.getErrorStream()));
				while ((output = brStdErr.readLine()) != null) {
					System.out.print(output);
				}
				break;
			} catch(EOFException ee) {
				System.out.println("waiting for process to end..");
				break;
			} catch(IllegalThreadStateException e) {
				// Means we're still running
				System.out.println("waiting for process to end..");
				Thread.sleep(1000);
			}
		}

		//isr.shutdown();



		System.out.println("Cleaning up...");
		//System.exit(0);
	}

	private void processFrame(MjpegFrame frame) {


		if (frame.getBytes().length > 0) {
			BufferedImage bi = Utilities.toBufferedImage(frame.getImage());
			if (bi != null) {
				//System.out.print("o");
				processImage(bi);
			} else {
				System.out.print("x");
			}
		} else {
			System.out.print("x");
		}
	}

	private void processImage(BufferedImage bi) {
		frameCounter++;
		try {
			
			if (fps > desiredFrameRate + 1) {
				manualFrameDelayMillis += 1;
			} else if (fps < desiredFrameRate - 1) {
				if (manualFrameDelayMillis >= 1) {
					manualFrameDelayMillis -= 1;
				}
			}
			
			if (manualFrameDelayMillis > 0) {
				Thread.sleep(manualFrameDelayMillis);
			}


			//setSize(bi.getWidth() + 25, bi.getHeight() + 400);

			long startTime = System.currentTimeMillis();
			int verticalCenter = Math.round(1.0f * bi.getHeight() * (1.0f * bandPos / 100.0f));
			int halfBand = Math.round(1.0f * bandWidth / 100 * bi.getHeight() / 2);
			int top = verticalCenter - halfBand;
			if (top < 0) {
				top = 0;
			}
			int bottom = verticalCenter + halfBand;
			if (bottom >= bi.getHeight()) {
				bottom = bi.getHeight() - 1;
			}

			Graphics2D biGraphics = bi.createGraphics();
			biGraphics.setPaint(Color.RED);
			biGraphics.draw(new Rectangle(0, top, bi.getWidth() - 1, halfBand * 2));
			
			// these should start reversed to intersect properly
			int croppedImageXMin =  bi.getWidth();
			int croppedImageXMax = 0;

			if (!this.adjusting) {

				currentRaster = (WritableRaster) bi.getData(new Rectangle(0, top, bi.getWidth(), bottom - top));
				drawFace(bi);

				int PIXEL_GROUP_SIZE = sensitivitySetting;

				if (lastRaster != null) {		


					biGraphics.setColor(Color.YELLOW);
					int x = 0;
					int yTop = verticalCenter - halfBand+1;
					int yHeight = (halfBand * 2) - 1;
					int xEnd = PIXEL_GROUP_SIZE - 1;
					Rectangle2D changedArea = new Rectangle2D.Double();
					int percentTotal = 0;
					int boxCounter = 0;

					while (x < bi.getWidth()) {

						int offPixelsPercent = getMismatchedPixelCount(allowedColorVariance, x, PIXEL_GROUP_SIZE, currentRaster, lastRaster);
						percentTotal += offPixelsPercent;
						boxCounter++;

						if (offPixelsPercent > Math.abs(sensitivitySetting - (maxSensitivity / 2))) {
							if (x< 1) { x = 1; }
							changedArea.setRect(x , yTop, xEnd, yHeight);
							biGraphics.draw(changedArea);
							if (croppedImageXMin > x) {
								croppedImageXMin = x;
							}
							
							croppedImageXMax = x + PIXEL_GROUP_SIZE ;
							
						}

						x += PIXEL_GROUP_SIZE;
					}

					System.out.printf(ANSI_RESTORE_POS+"Processing frame in %5sms.  Avg. Difference is %3d%%,  FPS=%d delay=%s", (System.currentTimeMillis() - startTime), (percentTotal / boxCounter), fps, manualFrameDelayMillis);

				}

				if (croppedImageXMax < croppedImageXMin) {
					int tmp = croppedImageXMin;
					croppedImageXMin = croppedImageXMax;
					croppedImageXMax = tmp;
				}
				int newXWidth = croppedImageXMax - croppedImageXMin;
				int aspect =  newXWidth * 100 / bi.getWidth();
				int newHeight = bi.getHeight() * aspect / 100;
				int newTop = verticalCenter - (newHeight / 2);
				if (newTop < 0) {
					newTop = 0;
				}
				if ((newTop + newHeight) > bi.getHeight()) {
					newTop -=  (newTop + newHeight) - bi.getHeight();  
				}
				try {
					Graphics2D g2d = bi.createGraphics();
					g2d.drawImage(bi.getSubimage(croppedImageXMin, newTop, newXWidth, newHeight), 0, 0, null);
					g2d.dispose();
				} catch (RasterFormatException e) {
					System.err.printf("\n%s: %d(%d), %d(%d)\n",e.getMessage(), croppedImageXMin, newXWidth, newTop, newHeight);
				}
			}
			
			biGraphics.dispose();
			
			if (autoCalibration) {
				calibrate();
			}

			panel.setImage(bi);

		} catch(Throwable t) {
			System.err.println("This is bad: "+ t.getMessage());
			t.printStackTrace();
		}
	}

	private void drawFace(BufferedImage bi) {
		if (!this.faceDetectionEnabled ) {
			return;
		}
		Graphics2D g2 = bi.createGraphics();
		long startTime = System.currentTimeMillis();
		faces = detector.detectFaces(ImageUtilities.createFImage(bi));
		System.out.println(""+(System.currentTimeMillis() - startTime) +"ms to detect faces");
		Iterator<DetectedFace> dfi = faces.iterator();
		while (dfi.hasNext()) {

			DetectedFace face = dfi.next();
			org.openimaj.math.geometry.shape.Rectangle bounds = face.getBounds();

			int dx = (int) (0.1 * bounds.width);
			int dy = (int) (0.2 * bounds.height);
			int x = (int) bounds.x - dx;
			int y = (int) bounds.y - dy;
			int w = (int) bounds.width + 2 * dx;
			int h = (int) bounds.height + dy;

			g2.setStroke(STROKE);
			g2.setColor(Color.GREEN);
			g2.drawRect(x, y, w, h);
		}
		g2.dispose();

	}

	private int getMismatchedPixelCount(int allowedColorVariance, int startX, int regionWidth, WritableRaster rasterA, WritableRaster rasterB) {
		//log.debug("[getMismatchedPixelCount] offset@{}, rasterA is {} x {} y@{}, rasterB is {} x {} y@{}", startX, rasterA.getWidth() - rasterA.getMinX(), rasterA.getHeight() - rasterA.getMinY(), rasterA.getMinY(), rasterB.getWidth() - rasterB.getMinX(), rasterB.getHeight() - rasterB.getMinY(), rasterA.getMinY());
		byte[] currentRGB;
		byte[] previousRGB;
		int offPixels = 0;
		int pixelCount = 0;
		int rightEdge = startX + regionWidth - 1;
		if (rightEdge > rasterA.getWidth()) {
			rightEdge = rasterA.getWidth();
		}
		int leftEdge;
		int bottom = rasterA.getMinY() + rasterA.getHeight();
		for (leftEdge = startX; leftEdge < rightEdge; leftEdge++) {
			for (int y = rasterA.getMinY(); y < bottom; y++){
				pixelCount++;
				try {
					currentRGB = (byte[]) rasterA.getDataElements(leftEdge, y, null);
					previousRGB = (byte[]) rasterB.getDataElements(leftEdge, y, null);
					if (
							(Math.abs(currentRGB[0] - previousRGB[0]) > allowedColorVariance) ||
							(Math.abs(currentRGB[1] - previousRGB[1]) > allowedColorVariance) ||
							(Math.abs(currentRGB[2] - previousRGB[2]) > allowedColorVariance)
							) {
						offPixels++;
					}
				} catch(Exception e) {
					//log.error("x,y of {},{} is bad",leftEdge,  y);
				}
			}
		}
		return (offPixels > 0) ? offPixels * 100 / pixelCount : 0;
	}

	public void onMjpegParserEvent(MjpegParserEvent event) {		
		try {
			processFrame(event.getMjpegFrame());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(MANUAL_CALIBRATION)) {
			settings.set("calibration", "manual");
			this.autoCalibration = false;
		} else if (e.getActionCommand().equals(AUTO_CALIBRATION)) {
			settings.set("calibration", "auto");
			this.autoCalibration = true;
		}
	}

	private void calibrate() {
		lastRaster = currentRaster;
	}

	public void setSensitivitySetting(int sensitivitySetting) {
		this.sensitivitySetting = sensitivitySetting;
	}

	private void setBandWidth(int i) {
		this.bandWidth = i;
	}
	
	private void setBandPosition(int i) {
		this.bandPos = i;
	}

	private void setAllowedColorVariance(int i) {
		this.allowedColorVariance = i;
	}


	/**
	 * 
	 */
	private void init() {
		setVisible(true);
		setSize(435,658);

		String size = settings.get("size", "");
		if (!size.equals("")) {
			String[] parts = size.split("x");
			if (parts.length == 2) {
				try {
					setSize(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
				} catch(Exception e) {
					// do nothing -- we tried
				}
			}
		}
		
		String location = settings.get("location");
		if (!location.equals("")) {
			String[] parts = location.split("x");
			if (parts.length == 2) {
				try {
					setLocation(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
				} catch(Exception e) {
					// do nothing -- we tried
				}
			}
		}
		
		addComponentListener(this);
		setTitle("Camera Track");
		getContentPane().add(panel);

		JButton calibrateButton = new JButton("Calibrate");
		calibrateButton.setLocation(100,100);
		calibrateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				calibrate();
			}
		});

		getContentPane().add(calibrateButton);

		JRadioButton manualButton = new JRadioButton(MANUAL_CALIBRATION);
		manualButton.setActionCommand(MANUAL_CALIBRATION);
		manualButton.setBounds(18, 8, 134, 23);
		manualButton.addActionListener(this);
		if (settings.get("calibration").equals("manual")) {
			manualButton.setSelected(true);
		}
		getContentPane().add(manualButton);

		JRadioButton autoButton = new JRadioButton(AUTO_CALIBRATION);
		autoButton.setActionCommand(AUTO_CALIBRATION);
		autoButton.setBounds(18, 8, 134, 23);
		autoButton.addActionListener(this);
		if (settings.get("calibration","auto").equals("auto")) {
			autoButton.setSelected(true);
		}
		getContentPane().add(autoButton);

		ButtonGroup group = new ButtonGroup();
		group.add(manualButton);
		group.add(autoButton);

		JLabel sensitivityLabel = new JLabel("Sensitivity");
		getContentPane().add(sensitivityLabel);
		setSensitivitySetting(Integer.parseInt(settings.get("sensitivity","50")));
		maxSensitivity = 100;
		JSlider sensitivityAdjustment = new JSlider(JSlider.HORIZONTAL, 1, maxSensitivity, Math.abs(sensitivitySetting - maxSensitivity));
		sensitivityAdjustment.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				JSlider sensitivity = (JSlider) e.getSource();
				int newSensitivity = sensitivity.getMaximum() - sensitivity.getValue();
				settings.set("sensitivity", ""+newSensitivity);
				setSensitivitySetting((newSensitivity > 0) ? newSensitivity : 1);
			}
		});
		sensitivityAdjustment.setSize(300, 25);
		getContentPane().add(sensitivityAdjustment);

		JLabel bandwidthLabel = new JLabel("Band Height");
		getContentPane().add(bandwidthLabel);
		setBandWidth(Integer.parseInt(settings.get("bandwidth","50")));
		JSlider bandWidthAdjustment = new JSlider(JSlider.HORIZONTAL, 1, 100, bandWidth);
		bandWidthAdjustment.setMajorTickSpacing(10);
		bandWidthAdjustment.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				JSlider bandWidth = (JSlider) e.getSource();
				int newSensitivity = bandWidth.getValue();
				settings.set("bandwidth", ""+newSensitivity);
				setBandWidth((newSensitivity > 0) ? newSensitivity : 1);
			}

		});
		bandWidthAdjustment.setSize(300, 25);
		getContentPane().add(bandWidthAdjustment);
		
		JLabel bandPosLabel = new JLabel("Band Position");
		getContentPane().add(bandPosLabel);
		setBandPosition(Integer.parseInt(settings.get("position", "50")));
		JSlider bandPosAdjustment = new JSlider(JSlider.HORIZONTAL, 1, 100, bandPos);
		bandPosAdjustment.setMajorTickSpacing(10);
		bandPosAdjustment.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
				CameraTrack.this.adjusting = false;
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				CameraTrack.this.adjusting = true;
			}
						
		});
		bandPosAdjustment.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				JSlider bandWidth = (JSlider) e.getSource();
				int newPosition = bandWidth.getValue();
				settings.set("position", ""+newPosition);
				setBandPosition((newPosition > 0) ? newPosition : 1);
			}

		});
		bandPosAdjustment.setSize(300, 25);
		getContentPane().add(bandPosAdjustment);


		JLabel colorVarianceLabel = new JLabel("Color Variance");
		getContentPane().add(colorVarianceLabel);
		setAllowedColorVariance(Integer.parseInt(settings.get("colorshift", "50")));
		JSlider colorVarianceAdjustment = new JSlider(JSlider.HORIZONTAL, 1, 200, allowedColorVariance);
		colorVarianceAdjustment.setMajorTickSpacing(10);
		colorVarianceAdjustment.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				JSlider colorVarianceAdjustment = (JSlider) e.getSource();
				int newSensitivity = colorVarianceAdjustment.getValue();
				settings.set("colorshift", ""+newSensitivity);
				setAllowedColorVariance((newSensitivity > 0) ? newSensitivity : 1);
			}

		});
		bandPosAdjustment.setSize(300, 25);
		getContentPane().add(bandPosAdjustment);


		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		layout.setHorizontalGroup(
				layout.createParallelGroup(Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
						.addGroup(layout.createParallelGroup(Alignment.LEADING)
								.addGroup(layout.createSequentialGroup()
										.addComponent(panel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										)
										.addGroup(layout.createSequentialGroup()
												.addComponent(calibrateButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												.addGap(10)
												.addComponent(manualButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												.addGap(10)
												.addComponent(autoButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												)
												.addGroup(layout.createSequentialGroup()
														.addComponent(sensitivityLabel)
														)
														.addGroup(layout.createSequentialGroup()
																.addComponent(sensitivityAdjustment, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																)
																.addGroup(layout.createSequentialGroup()
																		.addComponent(bandwidthLabel)
																		)
																		.addGroup(layout.createSequentialGroup()
																				.addComponent(bandWidthAdjustment, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																				)
																.addGroup(layout.createSequentialGroup()
																		.addComponent(bandPosLabel)
																		)
																		.addGroup(layout.createSequentialGroup()
																				.addComponent(bandPosAdjustment, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																				)
																				.addGroup(layout.createSequentialGroup()
																						.addComponent(colorVarianceLabel)
																						)
																						.addGroup(layout.createSequentialGroup()
																								.addComponent(colorVarianceAdjustment, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																								)

								)
						)
				);


		layout.setVerticalGroup(
				layout.createParallelGroup(Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
						.addComponent(panel)
						.addGroup(layout.createParallelGroup(Alignment.BASELINE)
								.addGap(10)
								.addComponent(calibrateButton)
								.addComponent(manualButton)
								.addComponent(autoButton)
								)
								.addGap(10)
								.addComponent(sensitivityLabel)
								.addGap(10)
								.addComponent(sensitivityAdjustment)
								.addGap(10)
								.addComponent(bandwidthLabel)
								.addGap(10)
								.addComponent(bandWidthAdjustment)
								.addGap(10)
								.addComponent(bandPosLabel)
								.addGap(10)
								.addComponent(bandPosAdjustment)
								.addGap(10)
								.addComponent(colorVarianceLabel)
								.addGap(10)
								.addComponent(colorVarianceAdjustment)
						));

		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	@Override
	public void componentResized(ComponentEvent e) {
		Dimension size = this.getSize();
		settings.set("size", ""+(int)size.getWidth()+"x"+(int)size.getHeight());
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		Point point = this.getLocationOnScreen();
		settings.set("location", ""+(int)point.getX()+"x"+(int)point.getY());
	}

	@Override
	public void componentShown(ComponentEvent e) {
	}

	@Override
	public void componentHidden(ComponentEvent e) {
	}

}