package net.swansonstuff.cameratest;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

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

public class CameraTrack extends JFrame implements MjpegParserListener, ActionListener {


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
	private int sensitivitySetting = 15;
	int allowedColorVariance = 100;			
	private boolean autoCalibration = true;
	public static final String	MANUAL_CALIBRATION	= "Manual Calibration";
	public static final String	AUTO_CALIBRATION	= "Auto Calibration";

	private WritableRaster lastRaster;
	private WritableRaster currentRaster;
	
	private static final HaarCascadeDetector detector = new HaarCascadeDetector();
	private List<DetectedFace> faces = null;
	private static final Stroke STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[] { 1.0f }, 0.0f);


	public CameraTrack(){
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
		
		// for ip camera
		/* 
		HttpURLConnection cameraUrl = (HttpURLConnection) new URL("http://192.168.102.14/axis-cgi/mjpg/video.cgi").openConnection();
		cameraUrl.setUseCaches(false);
		MjpegFrameParser parser = new MjpegFrameParser(cameraUrl.getInputStream());
		*/
		
		// for video streams via ffmpeg
		String streamName = args[0];;
		if ((args == null) || (args.length < 1) && (args[0].equals(""))) {
			System.err.println("Unable to start: Stream source required.");
			System.exit(1);
		}
		String fileName = "pipe:1";

		String resolution = "320x240";
		String sourceVideo4linux = "-f v4l2 -i /dev/video0";
		//String sourceVideoFile = "-i /home/dan/Desktop/video-capture.mp4";
		String sourceVideoFile = "-i /home/dan/Downloads/aj-office-prank.mov";
		String usbCamCmd = "ffmpeg "+sourceVideoFile+" -f mjpeg -s "+resolution+"  "+fileName;
		
		String streamCmd = "ffmpeg -probesize 32768 -i "+streamName+" -f mjpeg  "+fileName;
		
		IMediaReader reader = ToolFactory.makeReader("input.mpg");
		
		System.out.println("Running: "+usbCamCmd);
		Process transcoder = Runtime.getRuntime().exec(usbCamCmd);
		
		//ImageStreamReader isr = new ImageStreamReader(transcoder.getInputStream(), this);
		//isr.start();
		
		
		while (true) {
			try {
				MjpegFrame frame;
				//BufferedReader brStdOut = new BufferedReader(new InputStreamReader());
				MjpegInputStream brStdOut = new MjpegInputStream(transcoder.getInputStream());
				long startTime = System.currentTimeMillis();
				while ((frame = brStdOut.readMjpegFrame()) != null) {
						log.info("got frame in {}ms", System.currentTimeMillis() - startTime);
						 processFrame(frame);
						 startTime = System.currentTimeMillis();
				}

				int exitValue = transcoder.exitValue();
				System.out.println("Got exit code "+exitValue);
				String output;
				BufferedReader brStdErr = new BufferedReader(new InputStreamReader(transcoder.getErrorStream()));
				while ((output = brStdErr.readLine()) != null) {
						System.out.println(output);
				}
				break;
			} catch(IllegalThreadStateException e) {
				// Means we're still running
				System.out.println("waiting for process to end..");
				Thread.sleep(1000);
			}
		}
		
		//isr.shutdown();
		
		

		System.exit(0);
	}
	
	public void setSensitivitySetting(int sensitivitySetting) {
		this.sensitivitySetting = sensitivitySetting;
	}
	
	private void processFrame(MjpegFrame frame) {
			
		
		if (frame.getBytes().length > 0) {
			BufferedImage bi = Utilities.toBufferedImage(frame.getImage());
			if (bi != null) {
				System.out.print("o");
				processImage(bi);
			} else {
				System.out.print("x");
			}
		} else {
			System.out.print("x");
		}
	}
	
	private void processImage(BufferedImage bi) {
		
		setSize(bi.getWidth() + 25, bi.getHeight() + 400);

			long startTime = System.currentTimeMillis();
			int verticalCenter = bi.getHeight() / 2;
			int halfBand = (int) Math.ceil(1.0d * bandWidth / 100 * bi.getHeight() / 2);
			currentRaster = (WritableRaster) bi.getData(new Rectangle(0, verticalCenter - halfBand, bi.getWidth(), verticalCenter + halfBand));
			
			BufferedImage newbi =  new BufferedImage(currentRaster.getWidth(), currentRaster.getHeight() - currentRaster.getMinY(), bi.getType());
			Graphics2D graphics = newbi.createGraphics();
			
			drawFace(bi);
			
			graphics.drawImage(bi, 0, 0, currentRaster.getWidth(), currentRaster.getHeight(),
					newbi.getMinX(), newbi.getMinY(), newbi.getWidth(), newbi.getHeight(), null
					);
			 
			graphics.dispose();
			 
			graphics = bi.createGraphics();
			graphics.setPaint(Color.RED);
			graphics.draw(new Rectangle(0, verticalCenter - halfBand, bi.getWidth() - 1, halfBand * 2));
			
			int diff = 0;
			int PIXEL_GROUP_SIZE = sensitivitySetting;
			int DATA_BUCKETS = bi.getWidth() / PIXEL_GROUP_SIZE;
			
			if (lastRaster != null) {		

				if (lastRaster != null) {
					
					String[] deltaMap = new String[(int) Math.ceil(bi.getWidth() / PIXEL_GROUP_SIZE + 1)];
					int deltaIndex = 0;

					graphics.setColor(Color.YELLOW);
					int x = 0;
					int yTop = verticalCenter - halfBand+1;
					int yHeight = (halfBand * 2) - 1;
					int xEnd = PIXEL_GROUP_SIZE - 1;
					Rectangle2D changedArea = new Rectangle2D.Double();
					while (x < bi.getWidth()) {

						int offPixels = getMismatchedPixelCount(allowedColorVariance, x, PIXEL_GROUP_SIZE, currentRaster, lastRaster);

						if (1.0f * offPixels / halfBand > 1.0f) {
							if (x< 1) { x = 1; }
							changedArea.setRect(x , yTop, xEnd, yHeight);
							graphics.draw(changedArea);
							// mark as very different
							deltaMap[deltaIndex++] = "O";
							diff++;
						} else {
							// mark as close enough
							deltaMap[deltaIndex++] = ".";
						}
						
						x += PIXEL_GROUP_SIZE;
					}

					for (String c : deltaMap) {
						//System.out.print(c);
					}
					System.out.println(" Processing frame in "+(System.currentTimeMillis() - startTime)+"ms.  Difference("+diff+") is "+(100.0 * diff / DATA_BUCKETS )+"%");

				}

			}
			graphics.dispose();
			
			panel.setImage(bi);
			
			if (autoCalibration) {
				calibrate();
			}
	}

	private void drawFace(BufferedImage bi) {
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
		log.debug("[getMismatchedPixelCount] rasterA is {} x {}, rasterB is {} x {}", rasterA.getWidth() - rasterA.getMinX(), rasterA.getHeight() - rasterA.getMinY(), rasterB.getWidth() - rasterB.getMinX(), rasterB.getHeight() - rasterB.getMinY());
		byte[] currentRGB;
		byte[] previousRGB;
		int offPixels = 0;
			int rightEdge = startX + regionWidth;
			int leftEdge;
			for (leftEdge = startX; leftEdge < rightEdge; leftEdge++) {
				for (int y = rasterA.getMinY(); y < rasterA.getHeight(); y++){
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
						log.error("[getMismatchedPixelCount] caught exception {} comparing pixels {} {}", e.getMessage(), leftEdge, y, rightEdge, e);
						return offPixels;
					}
				}
			}
		return offPixels;
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
			this.autoCalibration = false;
		} else if (e.getActionCommand().equals(AUTO_CALIBRATION)) {
			this.autoCalibration = true;
		}
	}

	private void calibrate() {
		System.out.println("Calibrating");
		lastRaster = currentRaster;
	}
	
	private void setBandWidth(int i) {
		this.bandWidth = i;
	}
	
	private void setAllowedColorVariance(int i) {
		this.allowedColorVariance = i;
	}


	/**
	 * 
	 */
	private void init() {
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
		getContentPane().add(manualButton);
		
		JRadioButton autoButton = new JRadioButton(AUTO_CALIBRATION);
		autoButton.setActionCommand(AUTO_CALIBRATION);
		autoButton.setBounds(18, 8, 134, 23);
		autoButton.addActionListener(this);
		autoButton.setSelected(true);
		getContentPane().add(autoButton);
		
		ButtonGroup group = new ButtonGroup();
		group.add(manualButton);
		group.add(autoButton);
		
		JLabel sensitivityLabel = new JLabel("Sensitivity");
		getContentPane().add(sensitivityLabel);
		JSlider sensitivityAdjustment = new JSlider(JSlider.HORIZONTAL, 1, 50, sensitivitySetting);
		sensitivityAdjustment.addChangeListener(new ChangeListener() {
			
			public void stateChanged(ChangeEvent e) {
				JSlider sensitivity = (JSlider) e.getSource();
				int newSensitivity = sensitivity.getMaximum() - sensitivity.getValue();
				setSensitivitySetting((newSensitivity > 0) ? newSensitivity : 1);
			}
		});
		sensitivityAdjustment.setSize(300, 25);
		getContentPane().add(sensitivityAdjustment);

		JLabel bandwidthLabel = new JLabel("Band Height");
		getContentPane().add(bandwidthLabel);
		JSlider bandWidthAdjustment = new JSlider(JSlider.HORIZONTAL, 1, 100, bandWidth);
		bandWidthAdjustment.setMajorTickSpacing(10);
		bandWidthAdjustment.addChangeListener(new ChangeListener() {
			
			public void stateChanged(ChangeEvent e) {
				JSlider bandWidth = (JSlider) e.getSource();
				int newSensitivity = bandWidth.getValue();
				setBandWidth((newSensitivity > 0) ? newSensitivity : 1);
			}

		});
		bandWidthAdjustment.setSize(300, 25);
		getContentPane().add(bandWidthAdjustment);

		JLabel colorVarianceLabel = new JLabel("Color Variance");
		getContentPane().add(colorVarianceLabel);
		JSlider colorVarianceAdjustment = new JSlider(JSlider.HORIZONTAL, 1, 200, allowedColorVariance);
		colorVarianceAdjustment.setMajorTickSpacing(10);
		colorVarianceAdjustment.addChangeListener(new ChangeListener() {
			
			public void stateChanged(ChangeEvent e) {
				JSlider colorVarianceAdjustment = (JSlider) e.getSource();
				int newSensitivity = colorVarianceAdjustment.getValue();
				setAllowedColorVariance((newSensitivity > 0) ? newSensitivity : 1);
			}

		});
		bandWidthAdjustment.setSize(300, 25);
		getContentPane().add(bandWidthAdjustment);


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
									.addGap(28)
									.addComponent(calibrateButton)
									.addComponent(manualButton)
									.addComponent(autoButton)
							)
							.addGap(28)
							.addComponent(sensitivityLabel)
							.addGap(28)
							.addComponent(sensitivityAdjustment)
							.addGap(28)
							.addComponent(bandwidthLabel)
							.addGap(28)
							.addComponent(bandWidthAdjustment)
							.addGap(28)
							.addComponent(colorVarianceLabel)
							.addGap(28)
							.addComponent(colorVarianceAdjustment)
							));

		setVisible(true);
		setSize(435,608);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
}