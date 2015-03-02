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
import java.awt.event.KeyEvent;
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
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataListener;

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
import net.swansonstuff.cameratest.helpers.CameraSettingsTabelModel;
import net.swansonstuff.cameratest.helpers.FfmpegHelper;
import net.swansonstuff.cameratest.helpers.TranscoderHelper;
import net.swansonstuff.cameratest.helpers.VideoDeviceManager;

import javax.swing.JPanel;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.JScrollPane;

import java.awt.BorderLayout;

public class CameraTrack extends JFrame implements MjpegParserListener, ActionListener, ComponentListener {
	
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
	ImageImplement imagePanel = new ImageImplement();
	private int bandWidth = 300;
	private int bandHeight = 10;
	int bandPos = 50;
	private int sensitivitySetting = 15;
	int allowedColorVariance = 100;			
	private boolean autoCalibration;
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
	private int desiredFrameRate = 14;
	final FrameCounter fpsCounter = new FrameCounter();

	int manualFrameDelayMillis = 0;
	private final AppSettings settings;
	int maxSensitivity = 0;
	private ArduinoManager arduino;
	private JTable table;
	private VideoDeviceManager videoDeviceManager;


	public CameraTrack(){
		this.settings= new AppSettings(); 
		this.arduino = ArduinoManager.getInstance();
		this.videoDeviceManager = VideoDeviceManager.getInstance();
		this.autoCalibration = Boolean.parseBoolean(settings.get(AppSettings.SETTING_CALIBRATION, "false"));
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
		if (args == null || args.length < 1 || ((args.length > 0) && (args[0].trim().equals("")) ) ) {
			VideoDevice lastDeviceUsed = videoDeviceManager.searchDevice(settings.get(AppSettings.LAST_VIDEO_DEVICE));
			if (lastDeviceUsed == null) {
				if (videoDeviceManager.getDeviceCount() > 0) {
					args = new String[] {videoDeviceManager.retreiveDevices().firstElement().getLocation()};
				}
			} else {
				args = new String[] {lastDeviceUsed.getLocation()};
			}
			System.out.println("Using default Stream: "+args[0]);
		}
		
		String streamName = args[0];
		

		TranscoderHelper transcoderHelper = new FfmpegHelper();
		transcoderHelper.setFrameRate(desiredFrameRate);
		Process transcoder = transcoderHelper.getProcess(streamName);
		//ImageStreamReader isr = new ImageStreamReader(transcoder.getInputStream(), this);
		//isr.start();

		System.out.print("\n"+ANSI_SAVE_POS);
		while (true) {
			try {
				String errOutput;
				MjpegFrame frame;
				BufferedReader brStdErr = new BufferedReader(new InputStreamReader(transcoder.getErrorStream()));
				MjpegInputStream brStdOut = new MjpegInputStream(transcoder.getInputStream());
				long startTime = System.currentTimeMillis();

				System.out.print(ANSI_SAVE_POS);

				while ((frame = brStdOut.readMjpegFrame()) != null) {
					//log.info("got frame in {}ms", System.currentTimeMillis() - startTime);
					while (brStdErr.ready()) {
						if ((errOutput = brStdErr.readLine()) != null) {
							if (log.isDebugEnabled()) {
								System.out.println(errOutput);
							}
						}
					}
					processFrame(frame);
					startTime = System.currentTimeMillis();
				}

				int exitValue = transcoder.exitValue();
				System.out.println("Got exit code "+exitValue);
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
			int halfBand = Math.round(1.0f * bandHeight / 100 * bi.getHeight() / 2);
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
			int sideMargin = bi.getWidth() - (int)(1.0 * bi.getWidth() * (1.0 * bandWidth / 100));
			int croppedImageXMin =  bi.getWidth() - sideMargin;
			int croppedImageXMax = sideMargin;

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
					
					int leftEdgeOfChanges = -1;
					int rightEdgeOfChanges = -1;					

					while (x < bi.getWidth()) {

						int offPixelsPercent = getMismatchedPixelCount(allowedColorVariance, x, PIXEL_GROUP_SIZE, currentRaster, lastRaster);
						percentTotal += offPixelsPercent;
						boxCounter++;

						if (offPixelsPercent > Math.abs(sensitivitySetting - (maxSensitivity / 2))) {
							
							if (leftEdgeOfChanges < 0) { leftEdgeOfChanges = boxCounter; }
							rightEdgeOfChanges = boxCounter;
							
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
					
					if (leftEdgeOfChanges >= 0 && rightEdgeOfChanges >= leftEdgeOfChanges) {
						int centerOfChanges = (rightEdgeOfChanges + leftEdgeOfChanges) / 2;					
						int fieldPosition = 100 * centerOfChanges / boxCounter;
						int servoPosition = (180) * fieldPosition / 100;
						log.debug("[processImage] new position: "+servoPosition);
						updatePosition(servoPosition);
					} else {
						updatePosition(0);
					}
					

					log.trace(String.format(ANSI_RESTORE_POS+"Processing frame in %5sms.  Avg. Difference is %3d%%,  FPS=%d delay=%s", (System.currentTimeMillis() - startTime), (percentTotal / boxCounter), fps, manualFrameDelayMillis));

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
					//g2d.drawImage(bi.getSubimage(croppedImageXMin, newTop, newXWidth, newHeight), 0, 0, null);
					g2d.dispose();
				} catch (RasterFormatException e) {
					System.err.printf("\n%s: %d(%d), %d(%d)\n",e.getMessage(), croppedImageXMin, newXWidth, newTop, newHeight);
				}
			}
			
			biGraphics.dispose();
			
			if (autoCalibration) {
				calibrate();
			}

			imagePanel.setImage(bi);

		} catch(Throwable t) {
			System.err.println("This is bad: "+ t.getMessage());
			t.printStackTrace();
		}
	}

	/**
	 * @param servoPosition
	 */
	public void updatePosition(int servoPosition) {
		arduino.updatePosition(servoPosition);
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
			settings.set(AppSettings.SETTING_CALIBRATION, "manual");
			this.autoCalibration = false;
		} else if (e.getActionCommand().equals(AUTO_CALIBRATION)) {
			settings.set(AppSettings.SETTING_CALIBRATION, "auto");
			this.autoCalibration = true;
		}
	}

