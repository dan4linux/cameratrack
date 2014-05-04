import java.awt.image.BufferedImage;

import com.github.sarxos.webcam.Webcam;

public class usbcamtest {


	public static void main(String[] args) throws Exception
	{


		Webcam webcam = Webcam.getDefault();
		webcam.open();
		BufferedImage image = webcam.getImage();

	}

}