	private void calibrate() {
		lastRaster = currentRaster;
	}

	public void setSensitivitySetting(int sensitivitySetting) {
		this.sensitivitySetting = sensitivitySetting;
	}

	private void setBandHeight(int i) {
		this.bandHeight = i;
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
		
		JPanel contentPaneScroller = new JPanel(); 
		//contentPaneScroller.setAutoscrolls(true);
		getContentPane().add(new JScrollPane(contentPaneScroller));
		
		setVisible(true);
		contentPaneScroller.setSize(770,658);

		setTitle("Camera Track");

		initWindowSize();
		
		/*--------------------------------------------------------------
		 * Image Panel
		 --------------------------------------------------------------*/

		contentPaneScroller.add(imagePanel);
		imagePanel.setLayout(null);

		
		/*--------------------------------------------------------------
		 * controls
		 --------------------------------------------------------------*/
		
		JButton calibrateButton = new JButton("Calibrate");
		calibrateButton.setLocation(100,100);
		calibrateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				calibrate();
			}
		});

		contentPaneScroller.add(calibrateButton);

		JRadioButton manualButton = new JRadioButton(MANUAL_CALIBRATION);
		manualButton.setActionCommand(MANUAL_CALIBRATION);
		manualButton.setBounds(18, 8, 134, 23);
		manualButton.addActionListener(this);
		manualButton.setSelected(!autoCalibration);
		contentPaneScroller.add(manualButton);

		JRadioButton autoButton = new JRadioButton(AUTO_CALIBRATION);
		autoButton.setActionCommand(AUTO_CALIBRATION);
		autoButton.setBounds(18, 8, 134, 23);
		autoButton.addActionListener(this);
		autoButton.setSelected(autoCalibration);
		contentPaneScroller.add(autoButton);

		ButtonGroup group = new ButtonGroup();
		group.add(manualButton);
		group.add(autoButton);

		JLabel sensitivityLabel = new JLabel("Sensitivity");
		contentPaneScroller.add(sensitivityLabel);
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
		contentPaneScroller.add(sensitivityAdjustment);

		JLabel bandWidthLabel = new JLabel("Band Width");
		contentPaneScroller.add(bandWidthLabel);
		setBandWidth(Integer.parseInt(settings.get(AppSettings.SETTING_BAND_WIDTH,"100")));
		JSlider bandWidthAdjustment = new JSlider(JSlider.HORIZONTAL, 1, 100, bandWidth);
		bandWidthAdjustment.setMajorTickSpacing(10);
		bandWidthAdjustment.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				JSlider bandWidth = (JSlider) e.getSource();
				int newSensitivity = bandWidth.getValue();
				settings.set(AppSettings.SETTING_BAND_WIDTH, ""+newSensitivity);
				setBandHeight((newSensitivity > 0) ? newSensitivity : 1);
			}

		});
		bandWidthAdjustment.setSize(300, 25);
		contentPaneScroller.add(bandWidthAdjustment);
		
		
		JLabel bandHeightLabel = new JLabel("Band Height");
		contentPaneScroller.add(bandHeightLabel);
		setBandHeight(Integer.parseInt(settings.get(AppSettings.SETTING_BAND_HEIGHT,"50")));
		JSlider bandHeightAdjustment = new JSlider(JSlider.HORIZONTAL, 1, 100, bandHeight);
		bandHeightAdjustment.setMajorTickSpacing(10);
		bandHeightAdjustment.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				JSlider bandHeight = (JSlider) e.getSource();
				int newSensitivity = bandHeight.getValue();
				settings.set(AppSettings.SETTING_BAND_HEIGHT, ""+newSensitivity);
				setBandHeight((newSensitivity > 0) ? newSensitivity : 1);
			}

		});
		bandHeightAdjustment.setSize(300, 25);
		contentPaneScroller.add(bandHeightAdjustment);
		
		JLabel bandPosLabel = new JLabel("Band Position");
		contentPaneScroller.add(bandPosLabel);
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
		contentPaneScroller.add(bandPosAdjustment);


		JLabel colorVarianceLabel = new JLabel("Color Variance");
		contentPaneScroller.add(colorVarianceLabel);
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
		contentPaneScroller.add(bandPosAdjustment);
		
		JPanel configPanel = new JPanel();


		GroupLayout layout = new GroupLayout(contentPaneScroller);
		layout.setHorizontalGroup(
			layout.createParallelGroup(Alignment.LEADING)
				.addComponent(sensitivityLabel)
				.addComponent(sensitivityAdjustment, GroupLayout.DEFAULT_SIZE, 740, Short.MAX_VALUE)
				.addComponent(bandWidthLabel)
				.addComponent(bandWidthAdjustment, GroupLayout.DEFAULT_SIZE, 740, Short.MAX_VALUE)
				.addComponent(bandHeightLabel)
				.addComponent(bandHeightAdjustment, GroupLayout.DEFAULT_SIZE, 740, Short.MAX_VALUE)
				.addComponent(bandPosLabel)
				.addComponent(bandPosAdjustment, GroupLayout.DEFAULT_SIZE, 740, Short.MAX_VALUE)
				.addComponent(colorVarianceLabel)
				.addComponent(colorVarianceAdjustment, GroupLayout.DEFAULT_SIZE, 740, Short.MAX_VALUE)
				.addGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(Alignment.LEADING)
						.addGroup(layout.createSequentialGroup()
							.addComponent(calibrateButton, GroupLayout.DEFAULT_SIZE, 129, Short.MAX_VALUE)
							.addGap(10)
							.addComponent(manualButton, GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
							.addGap(10)
							.addComponent(autoButton, GroupLayout.DEFAULT_SIZE, 169, Short.MAX_VALUE))
						.addComponent(imagePanel, GroupLayout.DEFAULT_SIZE, 506, Short.MAX_VALUE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(configPanel, GroupLayout.PREFERRED_SIZE, 216, GroupLayout.PREFERRED_SIZE)
					.addGap(24))
		);
		layout.setVerticalGroup(
			layout.createParallelGroup(Alignment.TRAILING)
				.addGroup(layout.createSequentialGroup()
					.addContainerGap()
					.addGroup(layout.createParallelGroup(Alignment.TRAILING)
						.addComponent(imagePanel, GroupLayout.DEFAULT_SIZE, 370, Short.MAX_VALUE)
						.addComponent(configPanel, GroupLayout.PREFERRED_SIZE, 233, GroupLayout.PREFERRED_SIZE))
					.addGroup(layout.createParallelGroup(Alignment.BASELINE)
						.addGap(10)
						.addComponent(calibrateButton)
						.addComponent(manualButton)
						.addComponent(autoButton))
					.addGap(10)
					.addComponent(sensitivityLabel)
					.addGap(10)
					.addComponent(sensitivityAdjustment, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addGap(10)
					.addComponent(bandWidthLabel)
					.addGap(10)
					.addComponent(bandWidthAdjustment, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addGap(10)
					.addComponent(bandHeightLabel)
					.addGap(10)
					.addComponent(bandHeightAdjustment, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addGap(10)
					.addComponent(bandPosLabel)
					.addGap(10)
					.addComponent(bandPosAdjustment, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addGap(10)
					.addComponent(colorVarianceLabel)
					.addGap(10)
					.addComponent(colorVarianceAdjustment, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
		);
		
		
		
		/*--------------------------------------------------------------
		 * config Box
		 --------------------------------------------------------------*/
		
		configPanel.setLayout(null);
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(0, 25, 216, 105);
		configPanel.add(scrollPane);
		
		table = new JTable();
		final CameraSettingsTabelModel cstm = new CameraSettingsTabelModel();
		table.setModel(cstm);
		table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0), "selectNextColumnCell");
		scrollPane.setViewportView(table);

		
		JComboBox<VideoDevice> videoSource = new JComboBox<>();
		videoSource.setModel(new DefaultComboBoxModel<VideoDevice>(videoDeviceManager.retreiveDevices()));
		videoSource.setBounds(0, 0, 216, 24);
		videoSource.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ae) {
				if (ae.getActionCommand().equals("comboBoxChanged")) {
					try {
						@SuppressWarnings("unchecked")
						VideoDevice newDevice = (VideoDevice) ((JComboBox<VideoDevice>)ae.getSource()).getSelectedItem();
						settings.set(AppSettings.LAST_VIDEO_DEVICE, newDevice.getName());
						videoDeviceManager.setCurrentDevice(newDevice);
						videoDeviceManager.getDeviceSettings();
						cstm.loadDeviceProperties();
					} catch (Exception e) {
						log.error("[videoSourceComboBox:{}] {}", ae.getActionCommand(), e.getMessage(), e);
					}
					
					
				} else {
					log.trace("[videoSourceComboBox] uncaught action: {}", ae.getActionCommand());
				}
			}
			
		});
		configPanel.add(videoSource);
		settings.save();
				
		contentPaneScroller.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		addComponentListener(this);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	/**
	 * 
	 */
	private void initWindowSize() {
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